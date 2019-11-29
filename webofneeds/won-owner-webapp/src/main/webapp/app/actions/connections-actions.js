/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../api/owner-api.js";
import * as stateStore from "../redux/state-store.js";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors.js";

import { get, getIn, numOfEvts2pageSize } from "../utils.js";

import { ensureLoggedIn } from "./account-actions";

import { actionTypes, actionCreators } from "./actions.js";

import {
  buildCreateMessage,
  buildCloseMessage,
  buildChatMessage,
  buildRateMessage,
  buildConnectMessage,
} from "../won-message-utils.js";

import * as processUtils from "../redux/utils/process-utils.js";

export function connectionsChatMessageClaimOnSuccess(
  chatMessage,
  additionalContent,
  connectionUri
) {
  return (dispatch, getState) => {
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const socketUri = getIn(ownedAtom, ["connectionUri", "socketUri"]);
    const targetSocketUri = getIn(ownedAtom, [
      "connectionUri",
      "targetSocketUri",
    ]);

    buildChatMessage({
      chatMessage: chatMessage,
      additionalContent: additionalContent,
      referencedContentUris: undefined,
      socketUri: socketUri,
      targetSocketUri: targetSocketUri,
      isTTL: false,
    })
      .then(msgData =>
        Promise.all([
          won.wonMessageFromJsonLd(msgData.message),
          ownerApi.sendMessage(msgData.message),
        ])
      )
      .then(([optimisticEvent, jsonResp]) => {
        dispatch({
          type: actionTypes.connections.sendChatMessageClaimOnSuccess,
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            optimisticEvent,
          },
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
}

export function connectionsChatMessage(
  chatMessage,
  additionalContent,
  referencedContent,
  connectionUri,
  isTTL = false
) {
  return (dispatch, getState) => {
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const socketUri = getIn(ownedAtom, ["connectionUri", "socketUri"]);
    const targetSocketUri = getIn(ownedAtom, [
      "connectionUri",
      "targetSocketUri",
    ]);

    let referencedContentUris = undefined;
    /*TODO: Since we set messages to be (successfully) claimed/proposed/accepted... before we even know if the transition was successful we might
     need to rethink this implementation in favor of a dirtyState somehow, and remove the dirty state on successRemote of the message -> handling is in
     messages-actions.js (dispatchActionOnSuccessRemote part if(toRefreshData) ... but for now this will do*/
    if (referencedContent) {
      referencedContentUris = new Map();
      referencedContent.forEach((referencedMessages, key) => {
        let contentUris = [];

        referencedMessages.map(msg => {
          const correctUri = get(msg, "uri");
          if (correctUri)
            contentUris.push({
              "@id": correctUri,
            });
          //THE PARTS BELOW SHOULD NOT BE CALLED WITHIN THIS DISPATCH
          switch (key) {
            case "retracts":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsRetracted,
                payload: {
                  messageUri: get(msg, "uri"),
                  connectionUri: connectionUri,
                  atomUri: get(ownedAtom, "uri"),
                  retracted: true,
                },
              });
              break;
            case "rejects":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsRejected,
                payload: {
                  messageUri: get(msg, "uri"),
                  connectionUri: connectionUri,
                  atomUri: get(ownedAtom, "uri"),
                  rejected: true,
                },
              });
              break;
            case "proposesToCancel":
              dispatch({
                type:
                  actionTypes.messages.messageStatus.markAsCancellationPending,
                payload: {
                  messageUri: get(msg, "uri"),
                  connectionUri: connectionUri,
                  atomUri: get(ownedAtom, "uri"),
                  cancellationPending: true,
                },
              });
              break;
            case "accepts":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsAccepted,
                payload: {
                  messageUri: get(msg, "uri"),
                  connectionUri: connectionUri,
                  atomUri: get(ownedAtom, "uri"),
                  accepted: true,
                },
              });
              break;
            case "claims":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsClaimed,
                payload: {
                  messageUri: get(msg, "uri"),
                  connectionUri: connectionUri,
                  atomUri: get(ownedAtom, "uri"),
                  claimed: true,
                },
              });
              break;
            case "proposes":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsProposed,
                payload: {
                  messageUri: get(msg, "uri"),
                  connectionUri: connectionUri,
                  atomUri: get(ownedAtom, "uri"),
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
      socketUri: socketUri,
      targetSocketUri: targetSocketUri,
      isTTL,
    })
      .then(msgData =>
        Promise.all([
          won.wonMessageFromJsonLd(msgData.message),
          ownerApi.sendMessage(msgData.message),
        ])
      )
      .then(([optimisticEvent, jsonResp]) => {
        dispatch({
          type: referencedContentUris
            ? actionTypes.connections.sendChatMessageRefreshDataOnSuccess //If there are references in the message we need to Refresh the Data from the backend on msg success
            : actionTypes.connections.sendChatMessage,
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            optimisticEvent,
          },
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
}

export function connectionsConnectReactionAtom(
  connectToAtomUri,
  atomDraft,
  persona,
  connectToSocketType,
  atomDraftSocketType
) {
  return (dispatch, getState) =>
    connectReactionAtom(
      connectToAtomUri,
      atomDraft,
      persona,
      connectToSocketType,
      atomDraftSocketType,
      dispatch,
      getState
    ); // moved to separate function to make transpilation work properly
}

function connectReactionAtom(
  connectToAtomUri,
  atomDraft,
  personaUri,
  connectToSocketType,
  atomDraftSocketType,
  dispatch,
  getState
) {
  ensureLoggedIn(dispatch, getState).then(async () => {
    const state = getState();
    const connectToAtom = getIn(state, ["atoms", connectToAtomUri]);

    const nodeUri = getIn(state, ["config", "defaultNodeUri"]);

    // create new atom
    const { message, atomUri } = await buildCreateMessage(atomDraft, nodeUri);

    // create the new atom
    ownerApi
      .sendMessage(message)
      .then(jsonResp => {
        dispatch({
          type: actionTypes.atoms.create, // TODO custom action
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            atomUri: atomUri,
            atom: atomDraft,
          },
        });

        dispatch(
          actionCreators.router__stateGo("connections", {
            useCase: undefined,
            useCaseGroup: undefined,
            fromAtomUri: undefined,
            viewConnUri: undefined,
            mode: undefined,
          })
        );
      })
      .then(() => {
        // add persona if present
        if (personaUri) {
          const persona = getIn(state, ["atoms", personaUri]);
          ownerApi
            .serverSideConnect(
              atomUtils.getSocketUri(persona, won.HOLD.HolderSocketCompacted),
              `${atomUri}#holdableSocket`,
              false,
              true
            )
            .then(async response => {
              if (!response.ok) {
                const errorMsg = await response.text();
                throw new Error(`Could not connect identity: ${errorMsg}`);
              }
            });
        }

        const getSocketFromDraft = atomDraft => {
          const draftContent = atomDraft["content"];
          const draftSockets = draftContent["sockets"];

          if (draftSockets && atomDraftSocketType) {
            for (let socketKey in draftSockets) {
              if (draftSockets[socketKey] === atomDraftSocketType) {
                return socketKey;
              }
            }
          }

          const defaultSocket = draftContent["defaultSocket"];
          return defaultSocket && Object.keys(defaultSocket)[0];
        };

        const atomDraftSocketUri = getSocketFromDraft(atomDraft);

        if (generalSelectors.isAtomOwned(state, connectToAtomUri)) {
          const connectToSocketUri = connectToSocketType
            ? atomUtils.getSocketUri(connectToAtom, connectToSocketType)
            : atomUtils.getDefaultSocketUri(connectToAtom);

          if (atomDraftSocketUri && connectToSocketUri) {
            ownerApi
              .serverSideConnect(
                connectToSocketUri,
                `${atomUri}${atomDraftSocketUri}`,
                false,
                true
              )
              .then(async response => {
                if (!response.ok) {
                  const errorMsg = await response.text();
                  throw new Error(`Could not connect owned atoms: ${errorMsg}`);
                }
              });
          } else {
            throw new Error(
              `Could not connect owned atoms did not find necessary sockets`
            );
          }
        } else {
          const connectToSocketUri = connectToSocketType
            ? atomUtils.getSocketUri(connectToAtom, connectToSocketType)
            : atomUtils.getDefaultSocketUri(connectToAtom);

          // establish connection
          const cnctMsg = buildConnectMessage({
            connectMessage: "",
            socketUri: `${atomUri}${atomDraftSocketUri}`,
            targetSocketUri: connectToSocketUri,
          });

          won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent => {
            // connect action to be dispatched when the
            // ad hoc atom has been created:

            ownerApi.sendMessage(cnctMsg.message).then(jsonResp =>
              dispatch({
                type: actionTypes.atoms.connect,
                payload: {
                  eventUri: jsonResp.messageUri,
                  message: jsonResp.message,
                  optimisticEvent: optimisticEvent,
                  targetSocketUri: connectToSocketUri,
                  socketUri: `${atomUri}${atomDraftSocketUri}`,
                  atomUri: atomUri,
                  targetAtomUri: get(connectToAtom, "uri"),
                },
              })
            );
          });
        }
      });
  });
}

export function connectionsConnectAdHoc(
  theirAtomUri,
  textMessage,
  connectToSocketUri,
  persona
) {
  return (dispatch, getState) =>
    connectAdHoc(
      theirAtomUri,
      textMessage,
      connectToSocketUri,
      persona,
      dispatch,
      getState
    ); // moved to separate function to make transpilation work properly
}

function connectAdHoc(
  theirAtomUri,
  textMessage,
  connectToSocketUri,
  personaUri,
  dispatch,
  getState
) {
  ensureLoggedIn(dispatch, getState).then(async () => {
    const state = getState();
    const adHocDraft = {
      content: {
        responseToUri: theirAtomUri,
        flags: [
          "con:DirectResponse",
          "match:NoHintForCounterpart",
          "match:NoHintForMe",
        ],
      },
    };
    const nodeUri = getIn(state, ["config", "defaultNodeUri"]);

    // build create message for new atom
    const { message, atomUri } = await buildCreateMessage(adHocDraft, nodeUri);

    // create the new atom
    ownerApi
      .sendMessage(message)
      .then(jsonResp => {
        dispatch({
          type: actionTypes.atoms.create, // TODO custom action
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            atomUri: atomUri,
            atom: adHocDraft,
          },
        });
      })
      .then(async () => {
        // add persona
        if (personaUri) {
          const persona = getIn(state, ["atoms", personaUri]);
          const response = await ownerApi.serverSideConnect(
            atomUtils.getSocketUri(persona, won.HOLD.HolderSocketCompacted),
            `${atomUri}#holdableSocket`,
            false,
            true
          );
          if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(`Could not connect identity: ${errorMsg}`);
          }
        }
      })
      .then(() => {
        // set default socketUri
        let socketUri = `${atomUri}#chatSocket`;

        // establish connection
        const cnctMsg = buildConnectMessage({
          connectMessage: textMessage,
          socketUri: socketUri,
          targetSocketUri: connectToSocketUri,
        });

        won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent => {
          ownerApi.sendMessage(cnctMsg.message).then(jsonResp => {
            dispatch({
              type: actionTypes.atoms.connect,
              payload: {
                eventUri: jsonResp.messageUri,
                message: jsonResp.message,
                optimisticEvent: optimisticEvent,
                socketUri: socketUri,
                targetSocketUri: connectToSocketUri,
                atomUri: atomUri,
                targetAtomUri: theirAtomUri,
              },
            });
          });
        });
      });
  });
}

