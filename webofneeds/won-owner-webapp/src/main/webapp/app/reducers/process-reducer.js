/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { get, getIn, getUri, extractAtomUriBySocketUri } from "../utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { getAtomRequests } from "../redux/utils/process-utils.js";

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
  externalData: Immutable.Map(),
  atoms: Immutable.Map(),
  connections: Immutable.Map(),
  connectionContainers: Immutable.Map(),
  fetchTokens: Immutable.Map(),
});

export const emptyExternalDataProcess = Immutable.fromJS({
  loading: false,
  failedToLoad: false,
});

export const emptyFetchTokenProcess = Immutable.fromJS({
  requests: Immutable.List(),
});

export const emptyAtomProcess = Immutable.fromJS({
  loading: false,
  toLoad: false,
  loaded: false,
  failedToLoad: false,
  processUpdate: false,
  requests: Immutable.List(),
});

export const emptyConnectionContainerProcess = Immutable.fromJS({
  loading: false,
  toLoad: false,
  loaded: false,
  failedToLoad: false,
  requests: Immutable.List(),
});

export const emptyConnectionProcess = Immutable.fromJS({
  toLoad: false,
  loading: false,
  loadingMessages: false,
  nextPage: undefined,
  failedToLoad: false,
  requests: Immutable.List(),
  petriNetData: {
    loading: false,
    dirty: false,
    loaded: false,
    failCount: 0,
  },
  agreementDataset: {
    loading: false,
    loaded: false,
    failCount: 0,
  },
  agreementData: {
    loading: false,
    loaded: false,
    failCount: 0,
  },
  messages: Immutable.Map(),
});

export const emptyMessagesProcess = Immutable.fromJS({
  failedToLoad: false,
});

function updateExternalDataProcess(processState, externalDataUri, payload) {
  if (!externalDataUri) {
    return processState;
  }

  const oldExternalDataProcess = getIn(processState, [
    "externalData",
    externalDataUri,
  ]);
  const payloadImm = Immutable.fromJS(payload);

  return processState.setIn(
    ["externalData", externalDataUri],
    oldExternalDataProcess
      ? oldExternalDataProcess.mergeDeep(payloadImm)
      : emptyExternalDataProcess.mergeDeep(payloadImm)
  );
}

function updateFetchTokenProcess(
  processState,
  atomUri,
  tokenScopeUri,
  payload
) {
  if (!atomUri || !tokenScopeUri) {
    return processState;
  }

  const oldFetchTokenProcess = getIn(processState, [
    "fetchTokens",
    atomUri,
    tokenScopeUri,
  ]);
  const payloadImm = Immutable.fromJS(payload);

  return processState.setIn(
    ["fetchTokens", atomUri, tokenScopeUri],
    oldFetchTokenProcess
      ? oldFetchTokenProcess.mergeDeep(payloadImm)
      : emptyFetchTokenProcess.mergeDeep(payloadImm)
  );
}

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

