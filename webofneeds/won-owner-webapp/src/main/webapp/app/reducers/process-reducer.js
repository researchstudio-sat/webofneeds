/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getIn } from "../utils.js";
import { parseNeed } from "./need-reducer/parse-need.js";
import { parseMessage } from "./need-reducer/parse-message.js";
import * as processUtils from "../process-utils.js";

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
  processingNeedUrisFromOwnerLoad: false,
  needs: Immutable.Map(),
  connections: Immutable.Map(),
});

export const emptyNeedProcess = Immutable.fromJS({
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
    case actionTypes.needs.edit: {
      const needUri = action.payload.needUri;

      if (needUri) {
        processState = updateNeedProcess(processState, needUri, {
          processUpdate: true,
        });
      }

      return processState;
    }

    case actionTypes.needs.loadAllActiveNeedUrisFromOwner:
      return processState.set("processingNeedUrisFromOwnerLoad", true);

    case actionTypes.needs.storeNeedUrisFromOwner: {
      const needUris = action.payload.get("uris");
      needUris &&
        needUris.forEach(needUri => {
          if (!processUtils.isNeedLoaded(processState, needUri)) {
            processState = updateNeedProcess(processState, needUri, {
              toLoad: true,
            });
          }
        });
      return processState.set("processingNeedUrisFromOwnerLoad", false);
    }

    case actionTypes.personas.create:
    case actionTypes.needs.create:
    case actionTypes.needs.whatsNew:
    case actionTypes.needs.whatsAround:
      return processState.set("processingPublish", true);

    case actionTypes.failedToGetLocation:
      return processState.set("processingPublish", false);

    case actionTypes.needs.editFailure: {
      console.debug(
        "process-reducer actionTypes.needs.editFailure todo: impl / payload-> ",
        action.payload
      );
      //TODO: IMPL
      return processState;
    }

    case actionTypes.needs.editSuccessful: {
      const needUri = action.payload.needUri;

      if (needUri) {
        processState = updateNeedProcess(processState, needUri, {
          processUpdate: false,
        });
      }

      return processState;
    }

    case actionTypes.needs.createSuccessful: {
      processState = updateNeedProcess(processState, action.payload.needUri, {
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

    case actionTypes.connections.storeUrisToLoad: {
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
            toLoad: false,
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
        needs.map(need => {
          const parsedNeed = parseNeed(need);
          processState = updateNeedProcess(
            processState,
            parsedNeed.get("uri"),
            {
              toLoad: false,
              failedToLoad: false,
              loading: false,
              loaded: true,
            }
          );

          const heldNeedUris = parsedNeed.get("holds");
          if (heldNeedUris.size > 0) {
            heldNeedUris.map(heldNeedUri => {
              if (!processUtils.isNeedLoaded(processState, heldNeedUri)) {
                processState = updateNeedProcess(processState, heldNeedUri, {
                  toLoad: true,
                });
              }
            });
          }

          const groupMemberUris = parsedNeed.get("groupMembers");
          if (groupMemberUris.size > 0) {
            groupMemberUris.map(groupMemberUri => {
              if (!processUtils.isNeedLoaded(processState, groupMemberUri)) {
                processState = updateNeedProcess(processState, groupMemberUri, {
                  toLoad: true,
                });
              }
            });
          }
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

    //Necessary to flag the originatorUri of a message as need toLoad if the need is not currently in the state yet (e.g new groupmember sends message)
    case actionTypes.messages.processConnectionMessage:
      return addOriginatorNeedToLoad(processState, action.payload);

    case actionTypes.needs.delete:
    case actionTypes.needs.removeDeleted:
    case actionTypes.personas.removeDeleted: {
      const needUri = action.payload.get("uri");
      return processState.deleteIn(["needs", needUri]);
    }

    default:
      return processState;
  }
}

/*
 "alreadyProcessed" flag, which indicates that we do not care about the
 sent status anymore and assume that it has been successfully sent to each server (incl. the remote)
 "insertIntoConnUri" and "insertIntoNeedUri" are used for forwardedMessages so that the message is
 stored within the given connection/need and not in the original need or connection as we might not
 have these stored in the state
 */
export function addOriginatorNeedToLoad(
  processState,
  wonMessage,
  alreadyProcessed = false,
  insertIntoConnUri = undefined,
  insertIntoNeedUri = undefined
) {
  // we used to exclude messages without content here, using
  // if (wonMessage.getContentGraphs().length > 0) as the condition
  // however, after moving the facet info of connect/open messages from
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
      insertIntoConnUri && insertIntoNeedUri
    );
    if (parsedMessage) {
      const connectionUri =
        insertIntoConnUri || parsedMessage.get("belongsToUri");

      let needUri = insertIntoNeedUri;
      if (!needUri && parsedMessage.getIn(["data", "outgoingMessage"])) {
        // needUri is the message's hasSenderNeed
        needUri = wonMessage.getSenderNeed();
      } else if (!needUri) {
        // needUri is the remote message's hasReceiverNeed
        needUri = wonMessage.getReceiverNeed();
      }

      const originatorUri = parsedMessage.getIn(["data", "originatorUri"]);

      if (originatorUri) {
        //Message is originally from another need, we might need to add the need as well
        if (!processUtils.isNeedLoaded(processState, originatorUri)) {
          console.debug(
            "Originator Need is not in the state yet, we need to add it"
          );
          processState = updateNeedProcess(processState, originatorUri, {
            toLoad: true,
            loading: false,
          });
        }
      }

      if (needUri) {
        const hasContainedForwardedWonMessages = wonMessage.hasContainedForwardedWonMessages();

        if (hasContainedForwardedWonMessages) {
          const containedForwardedWonMessages = wonMessage.getContainedForwardedWonMessages();
          containedForwardedWonMessages.map(forwardedWonMessage => {
            processState = addOriginatorNeedToLoad(
              processState,
              forwardedWonMessage,
              true,
              connectionUri,
              needUri
            );
            //PARSE MESSAGE DIFFERENTLY FOR FORWARDED MESSAGES
          });
        }
      }
    }
  }
  return processState;
}
