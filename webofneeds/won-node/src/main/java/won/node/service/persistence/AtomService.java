package won.node.service.persistence;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.auth.AuthUtils;
import won.auth.WonAclEvaluator;
import won.auth.model.AclEvalResult;
import won.auth.model.DecisionValue;
import won.auth.model.GraphType;
import won.auth.model.OperationRequest;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.node.service.nodeconfig.URIService;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.*;
import won.protocol.repository.*;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper;
import won.protocol.vocabulary.WONMSG;

import javax.persistence.EntityManager;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static won.auth.model.Individuals.POSITION_ATOM_GRAPH;

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
    LockRepository lockRepository;
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
        acquireOwnerApplicationLock();
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

    public Atom replaceAtom(final WonMessage wonMessage, WonAclEvaluator evaluator, OperationRequest request)
                    throws NoSuchAtomException {
        Dataset newAtomContent = wonMessage.getMessageContent();
        List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
        // remove attachment and its signature from the atomContent
        removeAttachmentsFromAtomContent(newAtomContent, attachmentHolders);
        final AtomModelWrapper atomModelWrapper = new AtomModelWrapper(newAtomContent);
        URI atomURI = getAtomURIAndCheck(atomModelWrapper);
        checkReplaceAllowed(newAtomContent, atomURI, evaluator, request);
        checkCanThisMessageCreateOrModifyThisAtom(wonMessage, atomURI);
        checkResourcesInAtomContent(atomModelWrapper);
        URI messageURI = wonMessage.getMessageURI();
        atomModelWrapper.renameResourceWithPrefix(messageURI.toString(), atomURI.toString());
        final Atom atom = getAtomRequired(atomURI);
        DatasetHolder datasetHolder = atom.getDatatsetHolder();
        Dataset oldAtomDataset = atom.getDatatsetHolder().getDataset();
        URI keyGraphUri = WonRelativeUriHelper.createKeyGraphURIForAtomURI(atom.getAtomURI());
        // store the atom content
        boolean handleLegacyKey = false;
        if (!oldAtomDataset.containsNamedModel(keyGraphUri.toString())) {
            // there is no separate key graph in the atom, yet so there
            // must be the public key in a content graph. We have to remove it from
            // there. If the message does not contain a new one, we copy the old one to the
            // key graph
            // Note: we cannot delete the key from the content graph because that would
            // invalidate its signature.
            handleLegacyKey = true;
        }
        Iterator<String> oldGraphNames = oldAtomDataset.listNames();
        String derivedDataName = atom.getAtomURI().toString() + "#derivedData";
        boolean legacyKeyHandled = false;
        while (oldGraphNames.hasNext()) {
            String oldGraphName = oldGraphNames.next();
            if (oldGraphName.equals(derivedDataName)) {
                newAtomContent.addNamedModel(oldGraphName, oldAtomDataset.getNamedModel(oldGraphName));
                continue;
            }
            if (handleLegacyKey) {
                Model model = oldAtomDataset.getNamedModel(oldGraphName);
                PublicKey key = null;
                try {
                    key = WonKeysReaderWriter.readFromModel(model, atomURI.toString());
                    if (key != null) {
                        if (legacyKeyHandled) {
                            throw new IllegalStateException("More than one legacy key found in atom " + atomURI);
                        }
                        legacyKeyHandled = true;
                        if (!newAtomContent.containsNamedModel(keyGraphUri.toString())) {
                            // only copy the legacy key to the key graph if the message
                            // does not contain a key
                            Model m = ModelFactory.createDefaultModel();
                            WonKeysReaderWriter.writeToModel(m, m.getResource(atomURI.toString()), key);
                            newAtomContent.addNamedModel(keyGraphUri.toString(), m);
                        }
                    }
                } catch (Exception e) {
                    throw new WonMessageProcessingException("Cannot process replace message", e);
                }
            }
            if (!newAtomContent.containsNamedModel(oldGraphName)) {
                // copy graphs not contained in the message from old to new state
                // ie, graphs with same name are overwritten
                newAtomContent.addNamedModel(oldGraphName, oldAtomDataset.getNamedModel(oldGraphName));
            }
        }
        if (!newAtomContent.containsNamedModel(keyGraphUri.toString())) {
            throw new WonProtocolException(
                            String.format("Cannot replace %s, operation results in atom without key graph", atomURI));
        }
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
        Collection<String> sockets = getSocketsAndCheck(atomModelWrapper, atomURI);
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
        socketRepository.saveAll(newSocketEntities);
        socketRepository.saveAll(changedSockets);
        socketRepository.deleteAll(removedSockets);
        datasetHolder.setDataset(newAtomContent);
        atom.setDatatsetHolder(datasetHolder);
        atom.setAttachmentDatasetHolders(attachments);
        dataDerivationService.deriveDataIfNecessary(atom);
        return atomRepository.save(atom);
    }

    private void checkReplaceAllowed(Dataset atomContent, URI atomUri, WonAclEvaluator evaluator,
                    OperationRequest request) {
        Iterator<String> graphNames = atomContent.listNames();
        URI aclGraphUri = WonRelativeUriHelper.createAclGraphURIForAtomURI(atomUri);
        URI keyGraphUri = WonRelativeUriHelper.createKeyGraphURIForAtomURI(atomUri);
        OperationRequest or = AuthUtils.cloneShallow(request);
        or.setReqPosition(POSITION_ATOM_GRAPH);
        while (graphNames.hasNext()) {
            String graphName = graphNames.next();
            if (graphName.endsWith(WonMessage.SIGNATURE_URI_GRAPHURI_SUFFIX)) {
                // don't check permissions for signatures
                continue;
            }
            URI graphUri = URI.create(graphName);
            if (aclGraphUri.equals(graphUri)) {
                or.addReqGraphType(GraphType.ACL_GRAPH);
            } else if (keyGraphUri.equals(graphUri)) {
                or.addReqGraphType(GraphType.KEY_GRAPH);
            } else {
                or.addReqGraphType(GraphType.CONTENT_GRAPH);
            }
            or.addReqGraph(graphUri);
        }
        AclEvalResult result = evaluator.decide(or);
        if (DecisionValue.ACCESS_DENIED.equals(result.getDecision())) {
            throw new ForbiddenMessageException("Replace operation is not allowed");
        }
    }

    private Set<Socket> determineNewSockets(URI atomURI, List<Socket> existingSockets,
                    AtomModelWrapper atomModelWrapper) {
        Collection<String> sockets = atomModelWrapper.getSocketUris();
        if (sockets.size() == 0) {
            throw new IllegalAtomContentException("at least one property won:socket required ");
        }
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
        if (!atomURI.equals(wonMessage.getAtomURI())) {
            throw new WrongAddressingInformationException(
                            "atomURI of the message (" + wonMessage.getAtomURI() + ") and AtomURI of the content ("
                                            + atomURI + ") are not equal",
                            wonMessage.getMessageURI(), WONMSG.atom);
        }
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
        if (sockets.size() == 0) {
            throw new IllegalAtomContentException("at least one property won:socket required ");
        }
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
            atom.setAuthorizedApplications(authorizedApplications.stream().distinct().collect(
                            Collectors.toList()));
        } else {
            logger.debug("owner application is new - creating");
            List<OwnerApplication> ownerApplicationList = new ArrayList<>(1);
            OwnerApplication ownerApplication = new OwnerApplication();
            ownerApplication.setOwnerApplicationId(ownerApplicationID);
            ownerApplication = ownerApplicationRepository.save(ownerApplication);
            ownerApplicationList.add(ownerApplication);
            atom.setAuthorizedApplications(ownerApplicationList.stream().distinct().collect(
                            Collectors.toList()));
            logger.debug("setting OwnerApp ID: " + ownerApplicationList.get(0));
        }
        split.stop();
        stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase3");
        split = stopwatch.start();
        atom = atomRepository.save(atom);
        split.stop();
        return atom;
    }

    private void acquireOwnerApplicationLock() {
        Lock lock = lockRepository.getOwnerapplicationLock();
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
