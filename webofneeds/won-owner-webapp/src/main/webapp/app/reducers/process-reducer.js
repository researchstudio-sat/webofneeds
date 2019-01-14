/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getIn, get } from "../utils.js";
import { parseNeed } from "./need-reducer/parse-need.js";

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
  needs: Immutable.Map(),
  connections: Immutable.Map(),
});

export const emptyNeedProcess = Immutable.fromJS({
  loading: false,
  toLoad: false,
  failedToLoad: false,
});

export const emptyConnectionProcess = Immutable.fromJS({
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

function updateNeedProcess(processState, needUri, payload) {
  if (!needUri) {
    return processState;
  }

  const oldNeedProcess = getIn(processState, ["needs", needUri]);
  const payloadImm = Immutable.fromJS(payload);

  return processState.setIn(
    ["needs", needUri],
    oldNeedProcess
      ? oldNeedProcess.mergeDeep(payloadImm)
      : emptyNeedProcess.mergeDeep(payloadImm)
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
    case actionTypes.personas.create:
    case actionTypes.needs.create:
    case actionTypes.needs.whatsNew:
    case actionTypes.needs.whatsAround:
      return processState.set("processingPublish", true);

    case actionTypes.failedToGetLocation:
      return processState.set("processingPublish", false);

    case actionTypes.needs.createSuccessful: {
      const needUri =
        action.payload.need && get(parseNeed(action.payload.need), "uri");

      processState = updateNeedProcess(processState, needUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
      });
      return processState.set("processingPublish", false);
    }

    case actionTypes.needs.fetchSuggested: {
      const suggestedPosts = action.payload.get("suggestedPosts");

      if (!suggestedPosts) {
        return processState;
      }
      return suggestedPosts.reduce((updatedState, suggestedPost) => {
        const needUri = suggestedPost && get(parseNeed(suggestedPost), "uri");

        return updateNeedProcess(processState, needUri, {
          toLoad: false,
          failedToLoad: false,
          loading: false,
        });
      }, processState);
    }

    case actionTypes.account.logoutStarted:
      return processState.set("processingLogout", true);

    case actionTypes.account.logoutFinished:
      return processState.set("processingLogout", false);

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

    case actionTypes.needs.storeUriFailed:
    case actionTypes.personas.storeUriFailed: {
      return updateNeedProcess(processState, action.payload.get("uri"), {
        toLoad: false,
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
        console.log("fetchMessagesSuccess: loadedMessages: ", loadedMessages);
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
      const connUri = action.payload.optimisticEvent.getSender();

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

    case actionTypes.connections.storeActiveUrisInLoading: {
      const connUris = action.payload.get("connUris");

      connUris &&
        connUris.forEach(connUri => {
          processState = updateConnectionProcess(processState, connUri, {
            loading: true,
          });
        });
      return processState;
    }

    case actionTypes.messages.reopenNeed.failed:
    case actionTypes.messages.closeNeed.failed: {
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
            loading: false,
          });

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

    case actionTypes.needs.storeTheirs:
    case actionTypes.personas.storeTheirs:
    case actionTypes.needs.storeOwned: {
      let needs = action.payload.get("needs");

      needs &&
        needs.keySeq().forEach(needUri => {
          processState = updateNeedProcess(processState, needUri, {
            toLoad: false,
            failedToLoad: false,
            loading: false,
          });
        });
      return processState;
    }

    case actionTypes.needs.storeOwnedActiveUris: {
      const needUris = action.payload.get("uris");
      needUris &&
        needUris.forEach(needUri => {
          processState = updateNeedProcess(processState, needUri, {
            loading: true, //FIXME: once we dont actually retrieve the needs right after this dispatch we need to set "toLoad" instead of loading
          });
        });
      return processState;
    }

    case actionTypes.needs.storeOwnedInactiveUris: {
      const needUris = action.payload.get("uris");
      needUris &&
        needUris.forEach(needUri => {
          processState = updateNeedProcess(processState, needUri, {
            toLoad: true,
          });
        });
      return processState;
    }

    case actionTypes.personas.storeTheirUrisInLoading:
    case actionTypes.needs.storeTheirUrisInLoading: {
      const needUris = action.payload.get("uris");
      needUris &&
        needUris.forEach(needUri => {
          processState = updateNeedProcess(processState, needUri, {
            toLoad: false,
            loading: true,
          });
        });
      return processState;
    }

    case actionTypes.needs.delete:
      return processState.deleteIn(["needs", action.payload.ownNeedUri]);

    default:
      return processState;
  }
}
