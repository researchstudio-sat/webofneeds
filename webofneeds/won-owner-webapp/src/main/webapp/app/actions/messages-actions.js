/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import {
  actionTypes,
  actionCreators,
  getConnectionRelatedData,
} from "./actions.js";

import Immutable from "immutable";
import { getCorrectMessageUri } from "../selectors.js";

import {
  fetchDataForOwnedNeeds,
  fetchMessageEffects,
  isFetchMessageEffectsNeeded,
} from "../won-message-utils.js";

export function successfulCloseNeed(event) {
  return (dispatch, getState) => {
    //TODO maybe refactor these response message handling
    if (
      getState().getIn([
        "messages",
        "waitingForAnswer",
        event.getIsRemoteResponseTo(),
      ])
    ) {
      //dispatch(actionCreators.connections__denied(event));
    }
  };
}
export function failedCloseNeed(event) {
  return dispatch => {
    const needUri = event.getReceiverNeed();
    /*
        * TODO not sure if it's necessary to invalidate
        * the cache here as the previous action will just have
        * been an optimistic update of the state. Invalidation
        * should happen in the action that causes the interaction
        * with the server.
        */
    won
      .invalidateCacheForNeed(needUri) // mark need and it's connection container dirty
      .then(() => won.getConnectionUrisOfNeed(needUri))
      .then(connectionUris =>
        Promise.all(
          connectionUris.map(
            cnctUri => won.invalidateCacheForNewMessage(cnctUri, needUri) // mark connections dirty
          )
        )
      )
      .then(
        () =>
          // as the need and it's connections have been marked dirty
          // they will be reloaded on this action.
          fetchDataForOwnedNeeds([needUri])
        //fetchAllAccessibleAndRelevantData([needUri])
      )
      .then(allThatData =>
        dispatch({
          type: actionTypes.messages.closeNeed.failed,
          payload: allThatData,
        })
      );
  };
}

/*
         hasReceiverNeed: "https://192.168.124.53:8443/won/resource/need/1741189480636743700"
         hasSenderNeed: "https://192.168.124.53:8443/won/resource/need/1741189480636743700"
         has....Connection
         event.uri


         won.WONMSG.hasReceiverNeed = won.WONMSG.baseUri + "hasReceiverNeed";
         won.WONMSG.hasReceiverNeedCompacted = won.WONMSG.prefix + ":hasReceiverNeed";
         won.WONMSG.hasReceiver = won.WONMSG.baseUri + "hasReceiver"; // connection if connection event
         won.WONMSG.hasReceiverCompacted = won.WONMSG.prefix + ":hasReceiver";
         won.WONMSG.hasReceiverNode = won.WONMSG.baseUri + "hasReceiverNode";
         won.WONMSG.hasReceiverNodeCompacted = won.WONMSG.prefix + ":hasReceiverNode";
         won.WONMSG.hasSenderNeed = won.WONMSG.baseUri + "hasSenderNeed";
         won.WONMSG.hasSenderNeedCompacted = won.WONMSG.prefix + ":hasSenderNeed";
         won.WONMSG.hasSender = won.WONMSG.baseUri + "hasSender";
         won.WONMSG.hasSenderCompacted = won.WONMSG.prefix + ":hasSender";
         won.WONMSG.hasSenderNode = won.WONMSG.baseUri + "hasSenderNode";
         won.WONMSG.hasSenderNodeCompacted = won.WONMSG.prefix + ":hasSenderNode";
         */

export function successfulCloseConnection(event) {
  return (dispatch, getState) => {
    const state = getState();
    //TODO maybe refactor these response message handling
    if (
      state.getIn(["messages", "waitingForAnswer", event.getIsResponseTo()])
    ) {
      dispatch({
        type: actionTypes.messages.close.success,
        payload: event,
      });
    } else if (
      state.getIn([
        "messages",
        "waitingForAnswer",
        event.getIsRemoteResponseTo(),
      ])
    ) {
      dispatch({
        type: actionTypes.messages.close.success,
        payload: event,
      });
    } else {
      //when a connection is closed by the node (e.g. when you close/deactivate a need all its corresponding connections will be closed)
      dispatch({
        type: actionTypes.messages.close.success,
        payload: event,
      });
    }
  };
}

export function successfulCreate(event) {
  return dispatch => {
    //const state = getState();
    //TODO: if negative, use alternative need URI and send again
    //fetch need data and store in local RDF store
    //get URI of newly created need from message

    //load the data into the local rdf store and publish NeedCreatedEvent when done
    const needURI = event.getReceiverNeed();

    won.getNeed(needURI).then(need => {
      dispatch(
        actionCreators.needs__createSuccessful({
          publishEventUri: event.getIsResponseTo(),
          needUri: event.getSenderNeed(),
          need: need,
        })
      );
    });
  };
}

