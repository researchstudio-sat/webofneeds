/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getIn, get } from "../utils.js";
import { parseAtom, parseMetaAtom } from "./atom-reducer/parse-atom.js";
import { parseMessage } from "./atom-reducer/parse-message.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

const initialState = Immutable.fromJS({
  processingInitialLoad: true,
  processingPublish: false,
  processingLogout: false,
  processingLogin: false,
  processingLoginForEmail: undefined,
  processingAcceptTermsOfService: false,
  processingVerifyEmailAddress: false,
  processingResendVerificationEmail: false,
  processingSendAnonymousLinkEmail: false,
  processingWhatsNew: false,
  processingWhatsAround: false,
  atoms: Immutable.Map(),
  connections: Immutable.Map(),
});

export const emptyAtomProcess = Immutable.fromJS({
  loading: false,
  toLoad: false,
  loaded: false,
  failedToLoad: false,
  processUpdate: false,
});

export const emptyConnectionProcess = Immutable.fromJS({
  toLoad: false,
  loading: false,
  loadingMessages: false,
  failedToLoad: false,
  petriNetData: {
    loading: false,
    dirty: false,
    loaded: false,
  },
  agreementData: {
    loading: false,
    loaded: false,
  },
  messages: Immutable.Map(),
});

export const emptyMessagesProcess = Immutable.fromJS({
  loading: false,
  toLoad: false,
  failedToLoad: false,
});

function updateAtomProcess(processState, atomUri, payload) {
  if (!atomUri) {
    return processState;
  }

  const oldAtomProcess = getIn(processState, ["atoms", atomUri]);
  const payloadImm = Immutable.fromJS(payload);

  return processState.setIn(
    ["atoms", atomUri],
    oldAtomProcess
      ? oldAtomProcess.mergeDeep(payloadImm)
      : emptyAtomProcess.mergeDeep(payloadImm)
  );
}

function updateConnectionProcess(processState, connUri, payload) {
  if (!connUri) {
    return processState;
  }

  const oldConnectionProcess = getIn(processState, ["connections", connUri]);
  const payloadImm = Immutable.fromJS(payload);

  return processState.setIn(
    ["connections", connUri],
    oldConnectionProcess
      ? oldConnectionProcess.mergeDeep(payloadImm)
      : emptyConnectionProcess.mergeDeep(payloadImm)
  );
}

function updateMessageProcess(processState, connUri, messageUri, payload) {
  if (!connUri || !messageUri) {
    return processState;
  }

  const payloadImm = Immutable.fromJS(payload);

  const oldMessageProcess = getIn(processState, [
    "connections",
    connUri,
    "messages",
    messageUri,
  ]);

  return processState.setIn(
    ["connections", connUri, "messages", messageUri],
    oldMessageProcess
      ? oldMessageProcess.mergeDeep(payloadImm)
      : emptyMessagesProcess.mergeDeep(payloadImm)
  );
}

