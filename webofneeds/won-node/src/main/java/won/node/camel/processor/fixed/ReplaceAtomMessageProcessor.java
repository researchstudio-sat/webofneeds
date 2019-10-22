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
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.DatasetHolder;
import won.protocol.model.Socket;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * Processor for a REPLACE message. Effects:
 * <ul>
 * <li>Changes the atom content</li>
 * <li>Replaces attachments</li>
 * <li>Replaces sockets. All connections of deleted or modified sockets are
 * closed, unless they already are closed</li>
 * <li>Does not change the atom state (ACTIVE/INACTIVE)</li>
 * <li>Triggers a FROM_SYSTEM message in each established connection (via the
 * respective Reaction processor)</li>
 * </ul>
 */
@Service
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ReplaceMessageString)
public class ReplaceAtomMessageProcessor extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Atom atom = replaceAtom(wonMessage);
    }

    public Atom replaceAtom(final WonMessage wonMessage) throws NoSuchAtomException {
        Dataset atomContent = wonMessage.getMessageContent();
        List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
        // remove attachment and its signature from the atomContent
        removeAttachmentsFromAtomContent(atomContent, attachmentHolders);
        URI atomURI = getAtomURIFromWonMessage(atomContent);
        if (atomURI == null)
            throw new IllegalArgumentException("Could not determine atom URI within message content");
        if (!atomURI.equals(wonMessage.getSenderAtomURI()))
            throw new IllegalArgumentException("senderAtomURI and AtomURI of the content are not equal");
        final Atom atom = atomService.getAtomRequired(atomURI);
        URI messageURI = wonMessage.getMessageURI();
        messageService.saveMessageInContainer(messageURI, atomURI);
        // store the atom content
        DatasetHolder datasetHolder = atom.getDatatsetHolder();
        // get the derived data, we don't change that here
        Dataset atomDataset = atom.getDatatsetHolder().getDataset();
        Optional<Model> derivationModel = Optional
                        .ofNullable(atomDataset.getNamedModel(atom.getAtomURI() + "#derivedData"));
        // replace attachments
        List<DatasetHolder> attachments = new ArrayList<>(attachmentHolders.size());
        for (WonMessage.AttachmentHolder attachmentHolder : attachmentHolders) {
            datasetHolder = new DatasetHolder(attachmentHolder.getDestinationUri(),
                            attachmentHolder.getAttachmentDataset());
            attachments.add(datasetHolder);
        }
        // rename the content graphs and signature graphs so they start with the atom
        // uri
        RdfUtils.renameResourceWithPrefix(atomContent, messageURI.toString(), atomURI.toString());
        // analyzed change in socket data
        List<Socket> existingSockets = socketRepository.findByAtomURI(atomURI);
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomContent);
        Set<Socket> newSocketEntities = determineNewSockets(atomURI, existingSockets, atomModelWrapper);
        Set<Socket> removedSockets = determineRemovedSockets(atomURI, existingSockets, atomModelWrapper);
        Set<Socket> changedSockets = determineAndModifyChangedSockets(atomURI, existingSockets, atomModelWrapper);
        // close connections for changed or removed sockets
        Stream.concat(removedSockets.stream(), changedSockets.stream()).forEach(socket -> {
            List<Connection> connsToClose = connectionRepository.findByAtomURIAndSocketURIAndNotState(atomURI,
                            socket.getSocketURI(), ConnectionState.CLOSED);
            connsToClose.forEach(con -> {
                if (con.getState() != ConnectionState.CLOSED) {
                    // TODO: closing the connections should be done in a reaction
                    // the fact that it's done here complicates things and we'll refrain from
                    // moving the replace functionality into the atomService. Once we refactor
                    // the closing behaviour, we should move the code there.
                    closeConnection(atom, con,
                                    "Closed because the socket of this connection was changed or removed by the atom's owner.");
                }
            });
        });
        // add everything to the atom model class and save it
        socketRepository.save(newSocketEntities);
        socketRepository.save(changedSockets);
        socketRepository.delete(removedSockets);
        if (derivationModel.isPresent()) {
            atomContent.addNamedModel(atom.getAtomURI().toString() + "#derivedData", derivationModel.get());
        }
        datasetHolder.setDataset(atomContent);
        atom.setDatatsetHolder(datasetHolder);
        atom.setAttachmentDatasetHolders(attachments);
        return atomRepository.save(atom);
    }

    private Set<Socket> determineNewSockets(URI atomURI, List<Socket> existingSockets,
                    AtomModelWrapper atomModelWrapper) {
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        Optional<String> defaultSocket = atomModelWrapper.getDefaultSocket();
        if (sockets.size() == 0)
            throw new IllegalArgumentException("at least one property won:socket required ");
        // create new socket entities for the sockets not yet existing:
        Set<Socket> newSocketEntities = sockets.stream()
                        .filter(socketUri -> !existingSockets.stream()
                                        .anyMatch(socket -> socket.getSocketURI().toString().equals(socketUri)))
                        .map(socketUri -> {
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
        return newSocketEntities;
    }

    private Set<Socket> determineRemovedSockets(URI atomURI, List<Socket> existingSockets,
                    AtomModelWrapper atomModelWrapper) {
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        return existingSockets.stream().filter(socket -> !sockets.contains(socket.getSocketURI().toString()))
                        .collect(Collectors.toSet());
    }

    private Set<Socket> determineAndModifyChangedSockets(URI atomURI, List<Socket> existingSockets,
                    AtomModelWrapper atomModelWrapper) {
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        Optional<URI> defaultSocket = atomModelWrapper.getDefaultSocket().map(f -> URI.create(f));
        return existingSockets.stream().filter(socket -> {
            if (!sockets.contains(socket.getSocketURI().toString())) {
                // socket is removed, not changed
                return false;
            }
            boolean changed = false;
            boolean isNowDefaultSocket = defaultSocket.isPresent() && defaultSocket.get().equals(socket.getSocketURI());
            if (isNowDefaultSocket != socket.isDefaultSocket()) {
                // socket's default socket property has changed
                changed = true;
                socket.setDefaultSocket(isNowDefaultSocket);
            }
            Optional<URI> newSocketType = atomModelWrapper.getSocketType(socket.getSocketURI().toString())
                            .map(f -> URI.create(f));
            boolean typeChanged = newSocketType.isPresent() && !newSocketType.get().equals(socket.getTypeURI());
            if (typeChanged) {
                // socket's type has changed
                socket.setTypeURI(newSocketType.get());
                changed = true;
            }
            return changed;
        }).collect(Collectors.toSet());
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

    private URI getAtomURIFromWonMessage(final Dataset wonMessage) {
        URI atomURI;
        atomURI = WonRdfUtils.AtomUtils.getAtomURI(wonMessage);
        if (atomURI == null) {
            throw new IllegalArgumentException("at least one RDF node must be of type won:Atom");
        }
        return atomURI;
    }

    public void closeConnection(final Atom atom, final Connection con, String textMessage) {
        // send close from system to each connection
        // the close message is directed at our local connection. It will
        // be routed to the owner and forwarded to to remote connection
        URI messageURI = wonNodeInformationService.generateEventURI();
        WonMessage message = WonMessageBuilder
                        .setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_SYSTEM,
                                        con.getConnectionURI(), con.getAtomURI(), atom.getWonNodeURI(),
                                        con.getConnectionURI(), con.getAtomURI(), atom.getWonNodeURI(), textMessage)
                        .build();
        sendSystemMessage(message);
    }
}
