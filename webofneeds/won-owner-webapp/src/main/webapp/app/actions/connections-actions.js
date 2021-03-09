/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import vocab from "../service/vocab.js";
import { createAtomFromDraftAndDispatch } from "~/app/actions/atoms-actions";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as ownerApi from "../api/owner-api.js";
import * as stateStore from "../redux/state-store.js";

import {
  getIn,
  getUri,
  extractAtomUriBySocketUri,
  parseWorkerError,
} from "../utils.js";

import { checkLoginState } from "./account-actions";

import { actionTypes, actionCreators } from "./actions.js";

import {
  buildCloseMessage,
  buildChatMessage,
  buildConnectMessage,
} from "../won-message-utils.js";

export const connectionsChatMessageClaimOnSuccess = (
  chatMessage,
  additionalContent,
  senderSocketUri,
  targetSocketUri,
  connectionUri
) => dispatch => {
  const ownedAtomUri = extractAtomUriBySocketUri(senderSocketUri);

  let referencedContentUris = undefined;
  let messageUriToClaim = undefined;

  buildChatMessage({
    chatMessage: chatMessage,
    additionalContent: additionalContent,
    referencedContentUris: undefined,
    socketUri: senderSocketUri,
    targetSocketUri: targetSocketUri,
    isTTL: false,
  })
    .then(message => ownerApi.sendMessage(message))
    .then(jsonResp => {
      return won
        .wonMessageFromJsonLd(
          jsonResp.message,
          vocab.WONMSG.uriPlaceholder.message
        )
        .then(wonMessage => {
          dispatch({
            type: actionTypes.connections.sendChatMessage,
            payload: {
              messageUri: jsonResp.messageUri,
              message: jsonResp.message,
              optimisticEvent: wonMessage,
              senderSocketUri: senderSocketUri,
              targetSocketUri: targetSocketUri,
              connectionUri,
              atomUri: ownedAtomUri,
              claimed: true, // message needs to be marked as claimed directly
            },
          });

          // Send claim message
          let contentUris = [];
          messageUriToClaim = jsonResp.messageUri;
          if (messageUriToClaim)
            contentUris.push({
              "@id": messageUriToClaim,
            });
          referencedContentUris = new Map();
          referencedContentUris.set("claims", contentUris);
          return buildChatMessage({
            chatMessage: undefined,
            additionalContent: undefined,
            referencedContentUris: referencedContentUris,
            optimisticEvent: wonMessage,
            socketUri: senderSocketUri,
            targetSocketUri: targetSocketUri,
            isTTL: false,
          })
            .then(message => ownerApi.sendMessage(message))
            .then(jsonResp =>
              won
                .wonMessageFromJsonLd(
                  jsonResp.message,
                  vocab.WONMSG.uriPlaceholder.message
                )
                .then(wonMessage =>
                  dispatch({
                    type: referencedContentUris
                      ? actionTypes.connections
                          .sendChatMessageRefreshDataOnSuccess //If there are references in the message we need to Refresh the Data from the backend on msg success
                      : actionTypes.connections.sendChatMessage,
                    payload: {
                      messageUri: jsonResp.messageUri,
                      message: jsonResp.message,
                      wonMessage,
                      senderSocketUri: senderSocketUri,
                      targetSocketUri: targetSocketUri,
                      connectionUri,
                    },
                  })
                )
            );
        });
    })
    .catch(e => {
      console.error("Error while processing chat message: ", e);
      dispatch({
        type: actionTypes.connections.sendChatMessageFailed,
        payload: {
          error: e,
          message: e.message,
        },
      });
    });
};

