package won.owner.web.service;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.owner.model.User;
import won.owner.model.UserAtom;
import won.owner.repository.UserAtomRepository;
import won.owner.repository.UserRepository;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.model.AtomState;

@Component
public class UserAtomService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAtomRepository userAtomRepository;

    public UserAtomService() {
    }

    public void updateUserAtomAssociation(WonMessage wonMessage, User user) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        Long userId = user != null ? user.getId() : null;
        WonMessage focalMessage = wonMessage.getFocalMessage();
        if (focalMessage.getMessageType() == WonMessageType.SUCCESS_RESPONSE) {
            switch (focalMessage.getRespondingToMessageType()) {
                case CREATE_ATOM:
                    try {
                        saveAtomUriWithUser(focalMessage, user);
                    } catch (Exception e) {
                        logger.warn("could not associate atom " + atomUri + " with user " + userId, e);
                    }
                    break;
                case DEACTIVATE:
                    try {
                        deactivateAtomUri(focalMessage);
                    } catch (Exception e) {
                        logger.warn("could not deactivate atom " + atomUri + " of user " + userId, e);
                    }
                    break;
                case ACTIVATE:
                    try {
                        activateAtomUri(focalMessage);
                    } catch (Exception e) {
                        logger.warn("could not activate atom " + atomUri + " of user " + userId, e);
                    }
                    break;
                case DELETE:
                    try {
                        deleteUserAtom(focalMessage, user);
                    } catch (Exception e) {
                        logger.warn("could not delete atom " + atomUri + " of user " + userId, e);
                    }
                default:
                    // do nothing
            }
        }
    }

    private URI getOwnedAtomURI(WonMessage message) {
        return message.getEnvelopeType() == WonMessageDirection.FROM_SYSTEM ? message.getSenderAtomURI()
                        : message.getRecipientAtomURI();
    }

    private void saveAtomUriWithUser(final WonMessage wonMessage, User user) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        logger.debug("adding atom {} to atoms of user {}", atomUri, user.getId());
        UserAtom userAtom = new UserAtom(atomUri);
        // reload the user so we can save it
        // (the user object we get from getUserForSession is detached)
        user = userRepository.findOne(user.getId());
        userAtom = userAtomRepository.save(userAtom);
        logger.debug("saved user atom {}", userAtom.getId());
        user.addUserAtom(userAtom);
        userRepository.save(user);
        logger.debug("atom {} added to atoms of user {}", userAtom.getId(), user.getId());
    }

    private void deactivateAtomUri(final WonMessage wonMessage) {
        updateAtomUriState(wonMessage, AtomState.INACTIVE);
    }

    private void activateAtomUri(final WonMessage wonMessage) {
        updateAtomUriState(wonMessage, AtomState.ACTIVE);
    }

    private void updateAtomUriState(final WonMessage wonMessage, AtomState newState) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        UserAtom userAtom = userAtomRepository.findByAtomUri(atomUri);
        userAtom.setState(newState);
        // reload the user so we can save it
        // (the user object we get from getUserForSession is detached)
        userAtomRepository.save(userAtom);
    }

    public void setAtomDeleted(URI atomURI) {
        // Get the atom from owner application db
        UserAtom userAtom = userAtomRepository.findByAtomUri(atomURI);
        userAtom.setState(AtomState.DELETED);
        userAtomRepository.save(userAtom);
    }

    private void deleteUserAtom(final WonMessage wonMessage, User user) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        // Get the atom from owner application db
        UserAtom userAtom = userAtomRepository.findByAtomUri(atomUri);
        if (userAtom != null) {
            if (userAtom.getState() == AtomState.DELETED) {
                // reload the user so we can save it
                // (the user object we get from getUserForSession is detached)
                user = userRepository.findOne(user.getId());
                // Delete atom in users atom list and save changes
                user.getUserAtoms().remove(userAtom);
                user = userRepository.save(user);
                // Delete atom in atom repository
                userAtomRepository.delete(userAtom);
            } else {
                throw new IllegalStateException("atom not in state deleted");
            }
        }
    }
}
