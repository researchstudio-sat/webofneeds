package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.Message;
import org.apache.jena.query.Dataset;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.WrongAddressingInformationException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.model.ConnectionContainer;
import won.protocol.model.DatasetHolder;
import won.protocol.model.OwnerApplication;
import won.protocol.model.Socket;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

@Component
public class AtomService {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    SocketRepository socketRepository;
    @Autowired
    OwnerApplicationRepository ownerApplicationRepository;
    @Autowired
    MessageEventRepository messageEventRepository;
    @Autowired
    MessageService messageService;

    public Optional<Atom> getAtom(URI atomURI) {
        return atomRepository.findOneByAtomURI(atomURI);
    }

    public Atom getAtomRequired(URI atomURI) {
        return getAtom(atomURI).orElseThrow(() -> new NoSuchAtomException(atomURI));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Atom createAtom(final WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CREATE_ATOM);
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

    @Transactional(propagation = Propagation.MANDATORY)
    public void authorizeOwnerApplicationForAtom(final Message message, final Atom atom) {
        String ownerApplicationID = message.getHeader(WonCamelConstants.OWNER_APPLICATION_ID).toString();
        authorizeOwnerApplicationForAtom(ownerApplicationID, atom);
    }

    public URI getAtomURIFromWonMessage(final Dataset wonMessage) {
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

    public void activate(WonMessage wonMessage) throws NoSuchAtomException {
        URI messageURI = wonMessage.getMessageURI();
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        wonMessage.getMessageType().requireType(WonMessageType.ACTIVATE);
        activate(recipientAtomURI, messageURI);
    }

    public void activate(URI atomURI, URI messageURI) throws NoSuchAtomException {
        logger.debug("ACTIVATING atom. atomURI:{}", atomURI);
        Objects.requireNonNull(atomURI);
        Objects.requireNonNull(messageURI);
        Atom atom = getAtomRequired(atomURI);
        messageService.saveMessageInContainer(messageURI, atomURI);
        logger.debug("Setting Atom State: " + atom.getState());
        atom.setState(AtomState.ACTIVE);
        atomRepository.save(atom);
    }

    public void deactivate(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.DEACTIVATE);
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        URI messageURI = wonMessage.getMessageURI();
        if (recipientAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientAtom.toString()));
        }
        if (senderAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.senderAtom.toString()));
        }
        deactivate(recipientAtomURI, messageURI);
    }

    public void deactivate(URI atomURI, URI messageURI) {
        logger.debug("DEACTIVATING atom. atomURI:{}", atomURI);
        Objects.requireNonNull(atomURI);
        Objects.requireNonNull(messageURI);
        Atom atom = getAtomRequired(atomURI);
        messageService.saveMessageInContainer(messageURI, atomURI);
        logger.debug("Setting Atom State: " + atom.getState());
        atom.setState(AtomState.INACTIVE);
        atomRepository.save(atom);
    }

    public void atomMessageFromSystem(WonMessage wonMessage) {
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        URI messageURI = wonMessage.getMessageURI();
        if (recipientAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientAtom.toString()));
        }
        if (senderAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.senderAtom.toString()));
        }
        if (!recipientAtomURI.equals(senderAtomURI)) {
            throw new WrongAddressingInformationException("SenderAtomUri and recipientAtomUri must be identical",
                            URI.create(WONMSG.senderAtom.getURI()), URI.create(WONMSG.recipientAtom.getURI()));
        }
        messageService.saveMessageInContainer(messageURI, senderAtomURI);
    }
}