export const connectionsChatMessage = (
  chatMessage,
  additionalContent,
  referencedContent,
  senderSocketUri,
  targetSocketUri,
  connectionUri,
  isRDF = false
) => (dispatch, getState) => {
  const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);
  const ownedAtom = getIn(getState(), ["atoms", senderAtomUri]);

  let referencedContentUris = undefined;
  if (referencedContent) {
    referencedContentUris = new Map();
    referencedContent.forEach((referencedMessages, key) => {
      let contentUris = [];

      referencedMessages.map(msg => {
        const correctUri = getUri(msg);
        if (correctUri)
          contentUris.push({
            "@id": correctUri,
          });
        //THE PARTS BELOW SHOULD NOT BE CALLED WITHIN THIS DISPATCH
        switch (key) {
          case "retracts":
            dispatch({
              type: actionTypes.connections.agreementData.markAsRetracted,
              payload: {
                messageUri: getUri(msg),
                connectionUri: connectionUri,
                atomUri: getUri(ownedAtom),
                retracted: true,
              },
            });
            break;
          case "rejects":
            dispatch({
              type: actionTypes.connections.agreementData.markAsRejected,
              payload: {
                messageUri: getUri(msg),
                connectionUri: connectionUri,
                atomUri: getUri(ownedAtom),
                rejected: true,
              },
            });
            break;
          case "proposesToCancel":
            dispatch({
              type:
                actionTypes.connections.agreementData.markAsCancellationPending,
              payload: {
                messageUri: getUri(msg),
                connectionUri: connectionUri,
                atomUri: getUri(ownedAtom),
                cancellationPending: true,
              },
            });
            break;
          case "accepts":
            dispatch({
              type: actionTypes.connections.agreementData.markAsAccepted,
              payload: {
                messageUri: getUri(msg),
                connectionUri: connectionUri,
                atomUri: getUri(ownedAtom),
                accepted: true,
              },
            });
            break;
          case "claims":
            dispatch({
              type: actionTypes.connections.agreementData.markAsClaimed,
              payload: {
                messageUri: getUri(msg),
                connectionUri: connectionUri,
                atomUri: getUri(ownedAtom),
                claimed: true,
              },
            });
            break;
          case "proposes":
            dispatch({
              type: actionTypes.connections.agreementData.markAsProposed,
              payload: {
                messageUri: getUri(msg),
                connectionUri: connectionUri,
                atomUri: getUri(ownedAtom),
                proposed: true,
              },
            });
            break;
          default:
            console.error("referenced key/type is not valid: ", key);
            break;
        }
      });
      referencedContentUris.set(key, contentUris);
    });
  }

  buildChatMessage({
    chatMessage: chatMessage,
    additionalContent: additionalContent,
    referencedContentUris: referencedContentUris,
    socketUri: senderSocketUri,
    targetSocketUri: targetSocketUri,
    isRDF,
  })
    .then(message => ownerApi.sendMessage(message))
    .then(jsonResp =>
      won
        .wonMessageFromJsonLd(
          jsonResp.message,
          vocab.WONMSG.uriPlaceholder.message
        )
        .then(wonMessage =>
          dispatch({
            type: referencedContentUris
              ? actionTypes.connections.sendChatMessageRefreshDataOnSuccess //If there are references in the message we need to Refresh the Data from the backend on msg success
              : actionTypes.connections.sendChatMessage,
            payload: {
              messageUri: jsonResp.messageUri,
              message: jsonResp.message,
              optimisticEvent: wonMessage,
              senderSocketUri: senderSocketUri,
              targetSocketUri: targetSocketUri,
            },
          })
        )
    )
    .catch(e => {
      console.error("Error while processing chat message: ", e);
      dispatch({
        type: actionTypes.connections.sendChatMessageFailed,
        payload: {
          error: e,
          message: e.message,
        },
      });
    });
};

export const connectionsConnectReactionAtom = (
  connectToAtomUri,
  atomDraft,
  persona,
  connectToSocketType,
  atomDraftSocketType,
  callback
) => (dispatch, getState) =>
  connectReactionAtom(
    connectToAtomUri,
    atomDraft,
    persona,
    connectToSocketType,
    atomDraftSocketType,
    dispatch,
    getState,
    callback
  ); // moved to separate function to make transpilation work properly