function updateConnectionContainerProcess(processState, atomUri, payload) {
  if (!atomUri) {
    return processState;
  }

  const oldConnectionContainerProcess = getIn(processState, [
    "connectionContainers",
    atomUri,
  ]);
  const payloadImm = Immutable.fromJS(payload);

  return processState.setIn(
    ["connectionContainers", atomUri],
    oldConnectionContainerProcess
      ? oldConnectionContainerProcess.mergeDeep(payloadImm)
      : emptyConnectionContainerProcess.mergeDeep(payloadImm)
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
          if (!processUtils.isAtomProcessExisting(processState, atomUri)) {
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
          if (!processUtils.isAtomProcessExisting(processState, metaAtomUri)) {
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
          if (!processUtils.isAtomProcessExisting(processState, atomUri)) {
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
      const atomUri = getUri(action.payload);

      if (atomUri) {
        processState = updateAtomProcess(processState, atomUri, {
          processUpdate: false,
        });
      }

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

    case actionTypes.atoms.createFailure: {
      const atomUri = getUri(action.payload);
      return processState
        .deleteIn(["atoms", atomUri])
        .set("processingPublish", false);
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

    case actionTypes.atoms.fetchToken.failure: {
      const atomUri = getUri(action.payload);
      const tokenScopeUri = get(action.payload, "tokenScopeUri");
      const request = get(action.payload, "request");
      const updatedFetchTokenRequests = processUtils
        .getFetchTokenRequests(processState, atomUri, tokenScopeUri)
        .push(request);

      const remainingRequestCredentials = get(
        action.payload,
        "allRequestCredentials"
      ).filter(
        requestCredentials =>
          !processUtils.isUsedCredentialsUnsuccessfully(
            updatedFetchTokenRequests,
            requestCredentials
          )
      );

      console.debug(
        "remainingRequestCredentials to fetch TokenScopeUri",
        tokenScopeUri,
        " from ",
        atomUri,
        " -> ",
        remainingRequestCredentials.size
      );

      return updateFetchTokenProcess(processState, atomUri, tokenScopeUri, {
        requests: updatedFetchTokenRequests,
      });
    }

    case actionTypes.atoms.fetchToken.success: {
      const atomUri = getUri(action.payload);
      const tokenScopeUri = get(action.payload, "tokenScopeUri");
      const request = get(action.payload, "request");
      const fetchTokenRequests = processUtils.getFetchTokenRequests(
        processState,
        atomUri,
        tokenScopeUri
      );

      return updateFetchTokenProcess(processState, atomUri, tokenScopeUri, {
        requests: fetchTokenRequests.push(request),
      });
    }

    case actionTypes.atoms.storeUriFailed: {
      const atomUri = getUri(action.payload);
      const request = get(action.payload, "request");
      const updatedAtomRequests = processUtils
        .getAtomRequests(processState, atomUri)
        .push(request);

      const remainingRequestCredentials = get(
        action.payload,
        "allRequestCredentials"
      ).filter(
        requestCredentials =>
          !processUtils.isUsedCredentialsUnsuccessfully(
            updatedAtomRequests,
            requestCredentials
          )
      );

      return updateAtomProcess(processState, atomUri, {
        toLoad: remainingRequestCredentials.size > 0,
        loaded: false,
        failedToLoad: true,
        loading: false,
        requests: updatedAtomRequests,
      });
    }

    case actionTypes.atoms.storeConnectionContainerFailed: {
      const atomUri = getUri(action.payload);
      const request = get(action.payload, "request");
      const updatedConnectionContainerRequests = processUtils
        .getConnectionContainerRequests(processState, atomUri)
        .push(request);

      const remainingRequestCredentials = get(
        action.payload,
        "allRequestCredentials"
      ).filter(
        requestCredentials =>
          !processUtils.isUsedCredentials(
            updatedConnectionContainerRequests,
            requestCredentials
          )
      );

      return updateConnectionContainerProcess(processState, atomUri, {
        toLoad: remainingRequestCredentials.size > 0,
        // loaded: false, //do not set loaded to false, since we will check multiple times
        failedToLoad: true,
        loading: false,
        requests: updatedConnectionContainerRequests,
      });
    }

    case actionTypes.connections.storeUriFailed: {
      const connUri = get(action.payload, "connUri");
      const request = get(action.payload, "request");
      const connectionRequests = processUtils.getConnectionRequests(
        processState,
        connUri
      );

      return updateConnectionProcess(processState, connUri, {
        failedToLoad: true,
        loading: false,
        requests: connectionRequests.push(request),
      });
    }

    case actionTypes.connections.fetchMessagesStart: {
      const connUri = get(action.payload, "connectionUri");

      return updateConnectionProcess(processState, connUri, {
        loadingMessages: true,
        failedToLoad: false,
        requests: undefined,
      });
    }

    case actionTypes.connections.fetchMessagesEnd: {
      const connUri = get(action.payload, "connectionUri");
      const nextPage = get(action.payload, "nextPage");

      return updateConnectionProcess(processState, connUri, {
        loadingMessages: false,
        failedToLoad: false,
        requests: undefined,
        nextPage: nextPage,
      });
    }

    case actionTypes.connections.fetchMessagesSuccess: {
      const connUri = get(action.payload, "connectionUri");
      const nextPage = get(action.payload, "nextPage");

      const loadedMessages = get(action.payload, "events");
      if (loadedMessages) {
        processState = updateConnectionProcess(processState, connUri, {
          loadingMessages: false,
          failedToLoad: false,
          requests: undefined,
          nextPage: nextPage,
        });

        const connectionMessageProcess = getIn(processState, [
          "connections",
          connUri,
          "messages",
        ]);

        if (connectionMessageProcess && connectionMessageProcess.size > 0) {
          loadedMessages.map((message, messageUri) => {
            //Only set failedToLoad to false if the messageProcess existed (only happens if the message failed once)
            if (get(connectionMessageProcess, messageUri)) {
              processState = updateMessageProcess(
                processState,
                connUri,
                messageUri,
                { failedToLoad: false }
              );
            }
          });
        }
      }

      return processState;
    }

    case actionTypes.connections.fetchMessagesFailed: {
      const connUri = get(action.payload, "connectionUri");
      const nextPage = get(action.payload, "nextPage");

      const failedMessages = get(action.payload, "events");
      if (failedMessages) {
        processState = updateConnectionProcess(processState, connUri, {
          loadingMessages: false,
          failedToLoad: true,
          nextPage: nextPage,
        });

        failedMessages.map((message, messageUri) => {
          processState = updateMessageProcess(
            processState,
            connUri,
            messageUri,
            { failedToLoad: true }
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

    case actionTypes.connections.failedLoadingPetriNetData: {
      const connUri = action.payload.connectionUri;

      let failCount = processUtils.getConnectionPetriNetDataFailCount(
        processState,
        connUri
      );

      return updateConnectionProcess(processState, connUri, {
        petriNetData: {
          loading: false,
          dirty: false,
          failCount: ++failCount,
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
      const connectionUri = action.payload.connectionUri;
      console.debug("senderSocketUri: ", senderSocketUri);
      console.debug("targetSocketUri: ", targetSocketUri);

      console.debug("connectionUri", connectionUri);

      return processState;
    }

    case actionTypes.connections.updatePetriNetData: {
      const petriNetData = action.payload.petriNetData;
      const connUri = action.payload.connectionUri;

      if (!connUri || !petriNetData) {
        return processState;
      }
      return updateConnectionProcess(processState, connUri, {
        petriNetData: {
          loading: false,
          dirty: false,
          loaded: true,
          failCount: 0,
        },
      });
    }

    case actionTypes.connections.updateAgreementData: {
      const agreementData = action.payload.agreementData;
      const connUri = action.payload.connectionUri;

      if (!connUri || !agreementData) {
        return processState;
      }
      return updateConnectionProcess(processState, connUri, {
        agreementData: { loading: false, loaded: true, failCount: 0 },
      });
    }

    case actionTypes.connections.updateAgreementDataset: {
      const agreementDataset = action.payload.agreementDataset;
      const connUri = action.payload.connectionUri;

      if (!connUri || !agreementDataset) {
        return processState;
      }
      return updateConnectionProcess(processState, connUri, {
        agreementDataset: { loading: false, loaded: true, failCount: 0 },
      });
    }

    case actionTypes.connections.failedLoadingAgreementData: {
      const connUri = action.payload.connectionUri;

      let failCount = processUtils.getConnectionAgreementDataFailCount(
        processState,
        connUri
      );
      return updateConnectionProcess(processState, connUri, {
        agreementData: { loading: false, failCount: ++failCount },
      });
    }

    case actionTypes.connections.setLoadingAgreementData: {
      const connUri = action.payload.connectionUri;
      const loadingAgreementData = action.payload.loadingAgreementData;

      return updateConnectionProcess(processState, connUri, {
        agreementData: { loading: loadingAgreementData },
      });
    }

    case actionTypes.connections.setLoadingAgreementDataset: {
      const connUri = action.payload.connectionUri;
      const loadingAgreementDataset = action.payload.loadingAgreementDataset;

      return updateConnectionProcess(processState, connUri, {
        agreementDataset: { loading: loadingAgreementDataset },
      });
    }

    case actionTypes.connections.failedLoadingAgreementDataset: {
      const connUri = action.payload.connectionUri;

      let failCount = processUtils.getConnectionAgreementDatasetFailCount(
        processState,
        connUri
      );

      return updateConnectionProcess(processState, connUri, {
        agreementDataset: {
          loading: false,
          failCount: ++failCount,
          loaded: false,
        },
      });
    }

    case actionTypes.connections.setLoadedAgreementDataset: {
      const connUri = action.payload.connectionUri;
      const loadedAgreementDataset = action.payload.loadedAgreementDataset;

      return updateConnectionProcess(processState, connUri, {
        agreementDataset: loadedAgreementDataset
          ? { loaded: true, failCount: 0 }
          : { loaded: false },
      });
    }

    case actionTypes.connections.storeMetaConnections: {
      const atomUri = get(action.payload, "atomUri");
      const connections = get(action.payload, "connections");
      const request = get(action.payload, "request");
      const updatedConnectionContainerRequests = processUtils
        .getConnectionContainerRequests(processState, atomUri)
        .push(request);

      const remainingRequestCredentials = get(
        action.payload,
        "allRequestCredentials"
      ).filter(
        requestCredentials =>
          !processUtils.isUsedCredentials(
            updatedConnectionContainerRequests,
            requestCredentials
          )
      );

      console.debug(
        "remainingRequestCredentials for connectionContainer of ",
        atomUri,
        " -> ",
        remainingRequestCredentials.size
      );

      processState = updateConnectionContainerProcess(processState, atomUri, {
        toLoad: remainingRequestCredentials.size > 0,
        failedToLoad: false,
        requests: updatedConnectionContainerRequests,
        loading: false,
        loaded: true,
      });

      connections &&
        connections.map(conn => {
          processState = updateConnectionProcess(processState, getUri(conn), {
            toLoad: true,
          });
          const targetAtomUri = get(conn, "targetAtom");
          if (
            !processUtils.isAtomProcessExisting(processState, targetAtomUri)
          ) {
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
            requests: undefined,
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
            nextPage: {
              url: get(conn, "messageContainer"),
              params: {},
            },
            requests: undefined,
          });

          const targetAtomUri = get(conn, "targetAtom");
          const sourceAtomUri = get(conn, "sourceAtom");
          if (
            targetAtomUri &&
            !processUtils.isAtomProcessExisting(processState, targetAtomUri)
          ) {
            processState = updateAtomProcess(processState, targetAtomUri, {
              toLoad: true,
            });
          }
          if (
            sourceAtomUri &&
            !processUtils.isAtomProcessExisting(processState, sourceAtomUri)
          ) {
            processState = updateAtomProcess(processState, sourceAtomUri, {
              toLoad: true,
            });
          }
        });

      return processState;
    }

    case actionTypes.externalData.store: {
      const data = get(action, "payload");

      data &&
        data.mapKeys(uri => {
          processState = updateExternalDataProcess(processState, uri, {
            failedToLoad: false,
            loading: false,
          });
        });

      return processState;
    }

    case actionTypes.atoms.store: {
      const atom = get(action.payload, "atom");
      const request = get(action.payload, "request");

      if (atom) {
        const atomUri = getUri(atom);
        const atomRequests = getAtomRequests(processState, atomUri);
        processState = updateAtomProcess(processState, atomUri, {
          toLoad: false,
          failedToLoad: false,
          requests: atomRequests.push(request),
          loading: false,
          loaded: true,
        });
        processState = updateConnectionContainerProcess(processState, atomUri, {
          toLoad: true,
        });
      }
      return processState;
    }

    case actionTypes.atoms.markAsLoaded: {
      let atomUri = getUri(action.payload);

      return updateAtomProcess(processState, atomUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
        loaded: true,
      });
    }

    case actionTypes.atoms.markConnectionContainerAsLoaded: {
      let atomUri = getUri(action.payload);

      return updateConnectionContainerProcess(processState, atomUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
        loaded: true,
      });
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

    case actionTypes.atoms.storeUriInLoading: {
      const atomUri = getUri(action.payload);

      if (atomUri) {
        processState = updateAtomProcess(processState, atomUri, {
          toLoad: false,
          loading: true,
        });
      }
      return processState;
    }

    case actionTypes.atoms.storeConnectionContainerInLoading: {
      const atomUri = getUri(action.payload);

      if (atomUri) {
        processState = updateConnectionContainerProcess(processState, atomUri, {
          toLoad: false,
          loading: true,
        });
      }
      return processState;
    }

    case actionTypes.externalData.storeUriInLoading: {
      const externalDataUri = getUri(action.payload);

      if (externalDataUri) {
        processState = updateExternalDataProcess(
          processState,
          externalDataUri,
          {
            loading: true,
            failedToLoad: false,
          }
        );
      }
      return processState;
    }

    //Necessary to flag the originatorUri of a message as atom toLoad if the atom is not currently in the state yet (e.g new groupmember sends message)
    case actionTypes.messages.processConnectionMessage:
      return addMessageAtomsToLoad(processState, action.payload);

    case actionTypes.atoms.delete:
    case actionTypes.atoms.removeDeleted: {
      const atomUri = getUri(action.payload);
      return processState
        .deleteIn(["atoms", atomUri])
        .deleteIn(["connectionContainers", atomUri]);
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
  if (!wonMessage.isResponse()) {
    const senderAtomUri = extractAtomUriBySocketUri(
      wonMessage.getSenderSocket()
    );
    const targetAtomUri = extractAtomUriBySocketUri(
      wonMessage.getTargetSocket()
    );

    if (
      senderAtomUri &&
      !processUtils.isAtomProcessExisting(processState, senderAtomUri)
    ) {
      console.debug("Sender Atom is not in the state yet, we need to add it");
      processState = updateAtomProcess(processState, senderAtomUri, {
        toLoad: true,
        loading: false,
      });
    }

    if (
      targetAtomUri &&
      !processUtils.isAtomProcessExisting(processState, targetAtomUri)
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
