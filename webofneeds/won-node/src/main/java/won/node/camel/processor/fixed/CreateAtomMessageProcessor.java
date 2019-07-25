package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.query.Dataset;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.model.*;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: syim Date: 02.03.2015
 */
@Service
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.CreateMessageString)
public class CreateAtomMessageProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Atom atom = storeAtom(wonMessage);
        authorizeOwnerApplicationForAtom(message, atom);
    }

    private Atom storeAtom(final WonMessage wonMessage) {
        Dataset atomContent = wonMessage.getMessageContent();
        List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
        // remove attachment and its signature from the atomContent
        removeAttachmentsFromAtomContent(atomContent, attachmentHolders);
        URI messageURI = wonMessage.getMessageURI();
        URI atomURI = getAtomURIFromWonMessage(atomContent);
        if (!atomURI.equals(wonMessage.getSenderAtomURI()))
            throw new IllegalArgumentException("recipientAtomURI and AtomURI of the content are not equal");
        Atom atom = new Atom();
        atom.setState(AtomState.ACTIVE);
        atom.setAtomURI(atomURI);
        // ToDo (FS) check if the WON node URI corresponds with the WON node (maybe
        // earlier in the message layer)
        AtomMessageContainer atomMessageContainer = atomMessageContainerRepository.findOneByParentUri(atomURI);
        if (atomMessageContainer == null) {
            atomMessageContainer = new AtomMessageContainer(atom, atom.getAtomURI());
        } else {
            throw new UriAlreadyInUseException("Found an AtomMessageContainer for the atom we're about to create ("
                            + atomURI + ") - aborting");
        }
        // rename the content graphs and signature graphs so they start with the atom
        // uri
        RdfUtils.renameResourceWithPrefix(atomContent, messageURI.toString(), atomURI.toString());
        atom.setWonNodeURI(wonMessage.getRecipientNodeURI());
        ConnectionContainer connectionContainer = new ConnectionContainer(atom);
        atom.setConnectionContainer(connectionContainer);
        atom.setMessageContainer(atomMessageContainer);
        // store the atom content
        DatasetHolder datasetHolder = new DatasetHolder(atomURI, atomContent);
        // store attachments
        List<DatasetHolder> attachments = new ArrayList<>(attachmentHolders.size());
        for (WonMessage.AttachmentHolder attachmentHolder : attachmentHolders) {
            datasetHolder = new DatasetHolder(attachmentHolder.getDestinationUri(),
                            attachmentHolder.getAttachmentDataset());
            attachments.add(datasetHolder);
        }
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomContent);
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        Optional<String> defaultSocket = atomModelWrapper.getDefaultSocket();
        if (sockets.size() == 0)
            throw new IllegalArgumentException("at least one property won:socket required ");
        Set<Socket> socketEntities = sockets.stream().map(socketUri -> {
            Optional<String> socketType = atomModelWrapper.getSocketType(socketUri);
            if (!socketType.isPresent()) {
                throw new IllegalArgumentException("cannot determine type of socket " + socketUri);
            }
            Socket f = new Socket();
            f.setAtomURI(atomURI);
            f.setSocketURI(URI.create(socketUri));
            f.setTypeURI(URI.create(socketType.get()));
            if (defaultSocket.isPresent() && socketUri.equals(defaultSocket.get())) {
                f.setDefaultSocket(true);
            }
            return f;
        }).collect(Collectors.toSet());
        // add everything to the atom model class and save it
        atom.setDatatsetHolder(datasetHolder);
        atom.setAttachmentDatasetHolders(attachments);
        atom = atomRepository.save(atom);
        connectionContainerRepository.save(connectionContainer);
        socketEntities.forEach(socket -> socketRepository.saveAndFlush(socket));
        return atom;
    }

    private void removeAttachmentsFromAtomContent(Dataset atomContent,
                    List<WonMessage.AttachmentHolder> attachmentHolders) {
        for (WonMessage.AttachmentHolder attachmentHolder : attachmentHolders) {
            for (Iterator<String> it = attachmentHolder.getAttachmentDataset().listNames(); it.hasNext();) {
                String modelName = it.next();
                atomContent.removeNamedModel(modelName);
            }
        }
    }

    private void authorizeOwnerApplicationForAtom(final Message message, final Atom atom) {
        String ownerApplicationID = message.getHeader(WonCamelConstants.OWNER_APPLICATION_ID).toString();
        authorizeOwnerApplicationForAtom(ownerApplicationID, atom);
    }

    private URI getAtomURIFromWonMessage(final Dataset wonMessage) {
        URI atomURI;
        atomURI = WonRdfUtils.AtomUtils.getAtomURI(wonMessage);
        if (atomURI == null) {
            throw new IllegalArgumentException("at least one RDF node must be of type won:Atom");
        }
        return atomURI;
    }

    private void authorizeOwnerApplicationForAtom(final String ownerApplicationID, Atom atom) {
        String stopwatchName = getClass().getName() + ".authorizeOwnerApplicationForAtom";
        Stopwatch stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase1");
        Split split = stopwatch.start();
        List<OwnerApplication> ownerApplications = ownerApplicationRepository
                        .findByOwnerApplicationId(ownerApplicationID);
        split.stop();
        stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase2");
        split = stopwatch.start();
        if (ownerApplications.size() > 0) {
            logger.debug("owner application is already known");
            OwnerApplication ownerApplication = ownerApplications.get(0);
            List<OwnerApplication> authorizedApplications = atom.getAuthorizedApplications();
            if (authorizedApplications == null) {
                authorizedApplications = new ArrayList<OwnerApplication>(1);
            }
            authorizedApplications.add(ownerApplication);
            atom.setAuthorizedApplications(authorizedApplications);
        } else {
            logger.debug("owner application is new - creating");
            List<OwnerApplication> ownerApplicationList = new ArrayList<>(1);
            OwnerApplication ownerApplication = new OwnerApplication();
            ownerApplication.setOwnerApplicationId(ownerApplicationID);
            ownerApplicationList.add(ownerApplication);
            atom.setAuthorizedApplications(ownerApplicationList);
            logger.debug("setting OwnerApp ID: " + ownerApplicationList.get(0));
        }
        split.stop();
        stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase3");
        split = stopwatch.start();
        atom = atomRepository.save(atom);
        split.stop();
    }
}
