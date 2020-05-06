/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import vocab from "../service/vocab.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../api/owner-api.js";
import * as stateStore from "../redux/state-store.js";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors.js";

import { get, getIn } from "../utils.js";

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
  senderSocketUri,
  targetSocketUri,
  connectionUri
) {
  return dispatch => {
    const ownedAtomUri = generalSelectors.getAtomUriBySocketUri(
      senderSocketUri
    );

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
            vocab.WONMSG.uriPlaceholder.event
          )
          .then(wonMessage => {
            dispatch({
              type: actionTypes.connections.sendChatMessage,
              payload: {
                eventUri: jsonResp.messageUri,
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
                    vocab.WONMSG.uriPlaceholder.event
                  )
                  .then(wonMessage =>
                    dispatch({
                      type: referencedContentUris
                        ? actionTypes.connections
                            .sendChatMessageRefreshDataOnSuccess //If there are references in the message we need to Refresh the Data from the backend on msg success
                        : actionTypes.connections.sendChatMessage,
                      payload: {
                        eventUri: jsonResp.messageUri,
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
}

export function connectionsChatMessage(
  chatMessage,
  additionalContent,
  referencedContent,
  senderSocketUri,
  targetSocketUri,
  connectionUri,
  isTTL = false
) {
  return (dispatch, getState) => {
    const senderAtomUri = generalSelectors.getAtomUriBySocketUri(
      senderSocketUri
    );
    const ownedAtom = getIn(getState(), ["atoms", senderAtomUri]);

    let referencedContentUris = undefined;
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
                type: actionTypes.connections.agreementData.markAsRetracted,
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
                type: actionTypes.connections.agreementData.markAsRejected,
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
                  actionTypes.connections.agreementData
                    .markAsCancellationPending,
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
                type: actionTypes.connections.agreementData.markAsAccepted,
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
                type: actionTypes.connections.agreementData.markAsClaimed,
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
                type: actionTypes.connections.agreementData.markAsProposed,
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
      socketUri: senderSocketUri,
      targetSocketUri: targetSocketUri,
      isTTL,
    })
      .then(message => ownerApi.sendMessage(message))
      .then(jsonResp =>
        won
          .wonMessageFromJsonLd(
            jsonResp.message,
            vocab.WONMSG.uriPlaceholder.event
          )
          .then(wonMessage =>
            dispatch({
              type: referencedContentUris
                ? actionTypes.connections.sendChatMessageRefreshDataOnSuccess //If there are references in the message we need to Refresh the Data from the backend on msg success
                : actionTypes.connections.sendChatMessage,
              payload: {
                eventUri: jsonResp.messageUri,
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
}

export function connectionsConnectReactionAtom(
  connectToAtomUri,
  atomDraft,
  persona,
  connectToSocketType,
  atomDraftSocketType,
  history
) {
  return (dispatch, getState) =>
    connectReactionAtom(
      connectToAtomUri,
      atomDraft,
      persona,
      connectToSocketType,
      atomDraftSocketType,
      history,
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
  history,
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

        history.push("/connections");
      })
      .then(() => {
        // add persona if present
        if (personaUri) {
          const persona = getIn(state, ["atoms", personaUri]);
          const senderSocketUri = atomUtils.getSocketUri(
            persona,
            vocab.HOLD.HolderSocketCompacted
          );
          const targetSocketUri = `${atomUri}#holdableSocket`;
          ownerApi
            .serverSideConnect(senderSocketUri, targetSocketUri, false, true)
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

        const atomDraftSocketType = getSocketFromDraft(atomDraft);

        if (generalSelectors.isAtomOwned(state, connectToAtomUri)) {
          const targetSocketUri = connectToSocketType
            ? atomUtils.getSocketUri(connectToAtom, connectToSocketType)
            : atomUtils.getDefaultSocketUri(connectToAtom);

          if (atomDraftSocketType && targetSocketUri) {
            const senderSocketUri = `${atomUri}${atomDraftSocketType}`;

            ownerApi
              .serverSideConnect(targetSocketUri, senderSocketUri, false, true)
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
          const senderSocketUri = `${atomUri}${atomDraftSocketType}`;
          const targetSocketUri = connectToSocketType
            ? atomUtils.getSocketUri(connectToAtom, connectToSocketType)
            : atomUtils.getDefaultSocketUri(connectToAtom);

          // establish connection
          const cnctMsg = buildConnectMessage({
            connectMessage: "",
            socketUri: senderSocketUri,
            targetSocketUri: targetSocketUri,
          });

          ownerApi.sendMessage(cnctMsg).then(jsonResp =>
            // connect action to be dispatched when the
            // ad hoc atom has been created:

            won
              .wonMessageFromJsonLd(
                jsonResp.message,
                vocab.WONMSG.uriPlaceholder.event
              )
              .then(wonMessage =>
                dispatch({
                  type: actionTypes.atoms.connectSockets,
                  payload: {
                    eventUri: jsonResp.messageUri,
                    message: jsonResp.message,
                    optimisticEvent: wonMessage,
                    senderSocketUri: senderSocketUri,
                    targetSocketUri: targetSocketUri,
                  },
                })
              )
          );
        }
      });
  });
}

export function connectionsConnectAdHoc(targetSocketUri, message, personaUri) {
  return (dispatch, getState) =>
    connectAdHoc(targetSocketUri, message, personaUri, dispatch, getState); // moved to separate function to make transpilation work properly
}

function connectAdHoc(
  targetSocketUri,
  message,
  personaUri,
  dispatch,
  getState
) {
  ensureLoggedIn(dispatch, getState).then(async () => {
    const theirAtomUri = generalSelectors.getAtomUriBySocketUri(
      targetSocketUri
    );

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
          const senderSocketUri = atomUtils.getSocketUri(
            persona,
            vocab.HOLD.HolderSocketCompacted
          );
          const targetSocketUri = `${atomUri}#holdableSocket`;

          const response = await ownerApi.serverSideConnect(
            senderSocketUri,
            targetSocketUri,
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
        let senderSocketUri = `${atomUri}#chatSocket`;

        // establish connection
        const cnctMsg = buildConnectMessage({
          connectMessage: message,
          socketUri: senderSocketUri,
          targetSocketUri: targetSocketUri,
        });

        ownerApi.sendMessage(cnctMsg).then(jsonResp =>
          won
            .wonMessageFromJsonLd(
              jsonResp.message,
              vocab.WONMSG.uriPlaceholder.event
            )
            .then(wonMessage =>
              dispatch({
                type: actionTypes.atoms.connectSockets,
                payload: {
                  eventUri: jsonResp.messageUri,
                  message: jsonResp.message,
                  optimisticEvent: wonMessage,
                  senderSocketUri: senderSocketUri,
                  targetSocketUri: targetSocketUri,
                },
              })
            )
        );
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
      .then(message => ownerApi.sendMessage(message))
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
      .then(message => ownerApi.sendMessage(message))
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
      .then(message => ownerApi.sendMessage(message))
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
export function showLatestMessages(connectionUri, numberOfEvents) {
  return (dispatch, getState) => {
    const state = getState();
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

    return stateStore.fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents
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

  stateStore.fetchMessages(
    dispatch,
    state,
    connectionUri,
    atomUri,
    numberOfEvents
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
export function showMoreMessages(connectionUri, numberOfEvents) {
  return (dispatch, getState) => {
    const state = getState();
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

    return stateStore.fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents,
      oldestMessageUri
    );
  };
}
export function markAsRetracted(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      retracted: event.retracted,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsRetracted,
      payload: payload,
    });
  };
}
export function markAsRejected(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      rejected: event.rejected,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsRejected,
      payload: payload,
    });
  };
}

export function markAsProposed(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      proposed: event.proposed,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsProposed,
      payload: payload,
    });
  };
}

export function markAsClaimed(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      claimed: event.claimed,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsClaimed,
      payload: payload,
    });
  };
}

export function markAsAccepted(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      accepted: event.accepted,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsAccepted,
      payload: payload,
    });
  };
}

export function markAsAgreed(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      agreed: event.agreed,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsAgreed,
      payload: payload,
    });
  };
}

export function markAsCancelled(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      cancelled: event.cancelled,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsCancelled,
      payload: payload,
    });
  };
}

export function markAsCancellationPending(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      cancellationPending: event.cancellationPending,
    };

    dispatch({
      type: actionTypes.connections.agreementData.markAsCancellationPending,
      payload: payload,
    });
  };
}
