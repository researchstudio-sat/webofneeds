package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.service.nodeconfig.URIService;
import won.protocol.exception.IllegalAtomContentException;
import won.protocol.exception.IllegalAtomURIException;
import won.protocol.exception.IllegalSocketModificationException;
import won.protocol.exception.MissingMessagePropertyException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.UriAlreadyInUseException;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.exception.WrongAddressingInformationException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.model.ConnectionContainer;
import won.protocol.model.ConnectionState;
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
    @Autowired
    DataDerivationService dataDerivationService;
    @Autowired
    URIService uriService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    EntityManager entityManager;

    public Optional<Atom> getAtomForUpdate(URI atomURI) {
        Optional<Atom> atom = atomRepository.findOneByAtomURIForUpdate(atomURI);
        entityManager.refresh(atom.get());
        return atom;
    }

    public Optional<Atom> getAtom(URI atomURI) {
        return atomRepository.findOneByAtomURI(atomURI);
    }

    public Optional<Atom> lockAtom(URI atomURI) {
        Optional<Atom> atom = atomRepository.findOneByAtomURIForUpdate(atomURI);
        entityManager.refresh(atom.get());
        return atom;
    }

    public Atom lockAtomRequired(URI atomURI) {
        return atomRepository.findOneByAtomURIForUpdate(atomURI).orElseThrow(() -> new NoSuchAtomException(atomURI));
    }

    public Atom getAtomRequired(URI atomURI) {
        return getAtom(atomURI).orElseThrow(() -> new NoSuchAtomException(atomURI));
    }

    public Optional<Atom> getAtomForMessage(WonMessage msg, WonMessageDirection direction) {
        URI socketURI = null;
        URI atomURI = null;
        if (msg.getMessageTypeRequired().isConnectionSpecificMessage()) {
            if (direction.isFromExternal()) {
                socketURI = msg.getRecipientSocketURI();
            } else {
                socketURI = msg.getSenderSocketURI();
            }
            if (socketURI == null) {
                return Optional.empty();
            }
            atomURI = WonMessageUtils.stripFragment(socketURI);
        } else {
            atomURI = msg.getRecipientAtomURI();
        }
        if (atomURI == null) {
            atomURI = msg.getSenderAtomURI();
        }
        if (atomURI != null) {
            return getAtom(atomURI);
        }
        return Optional.empty();
    }

    public Atom getAtomForMessageRequired(WonMessage msg, WonMessageDirection direction) {
        URI socketURI = null;
        URI atomURI = null;
        if (msg.getMessageTypeRequired().isConnectionSpecificMessage()) {
            if (direction.isFromExternal()) {
                socketURI = msg.getRecipientSocketURIRequired();
            } else {
                socketURI = msg.getSenderSocketURIRequired();
            }
            atomURI = WonMessageUtils.stripFragment(socketURI);
        } else {
            atomURI = msg.getRecipientAtomURI();
        }
        if (atomURI == null) {
            atomURI = msg.getSenderAtomURI();
        }
        if (atomURI != null) {
            return getAtomRequired(atomURI);
        } else {
            throw new WonMessageProcessingException("Could not obtain atom URI from messsage " + msg.getMessageURI());
        }
    }

    public Atom createAtom(final WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CREATE_ATOM);
        Dataset atomContent = wonMessage.getMessageContent();
        List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
        // remove attachment and its signature from the atomContent
        removeAttachmentsFromAtomContent(atomContent, attachmentHolders);
        URI messageURI = wonMessage.getMessageURI();
        final AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomContent);
        URI atomURI = getAtomURIAndCheck(atomModelWrapper);
        // rename the content graphs and signature graphs so they start with the atom
        // uri
        Collection<String> sockets = getSocketsAndCheck(atomModelWrapper, atomURI);
        Set<Socket> socketEntities = sockets.stream().map(socketUri -> {
            Optional<String> socketType = getSocketTypeAndCheck(atomModelWrapper, socketUri);
            Socket f = new Socket();
            f.setAtomURI(atomURI);
            f.setSocketURI(URI.create(socketUri));
            f.setTypeURI(URI.create(socketType.get()));
            return f;
        }).collect(Collectors.toSet());
        checkResourcesInAtomContent(atomModelWrapper);
        // add everything to the atom model class and save it
        checkCanThisMessageCreateOrModifyThisAtom(wonMessage, atomURI);
        Atom atom = new Atom();
        atom.setState(AtomState.ACTIVE);
        atom.setAtomURI(atomURI);
        // make a new wrapper because we just changed the underlying dataset
        atom.setWonNodeURI(URI.create(uriService.getResourceURIPrefix()));
        ConnectionContainer connectionContainer = new ConnectionContainer(atom);
        atom.setConnectionContainer(connectionContainer);
        AtomMessageContainer atomMessageContainer = atomMessageContainerRepository.findOneByParentUri(atomURI);
        if (atomMessageContainer == null) {
            atomMessageContainer = new AtomMessageContainer(atom, atom.getAtomURI());
        } else {
            throw new UriAlreadyInUseException("Found an AtomMessageContainer for the atom we're about to create ("
                            + atomURI + ") - aborting");
        }
        atom.setMessageContainer(atomMessageContainer);
        // store the atom content
        atomModelWrapper.renameResourceWithPrefix(messageURI.toString(), atomURI.toString());
        DatasetHolder datasetHolder = new DatasetHolder(atomURI, atomModelWrapper.getDataset());
        // store attachments
        List<DatasetHolder> attachments = new ArrayList<>(attachmentHolders.size());
        for (WonMessage.AttachmentHolder attachmentHolder : attachmentHolders) {
            datasetHolder = new DatasetHolder(attachmentHolder.getDestinationUri(),
                            attachmentHolder.getAttachmentDataset());
            attachments.add(datasetHolder);
        }
        atom.setDatatsetHolder(datasetHolder);
        atom.setAttachmentDatasetHolders(attachments);
        atom = atomRepository.save(atom);
        connectionContainerRepository.save(connectionContainer);
        socketEntities.forEach(socket -> socketRepository.save(socket));
        dataDerivationService.deriveDataIfNecessary(atom);
        return atom;
    }

    public Atom replaceAtom(final WonMessage wonMessage) throws NoSuchAtomException {
        Dataset atomContent = wonMessage.getMessageContent();
        List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
        // remove attachment and its signature from the atomContent
        removeAttachmentsFromAtomContent(atomContent, attachmentHolders);
        final AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomContent);
        URI atomURI = getAtomURIAndCheck(atomModelWrapper);
        checkCanThisMessageCreateOrModifyThisAtom(wonMessage, atomURI);
        checkResourcesInAtomContent(atomModelWrapper);
        Collection<String> sockets = getSocketsAndCheck(atomModelWrapper, atomURI);
        final Atom atom = getAtomRequired(atomURI);
        URI messageURI = wonMessage.getMessageURI();
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
        // analyzed change in socket data
        atomModelWrapper.renameResourceWithPrefix(messageURI.toString(), atomURI.toString());
        List<Socket> existingSockets = socketRepository.findByAtomURI(atomURI);
        Set<Socket> newSocketEntities = determineNewSockets(atomURI, existingSockets, atomModelWrapper);
        Set<Socket> removedSockets = determineRemovedSockets(atomURI, existingSockets, atomModelWrapper);
        Set<Socket> changedSockets = determineAndModifyChangedSockets(atomURI, existingSockets, atomModelWrapper);
        // close connections for changed or removed sockets
        removedSockets
                        .stream()
                        .filter(socket -> connectionService.existOpenConnections(atomURI, socket.getSocketURI()))
                        .findFirst()
                        .ifPresent(socket -> new IllegalSocketModificationException("Cannot remove socket "
                                        + socket.getSocketURI() + ": socket has connections in state "
                                        + ConnectionState.CONNECTED));
        changedSockets
                        .stream()
                        .filter(socket -> connectionService.existOpenConnections(atomURI, socket.getSocketURI()))
                        .findFirst()
                        .ifPresent(socket -> new IllegalSocketModificationException("Cannot change socket "
                                        + socket.getSocketURI() + ": socket has connections in state "
                                        + ConnectionState.CONNECTED));
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
        dataDerivationService.deriveDataIfNecessary(atom);
        return atomRepository.save(atom);
    }

    private Set<Socket> determineNewSockets(URI atomURI, List<Socket> existingSockets,
                    AtomModelWrapper atomModelWrapper) {
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        if (sockets.size() == 0)
            throw new IllegalAtomContentException("at least one property won:socket required ");
        // create new socket entities for the sockets not yet existing:
        Set<Socket> newSocketEntities = sockets.stream()
                        .filter(socketUri -> !existingSockets.stream()
                                        .anyMatch(socket -> socket.getSocketURI().toString().equals(socketUri)))
                        .map(socketUri -> {
                            Optional<String> socketType = atomModelWrapper.getSocketType(socketUri);
                            if (!socketType.isPresent()) {
                                throw new IllegalAtomContentException("cannot determine type of socket " + socketUri);
                            }
                            Socket f = new Socket();
                            f.setAtomURI(atomURI);
                            f.setSocketURI(URI.create(socketUri));
                            f.setTypeURI(URI.create(socketType.get()));
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
        return existingSockets.stream().filter(socket -> {
            if (!sockets.contains(socket.getSocketURI().toString())) {
                // socket is removed, not changed
                return false;
            }
            boolean changed = false;
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

    private void checkResourcesInAtomContent(AtomModelWrapper atomModelWrapper) {
        String atomURI = atomModelWrapper.getAtomUri();
        final String illegalPrefix = atomURI + "/";
        if (RdfUtils
                        .toURIStream(atomModelWrapper.getDataset(), true)
                        .anyMatch(uri -> uri.startsWith(illegalPrefix))) {
            throw new IllegalAtomContentException(
                            "URIs in atom content cannot be a sub-paths of the atom URI (i.e., they cannot start with '"
                                            + illegalPrefix
                                            + "'). If you need URIs for resources in your content, use fragments "
                                            + "of the atom URI (i.e., URIs that start with '" + atomURI + "#')");
        }
    }

    private void checkCanThisMessageCreateOrModifyThisAtom(final WonMessage wonMessage, URI atomURI) {
        if (!atomURI.equals(wonMessage.getAtomURI()))
            throw new WrongAddressingInformationException(
                            "atomURI of the message (" + wonMessage.getAtomURI() + ") and AtomURI of the content ("
                                            + atomURI + ") are not equal",
                            wonMessage.getMessageURI(), WONMSG.atom);
        if (!uriService.isAtomURI(atomURI)) {
            throw new IllegalAtomURIException("Atom URI " + atomURI + "does not match this node's prefix "
                            + uriService.getAtomResourceURIPrefix());
        }
    }

    private Optional<String> getSocketTypeAndCheck(final AtomModelWrapper atomModelWrapper, String socketUri) {
        Optional<String> socketType = atomModelWrapper.getSocketType(socketUri);
        if (!socketType.isPresent()) {
            throw new IllegalAtomContentException("Missing SocketDefinition for socket " + socketUri
                            + ". Add a '[socket] won:socketDefinition [SocketDefinition]' triple!");
        }
        return socketType;
    }

    private Collection<String> getSocketsAndCheck(final AtomModelWrapper atomModelWrapper, URI atomURI) {
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        sockets.parallelStream().forEach(socketUri -> {
            if (!socketUri.toString().startsWith(atomURI.toString() + "#")) {
                throw new IllegalAtomContentException(
                                "Socket URIs must be fragments of atom URIs (i.e. [socketURI] = [atomURI] + '#' + [id]). "
                                                + "This rule is violated for atom '" + atomURI + "' and socket '"
                                                + socketUri + "'");
            }
            getSocketTypeAndCheck(atomModelWrapper, socketUri);
        });
        if (sockets.size() == 0)
            throw new IllegalAtomContentException("at least one property won:socket required ");
        return sockets;
    }

    private URI getAtomURIAndCheck(final AtomModelWrapper atomModelWrapper) {
        URI atomURI;
        String atomURIString = atomModelWrapper.getAtomUri();
        if (atomURIString == null) {
            throw new IllegalAtomContentException("No '[subj] rdf:type won:Atom' triple found in atom content");
        }
        try {
            atomURI = new URI(atomURIString);
        } catch (URISyntaxException e) {
            throw new IllegalAtomURIException("Not a valid atom URI: " + atomModelWrapper.getAtomUri());
        }
        if (atomURI.getRawFragment() != null) {
            throw new IllegalAtomURIException(
                            "Atom URI must not be a fragment URI (i.e., no trailing '#' + [fragment-id] allowed). This is not allowed: "
                                            + atomURI);
        }
        return atomURI;
    }

    public Atom authorizeOwnerApplicationForAtom(final String ownerApplicationID, Atom atom) {
        String stopwatchName = getClass().getName() + ".authorizeOwnerApplicationForAtom";
        Stopwatch stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase1");
        Split split = stopwatch.start();
        Optional<OwnerApplication> ownerApplications = ownerApplicationRepository
                        .findOneByOwnerApplicationId(ownerApplicationID);
        split.stop();
        stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase2");
        split = stopwatch.start();
        if (ownerApplications.isPresent()) {
            logger.debug("owner application is already known");
            OwnerApplication ownerApplication = ownerApplications.get();
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
            ownerApplication = ownerApplicationRepository.save(ownerApplication);
            ownerApplicationList.add(ownerApplication);
            atom.setAuthorizedApplications(ownerApplicationList);
            logger.debug("setting OwnerApp ID: " + ownerApplicationList.get(0));
        }
        split.stop();
        stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase3");
        split = stopwatch.start();
        atom = atomRepository.save(atom);
        split.stop();
        return atom;
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
        logger.debug("atom State: " + atom.getState());
        atom.setState(AtomState.ACTIVE);
        dataDerivationService.deriveDataIfNecessary(atom);
        atomRepository.save(atom);
        logger.debug("atom State: " + atom.getState());
    }

    public void deactivate(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.DEACTIVATE);
        URI atomURI = wonMessage.getAtomURI();
        URI messageURI = wonMessage.getMessageURI();
        if (atomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.atom.toString()));
        }
        deactivate(atomURI, messageURI);
    }

    public void deactivate(URI atomURI, URI messageURI) {
        logger.debug("DEACTIVATING atom. atomURI:{}", atomURI);
        Objects.requireNonNull(atomURI);
        Objects.requireNonNull(messageURI);
        Optional<Atom> atom = getAtomForUpdate(atomURI);
        logger.debug("atom State: " + atom.get().getState());
        atom.get().setState(AtomState.INACTIVE);
        dataDerivationService.deriveDataIfNecessary(atom.get());
        atomRepository.save(atom.get());
        logger.debug("atom State: " + atom.get().getState());
    }

    public void atomMessageFromSystem(WonMessage wonMessage) {
        URI atomURI = wonMessage.getAtomURI();
        if (atomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.atom.getURI()));
        }
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
}