export function openMessageReceived(event) {
  return (dispatch, getState) => {
    const ownConnectionUri = event.getReceiver();
    const ownNeedUri = event.getReceiverNeed();
    const theirNeedUri = event.getSenderNeed();

    const state = getState();
    let connectionP;
    if (state.getIn(["connections", ownConnectionUri])) {
      // already in state. invalidate the version in the rdf-store.
      connectionP = Promise.resolve(
        state.getIn(["connections", ownConnectionUri])
      );
      won.invalidateCacheForNewConnection(ownConnectionUri, ownNeedUri);
    } else {
      // need to fetch
      connectionP = won
        .getConnectionWithEventUris(ownConnectionUri, {
          requesterWebId: ownNeedUri,
        })
        .then(cnct => Immutable.fromJS(cnct));
    }

    Promise.all([
      connectionP,
      won.getNeed(theirNeedUri),
      won.getNeed(ownNeedUri), //uses ownNeed (but does not need connections uris to be loaded) in connectMessageReceived
    ]).then(([connection, theirNeed, ownNeed]) => {
      dispatch({
        type: actionTypes.messages.openMessageReceived,
        payload: {
          updatedConnection: ownConnectionUri,
          connection: connection,
          ownNeedUri: ownNeedUri,
          ownNeed: ownNeed,
          remoteNeed: theirNeed,
          receivedEvent: event.getMessageUri(), // the more relevant event. used for unread-counter.
          message: event,
        },
      });
    });
  };
}
export function processAgreementMessage(event) {
  return dispatch => {
    dispatch({
      type: actionTypes.messages.processAgreementMessage,
      payload: event,
    });
  };
}

export function processConnectionMessage(event) {
  return (dispatch, getState) => {
    if (isFetchMessageEffectsNeeded(event)) {
      //TODO: INCLUDE PETRINETDATA RETRIEVAL IN HERE
      const _needUri = event.getSenderNeed();
      const isSentEvent = getState().getIn(["needs", _needUri, "ownNeed"]);

      let connectionUri;
      let needUri;

      if (isSentEvent) {
        connectionUri = event.getSender();
        needUri = event.getSenderNeed();
      } else {
        connectionUri = event.getReceiver();
        needUri = event.getReceiverNeed();
      }

      const messages = getState().getIn([
        "needs",
        needUri,
        "connections",
        connectionUri,
        "messages",
      ]);

      fetchMessageEffects(connectionUri, event.getMessageUri()).then(
        response => {
          if (response && response.length > 0) {
            console.log("agreement response : ", response);
          }
          for (const effect of response) {
            console.log("effect : ", effect, "effect-type: ", effect.type);
            switch (effect.type) {
              case "ACCEPTS":
                if (effect.accepts) {
                  let acceptedMessageUris = Array.isArray(
                    effect.acceptedMessageUri
                  )
                    ? effect.acceptedMessageUri
                    : [effect.acceptedMessageUri];
                  acceptedMessageUris.forEach(acceptedMessageUri => {
                    let messageUri = getCorrectMessageUri(
                      messages,
                      acceptedMessageUri
                    );
                    dispatch({
                      type: actionTypes.messages.messageStatus.markAsAccepted,
                      payload: {
                        messageUri: messageUri,
                        connectionUri: connectionUri,
                        needUri: needUri,
                        accepted: true,
                      },
                    });
                  });
                }
                break;
              case "PROPOSES":
                if (effect.proposalType === "CANCELS") {
                  let proposesToCancelUris = Array.isArray(
                    effect.proposesToCancel
                  )
                    ? effect.proposesToCancel
                    : [effect.proposesToCancel];

                  proposesToCancelUris.forEach(proposesToCancelURI => {
                    let messageUri = getCorrectMessageUri(
                      messages,
                      proposesToCancelURI
                    );
                    console.log(
                      "proposesToCancelURI: ",
                      proposesToCancelURI,
                      "messageUri: ",
                      messageUri
                    );
                    dispatch({
                      type:
                        actionTypes.messages.messageStatus
                          .markAsCancellationPending,
                      payload: {
                        messageUri: messageUri,
                        connectionUri: connectionUri,
                        needUri: needUri,
                        cancellationPending: true,
                      },
                    });
                  });
                }
                break;

              case "REJECTS":
                if (effect.rejects) {
                  let rejectedMessageUris = Array.isArray(
                    effect.rejectedMessageUri
                  )
                    ? effect.rejectedMessageUri
                    : [effect.rejectedMessageUri];

                  rejectedMessageUris.forEach(rejectedMessageUri => {
                    let messageUri = getCorrectMessageUri(
                      messages,
                      rejectedMessageUri
                    );
                    dispatch({
                      type: actionTypes.messages.messageStatus.markAsRejected,
                      payload: {
                        messageUri: messageUri,
                        connectionUri: connectionUri,
                        needUri: needUri,
                        rejected: true,
                      },
                    });
                  });
                }
                break;

              case "RETRACTS":
                if (effect.retracts) {
                  let retractedMessageUris = Array.isArray(
                    effect.retractedMessageUri
                  )
                    ? effect.retractedMessageUri
                    : [effect.retractedMessageUri];

                  retractedMessageUris.forEach(retractedMessageUri => {
                    let messageUri = getCorrectMessageUri(
                      messages,
                      retractedMessageUri
                    );
                    dispatch({
                      type: actionTypes.messages.messageStatus.markAsRetracted,
                      payload: {
                        messageUri: messageUri,
                        connectionUri: connectionUri,
                        needUri: needUri,
                        retracted: true,
                      },
                    });
                  });
                }
                break;

              default:
                break;
            }
          }

          dispatch({
            type: actionTypes.messages.processConnectionMessage,
            payload: event,
          });
        }
      );
    } else {
      dispatch({
        type: actionTypes.messages.processConnectionMessage,
        payload: event,
      });
    }
  };
}