export function connectionsClose(connectionUri) {
  return (dispatch, getState) => {
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );

    const socketUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "socketUri",
    ]);
    const targetSocketUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetSocketUri",
    ]);

    buildCloseMessage(socketUri, targetSocketUri)
      .then(({ message }) => ownerApi.sendMessage(message))
      .then(jsonResp => {
        dispatch({
          type: actionTypes.connections.close,
          payload: {
            connectionUri: connectionUri,
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          },
        });
      });
  };
}

export function connectionsCloseRemote(message) {
  //Closes the 'targetConnection' again, if closeConnections(...) only closes the 'own' connection
  return dispatch => {
    const socketUri = message.getSenderSocket();
    const targetSocketUri = message.getTargetSocket();

    buildCloseMessage(socketUri, targetSocketUri)
      .then(closeMessage => ownerApi.sendMessage(closeMessage.message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );
      });
  };
}

export function connectionsRate(connectionUri, rating) {
  return (dispatch, getState) => {
    const state = getState();

    const ownedAtom = get(state, "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const theirAtomUri = getIn(state, [
      "atoms",
      get(ownedAtom, "uri"),
      "connections",
      connectionUri,
      "targetAtomUri",
    ]);
    const theirAtom = getIn(state, ["atoms", theirAtomUri]);
    const theirConnectionUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetConnectionUri",
    ]);

    won
      .getConnectionWithEventUris(connectionUri, {
        requesterWebId: get(ownedAtom, "uri"),
      })
      .then(connection => {
        let msgToRateFor = {
          connection: connection,
        };

        return buildRateMessage(
          msgToRateFor,
          get(ownedAtom, "uri"),
          theirAtomUri,
          get(ownedAtom, "nodeUri"),
          get(theirAtom, "nodeUri"),
          theirConnectionUri,
          rating
        );
      })
      .then(({ message }) => ownerApi.sendMessage(message))
      .then(jsonResp =>
        dispatch({
          type: actionTypes.connections.rate,
          payload: {
            connectionUri: connectionUri,
            rating: rating,
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          },
        })
      );
  };
}

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
export function showLatestMessages(connectionUriParam, numberOfEvents) {
  return (dispatch, getState) => {
    const state = getState();
    const connectionUri =
      connectionUriParam || generalSelectors.getConnectionUriFromRoute(state);
    const atom =
      connectionUri &&
      generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri);
    const atomUri = get(atom, "uri");
    const connection =
      connectionUri && getOwnedConnectionByUri(state, connectionUri);
    const processState = get(state, "process");
    if (
      !connectionUri ||
      !connection ||
      processUtils.isConnectionLoading(processState, connectionUri) ||
      processUtils.isConnectionLoadingMessages(processState, connectionUri)
    ) {
      return Promise.resolve(); //only load if not already started and connection itself not loading
    }

    const fetchParams = {
      requesterWebId: atomUri,
      pagingSize: numOfEvts2pageSize(numberOfEvents),
      deep: true,
    };
    return stateStore.fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents,
      fetchParams
    );
  };
}