/**
 * Sends a connect and creates a connectMessage from senderSocketUri to targetSocketUri
 * if isTargetOwned is set to true, the connect will be made serverside (open connection on both ends)
 * if isSenderPending is set to true, we know that the senderAtom might not be created yet, and thus
 * wait 2000ms before sending the connectMessage to the node (this only happens in the case of !isTargetAtomOwned
 * @param senderSocketUri
 * @param targetSocketUri
 * @param connectMessage
 * @param isTargetOwned
 * @param dispatch
 * @param isSenderPending
 * @returns {Promise<unknown>|*}
 */
export const connectAtomSockets = (
  senderSocketUri,
  targetSocketUri,
  connectMessage,
  isTargetOwned,
  dispatch,
  isSenderPending = false
) => {
  if (!senderSocketUri) {
    throw new Error("SenderSocketUri not present");
  }

  if (!targetSocketUri) {
    throw new Error("TargetSocketUri not present");
  }

  if (isTargetOwned || isSenderPending) {
    return ownerApi
      .serverSideConnect(
        senderSocketUri,
        targetSocketUri,
        isSenderPending,
        false,
        isTargetOwned, //if target is Owned we set autoOpen to true
        connectMessage
      )
      .catch(err => {
        const error = parseWorkerError(err);
        console.error("ServerSideConnect failed", error);
      });
  } else {
    // establish connection
    const cnctMsg = buildConnectMessage({
      connectMessage: connectMessage,
      socketUri: senderSocketUri,
      targetSocketUri: targetSocketUri,
    });

    return ownerApi.sendMessage(cnctMsg).then(jsonResp =>
      won
        .wonMessageFromJsonLd(
          jsonResp.message,
          vocab.WONMSG.uriPlaceholder.message
        )
        .then(wonMessage =>
          dispatch({
            type: actionTypes.atoms.connectSockets,
            payload: {
              messageUri: jsonResp.messageUri,
              message: jsonResp.message,
              optimisticEvent: wonMessage,
              senderSocketUri: senderSocketUri,
              targetSocketUri: targetSocketUri,
            },
          })
        )
    );
  }
};

const connectReactionAtom = (
  connectToAtomUri,
  atomDraft,
  personaUri,
  connectToSocketType,
  atomDraftSocketType,
  dispatch,
  getState,
  callback
) => {
  return checkLoginState(dispatch, getState, state => {
    const nodeUri = generalSelectors.getDefaultNodeUri(state);

    const holder = generalSelectors.getAtom(personaUri)(state);

    if (personaUri && !holder) {
      console.warn(
        "Could not find holder with Uri: ",
        personaUri,
        ", holder not be stored in the state"
      );
    }

    const getSocketStringFromDraft = atomDraft => {
      const draftContent = atomDraft["content"];
      const draftSockets = draftContent["sockets"];

      if (draftSockets && atomDraftSocketType) {
        for (let socketKey in draftSockets) {
          if (draftSockets[socketKey] === atomDraftSocketType) {
            return socketKey;
          }
        }
      }
    };

    const connectToSocketUri = atomUtils.getSocketUri(
      generalSelectors.getAtom(connectToAtomUri)(state),
      connectToSocketType
    );

    return createAtomFromDraftAndDispatch(atomDraft, nodeUri, dispatch)
      .then(atomUri => connectHolderToCreatedAtomUri(holder, atomUri))
      .then(atomUri =>
        connectAtomSockets(
          `${atomUri}${getSocketStringFromDraft(atomDraft)}`,
          connectToSocketUri,
          "",
          generalSelectors.isAtomOwned(connectToAtomUri)(state),
          dispatch,
          true
        )
      )
      .then(() => callback && callback());
  });
};

export const connectionsConnectAdHoc = (
  connectToSocketUri,
  connectMessage,
  adHocUseCaseIdentifier,
  targetAtom,
  personaUriForAdHocAtom,
  callback
) => (dispatch, getState) =>
  connectAdHoc(
    connectToSocketUri,
    connectMessage,
    adHocUseCaseIdentifier,
    targetAtom,
    personaUriForAdHocAtom,
    dispatch,
    getState,
    callback
  ); // moved to separate function to make transpilation work properly