export function connectMessageReceived(event) {
  return (dispatch, getState) => {
    const ownConnectionUri = event.getReceiver();
    const ownNeedUri = event.getReceiverNeed();
    const theirNeedUri = event.getSenderNeed();

    const state = getState();
    let connectionP;
    if (state.getIn(["connections", ownConnectionUri])) {
      // already in state. invalidate the version in the rdf-store.
      connectionP = Promise.resolve(
        state.getIn(["connections", ownConnectionUri])
      );
      won.invalidateCacheForNewConnection(ownConnectionUri, ownNeedUri);
    } else {
      // need to fetch
      connectionP = won
        .getConnectionWithEventUris(ownConnectionUri, {
          requesterWebId: ownNeedUri,
        })
        .then(cnct => Immutable.fromJS(cnct));
    }

    Promise.all([
      connectionP,
      won.getNeed(theirNeedUri),
      won.getNeed(ownNeedUri), //uses ownNeed (but does not need connections uris to be loaded) in connectMessageReceived
    ]).then(([connection, theirNeed, ownNeed]) => {
      dispatch({
        type: actionTypes.messages.connectMessageReceived,
        payload: {
          updatedConnection: ownConnectionUri,
          connection: connection.set(
            "hasConnectionState",
            won.WON.RequestReceived
          ),
          ownNeedUri: ownNeedUri,
          ownNeed: ownNeed,
          remoteNeed: theirNeed,
          receivedEvent: event.getMessageUri(), // the more relevant event. used for unread-counter.
          message: event,
        },
      });
    });
  };
}

export function markAsRetracted(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getCorrectMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      retracted: event.retracted,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsRetracted,
      payload: payload,
    });
  };
}

export function updateMessageStatus(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getCorrectMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      messageStatus: event.messageStatus,
    };

    dispatch({
      type: actionTypes.messages.updateMessageStatus,
      payload: payload,
    });
  };
}

export function markAsRejected(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getCorrectMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      rejected: event.rejected,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsRejected,
      payload: payload,
    });
  };
}

export function markAsAccepted(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getCorrectMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      accepted: event.accepted,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsAccepted,
      payload: payload,
    });
  };
}

export function markAsCancelled(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getCorrectMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      cancelled: event.cancelled,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsCancelled,
      payload: payload,
    });
  };
}

export function markAsCancellationPending(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getCorrectMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      cancellationPending: event.cancellationPending,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsCancellationPending,
      payload: payload,
    });
  };
}

export function needMessageReceived(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' need in the state - otherwise we'll ignore the hint
    const need = getState().getIn(["needs", event.getReceiverNeed()]);
    if (!need) {
      console.log(
        "ignoring needMessage for a need that is not ours:",
        event.getReceiverNeed()
      );
    }
    dispatch({
      type: actionTypes.messages.needMessageReceived,
      payload: {
        needUri: event.getReceiverNeed(),
        humanReadable: need.get("humanReadable"),
        message: event.getTextMessage(),
      },
    });
  };
}

