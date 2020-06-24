/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import vocab from "../service/vocab.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as wonUtils from "../won-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as ownerApi from "../api/owner-api.js";
import * as stateStore from "../redux/state-store.js";

import {
  get,
  getIn,
  generateFakePersonaName,
  extractAtomUriBySocketUri,
  delay,
} from "../utils.js";

import { ensureLoggedIn } from "./account-actions";

import { actionTypes, actionCreators } from "./actions.js";

import {
  buildCreateMessage,
  buildCloseMessage,
  buildChatMessage,
  buildRateMessage,
  buildConnectMessage,
} from "../won-message-utils.js";

export function connectionsChatMessageClaimOnSuccess(
  chatMessage,
  additionalContent,
  senderSocketUri,
  targetSocketUri,
  connectionUri
) {
  return dispatch => {
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
  isRDF = false
) {
  return (dispatch, getState) => {
    const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);
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
      isRDF,
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
    const connectToAtom = generalSelectors.getAtom(connectToAtomUri)(state);

    const nodeUri = generalSelectors.getDefaultNodeUri(state);

    // create new atom
    const { message, atomUri } = buildCreateMessage(atomDraft, nodeUri);

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
      })
      .then(() => {
        // add persona if present
        if (personaUri) {
          const persona = generalSelectors.getAtom(personaUri)(state);
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

        const atomDraftSocketTypeString = getSocketStringFromDraft(atomDraft);

        const targetSocketUri = atomUtils.getSocketUri(
          connectToAtom,
          connectToSocketType
        );
        const senderSocketUri =
          atomDraftSocketTypeString && `${atomUri}${atomDraftSocketTypeString}`;

        if (senderSocketUri && targetSocketUri) {
          if (generalSelectors.isAtomOwned(connectToAtomUri)(state)) {
            ownerApi
              .serverSideConnect(targetSocketUri, senderSocketUri, false, true)
              .then(async response => {
                if (!response.ok) {
                  const errorMsg = await response.text();
                  throw new Error(`Could not connect owned atoms: ${errorMsg}`);
                }
              });
          } else {
            // establish connection
            const cnctMsg = buildConnectMessage({
              connectMessage: "",
              socketUri: senderSocketUri,
              targetSocketUri: targetSocketUri,
            });
            //TODO: DELAY WORKAROUND TO FIX CONNECT ISSUES
            delay(2000)
              .then(() => ownerApi.sendMessage(cnctMsg))
              .then(jsonResp =>
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
        } else {
          throw new Error(
            `Could not connect owned atoms did not find necessary sockets`
          );
        }
      });
  });
}

export function connectionsConnectAdHoc(targetSocketUri, connectMessage) {
  return (dispatch, getState) =>
    connectAdHoc(targetSocketUri, connectMessage, dispatch, getState); // moved to separate function to make transpilation work properly
}

function connectAdHoc(targetSocketUri, connectMessage, dispatch, getState) {
  ensureLoggedIn(dispatch, getState).then(async () => {
    const theirAtomUri = extractAtomUriBySocketUri(targetSocketUri);

    const state = getState();
    const adHocDraft = useCaseUtils.getUseCase("persona").draft;
    adHocDraft.content.personaName = generateFakePersonaName(
      wonUtils.getRandomWonId()
    );
    adHocDraft.content.description = "Automatically generated Persona";

    const nodeUri = generalSelectors.getDefaultNodeUri(state);

    // build create message for new atom
    const { message, atomUri } = buildCreateMessage(adHocDraft, nodeUri);

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
      .then(() => {
        // set default socketUri
        let senderSocketUri = `${atomUri}#chatSocket`;

        if (generalSelectors.isAtomOwned(theirAtomUri)(state)) {
          ownerApi
            .serverSideConnect(senderSocketUri, targetSocketUri, true, false)
            .then(async response => {
              if (!response.ok) {
                const errorMsg = await response.text();
                throw new Error(
                  `Could not connect sockets(${senderSocketUri}<->${targetSocketUri}): ${errorMsg}`
                );
              }
            });
        } else {
          // establish connection
          const cnctMsg = buildConnectMessage({
            connectMessage: connectMessage,
            socketUri: senderSocketUri,
            targetSocketUri: targetSocketUri,
          });

          //TODO: DELAY WORKAROUND TO FIX CONNECT ISSUES
          delay(2000)
            .then(() => ownerApi.sendMessage(cnctMsg))
            .then(jsonResp =>
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
    const theirAtom = generalSelectors.getAtom(theirAtomUri)(state);
    const theirConnectionUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetConnectionUri",
    ]);

    won
      .getConnection(connectionUri, {
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
      generalSelectors.getOwnedAtomByConnectionUri(connectionUri)(state);
    const atomUri = get(atom, "uri");
    const connection =
      connectionUri && getIn(atom, ["connections", connectionUri]);
    const processState = generalSelectors.getProcessState(state);
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
      generalSelectors.getOwnedAtomByConnectionUri(connectionUri)(state);
    const atomUri = get(atom, "uri");
    const connection = getIn(atom, ["connections", connectionUri]);
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

    return stateStore.fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents,
      resumeAfterUri
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