/**
 * Connect a given holderAtom to the atomUri for an atom that has
 * been created or is in the process of creation
 * @param holder
 * @param atomUri
 * @returns {*}
 */
export const connectHolderToCreatedAtomUri = (holder, atomUri) => {
  const personaHolderSocketUri = atomUtils.getSocketUri(
    holder,
    vocab.HOLD.HolderSocketCompacted
  );

  return personaHolderSocketUri
    ? ownerApi
        .serverSideConnect(
          personaHolderSocketUri,
          `${atomUri}#holdableSocket`,
          false,
          true,
          true //we set autoopen to true since we know that the holder and created atom both belong to the user
        )
        .catch(err => {
          const error = parseWorkerError(err);
          console.error("ServerSideConnect failed", error);
        })
        .then(() => atomUri)
    : atomUri;
};

const connectAdHoc = (
  connectToSocketUri,
  connectMessage,
  adHocUseCaseIdentifier,
  targetAtom,
  personaUriForAdHocAtom,
  dispatch,
  getState,
  callback
) => {
  return checkLoginState(dispatch, getState, state => {
    const nodeUri = generalSelectors.getDefaultNodeUri(state);

    const holder = generalSelectors.getAtom(personaUriForAdHocAtom)(getState());

    if (personaUriForAdHocAtom && !holder) {
      console.warn(
        "Could not find holder with Uri: ",
        personaUriForAdHocAtom,
        ", holder not be stored in the state"
      );
    }

    const atomDraft = useCaseUtils.getUseCase(adHocUseCaseIdentifier).draft;

    if (targetAtom) {
      // For some special create cases we move some content of the fromAtom to the createAtomDraft
      const contentTypes =
        getIn(targetAtom, ["content", "type"]) &&
        getIn(targetAtom, ["content", "type"])
          .toSet()
          .remove(vocab.WON.AtomCompacted);

      if (
        contentTypes.includes("s:PlanAction") ||
        contentTypes.includes(vocab.WXPERSONA.InterestCompacted) ||
        contentTypes.includes(vocab.WXPERSONA.ExpertiseCompacted)
      ) {
        const eventObjectAboutUris = getIn(targetAtom, [
          "content",
          "eventObjectAboutUris",
        ]);
        if (eventObjectAboutUris) {
          atomDraft.content.eventObjectAboutUris = eventObjectAboutUris.toArray();
        }
      }
    }
    const connectToAtomUri = extractAtomUriBySocketUri(connectToSocketUri);

    return createAtomFromDraftAndDispatch(atomDraft, nodeUri, dispatch)
      .then(atomUri => connectHolderToCreatedAtomUri(holder, atomUri))
      .then(atomUri =>
        connectAtomSockets(
          `${atomUri}#chatSocket`,
          connectToSocketUri,
          connectMessage,
          generalSelectors.isAtomOwned(connectToAtomUri)(getState()),
          dispatch,
          true
        )
      )
      .then(() => callback && callback());
  });
};

export const connectionsClose = connectionUri => (dispatch, getState) => {
  const ownedAtom = generalSelectors.getAtomByConnectionUri(connectionUri)(
    getState()
  );

  const connection = atomUtils.getConnection(ownedAtom, connectionUri);
  const socketUri = connectionUtils.getSocketUri(connection);
  const targetSocketUri = connectionUtils.getTargetSocketUri(connection);

  buildCloseMessage(socketUri, targetSocketUri)
    .then(message => ownerApi.sendMessage(message))
    .then(jsonResp => {
      dispatch({
        type: actionTypes.connections.close,
        payload: {
          connectionUri: connectionUri,
          messageUri: jsonResp.messageUri,
          message: jsonResp.message,
        },
      });
    });
};