export function hintMessageReceived(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' need in the state - otherwise we'll ignore the hint
    if (!getState().getIn(["needs", event.getReceiverNeed()])) {
      console.log(
        "ignoring hint for a need that is not ours:",
        event.getReceiverNeed()
      );
    } else if (
      getState().getIn(["needs", event.getMatchCounterpart(), "state"]) ===
      won.WON.InactiveCompacted
    ) {
      console.log(
        "ignoring hint for an inactive  need:",
        event.getMatchCounterpart()
      );
    } else {
      //event.eventType = won.messageType2EventType[event.hasMessageType]; TODO needed?
      won
        .invalidateCacheForNewConnection(
          event.getReceiver(),
          event.getReceiverNeed()
        )
        .then(() => {
          let needUri = event.getReceiverNeed();
          //TODO: why do add the matchscore and counterpart when we don't use the event?

          event.matchScore = event.getMatchScore();
          event.matchCounterpartURI = event.getMatchCounterpart();

          getConnectionRelatedData(
            needUri,
            event.getMatchCounterpart(),
            event.getReceiver()
          ).then(data => {
            data.receivedEvent = event.getMessageUri();
            data.updatedConnection = event.getReceiver();
            dispatch({
              type: actionTypes.messages.hintMessageReceived,
              payload: data,
            });
          });

          // /add some properties to the eventData so as to make them easily accessible to consumers
          //of the hint event
          // below is commented as it seems to cause to hint event data loaded/displayed
          //if (eventData.matchCounterpartURI != null) {
          //    //load the data of the need the hint is about, if required
          //    //linkedDataService.ensureLoaded(eventData.uri);
          //    linkedDataService.ensureLoaded(eventData.matchCounterpartURI);
          //}
        });
    }
  };
}

/**
 * Dispatches actions registered for the "successOwn" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "failureOwn" event
 */
export function dispatchActionOnSuccessOwn(event) {
  return (dispatch, getState) => {
    const toDispatchList = getState().getIn([
      "messages",
      "dispatchOnSuccessOwn",
      event.getIsResponseTo(),
    ]);
    if (toDispatchList) {
      toDispatchList.forEach(d => {
        dispatch(d);
      });
    }
    //the reducer will delete the toDispatchList for successOwn and failureOwn
    dispatch({
      type: actionTypes.messages.dispatchActionOn.successOwn,
      payload: {
        eventUri: event.getIsResponseTo(),
      },
    });
  };
}

/**
 * Dispatches actions registered for the "failureOwn" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "successOwn" event
 */
export function dispatchActionOnFailureOwn(event) {
  return (dispatch, getState) => {
    const toDispatchList = getState().getIn([
      "messages",
      "dispatchOnFailureOwn",
      event.getIsResponseTo(),
    ]);
    if (toDispatchList) {
      toDispatchList.forEach(d => {
        dispatch(d);
      });
    }
    //the reducer will delete the toDispatchList for successOwn and failureOwn
    dispatch({
      type: actionTypes.messages.dispatchActionOn.failureOwn,
      payload: {
        eventUri: event.getIsResponseTo(),
      },
    });
  };
}

/**
 * Dispatches actions registered for the "successRemote" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "failureRemote" event
 */
export function dispatchActionOnSuccessRemote(event) {
  return (dispatch, getState) => {
    const toDispatchList = getState().getIn([
      "messages",
      "dispatchOnSuccessRemote",
      event.getIsRemoteResponseTo(),
    ]);
    if (toDispatchList) {
      toDispatchList.forEach(d => {
        if (d.type) {
          dispatch(d);
        } else {
          // if an adHocConnection was successfully created, go to the correct connectionUri
          if (d.connectionUri === "responseEvent::receiverUri") {
            dispatch(
              actionCreators.router__stateGoCurrent({
                connectionUri: event.getReceiver(),
              })
            );
          }
        }
      });
    }
    //the reducer will delete the toDispatchList for successOwn and failureOwn
    dispatch({
      type: actionTypes.messages.dispatchActionOn.successRemote,
      payload: {
        eventUri: event.getIsRemoteResponseTo(),
      },
    });
  };
}

/**
 * Dispatches actions registered for the "failureRemote" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "successRemote" event
 */
export function dispatchActionOnFailureRemote(event) {
  return (dispatch, getState) => {
    const toDispatchList = getState().getIn([
      "messages",
      "dispatchOnFailureRemote",
      event.getIsRemoteResponseTo(),
    ]);
    if (toDispatchList) {
      toDispatchList.forEach(d => {
        dispatch(d);
      });
    }
    //the reducer will delete the toDispatchList for successOwn and failureOwn
    dispatch({
      type: actionTypes.messages.dispatchActionOn.failureRemote,
      payload: {
        eventUri: event.getIsRemoteResponseTo(),
      },
    });
  };
}