export function loadLatestMessagesOfConnection({
  connectionUri,
  numberOfEvents,
  state,
  dispatch,
}) {
  const atom =
    connectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri);
  const atomUri = get(atom, "uri");
  const connection =
    connectionUri && getOwnedConnectionByUri(state, connectionUri);
  const processState = get(state, "process");
  if (
    !connectionUri ||
    !connection ||
    processUtils.isConnectionLoading(processState, connectionUri) ||
    processUtils.isConnectionLoadingMessages(processState, connectionUri)
  ) {
    return Promise.resolve(); //only load if not already started and connection itself not loading
  }

  const fetchParams = {
    requesterWebId: atomUri,
    pagingSize: numOfEvts2pageSize(numberOfEvents),
    deep: true,
  };

  stateStore.fetchMessages(
    dispatch,
    state,
    connectionUri,
    atomUri,
    numberOfEvents,
    fetchParams
  );
}

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
export function showMoreMessages(connectionUriParam, numberOfEvents) {
  return (dispatch, getState) => {
    const state = getState();
    const connectionUri =
      connectionUriParam || generalSelectors.getConnectionUriFromRoute(state);
    const atom =
      connectionUri &&
      generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri);
    const atomUri = get(atom, "uri");
    const connection = getIn(atom, ["connections", connectionUri]);
    const connectionMessages = get(connection, "messages");
    const processState = get(state, "process");
    if (
      !connection ||
      processUtils.isConnectionLoading(processState, connectionUri) ||
      processUtils.isConnectionLoadingMessages(processState, connectionUri)
    ) {
      return; //only load if not already started and connection itself not loading
    }

    // determine the oldest loaded event
    const sortedConnectionMessages = connectionMessages
      .valueSeq()
      .sort((msg1, msg2) => get(msg1, "date") - get(msg2, "date"));

    const oldestMessageUri = get(sortedConnectionMessages.first(), "uri");
    const messageHashValue =
      oldestMessageUri && oldestMessageUri.replace(/wm:\/(.*)/, "$1"); // everything following the `wm:/`

    const fetchParams = {
      requesterWebId: atomUri,
      pagingSize: numOfEvts2pageSize(numberOfEvents),
      deep: true,
      resumebefore: messageHashValue,
    };

    return stateStore.fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents,
      fetchParams
    );
  };
}