export const connectionsCloseRemote = message =>
  //Closes the 'targetConnection' again, if closeConnections(...) only closes the 'own' connection
  dispatch => {
    const socketUri = message.getSenderSocket();
    const targetSocketUri = message.getTargetSocket();

    buildCloseMessage(socketUri, targetSocketUri)
      .then(message => ownerApi.sendMessage(message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            messageUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );
      });
  };

/**
 * @param connectionUri
 * @param numberOfEvents
 *   The approximate number of chat-message
 *   that the view atoms. Note that the
 *   actual number varies due the varying number
 *   of success-responses the server includes and
 *   because the API only accepts a count of
 *   events that include the latter.
 * @return {Function}
 */
export const showLatestMessages = (connectionUri, numberOfEvents) => (
  dispatch,
  getState
) => {
  const state = getState();
  const atom =
    connectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)(state);
  const atomUri = getUri(atom);
  const connection = atomUtils.getConnection(atom, connectionUri);
  const processState = generalSelectors.getProcessState(state);
  if (
    !connectionUri ||
    !connection ||
    processUtils.isConnectionLoading(processState, connectionUri) ||
    processUtils.isConnectionLoadingMessages(processState, connectionUri)
  ) {
    return Promise.resolve(); //only load if not already started and connection itself not loading
  }

  return stateStore
    .determineRequestCredentials(state, atomUri)
    .then(requestCredentials =>
      stateStore.fetchMessages(
        dispatch,
        state,
        connectionUri,
        requestCredentials,
        numberOfEvents
      )
    );
};

/**
 * @param connectionUri
 * @param numberOfEvents
 *   The approximate number of chat-message
 *   that the view needs. Note that the
 *   actual number varies due the varying number
 *   of success-responses the server includes and
 *   because the API only accepts a count of
 *   events that include the latter.
 * @return {Function}
 */
export const showMoreMessages = (connectionUri, numberOfEvents) => (
  dispatch,
  getState
) => {
  const state = getState();
  const atom =
    connectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)(state);
  const atomUri = getUri(atom);
  const connection = atomUtils.getConnection(atom, connectionUri);
  const processState = generalSelectors.getProcessState(state);
  if (
    !connection ||
    processUtils.isConnectionLoading(processState, connectionUri) ||
    processUtils.isConnectionLoadingMessages(processState, connectionUri)
  ) {
    return; //only load if not already started and connection itself not loading
  }
  const resumeAfterUri = processUtils.getResumeAfterUriForConnection(
    processState,
    connectionUri
  );
  return stateStore
    .determineRequestCredentials(getState(), atomUri)
    .then(requestCredentials =>
      stateStore.fetchMessages(
        dispatch,
        state,
        connectionUri,
        requestCredentials,
        numberOfEvents,
        resumeAfterUri
      )
    );
};

export const markAsRetracted = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    retracted: wonMessage.retracted,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsRetracted,
    payload: payload,
  });
};

export const markAsRejected = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    rejected: wonMessage.rejected,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsRejected,
    payload: payload,
  });
};

export const markAsProposed = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    proposed: wonMessage.proposed,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsProposed,
    payload: payload,
  });
};

export const markAsClaimed = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    claimed: wonMessage.claimed,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsClaimed,
    payload: payload,
  });
};

export const markAsAccepted = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    accepted: wonMessage.accepted,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsAccepted,
    payload: payload,
  });
};

export const markAsAgreed = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    agreed: wonMessage.agreed,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsAgreed,
    payload: payload,
  });
};

export const markAsCancelled = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    cancelled: wonMessage.cancelled,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsCancelled,
    payload: payload,
  });
};

export const markAsCancellationPending = wonMessage => dispatch => {
  const payload = {
    messageUri: wonMessage.messageUri,
    connectionUri: wonMessage.connectionUri,
    atomUri: wonMessage.atomUri,
    cancellationPending: wonMessage.cancellationPending,
  };

  dispatch({
    type: actionTypes.connections.agreementData.markAsCancellationPending,
    payload: payload,
  });
};
