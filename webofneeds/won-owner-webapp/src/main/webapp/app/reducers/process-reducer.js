/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { get, getIn } from "../utils.js";
import { parseAtom } from "./atom-reducer/parse-atom.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

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
  processingMetaAtoms: false,
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

    case actionTypes.atoms.fetchMetaAtoms:
      return processState.set("processingMetaAtoms", true);

    case actionTypes.atoms.fetchWhatsNew:
      return processState.set("processingWhatsNew", true);

    case actionTypes.atoms.storeWhatsNew: {
      const metaAtoms = get(action.payload, "metaAtoms");
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

    case actionTypes.atoms.storeMetaAtoms: {
      const metaAtoms = get(action.payload, "metaAtoms");

      metaAtoms &&
        metaAtoms.map((metaAtom, metaAtomUri) => {
          if (!processUtils.isAtomLoaded(processState, metaAtomUri)) {
            processState = updateAtomProcess(processState, metaAtomUri, {
              toLoad: true,
            });
          }
        });

      return processState.set("processingMetaAtoms", false);
    }

    case actionTypes.atoms.storeWhatsAround: {
      const metaAtoms = get(action.payload, "metaAtoms");
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
      return updateAtomProcess(processState, get(action.payload, "uri"), {
        toLoad: false,
        loaded: false,
        failedToLoad: true,
        loading: false,
      });
    }

    case actionTypes.connections.storeUriFailed: {
      return updateConnectionProcess(
        processState,
        get(action.payload, "connUri"),
        { failedToStore: true, loading: false }
      );
    }

    case actionTypes.connections.fetchMessagesStart: {
      const connUri = get(action.payload, "connectionUri");

      return updateConnectionProcess(processState, connUri, {
        loadingMessages: true,
        failedToLoad: false,
      });
    }

    case actionTypes.connections.fetchMessagesEnd: {
      const connUri = get(action.payload, "connectionUri");

      return updateConnectionProcess(processState, connUri, {
        loadingMessages: false,
        failedToLoad: false,
      });
    }

    case actionTypes.connections.messageUrisInLoading: {
      const connUri = get(action.payload, "connectionUri");
      const messageUris = get(action.payload, "uris");

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
      const connUri = get(action.payload, "connectionUri");

      const loadedMessages = get(action.payload, "events");
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
      const connUri = get(action.payload, "connectionUri");
      const failedMessages = get(action.payload, "events");

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
      console.debug(
        "sendChatMessageClaimOnSuccess/sendChatMessageRefreshDataOnSuccess -> needs to be implemented"
      );
      const senderSocketUri = action.payload.senderSocketUri;
      const targetSocketUri = action.payload.targetSocketUri;
      console.debug("senderSocketUri: ", senderSocketUri);
      console.debug("targetSocketUri: ", targetSocketUri);
      /* const connUri = action.payload.optimisticEvent.getSenderConnection(); // This has been removed

      return updateConnectionProcess(processState, connUri, {
        petriNetData: { dirty: true },
      });*/
      return processState; //todo figure out how to extract the connection based on the socketUris
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
      const connections = get(action.payload, "connections");

      connections &&
        connections.map(conn => {
          processState = updateConnectionProcess(
            processState,
            get(conn, "connectionUri"),
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
      const connUris = get(action.payload, "connUris");

      connUris &&
        connUris.forEach(connUri => {
          processState = updateConnectionProcess(processState, connUri, {
            toLoad: false,
            loading: true,
          });
        });
      return processState;
    }

    case actionTypes.connections.storeActive: {
      let connections = get(action.payload, "connections");

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
          const eventsOfConnection = get(conn, "hasEvents");
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

    case actionTypes.atoms.store:
    case actionTypes.personas.store: {
      let atoms = get(action.payload, "atoms");

      atoms &&
        atoms.map(atom => {
          const parsedAtom = parseAtom(atom);
          if (parsedAtom) {
            processState = updateAtomProcess(
              processState,
              get(parsedAtom, "uri"),
              {
                toLoad: false,
                failedToLoad: false,
                loading: false,
                loaded: true,
              }
            );

            const heldAtomUris = get(parsedAtom, "holds");
            heldAtomUris.map(heldAtomUri => {
              if (!processUtils.isAtomLoaded(processState, heldAtomUri)) {
                processState = updateAtomProcess(processState, heldAtomUri, {
                  toLoad: true,
                });
              }
            });

            const groupMemberUris = get(parsedAtom, "groupMembers");
            groupMemberUris.map(groupMemberUri => {
              if (!processUtils.isAtomLoaded(processState, groupMemberUri)) {
                processState = updateAtomProcess(processState, groupMemberUri, {
                  toLoad: true,
                });
              }
            });

            const buddyUris = get(parsedAtom, "buddies");
            buddyUris.map(buddyUri => {
              if (!processUtils.isAtomLoaded(processState, buddyUri)) {
                processState = updateAtomProcess(processState, buddyUri, {
                  toLoad: true,
                });
              }
            });
          }
        });
      return processState;
    }

    case actionTypes.atoms.storeOwnedMetaAtoms: {
      const metaAtoms = get(action.payload, "metaAtoms");

      metaAtoms &&
        metaAtoms.map((metaAtom, metaAtomUri) => {
          processState = updateAtomProcess(processState, metaAtomUri, {
            toLoad: true,
          });
        });

      return processState;
    }

    case actionTypes.personas.storeUriInLoading:
    case actionTypes.atoms.storeUriInLoading: {
      const atomUri = get(action.payload, "uri");

      if (atomUri) {
        processState = updateAtomProcess(processState, atomUri, {
          toLoad: false,
          loading: true,
        });
      }
      return processState;
    }

    //Necessary to flag the originatorUri of a message as atom toLoad if the atom is not currently in the state yet (e.g new groupmember sends message)
    case actionTypes.messages.processConnectionMessage:
      return addMessageAtomsToLoad(processState, action.payload);

    case actionTypes.atoms.delete:
    case actionTypes.atoms.removeDeleted:
    case actionTypes.personas.removeDeleted: {
      const atomUri = get(action.payload, "uri");
      return processState.deleteIn(["atoms", atomUri]);
    }

    default:
      return processState;
  }
}

/*
 Some connectionMessages could be from another atom that is not known yet (e.g. groupchat) this behaviour ensures that we will load
 these messages accordingly
 */
export function addMessageAtomsToLoad(processState, wonMessage) {
  if (!wonMessage.isResponse() || wonMessage.getContentGraphs().length > 0) {
    const senderAtomUri = generalSelectors.getAtomUriBySocketUri(
      wonMessage.getSenderSocket()
    );
    const targetAtomUri = generalSelectors.getAtomUriBySocketUri(
      wonMessage.getTargetSocket()
    );

    if (
      senderAtomUri &&
      !processUtils.isAtomLoaded(processState, senderAtomUri)
    ) {
      console.debug("Sender Atom is not in the state yet, we need to add it");
      processState = updateAtomProcess(processState, senderAtomUri, {
        toLoad: true,
        loading: false,
      });
    }

    if (
      targetAtomUri &&
      !processUtils.isAtomLoaded(processState, targetAtomUri)
    ) {
      console.debug("Target Atom is not in the state yet, we need to add it");
      processState = updateAtomProcess(processState, targetAtomUri, {
        toLoad: true,
        loading: false,
      });
    }
  }
  return processState;
}
