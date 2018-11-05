/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import Immutable from "immutable";

import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { selectConnectionByUri } from "../selectors/connection-selectors.js";

import { getIn, get, cloneAsMutable } from "../utils.js";

import { ensureLoggedIn } from "./account-actions";

import { actionTypes, actionCreators } from "./actions.js";

import {
  buildCreateMessage,
  buildOpenMessage,
  buildCloseMessage,
  buildChatMessage,
  buildRateMessage,
  buildConnectMessage,
} from "../won-message-utils.js";

export function connectionsChatMessageClaimOnSuccess(
  chatMessage,
  additionalContent,
  connectionUri
) {
  return (dispatch, getState) => {
    const ownNeed = getState()
      .get("needs")
      .filter(need => need.getIn(["connections", connectionUri]))
      .first();
    const theirNeedUri = getState().getIn([
      "needs",
      ownNeed.get("uri"),
      "connections",
      connectionUri,
      "remoteNeedUri",
    ]);
    const theirNeed = getState().getIn(["needs", theirNeedUri]);
    const theirConnectionUri = ownNeed.getIn([
      "connections",
      connectionUri,
      "remoteConnectionUri",
    ]);

    buildChatMessage({
      chatMessage: chatMessage,
      additionalContent: additionalContent,
      referencedContentUris: undefined,
      connectionUri,
      ownNeedUri: ownNeed.get("uri"),
      theirNeedUri: theirNeedUri,
      ownNodeUri: ownNeed.get("nodeUri"),
      theirNodeUri: theirNeed.get("nodeUri"),
      theirConnectionUri,
      isTTL: false,
    })
      .then(msgData =>
        Promise.all([
          won.wonMessageFromJsonLd(msgData.message),
          msgData.message,
        ])
      )
      .then(([optimisticEvent, jsonldMessage]) => {
        dispatch({
          type: actionTypes.connections.sendChatMessageClaimOnSuccess,
          payload: {
            eventUri: optimisticEvent.getMessageUri(),
            message: jsonldMessage,
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
    const ownNeed = getState()
      .get("needs")
      .filter(need => need.getIn(["connections", connectionUri]))
      .first();
    const theirNeedUri = getState().getIn([
      "needs",
      ownNeed.get("uri"),
      "connections",
      connectionUri,
      "remoteNeedUri",
    ]);
    const theirNeed = getState().getIn(["needs", theirNeedUri]);
    const theirConnectionUri = ownNeed.getIn([
      "connections",
      connectionUri,
      "remoteConnectionUri",
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
          const correctUri = msg.get("remoteUri") || msg.get("uri");
          if (correctUri) contentUris.push({ "@id": correctUri });
          //THE PARTS BELOW SHOULD NOT BE CALLED WITHIN THIS DISPATCH
          switch (key) {
            case "retracts":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsRetracted,
                payload: {
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  retracted: true,
                },
              });
              break;
            case "rejects":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsRejected,
                payload: {
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  rejected: true,
                },
              });
              break;
            case "proposesToCancel":
              dispatch({
                type:
                  actionTypes.messages.messageStatus.markAsCancellationPending,
                payload: {
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  cancellationPending: true,
                },
              });
              break;
            case "accepts":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsAccepted,
                payload: {
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  accepted: true,
                },
              });
              break;
            case "claims":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsClaimed,
                payload: {
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  claimed: true,
                },
              });
              break;
            case "proposes":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsProposed,
                payload: {
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
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
      connectionUri,
      ownNeedUri: ownNeed.get("uri"),
      theirNeedUri: theirNeedUri,
      ownNodeUri: ownNeed.get("nodeUri"),
      theirNodeUri: theirNeed.get("nodeUri"),
      theirConnectionUri,
      isTTL,
    })
      .then(msgData =>
        Promise.all([
          won.wonMessageFromJsonLd(msgData.message),
          msgData.message,
        ])
      )
      .then(([optimisticEvent, jsonldMessage]) => {
        dispatch({
          type: referencedContentUris
            ? actionTypes.connections.sendChatMessageRefreshDataOnSuccess //If there are references in the message we need to Refresh the Data from the backend on msg success
            : actionTypes.connections.sendChatMessage,
          payload: {
            eventUri: optimisticEvent.getMessageUri(),
            message: jsonldMessage,
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

export function connectionsOpen(connectionUri, textMessage) {
  return async (dispatch, getState) => {
    const ownNeed = getState()
      .get("needs")
      .filter(need => need.getIn(["connections", connectionUri]))
      .first();
    const theirNeedUri = getState().getIn([
      "needs",
      ownNeed.get("uri"),
      "connections",
      connectionUri,
      "remoteNeedUri",
    ]);
    const theirNeed = getState().getIn(["needs", theirNeedUri]);
    const theirConnectionUri = ownNeed.getIn([
      "connections",
      connectionUri,
      "remoteConnectionUri",
    ]);

    const openMsg = await buildOpenMessage(
      connectionUri,
      ownNeed.get("uri"),
      theirNeedUri,
      ownNeed.get("nodeUri"),
      theirNeed.get("nodeUri"),
      theirConnectionUri,
      textMessage
    );

    const optimisticEvent = await won.wonMessageFromJsonLd(openMsg.message);

    dispatch({
      type: actionTypes.connections.open,
      payload: {
        connectionUri,
        textMessage,
        eventUri: openMsg.eventUri,
        message: openMsg.message,
        optimisticEvent,
      },
    });

    dispatch(
      actionCreators.router__stateGoCurrent({
        connectionUri: optimisticEvent.getSender(),
      })
    );
  };
}

export function connectionsConnectAdHoc(theirNeedUri, textMessage) {
  return (dispatch, getState) =>
    connectAdHoc(theirNeedUri, textMessage, dispatch, getState); // moved to separate function to make transpilation work properly
}
function connectAdHoc(theirNeedUri, textMessage, dispatch, getState) {
  ensureLoggedIn(dispatch, getState).then(async () => {
    const state = getState();
    const theirNeed = getIn(state, ["needs", theirNeedUri]);
    const adHocDraft = generateResponseNeedTo(theirNeed);
    const nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    const { message, eventUri, needUri } = await buildCreateMessage(
      adHocDraft,
      nodeUri
    );
    const cnctMsg = buildConnectMessage({
      ownNeedUri: needUri,
      theirNeedUri: theirNeedUri,
      ownNodeUri: nodeUri,
      theirNodeUri: theirNeed.get("nodeUri"),
      textMessage: textMessage,
    });

    won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent => {
      // connect action to be dispatched when the
      // ad hoc need has been created:
      const connectAction = {
        type: actionTypes.needs.connect,
        payload: {
          eventUri: cnctMsg.eventUri,
          message: cnctMsg.message,
          optimisticEvent: optimisticEvent,
        },
      };

      // register a "stateGoCurrent" action to be dispatched messages-actions
      // after connectionUri is available
      dispatch({
        type: actionTypes.messages.dispatchActionOn.registerSuccessRemote,
        payload: {
          eventUri: cnctMsg.eventUri,
          actionToDispatch: {
            effect: "stateGoCurrent",
            connectionUri: "responseEvent::receiverUri",
            postUri: theirNeed,
            needUri: needUri,
          },
        },
      });

      // register the connect action to be dispatched when
      // need creation is successful
      dispatch({
        type: actionTypes.messages.dispatchActionOn.registerSuccessOwn,
        payload: {
          eventUri: eventUri,
          actionToDispatch: connectAction,
        },
      });

      // create the new need
      dispatch({
        type: actionTypes.needs.create, // TODO custom action
        payload: { eventUri, message, needUri, need: adHocDraft },
      });
    });
  });
}

function generateResponseNeedTo(theirNeed) {
  const theirSeeks = get(theirNeed, "seeks");
  const theirIs = get(theirNeed, "is");
  return {
    is: theirSeeks ? generateResponseContentNodeTo(theirSeeks) : undefined,
    seeks: theirIs ? generateResponseContentNodeTo(theirIs) : undefined,
  };
}

function generateResponseContentNodeTo(contentNode) {
  const theirTitle = get(contentNode, "title");
  return {
    title: theirTitle ? "Re: " + theirTitle : undefined,
    description: theirTitle ? "Direct response to: " + theirTitle : undefined,
    tags: cloneAsMutable(get(contentNode, "tags")),
    location: cloneAsMutable(get(contentNode, "location")),
    noHints: true,
  };
}

export function connectionsClose(connectionUri) {
  return (dispatch, getState) => {
    const ownNeed = getState()
      .get("needs")
      .filter(need => need.getIn(["connections", connectionUri]))
      .first();
    const theirNeedUri = getState().getIn([
      "needs",
      ownNeed.get("uri"),
      "connections",
      connectionUri,
      "remoteNeedUri",
    ]);
    const theirNeed = getState().getIn(["needs", theirNeedUri]);
    const theirConnectionUri = ownNeed.getIn([
      "connections",
      connectionUri,
      "remoteConnectionUri",
    ]);

    buildCloseMessage(
      connectionUri,
      ownNeed.get("uri"),
      theirNeedUri,
      ownNeed.get("nodeUri"),
      theirNeed.get("nodeUri"),
      theirConnectionUri
    ).then(({ eventUri, message }) => {
      dispatch({
        type: actionTypes.connections.close,
        payload: {
          connectionUri,
          eventUri,
          message,
        },
      });
    });
  };
}

export function connectionsCloseRemote(message) {
  //Closes the 'remoteConnection' again, if closeConnections(...) only closes the 'own' connection
  return dispatch => {
    const connectionUri = message.getSender();
    const remoteNeedUri = message.getSenderNeed();
    const remoteNode = message.getSenderNode();
    const ownNeedUri = message.getReceiverNeed();
    const ownNode = message.getReceiverNode();

    buildCloseMessage(
      connectionUri,
      remoteNeedUri,
      ownNeedUri,
      ownNode,
      remoteNode,
      null
    ).then(closeMessage => {
      dispatch(
        actionCreators.messages__send({
          eventUri: closeMessage.eventUri,
          message: closeMessage.message,
        })
      );
    });
  };
}

export function connectionsRate(connectionUri, rating) {
  return (dispatch, getState) => {
    const state = getState();

    const ownNeed = state
      .get("needs")
      .filter(need => need.getIn(["connections", connectionUri]))
      .first();
    const theirNeedUri = state.getIn([
      "needs",
      ownNeed.get("uri"),
      "connections",
      connectionUri,
      "remoteNeedUri",
    ]);
    const theirNeed = state.getIn(["needs", theirNeedUri]);
    const theirConnectionUri = ownNeed.getIn([
      "connections",
      connectionUri,
      "remoteConnectionUri",
    ]);

    won
      .getConnectionWithEventUris(connectionUri, {
        requesterWebId: ownNeed.get("uri"),
      })
      .then(connection => {
        let msgToRateFor = { connection: connection };

        return buildRateMessage(
          msgToRateFor,
          ownNeed.get("uri"),
          theirNeedUri,
          ownNeed.get("nodeUri"),
          theirNeed.get("nodeUri"),
          theirConnectionUri,
          rating
        );
      })
      .then(({ eventUri, message }) =>
        dispatch({
          type: actionTypes.connections.rate,
          payload: {
            connectionUri,
            rating,
            eventUri,
            message,
          },
        })
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
export function showLatestMessages(connectionUriParam, numberOfEvents) {
  return async (dispatch, getState) => {
    const state = getState();
    await loadLatestMessagesOfConnection({
      connectionUri: connectionUriParam,
      numberOfEvents,
      state,
      actionTypesToDispatch: {
        start: actionTypes.connections.showLatestMessages,
        success: actionTypes.connections.showLatestMessages,
        failure: actionTypes.connections.showLatestMessages,
      },
      dispatch,
    });
  };
}

export async function loadLatestMessagesOfConnection({
  connectionUri,
  numberOfEvents,
  state,
  actionTypesToDispatch,
  dispatch,
}) {
  const connectionUri_ = connectionUri || getConnectionUriFromRoute(state);
  const need =
    connectionUri_ && getOwnedNeedByConnectionUri(state, connectionUri_);
  const needUri = need && need.get("uri");
  const connection =
    connectionUri_ && selectConnectionByUri(state, connectionUri_);
  if (
    !connectionUri_ ||
    !connection ||
    connection.get("isLoadingMessages") // only start loading once.
  ) {
    return;
  }

  if (actionTypesToDispatch.start) {
    dispatch({
      type: actionTypesToDispatch.start,
      payload: Immutable.fromJS({
        connectionUri: connectionUri_,
        isLoadingMessages: true,
      }),
    });
  }

  try {
    const events = await won.getWonMessagesOfConnection(connectionUri_, {
      requesterWebId: needUri,
      pagingSize: numOfEvts2pageSize(numberOfEvents),
      deep: true,
    });

    if (actionTypesToDispatch.success) {
      dispatch({
        type: actionTypesToDispatch.success,
        payload: Immutable.fromJS({
          connectionUri: connectionUri_,
          events: events,
        }),
      });
    }
  } catch (error) {
    if (actionTypesToDispatch.failure) {
      dispatch({
        type: actionTypesToDispatch.failure,
        payload: Immutable.fromJS({
          connectionUri: connectionUri_,
          error: error,
        }),
      });
    }
  }
}

//TODO replace the won.getEventsOfConnection with this version (and make sure it works for all previous uses).
/**
 * Gets the events and uses the paging-parameters
 * in a meaningful fashion.
 * @param eventContainerUri
 * @param params
 * @return {*}
 */
/*
 function getEvents(connectionUri, params) {
 const eventP = won
 .getNode(connectionUri, params)
 .then(cnct =>
 won.getNode(cnct.hasEventContainer, params)
 )
 .then(eventContainer => is('Array', eventContainer.member) ?
 eventContainer.member :
 [eventContainer.member]
 )
 .then(eventUris => urisToLookupMap(
 eventUris,
 uri => won.getEvent(
 uri,
 { requesterWebId: params.requesterWebId }
 )
 ));

 return eventP;
 }
 */

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
      connectionUriParam || getConnectionUriFromRoute(state);
    const need =
      connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
    const needUri = need && need.get("uri");
    const connection = need && need.getIn(["connections", connectionUri]);
    const connectionMessages = connection && connection.get("messages");

    if (!connection || connection.get("isLoadingMessages")) return; // only start loading once, or not if no connection was found

    // determine the oldest loaded event
    const sortedConnectionMessages = connectionMessages
      .valueSeq()
      .sort((msg1, msg2) => msg1.get("date") - msg2.get("date"));
    const oldestMessage = sortedConnectionMessages.first();

    const messageHashValue =
      oldestMessage &&
      oldestMessage.get("uri").replace(/.*\/event\/(.*)/, "$1"); // everything following the `/event/`
    dispatch({
      type: actionTypes.connections.showMoreMessages,
      payload: Immutable.fromJS({ connectionUri, isLoadingMessages: true }),
    });

    won
      .getWonMessagesOfConnection(connectionUri, {
        requesterWebId: needUri,
        pagingSize: numOfEvts2pageSize(numberOfEvents),
        deep: true,
        resumebefore: messageHashValue,
      })
      .then(events =>
        dispatch({
          type: actionTypes.connections.showMoreMessages,
          payload: Immutable.fromJS({
            connectionUri: connectionUri,
            events: events,
          }),
        })
      )
      .catch(error => {
        console.error("Failed loading more events: ", error);
        dispatch({
          type: actionTypes.connections.showMoreMessages,
          payload: Immutable.fromJS({
            connectionUri: connectionUri,
            error: error,
          }),
        });
      });
  };
}

function numOfEvts2pageSize(numberOfEvents) {
  // `*3*` to compensate for the *roughly* 2 additional success events per chat message
  return numberOfEvents * 3;
}