export default function(processState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.reset:
      return initialState;

    case actionTypes.atoms.edit: {
      const atomUri = action.payload.atomUri;

      if (atomUri) {
        processState = updateAtomProcess(processState, atomUri, {
          processUpdate: true,
        });
      }

      return processState;
    }

    case actionTypes.atoms.fetchWhatsAround:
      return processState.set("processingWhatsAround", true);

    case actionTypes.atoms.fetchWhatsNew:
      return processState.set("processingWhatsNew", true);

    case actionTypes.atoms.storeWhatsNew: {
      const metaAtoms = action.payload.get("metaAtoms");
      const atomUris = metaAtoms && [...metaAtoms.keys()];
      atomUris &&
        atomUris.forEach(atomUri => {
          if (!processUtils.isAtomLoaded(processState, atomUri)) {
            processState = updateAtomProcess(processState, atomUri, {
              toLoad: true,
            });
          }
        });
      return processState.set("processingWhatsNew", false);
    }

    case actionTypes.atoms.storeWhatsAround: {
      const metaAtoms = action.payload.get("metaAtoms");
      const atomUris = metaAtoms && [...metaAtoms.keys()];
      atomUris &&
        atomUris.forEach(atomUri => {
          if (!processUtils.isAtomLoaded(processState, atomUri)) {
            processState = updateAtomProcess(processState, atomUri, {
              toLoad: true,
            });
          }
        });
      return processState.set("processingWhatsAround", false);
    }

    case actionTypes.personas.create:
    case actionTypes.atoms.create:
      return processState.set("processingPublish", true);

    case actionTypes.failedToGetLocation:
      return processState.set("processingPublish", false);

    case actionTypes.atoms.editFailure: {
      console.debug(
        "process-reducer actionTypes.atoms.editFailure todo: impl / payload-> ",
        action.payload
      );
      //TODO: IMPL
      return processState;
    }

    case actionTypes.atoms.editSuccessful: {
      const atomUri = action.payload.atomUri;

      if (atomUri) {
        processState = updateAtomProcess(processState, atomUri, {
          processUpdate: false,
        });
      }

      return processState;
    }

    case actionTypes.atoms.createSuccessful: {
      processState = updateAtomProcess(processState, action.payload.atomUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
        loaded: true,
      });
      return processState.set("processingPublish", false);
    }

    case actionTypes.account.logoutStarted:
      return processState.set("processingLogout", true);

    case actionTypes.account.logoutFinished:
      return processState
        .set("processingLogout", false)
        .set("processingInitialLoad", false);

    case actionTypes.account.loginStarted:
      return processState
        .set("processingLogin", true)
        .set("processingLoginForEmail", getIn(action, ["payload", "email"]));

    case actionTypes.account.acceptTermsOfServiceStarted:
      return processState.set("processingAcceptTermsOfService", true);

    case actionTypes.account.verifyEmailAddressStarted:
      return processState.set("processingVerifyEmailAddress", true);

    case actionTypes.account.verifyEmailAddressSuccess:
    case actionTypes.account.verifyEmailAddressFailed:
      return processState.set("processingVerifyEmailAddress", false);

    case actionTypes.account.acceptTermsOfServiceSuccess:
    case actionTypes.account.acceptTermsOfServiceFailed:
      return processState.set("processingAcceptTermsOfService", false);

    case actionTypes.account.resendVerificationEmailStarted:
      return processState.set("processingResendVerificationEmail", true);

    case actionTypes.account.resendVerificationEmailSuccess:
    case actionTypes.account.resendVerificationEmailFailed:
      return processState.set("processingResendVerificationEmail", false);

    case actionTypes.account.loginFinished:
    case actionTypes.account.loginFailed:
      return processState
        .set("processingLogin", false)
        .set("processingLoginForEmail", undefined);

    case actionTypes.initialLoadFinished: {
      return processState.set("processingInitialLoad", false);
    }

    case actionTypes.account.sendAnonymousLinkEmailStarted:
      return processState.set("processingSendAnonymousLinkEmail", true);

    case actionTypes.account.sendAnonymousLinkEmailFailed:
    case actionTypes.account.sendAnonymousLinkEmailSuccess:
      return processState.set("processingSendAnonymousLinkEmail", false);

    case actionTypes.atoms.storeUriFailed:
    case actionTypes.personas.storeUriFailed: {
      return updateAtomProcess(processState, action.payload.get("uri"), {
        toLoad: false,
        loaded: false,
        failedToLoad: true,
        loading: false,
      });
    }

    case actionTypes.connections.storeUriFailed: {
      return updateConnectionProcess(
        processState,
        action.payload.get("connUri"),
        { failedToStore: true, loading: false }
      );
    }

    case actionTypes.connections.fetchMessagesStart: {
      const connUri = action.payload.get("connectionUri");

      return updateConnectionProcess(processState, connUri, {
        loadingMessages: true,
        failedToLoad: false,
      });
    }

    case actionTypes.connections.fetchMessagesEnd: {
      const connUri = action.payload.get("connectionUri");

      return updateConnectionProcess(processState, connUri, {
        loadingMessages: false,
        failedToLoad: false,
      });
    }

    case actionTypes.connections.messageUrisInLoading: {
      const connUri = action.payload.get("connectionUri");
      const messageUris = action.payload.get("uris");

      if (messageUris) {
        messageUris.map(messageUri => {
          processState = updateMessageProcess(
            processState,
            connUri,
            messageUri,
            { toLoad: false, loading: true }
          );
        });
      }

      return processState;
    }

    case actionTypes.connections.fetchMessagesSuccess: {
      const connUri = action.payload.get("connectionUri");

      const loadedMessages = action.payload.get("events");
      if (loadedMessages) {
        processState = updateConnectionProcess(processState, connUri, {
          loadingMessages: false,
          failedToLoad: false,
        });

        loadedMessages.map((message, messageUri) => {
          processState = updateMessageProcess(
            processState,
            connUri,
            messageUri,
            { toLoad: false, loading: false, failedToLoad: false }
          );
        });
      }

      return processState;
    }

    case actionTypes.connections.fetchMessagesFailed: {
      const connUri = action.payload.get("connectionUri");
      const failedMessages = action.payload.get("events");

      if (failedMessages) {
        processState = updateConnectionProcess(processState, connUri, {
          loadingMessages: false,
          failedToLoad: true,
        });

        failedMessages.map((message, messageUri) => {
          processState = updateMessageProcess(
            processState,
            connUri,
            messageUri,
            { toLoad: false, loading: false, failedToLoad: true }
          );
        });
      }

      return processState;
    }

    case actionTypes.connections.setLoadingPetriNetData: {
      const loadingPetriNetData = action.payload.loadingPetriNetData;
      const connUri = action.payload.connectionUri;
      return updateConnectionProcess(processState, connUri, {
        petriNetData: {
          loading: loadingPetriNetData,
          dirty: loadingPetriNetData,
        },
      });
    }

    case actionTypes.connections.sendChatMessageClaimOnSuccess:
    case actionTypes.connections.sendChatMessageRefreshDataOnSuccess: {
      const connUri = action.payload.optimisticEvent.getSenderConnection();

      return updateConnectionProcess(processState, connUri, {
        petriNetData: { dirty: true },
      });
    }

    case actionTypes.connections.updatePetriNetData: {
      const petriNetData = action.payload.petriNetData;
      const connUri = action.payload.connectionUri;

      if (!connUri || !petriNetData) {
        return processState;
      }
      return updateConnectionProcess(processState, connUri, {
        petriNetData: { loading: false, dirty: false, loaded: true },
      });
    }

    case actionTypes.connections.updateAgreementData: {
      const agreementData = action.payload.agreementData;
      const connUri = action.payload.connectionUri;

      if (!connUri || !agreementData) {
        return processState;
      }
      return updateConnectionProcess(processState, connUri, {
        agreementData: { loading: false, loaded: true },
      });
    }

    case actionTypes.connections.setLoadingAgreementData: {
      const connUri = action.payload.connectionUri;
      const loadingAgreementData = action.payload.loadingAgreementData;

      return updateConnectionProcess(processState, connUri, {
        agreementData: { loading: loadingAgreementData },
      });
    }

    case actionTypes.connections.storeMetaConnections: {
      const connections = action.payload.get("connections");

      connections &&
        connections.map(conn => {
          processState = updateConnectionProcess(
            processState,
            conn.get("connectionUri"),
            {
              toLoad: true,
            }
          );
          const targetAtomUri = get(conn, "targetAtomUri");
          if (!processUtils.isAtomLoaded(processState, targetAtomUri)) {
            processState = updateAtomProcess(processState, targetAtomUri, {
              toLoad: true,
            });
          }
        });
      return processState;
    }

    case actionTypes.connections.storeActiveUrisInLoading: {
      const connUris = action.payload.get("connUris");

      connUris &&
        connUris.forEach(connUri => {
          processState = updateConnectionProcess(processState, connUri, {
            toLoad: false,
            loading: true,
          });
        });
      return processState;
    }

    case actionTypes.messages.reopenAtom.failed:
    case actionTypes.messages.closeAtom.failed: {
      let connections = action.payload.connections;

      connections &&
        connections.keySeq().forEach(connUri => {
          processState = updateConnectionProcess(processState, connUri, {
            loading: false,
          });
        });
      return processState;
    }

    case actionTypes.connections.storeActive: {
      let connections = action.payload.get("connections");

      connections &&
        connections.map((conn, connUri) => {
          processState = updateConnectionProcess(processState, connUri, {
            toLoad: false,
            loading: false,
          });

          const targetAtomUri = get(conn, "targetAtom");
          const sourceAtomUri = get(conn, "sourceAtom");
          if (
            targetAtomUri &&
            !processUtils.isAtomLoaded(processState, targetAtomUri) &&
            !processUtils.isAtomLoading(processState, targetAtomUri)
          ) {
            processState = updateAtomProcess(processState, targetAtomUri, {
              toLoad: true,
            });
          }
          if (
            sourceAtomUri &&
            !processUtils.isAtomLoaded(processState, sourceAtomUri) &&
            !processUtils.isAtomLoading(processState, sourceAtomUri)
          ) {
            processState = updateAtomProcess(processState, sourceAtomUri, {
              toLoad: true,
            });
          }
          const eventsOfConnection = conn.get("hasEvents");
          eventsOfConnection &&
            eventsOfConnection.map(eventUri => {
              processState = updateMessageProcess(
                processState,
                connUri,
                eventUri,
                { toLoad: true }
              );
            });
        });

      return processState;
    }

    case actionTypes.atoms.storeTheirs:
    case actionTypes.personas.storeTheirs:
    case actionTypes.atoms.storeOwned: {
      let atoms = action.payload.get("atoms");

      atoms &&
        atoms.map(atom => {
          const parsedAtom = parseAtom(atom);
          processState = updateAtomProcess(
            processState,
            parsedAtom.get("uri"),
            {
              toLoad: false,
              failedToLoad: false,
              loading: false,
              loaded: true,
            }
          );

          const heldAtomUris = parsedAtom.get("holds");
          heldAtomUris.map(heldAtomUri => {
            if (!processUtils.isAtomLoaded(processState, heldAtomUri)) {
              processState = updateAtomProcess(processState, heldAtomUri, {
                toLoad: true,
              });
            }
          });

          const groupMemberUris = parsedAtom.get("groupMembers");
          groupMemberUris.map(groupMemberUri => {
            if (!processUtils.isAtomLoaded(processState, groupMemberUri)) {
              processState = updateAtomProcess(processState, groupMemberUri, {
                toLoad: true,
              });
            }
          });

          const buddyUris = parsedAtom.get("buddies");
          buddyUris.map(buddyUri => {
            if (!processUtils.isAtomLoaded(processState, buddyUri)) {
              processState = updateAtomProcess(processState, buddyUri, {
                toLoad: true,
              });
            }
          });
        });
      return processState;
    }

    case actionTypes.atoms.storeOwnedMetaAtoms: {
      const metaAtoms = action.payload.get("metaAtoms");

      metaAtoms &&
        metaAtoms.map((metaAtom, metaAtomUri) => {
          const metaAtomImm = parseMetaAtom(metaAtom);
          if (atomUtils.isActive(metaAtomImm)) {
            processState = updateAtomProcess(processState, metaAtomUri, {
              loading: true,
            });
          } else if (atomUtils.isInactive(metaAtomImm)) {
            processState = updateAtomProcess(processState, metaAtomUri, {
              toLoad: true,
            });
          }
        });

      return processState;
    }

    case actionTypes.personas.storeTheirUrisInLoading:
    case actionTypes.atoms.storeTheirUrisInLoading: {
      const atomUris = action.payload.get("uris");
      atomUris &&
        atomUris.forEach(atomUri => {
          processState = updateAtomProcess(processState, atomUri, {
            toLoad: false,
            loading: true,
          });
        });
      return processState;
    }

    //Necessary to flag the originatorUri of a message as atom toLoad if the atom is not currently in the state yet (e.g new groupmember sends message)
    case actionTypes.messages.processConnectionMessage:
      return addOriginatorAtomToLoad(processState, action.payload);

    case actionTypes.atoms.delete:
    case actionTypes.atoms.removeDeleted:
    case actionTypes.personas.removeDeleted: {
      const atomUri = action.payload.get("uri");
      return processState.deleteIn(["atoms", atomUri]);
    }

    default:
      return processState;
  }
}

