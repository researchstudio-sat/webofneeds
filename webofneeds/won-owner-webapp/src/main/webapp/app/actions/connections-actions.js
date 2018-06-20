/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import Immutable from "immutable";

import urljoin from "url-join";

import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
  selectConnection,
} from "../selectors.js";

import { getIn, get, cloneAsMutable, deepFreeze } from "../utils.js";

import { ensureLoggedIn } from "./account-actions";

import { actionTypes, actionCreators } from "./actions.js";

import { ownerBaseUrl } from "config";

import {
  buildCreateMessage,
  buildOpenMessage,
  buildCloseMessage,
  buildChatMessage,
  buildRateMessage,
  buildConnectMessage,
  callAgreementsFetch,
  callAgreementEventFetch,
} from "../won-message-utils.js";

const keySet = deepFreeze(
  new Set([
    "agreementUris",
    "pendingProposalUris",
    "cancellationPendingAgreementUris",
  ])
);

export function connectionsChatMessage(
  chatMessage,
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

    buildChatMessage({
      chatMessage: chatMessage,
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
        // dispatch(actionCreators.messages__send(messageData));
        dispatch({
          type: actionTypes.connections.sendChatMessage,
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
    description: "Direct response to : " + theirTitle,
    //type: reNeedType,
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
    ).then(closeMessage => {
      dispatch(
        actionCreators.messages__send({
          eventUri: closeMessage.eventUri,
          message: closeMessage.message,
        })
      );
      dispatch({
        type: actionTypes.connections.close,
        payload: { connectionUri },
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

    won
      .getConnectionWithEventUris(connectionUri)
      .then(connection => {
        let msgToRateFor = { connection: connection };

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
      .then(action =>
        dispatch(
          actionCreators.messages__send({
            eventUri: action.eventUri,
            message: action.message,
          })
        )
      );
  };
}

export function loadAgreementData(ownNeedUri, connectionUri, agreementData) {
  return (dispatch, getState) => {
    const url = urljoin(
      ownerBaseUrl,
      "/rest/agreement/getAgreementProtocolUris",
      "?connectionUri=" + connectionUri
    );
    let hasChanged = false;
    callAgreementsFetch(url)
      .then(response => {
        const agreementHeadData = transformDataToSet(response);

        for (let key of keySet) {
          if (agreementHeadData.hasOwnProperty(key)) {
            for (let event of agreementHeadData[key]) {
              const chatMessages = getState()
                .getIn([
                  "needs",
                  ownNeedUri,
                  "connections",
                  connectionUri,
                  "messages",
                ])
                .toArray();
              addAgreementDataToSate(
                dispatch,
                chatMessages,
                ownNeedUri,
                connectionUri,
                event,
                agreementData,
                key
              );
              hasChanged = true;
            }
          }
        }

        //Remove all retracted/rejected messages
        if (
          agreementHeadData["rejectedMessageUris"] ||
          agreementHeadData["retractedMessageUris"]
        ) {
          let removalSet = new Set([
            ...agreementHeadData["rejectedMessageUris"],
            ...agreementHeadData["retractedMessageUris"],
          ]);

          for (let uri of removalSet) {
            //for(key of keySet) {
            const key = "pendingProposalUris";
            const data = agreementData;
            for (let obj of data[key]) {
              if (obj.stateUri === uri || obj.headUri === uri) {
                console.log("Message " + uri + " was removed");
                //Update State!
                data[key].delete(obj);
                hasChanged = true;
              }
            }
            if (hasChanged) {
              dispatch({
                type: actionCreators.connections__updateAgreementData,
                payload: {
                  connectionUri: connectionUri,
                  agreementData: data,
                },
              });
            }
          }
        }
      })
      .then(() => {
        if (!hasChanged) {
          dispatch({
            type: actionCreators.connections__setLoadingMessages,
            payload: {
              connectionUri: connectionUri,
              isLoadingMessages: false,
            },
          });
        }
      })
      .catch(error => {
        console.error("Error:", error);
        dispatch({
          type: actionCreators.connections__setLoadingMessages,
          payload: {
            connectionUri: connectionUri,
            isLoadingMessages: false,
          },
        });
      });
  };
}

export function addAgreementDataToSate(
  dispatch,
  chatMessages,
  ownNeedUri,
  connectionUri,
  eventUri,
  agreementData,
  key,
  obj
) {
  return callAgreementEventFetch(ownNeedUri, eventUri).then(response => {
    won.wonMessageFromJsonLd(response).then(msg => {
      let agreementObject = obj;

      if (msg.isFromOwner() && msg.getReceiverNeed() === ownNeedUri) {
        /*if we find out that the receiverneed of the crawled event is actually our
                 need we will call the method again but this time with the correct eventUri
                 */
        if (!agreementObject) {
          agreementObject = {
            stateUri: undefined,
            headUri: undefined,
          };
        }
        agreementObject.headUri = msg.getMessageUri();
        addAgreementDataToSate(
          dispatch,
          chatMessages,
          ownNeedUri,
          connectionUri,
          msg.getRemoteMessageUri(),
          agreementData,
          key,
          agreementObject
        );
      } else {
        if (!agreementObject) {
          agreementObject = {
            stateUri: undefined,
            headUri: undefined,
          };
          agreementObject.headUri = msg.getMessageUri();
        }

        agreementObject.stateUri = msg.getMessageUri();
        agreementData[key].add(agreementObject);

        //Dont load in state again!
        let found = false;
        for (const chatMessage of chatMessages) {
          console.log("ChatMessage: ", chatMessage);
          console.log("ChatMessageSize: ", chatMessages.size);
          if (agreementObject.stateUri === chatMessage.get("uri")) {
            found = true;
          }
        }
        if (!found) {
          dispatch({
            type: actionCreators.messages__connectionMessageReceived,
            msg: msg,
          });
        }
        dispatch({
          type: actionCreators.connections__updateAgreementData,
          payload: {
            connectionUri: connectionUri,
            agreementData: agreementData,
          },
        });
      }
    });
  });
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
  return (dispatch, getState) => {
    const state = getState();
    const connectionUri = connectionUriParam || selectOpenConnectionUri(state);
    const need =
      connectionUri && selectNeedByConnectionUri(state, connectionUri);
    const needUri = need && need.get("uri");
    const connection = connectionUri && selectConnection(state, connectionUri);
    if (!connectionUri || !connection) return;

    const connectionMessages = connection.get("messages");
    if (
      connection.get("isLoadingMessages") ||
      !connectionMessages ||
      connectionMessages.size > 0
    )
      return; // only start loading once.

    dispatch({
      type: actionTypes.connections.showLatestMessages,
      payload: Immutable.fromJS({ connectionUri, isLoadingMessages: true }),
    });

    won
      .getWonMessagesOfConnection(connectionUri, {
        requesterWebId: needUri,
        pagingSize: numOfEvts2pageSize(numberOfEvents),
        deep: true,
      })
      .then(events =>
        dispatch({
          type: actionTypes.connections.showLatestMessages,
          payload: Immutable.fromJS({
            connectionUri: connectionUri,
            events: events,
          }),
        })
      )
      .catch(error => {
        console.error("Failed loading the latest events: ", error);
        dispatch({
          type: actionTypes.connections.showLatestMessages,
          payload: Immutable.fromJS({
            connectionUri: connectionUri,
            error: error,
          }),
        });
      });
  };
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
    const connectionUri = connectionUriParam || selectOpenConnectionUri(state);
    const need =
      connectionUri && selectNeedByConnectionUri(state, connectionUri);
    const needUri = need && need.get("uri");
    const connection = need && need.getIn(["connections", connectionUri]);
    const connectionMessages = connection && connection.get("messages");

    if (connection.get("isLoadingMessages")) return; // only start loading once.

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

function transformDataToSet(response) {
  const tmpAgreementData = {
    agreementUris: new Set(response.agreementUris),
    pendingProposalUris: new Set(response.pendingProposalUris),
    pendingProposals: new Set(response.pendingProposals),
    acceptedCancellationProposalUris: new Set(
      response.acceptedCancellationProposalUris
    ),
    cancellationPendingAgreementUris: new Set(
      response.cancellationPendingAgreementUris
    ),
    pendingCancellationProposalUris: new Set(
      response.pendingCancellationProposalUris
    ),
    cancelledAgreementUris: new Set(response.cancelledAgreementUris),
    rejectedMessageUris: new Set(response.rejectedMessageUris),
    retractedMessageUris: new Set(response.retractedMessageUris),
  };
  return filterAgreementSet(tmpAgreementData);
}

function filterAgreementSet(tmpAgreementData) {
  for (let prop of tmpAgreementData.cancellationPendingAgreementUris) {
    if (tmpAgreementData.agreementUris.has(prop)) {
      tmpAgreementData.agreementUris.delete(prop);
    }
  }

  return tmpAgreementData;
}

function numOfEvts2pageSize(numberOfEvents) {
  // `*3*` to compensate for the *roughly* 2 additional success events per chat message
  return numberOfEvents * 3;
}
