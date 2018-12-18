/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getIn, get } from "../utils.js";
import { parseConnection } from "./need-reducer/parse-connection.js";
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

/*function updateConnectionProcess(processState, connUri, payload) {
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
}*/

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
      const needUri = get(parseNeed(action.payload.need), "uri");

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
        const needUri = get(parseNeed(suggestedPost), "uri");

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
      return processState
        .setIn(
          ["connections", action.payload.get("connUri"), "failedToStore"],
          true
        )
        .setIn(
          ["connections", action.payload.get("connUri"), "loading"],
          false
        );
    }

    case actionTypes.connections.setLoadingMessages: {
      const loadingMessages = action.payload.loadingMessages;
      const connUri = action.payload.connectionUri;

      return processState.setIn(
        ["connections", connUri, "loadingMessages"],
        loadingMessages
      );
    }

    case actionTypes.reconnect.startingToLoadConnectionData:
    case actionTypes.reconnect.receivedConnectionData:
    case actionTypes.reconnect.connectionFailedToLoad:
    case actionTypes.connections.showLatestMessages:
    case actionTypes.connections.showMoreMessages: {
      const loadingMessages = action.payload.get("loadingMessages");
      const connUri = action.payload.get("connectionUri");

      if (loadingMessages && connUri) {
        processState = processState.setIn(
          ["connections", connUri, "loadingMessages"],
          true
        );
      }

      const loadedMessages = action.payload.get("events");
      if (loadedMessages) {
        processState = processState.setIn(
          ["connections", connUri, "loadingMessages"],
          false
        );
      }
      const error = action.payload.get("error");

      if (error && connUri) {
        processState = processState.setIn(
          ["connections", connUri, "loadingMessages"],
          false
        );
      }

      return processState;
    }

    case actionTypes.connections.setLoadingPetriNetData: {
      const loadingPetriNetData = action.payload.loadingPetriNetData;
      const connUri = action.payload.connectionUri;

      return processState
        .setIn(
          ["connections", connUri, "petriNetData", "loading"],
          loadingPetriNetData
        )
        .setIn(
          ["connections", connUri, "petriNetData", "dirty"],
          loadingPetriNetData
        );
    }

    case actionTypes.connections.sendChatMessageClaimOnSuccess:
    case actionTypes.connections.sendChatMessageRefreshDataOnSuccess: {
      const connUri = action.payload.optimisticEvent.getSender();

      return processState.setIn(
        ["connections", connUri, "petriNetData", "dirty"],
        true
      );
    }

    case actionTypes.connections.updatePetriNetData: {
      const petriNetData = action.payload.petriNetData;
      const connUri = action.payload.connectionUri;

      if (!connUri || !petriNetData) {
        return processState;
      }

      return processState
        .setIn(["connections", connUri, "petriNetData", "loading"], false)
        .setIn(["connections", connUri, "petriNetData", "dirty"], false)
        .setIn(["connections", connUri, "petriNetData", "loaded"], true);
    }

    case actionTypes.connections.updateAgreementData: {
      const agreementData = action.payload.agreementData;
      const connUri = action.payload.connectionUri;

      if (!connUri || !agreementData) {
        return processState;
      }

      return processState
        .setIn(["connections", connUri, "agreementData", "loaded"], true)
        .setIn(["connections", connUri, "agreementData", "loading"], false);
    }

    case actionTypes.connections.setLoadingAgreementData: {
      const connUri = action.payload.connectionUri;
      const loadingAgreementData = action.payload.loadingAgreementData;

      return processState.setIn(
        ["connections", connUri, "agreementData", "loading"],
        loadingAgreementData
      );
    }

    case actionTypes.connections.storeActiveUrisInLoading: {
      const connUris = action.payload.get("connUris");

      connUris &&
        connUris.forEach(connUri => {
          processState = processState.setIn(
            ["connections", connUri, "loading"],
            true
          );
        });
      return processState;
    }

    case actionTypes.messages.connectMessageSent:
    case actionTypes.messages.openMessageSent: {
      const parsedConnection = parseConnection(action.payload.connection);

      return processState.setIn(
        ["connections", parsedConnection.getIn(["data", "uri"]), "loading"],
        false
      );
    }

    case actionTypes.messages.openMessageReceived:
    case actionTypes.messages.connectMessageReceived: {
      //FIXME: This does not include the remotePersona yet (receiving connect or open requests from a non known remoteNeed will not load the personas for now
      const connUri = getIn(parseConnection(action.payload.connection), [
        "data",
        "uri",
      ]);
      if (!connUri) {
        return processState;
      }

      const remoteNeedUri = get(parseNeed(action.payload.remoteNeed), "uri");

      processState = updateNeedProcess(processState, remoteNeedUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
      });

      return processState.setIn(["connections", connUri, "loading"], false);
    }

    case actionTypes.messages.hintMessageReceived: {
      const {
        ownedNeed,
        remoteNeed,
        connection,
        ownPersona,
        remotePersona,
      } = action.payload;

      const connUri = getIn(parseConnection(connection), ["data", "uri"]);

      if (!connUri) {
        return processState;
      }

      const ownedNeedUri = get(parseNeed(ownedNeed), "uri");
      const remoteNeedUri = get(parseNeed(remoteNeed), "uri");
      const ownPersonaUri = get(parseNeed(ownPersona), "uri");
      const remotePersonaUri = get(parseNeed(remotePersona), "uri");

      processState = updateNeedProcess(processState, ownedNeedUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
      });

      processState = updateNeedProcess(processState, remoteNeedUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
      });

      processState = updateNeedProcess(processState, ownPersonaUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
      });

      processState = updateNeedProcess(processState, remotePersonaUri, {
        toLoad: false,
        failedToLoad: false,
        loading: false,
      });

      return processState.setIn(["connections", connUri, "loading"], false);
    }

    case actionTypes.messages.reopenNeed.failed:
    case actionTypes.messages.closeNeed.failed: {
      let connections = action.payload.connections;

      connections &&
        connections.keySeq().forEach(connUri => {
          processState = processState.setIn(
            ["connections", connUri, "loading"],
            false
          );
        });
      return processState;
    }

    case actionTypes.connections.storeActive: {
      let connections = action.payload.get("connections");

      connections &&
        connections.keySeq().forEach(connUri => {
          processState = processState.setIn(
            ["connections", connUri, "loading"],
            false
          );
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
