package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.query.Dataset;
import org.springframework.stereotype.Service;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.DatasetHolder;
import won.protocol.model.Facet;
import won.protocol.model.Need;
import won.protocol.model.NeedEventContainer;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * Processor for a REPLACE message. Effects:
 * <ul>
 * <li>Changes the need content</li>
 * <li>Replaces attachments</li>
 * <li>Replaces facets. All connections of deleted or modified facets are
 * closed, unless they already are closed</li>
 * <li>Does not change the need state (ACTIVE/INACTIVE)</li>
 * <li>Triggers a FROM_SYSTEM message in each established connection (via the
 * respective Reaction processor)</li>
 * </ul>
 */
@Service
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_REPLACE_STRING)
public class ReplaceNeedMessageProcessor extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Need need = storeNeed(wonMessage);
    }

    private Need storeNeed(final WonMessage wonMessage) throws NoSuchNeedException {
        Dataset needContent = wonMessage.getMessageContent();
        List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
        // remove attachment and its signature from the needContent
        removeAttachmentsFromNeedContent(needContent, attachmentHolders);
        URI needURI = getNeedURIFromWonMessage(needContent);
        if (needURI == null)
            throw new IllegalArgumentException("Could not determine need URI within message content");
        if (!needURI.equals(wonMessage.getSenderNeedURI()))
            throw new IllegalArgumentException("senderNeedURI and NeedURI of the content are not equal");
        final Need need = DataAccessUtils.loadNeed(needRepository, needURI);
        NeedEventContainer needEventContainer = need.getEventContainer();
        if (needEventContainer == null) {
            throw new IllegalStateException("Trying to replace need that does not have an event container");
        }
        needEventContainer.getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        // store the need content
        DatasetHolder datasetHolder = need.getDatatsetHolder();
        // replace attachments
        List<DatasetHolder> attachments = new ArrayList<>(attachmentHolders.size());
        for (WonMessage.AttachmentHolder attachmentHolder : attachmentHolders) {
            datasetHolder = new DatasetHolder(attachmentHolder.getDestinationUri(),
                            attachmentHolder.getAttachmentDataset());
            attachments.add(datasetHolder);
        }
        // analyzed change in facet data
        List<Facet> existingFacets = facetRepository.findByNeedURI(needURI);
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needContent);
        Set<Facet> newFacetEntities = determineNewFacets(needURI, existingFacets, needModelWrapper);
        Set<Facet> removedFacets = determineRemovedFacets(needURI, existingFacets, needModelWrapper);
        Set<Facet> changedFacets = determineAndModifyChangedFacets(needURI, existingFacets, needModelWrapper);
        // close connections for changed or removed facets
        Stream.concat(removedFacets.stream(), changedFacets.stream()).forEach(facet -> {
            List<Connection> connsToClose = connectionRepository.findByNeedURIAndFacetURIAndNotState(needURI,
                            facet.getFacetURI(), ConnectionState.CLOSED);
            connsToClose.forEach(con -> {
                if (con.getState() != ConnectionState.CLOSED) {
                    closeConnection(need, con,
                                    "Closed because the facet of this connection was changed or removed by the need's owner.");
                }
            });
        });
        // add everything to the need model class and save it
        facetRepository.save(newFacetEntities);
        facetRepository.save(changedFacets);
        facetRepository.delete(removedFacets);
        datasetHolder.setDataset(needContent);
        need.setDatatsetHolder(datasetHolder);
        need.setAttachmentDatasetHolders(attachments);
        return needRepository.save(need);
    }

    private Set<Facet> determineNewFacets(URI needURI, List<Facet> existingFacets, NeedModelWrapper needModelWrapper) {
        Collection<String> facets = needModelWrapper.getFacetUris();
        Optional<String> defaultFacet = needModelWrapper.getDefaultFacet();
        if (facets.size() == 0)
            throw new IllegalArgumentException("at least one property won:hasFacet required ");
        // create new facet entities for the facets not yet existing:
        Set<Facet> newFacetEntities = facets.stream()
                        .filter(facetUri -> !existingFacets.stream()
                                        .anyMatch(facet -> facet.getFacetURI().toString().equals(facetUri)))
                        .map(facetUri -> {
                            Optional<String> facetType = needModelWrapper.getFacetType(facetUri);
                            if (!facetType.isPresent()) {
                                throw new IllegalArgumentException("cannot determine type of facet " + facetUri);
                            }
                            Facet f = new Facet();
                            f.setNeedURI(needURI);
                            f.setFacetURI(URI.create(facetUri));
                            f.setTypeURI(URI.create(facetType.get()));
                            if (defaultFacet.isPresent() && facetUri.equals(defaultFacet.get())) {
                                f.setDefaultFacet(true);
                            }
                            return f;
                        }).collect(Collectors.toSet());
        return newFacetEntities;
    }

    private Set<Facet> determineRemovedFacets(URI needURI, List<Facet> existingFacets,
                    NeedModelWrapper needModelWrapper) {
        Collection<String> facets = needModelWrapper.getFacetUris();
        return existingFacets.stream().filter(facet -> !facets.contains(facet.getFacetURI().toString()))
                        .collect(Collectors.toSet());
    }

    private Set<Facet> determineAndModifyChangedFacets(URI needURI, List<Facet> existingFacets,
                    NeedModelWrapper needModelWrapper) {
        Collection<String> facets = needModelWrapper.getFacetUris();
        Optional<URI> defaultFacet = needModelWrapper.getDefaultFacet().map(f -> URI.create(f));
        return existingFacets.stream().filter(facet -> {
            if (!facets.contains(facet.getFacetURI().toString())) {
                // facet is removed, not changed
                return false;
            }
            boolean changed = false;
            boolean isNowDefaultFacet = defaultFacet.isPresent() && defaultFacet.get().equals(facet.getFacetURI());
            if (isNowDefaultFacet != facet.isDefaultFacet()) {
                // facet's default facet property has changed
                changed = true;
                facet.setDefaultFacet(isNowDefaultFacet);
            }
            Optional<URI> newFacetType = needModelWrapper.getFacetType(facet.getFacetURI().toString())
                            .map(f -> URI.create(f));
            boolean typeChanged = newFacetType.isPresent() && !facet.getFacetType().getURI().equals(newFacetType.get());
            if (typeChanged) {
                // facet's type has changed
                facet.setTypeURI(newFacetType.get());
                changed = true;
            }
            return changed;
        }).collect(Collectors.toSet());
    }

    private void removeAttachmentsFromNeedContent(Dataset needContent,
                    List<WonMessage.AttachmentHolder> attachmentHolders) {
        for (WonMessage.AttachmentHolder attachmentHolder : attachmentHolders) {
            for (Iterator<String> it = attachmentHolder.getAttachmentDataset().listNames(); it.hasNext();) {
                String modelName = it.next();
                needContent.removeNamedModel(modelName);
            }
        }
    }

    private URI getNeedURIFromWonMessage(final Dataset wonMessage) {
        URI needURI;
        needURI = WonRdfUtils.NeedUtils.getNeedURI(wonMessage);
        if (needURI == null) {
            throw new IllegalArgumentException("at least one RDF node must be of type won:Need");
        }
        return needURI;
    }

    public void closeConnection(final Need need, final Connection con, String textMessage) {
        // send close from system to each connection
        // the close message is directed at our local connection. It will
        // be routed to the owner and forwarded to to remote connection
        URI messageURI = wonNodeInformationService.generateEventURI();
        WonMessage message = WonMessageBuilder
                        .setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_SYSTEM,
                                        con.getConnectionURI(), con.getNeedURI(), need.getWonNodeURI(),
                                        con.getConnectionURI(), con.getNeedURI(), need.getWonNodeURI(), textMessage)
                        .build();
        sendSystemMessage(message);
    }
}