/*
 "alreadyProcessed" flag, which indicates that we do not care about the
 sent status anymore and assume that it has been successfully sent to each server (incl. the remote)
 "insertIntoConnUri" and "insertIntoAtomUri" are used for forwardedMessages so that the message is
 stored within the given connection/atom and not in the original atom or connection as we might not
 have these stored in the state
 */
export function addOriginatorAtomToLoad(
  processState,
  wonMessage,
  alreadyProcessed = false,
  insertIntoConnUri = undefined,
  insertIntoAtomUri = undefined
) {
  // we used to exclude messages without content here, using
  // if (wonMessage.getContentGraphs().length > 0) as the condition
  // however, after moving the socket info of connect/open messages from
  // content to envelope and making them optional, connect messages
  // actually can have no content. This never happened before, and
  // as one might expect, caused very weird behaviour when it did:
  // It was processed correctly after a reload, but as an
  // outgoing message, the success/failure responses coming in
  // would still cause an entry to be created in the messages array,
  // but holding only the 'isReceivedByOwn','isReceivedByRemote' etc fields,
  // throwing off the message rendering.
  // New solution: parse anything that is not a response, but allow responses with content
  if (!wonMessage.isResponse() || wonMessage.getContentGraphs().length > 0) {
    let parsedMessage = parseMessage(
      wonMessage,
      alreadyProcessed,
      insertIntoConnUri && insertIntoAtomUri
    );
    if (parsedMessage) {
      const connectionUri =
        insertIntoConnUri || parsedMessage.get("belongsToUri");

      let atomUri = insertIntoAtomUri;
      if (!atomUri && parsedMessage.getIn(["data", "outgoingMessage"])) {
        // atomUri is the message's senderAtom
        atomUri = wonMessage.getSenderAtom();
      } else if (!atomUri) {
        // atomUri is the remote message's recipientAtom
        atomUri = wonMessage.getRecipientAtom();
      }

      const originatorUri = parsedMessage.getIn(["data", "originatorUri"]);

      if (originatorUri) {
        //Message is originally from another atom, we might need to add the atom as well
        if (!processUtils.isAtomLoaded(processState, originatorUri)) {
          console.debug(
            "Originator Atom is not in the state yet, we need to add it"
          );
          processState = updateAtomProcess(processState, originatorUri, {
            toLoad: true,
            loading: false,
          });
        }
      }

      if (atomUri) {
        const hasContainedForwardedWonMessages = wonMessage.hasContainedForwardedWonMessages();

        if (hasContainedForwardedWonMessages) {
          const containedForwardedWonMessages = wonMessage.getContainedForwardedWonMessages();
          containedForwardedWonMessages.map(forwardedWonMessage => {
            processState = addOriginatorAtomToLoad(
              processState,
              forwardedWonMessage,
              true,
              connectionUri,
              atomUri
            );
            //PARSE MESSAGE DIFFERENTLY FOR FORWARDED MESSAGES
          });
        }
      }
    }
  }
  return processState;
}
