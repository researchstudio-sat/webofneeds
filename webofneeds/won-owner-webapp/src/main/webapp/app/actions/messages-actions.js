/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import { actionTypes, actionCreators } from "./actions.js";
import { entries } from "../utils.js";

import Immutable from "immutable";
import { getOwnMessageUri } from "../message-utils.js";

import {
  fetchDataForOwnedNeeds,
  fetchMessageEffects,
  fetchPetriNetUris,
  isFetchMessageEffectsNeeded,
  buildChatMessage,
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

export function processOpenMessage(event) {
  return (dispatch, getState) => {
    const receiverConnectionUri = event.getReceiver();
    const receiverNeedUri = event.getReceiverNeed();

    const senderNeedUri = event.getSenderNeed();
    const senderConnectionUri = event.getSender();

    const currentState = getState();
    const senderNeed = currentState.getIn(["needs", senderNeedUri]);
    const receiverNeed = currentState.getIn(["needs", receiverNeedUri]);
    const isOwnSenderNeed = senderNeed && senderNeed.get("isOwned");
    const isOwnReceiverNeed = receiverNeed && receiverNeed.get("isOwned");

    let senderConnectionP;
    if (!senderConnectionUri || !isOwnSenderNeed) {
      console.debug(
        "senderConnectionUri was null or senderNeed is not ownedNeed, resolve promise with undefined -> ignore the connection"
      );
      senderConnectionP = Promise.resolve(undefined);
    } else if (
      senderNeed &&
      senderNeed.getIn(["connections", senderConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      senderConnectionP = Promise.resolve(
        senderNeed.getIn(["connections", senderConnectionUri])
      );
      won.invalidateCacheForNewConnection(senderConnectionUri, senderNeedUri);
    } else {
      // need to fetch
      senderConnectionP = won
        .getConnectionWithEventUris(senderConnectionUri, {
          requesterWebId: senderNeedUri,
        })
        .then(cnct => Immutable.fromJS(cnct));
    }

    let receiverConnectionP;
    if (!receiverConnectionUri || !isOwnReceiverNeed) {
      console.debug(
        "receiverConnectionUri was null or receiverNeed is not ownedNeed, resolve promise with undefined -> ignore the connection"
      );
      receiverConnectionP = Promise.resolve(undefined);
    } else if (
      receiverNeed &&
      receiverNeed.getIn(["connections", receiverConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      receiverConnectionP = Promise.resolve(
        receiverNeed.getIn(["connections", receiverConnectionUri])
      );
      won.invalidateCacheForNewConnection(
        receiverConnectionUri,
        receiverNeedUri
      );
    } else {
      // need to fetch
      receiverConnectionP = won
        .getConnectionWithEventUris(receiverConnectionUri, {
          requesterWebId: receiverNeedUri,
        })
        .then(cnct => Immutable.fromJS(cnct));
    }

    Promise.all([
      senderConnectionP,
      receiverConnectionP,
      won.getNeed(senderNeedUri), //TODO: PROMISE IF LOADED (WE MIGHT HAVE IT IN THE STATE ALREADY)
      won.getNeed(receiverNeedUri), //TODO: PROMISE IF LOADED (WE MIGHT HAVE IT IN THE STATE ALREADY)
    ]).then(
      ([senderConnection, receiverConnection, senderNeed, receiverNeed]) => {
        if (receiverConnection) {
          dispatch({
            type: actionTypes.messages.openMessageReceived,
            payload: {
              updatedConnection: receiverConnectionUri,
              connection: receiverConnection,
              ownedNeedUri: receiverNeedUri,
              ownedNeed: receiverNeed,
              remoteNeed: senderNeed,
              receivedEvent: event.getMessageUri(), // the more relevant event. used for unread-counter.
              message: event,
            },
          });
        }

        if (senderConnection) {
          dispatch({
            type: actionTypes.messages.openMessageSent,
            payload: {
              senderConnectionUri: senderConnectionUri,
              senderNeedUri: senderNeedUri,
              receiverNeedUri: receiverNeedUri,
              eventUri: event.getMessageUri(),
              event: event,
              connection: senderConnection,
            },
          });
        }
      }
    );
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
      const _needUri = event.getSenderNeed();
      const isSentEvent = getState().getIn(["needs", _needUri, "isOwned"]);

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

      //PETRINET DATA PART START *********************
      dispatch({
        type: actionTypes.connections.setLoadingPetriNetData,
        payload: {
          connectionUri: connectionUri,
          isLoadingPetriNetData: true,
        },
      });

      fetchPetriNetUris(connectionUri)
        .then(response => {
          const petriNetData = {};

          response.forEach(entry => {
            if (entry.processURI) {
              petriNetData[entry.processURI] = entry;
            }
          });

          const petriNetDataImm = Immutable.fromJS(petriNetData);

          dispatch({
            type: actionTypes.connections.updatePetriNetData,
            payload: {
              connectionUri: connectionUri,
              petriNetData: petriNetDataImm,
            },
          });
        })
        .catch(error => {
          console.error("Error:", error);
          dispatch({
            type: actionTypes.connections.setLoadingPetriNetData,
            payload: {
              connectionUri: connectionUri,
              isLoadingPetriNetData: false,
            },
          });
        });

      //PETRINET DATA PART END **************************
      fetchMessageEffects(connectionUri, event.getMessageUri()).then(
        response => {
          for (const effect of response) {
            switch (effect.type) {
              case "ACCEPTS":
                if (effect.accepts) {
                  let acceptedMessageUris = Array.isArray(
                    effect.acceptedMessageUri
                  )
                    ? effect.acceptedMessageUri
                    : [effect.acceptedMessageUri];
                  acceptedMessageUris.forEach(acceptedMessageUri => {
                    let messageUri = getOwnMessageUri(
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
              case "CLAIMS":
                if (effect.claims) {
                  let claimedMessageUris = Array.isArray(effect.claims)
                    ? effect.claims
                    : [effect.claims];

                  claimedMessageUris.forEach(claimedMessageUris => {
                    let messageUri = getOwnMessageUri(
                      messages,
                      claimedMessageUris
                    );
                    dispatch({
                      type: actionTypes.messages.messageStatus.markAsClaimed,
                      payload: {
                        messageUri: messageUri,
                        connectionUri: connectionUri,
                        needUri: needUri,
                        claimed: true,
                      },
                    });
                  });
                }
                break;

              case "PROPOSES":
                if (effect.proposes) {
                  let proposedMessageUris = Array.isArray(effect.proposes)
                    ? effect.proposes
                    : [effect.proposes];

                  proposedMessageUris.forEach(proposedMessageUri => {
                    let messageUri = getOwnMessageUri(
                      messages,
                      proposedMessageUri
                    );
                    dispatch({
                      type: actionTypes.messages.messageStatus.markAsProposed,
                      payload: {
                        messageUri: messageUri,
                        connectionUri: connectionUri,
                        needUri: needUri,
                        proposed: true,
                      },
                    });
                  });
                }

                if (effect.proposalType === "CANCELS") {
                  let proposesToCancelUris = Array.isArray(
                    effect.proposesToCancel
                  )
                    ? effect.proposesToCancel
                    : [effect.proposesToCancel];

                  proposesToCancelUris.forEach(proposesToCancelURI => {
                    let messageUri = getOwnMessageUri(
                      messages,
                      proposesToCancelURI
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
                    let messageUri = getOwnMessageUri(
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
                    let messageUri = getOwnMessageUri(
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

export function processConnectMessage(event) {
  return (dispatch, getState) => {
    const receiverConnectionUri = event.getReceiver();
    const receiverNeedUri = event.getReceiverNeed();

    const senderNeedUri = event.getSenderNeed();
    const senderConnectionUri = event.getSender();

    const currentState = getState();
    const senderNeed = currentState.getIn(["needs", senderNeedUri]);
    const receiverNeed = currentState.getIn(["needs", receiverNeedUri]);
    const isOwnSenderNeed = senderNeed && senderNeed.get("isOwned");
    const isOwnReceiverNeed = receiverNeed && receiverNeed.get("isOwned");

    let senderCP;
    if (!senderConnectionUri || !isOwnSenderNeed) {
      console.debug(
        "senderConnectionUri was null or senderNeed is not ownedNeed, resolve promise with undefined -> ignore the connection"
      );
      senderCP = Promise.resolve(undefined);
    } else if (
      senderNeed &&
      senderNeed.getIn(["connections", senderConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      senderCP = Promise.resolve(
        senderNeed.getIn(["connections", senderConnectionUri])
      );
      won.invalidateCacheForNewConnection(senderConnectionUri, senderNeedUri);
    } else {
      // need to fetch
      senderCP = won
        .getConnectionWithEventUris(senderConnectionUri, {
          requesterWebId: senderNeedUri,
        })
        .then(cnct => Immutable.fromJS(cnct));
    }

    let receiverCP;
    if (!receiverConnectionUri || !isOwnReceiverNeed) {
      console.debug(
        "receiverConnectionUri was null or receiverNeed is not ownedNeed, resolve promise with undefined -> ignore the connection"
      );
      receiverCP = Promise.resolve(undefined);
    } else if (
      receiverNeed &&
      receiverNeed.getIn(["connections", receiverConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      receiverCP = Promise.resolve(
        receiverNeed.getIn(["connections", receiverConnectionUri])
      );
      won.invalidateCacheForNewConnection(
        receiverConnectionUri,
        receiverNeedUri
      );
    } else {
      // need to fetch
      receiverCP = won
        .getConnectionWithEventUris(receiverConnectionUri, {
          requesterWebId: receiverNeedUri,
        })
        .then(cnct => Immutable.fromJS(cnct));
    }

    //TODO: A CONNECT MESSAGE TO/FROM A NEED THAT IS NOT KNOWN BUT CONTAINS A PERSONA WILL RESULT IN THE PERSONA BEING NOT VISIBLE
    //we have to retrieve the personas too
    Promise.all([
      senderCP,
      receiverCP,
      won.getNeed(senderNeedUri), //TODO: PROMISE IF LOADED (WE MIGHT HAVE IT IN THE STATE ALREADY) ALSO WHAT ABOUT PERSONA RETRIEVAL
      won.getNeed(receiverNeedUri), //TODO: PROMISE IF LOADED (WE MIGHT HAVE IT IN THE STATE ALREADY) ALSO WHAT ABOUT PERSONA RETRIEVAL
    ]).then(
      ([senderConnection, receiverConnection, senderNeed, receiverNeed]) => {
        if (receiverConnection) {
          dispatch({
            type: actionTypes.messages.connectMessageReceived,
            payload: {
              updatedConnection: receiverConnectionUri,
              connection: receiverConnection.set(
                "hasConnectionState",
                won.WON.RequestReceived
              ),
              ownedNeedUri: receiverNeedUri,
              ownedNeed: receiverNeed,
              remoteNeed: senderNeed,
              receivedEvent: event.getMessageUri(), // the more relevant event. used for unread-counter.
              message: event,
            },
          });
        }

        if (senderConnection) {
          dispatch({
            type: actionTypes.messages.connectMessageSent,
            payload: {
              senderConnectionUri: senderConnectionUri,
              senderNeedUri: senderNeedUri,
              receiverNeedUri: receiverNeedUri,
              eventUri: event.getMessageUri(),
              event: event,
              connection: senderConnection.set(
                "hasConnectionState",
                won.WON.RequestSent
              ),
            },
          });
        }
      }
    );
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
    const messageUri = getOwnMessageUri(messages, event.messageUri);

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
    const messageUri = getOwnMessageUri(messages, event.messageUri);

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
    const messageUri = getOwnMessageUri(messages, event.messageUri);

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

export function markAsProposed(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      proposed: event.proposed,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsProposed,
      payload: payload,
    });
  };
}

export function markAsClaimed(event) {
  return (dispatch, getState) => {
    const messages = getState().getIn([
      "needs",
      event.needUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      needUri: event.needUri,
      claimed: event.claimed,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsClaimed,
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
    const messageUri = getOwnMessageUri(messages, event.messageUri);

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
    const messageUri = getOwnMessageUri(messages, event.messageUri);

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
    const messageUri = getOwnMessageUri(messages, event.messageUri);

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
      console.debug(
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

export function processHintMessage(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' need in the state - otherwise we'll ignore the hint
    if (!getState().getIn(["needs", event.getReceiverNeed()])) {
      console.debug(
        "ignoring hint for a need that is not ours:",
        event.getReceiverNeed()
      );
    } else if (
      getState().getIn(["needs", event.getMatchCounterpart(), "state"]) ===
      won.WON.InactiveCompacted
    ) {
      console.debug(
        "ignoring hint for an inactive  need:",
        event.getMatchCounterpart()
      );
    } else {
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
    const messageUri = event.getIsRemoteResponseTo();
    const connectionUri = event.getReceiver();

    const toDispatchList = getState().getIn([
      "messages",
      "dispatchOnSuccessRemote",
      messageUri,
    ]);

    const toAutoClaim = getState().getIn([
      "messages",
      "claimOnSuccess",
      messageUri,
    ]);

    const toRefreshData = getState().getIn([
      "messages",
      "refreshDataOnSuccess",
      messageUri,
    ]);

    if (toRefreshData) {
      dispatch({
        type: actionTypes.connections.setLoadingPetriNetData,
        payload: {
          connectionUri: connectionUri,
          isLoadingPetriNetData: true,
        },
      });

      fetchPetriNetUris(connectionUri)
        .then(response => {
          const petriNetData = {};

          response.forEach(entry => {
            if (entry.processURI) {
              petriNetData[entry.processURI] = entry;
            }
          });

          const petriNetDataImm = Immutable.fromJS(petriNetData);

          dispatch({
            type: actionTypes.connections.updatePetriNetData,
            payload: {
              connectionUri: connectionUri,
              petriNetData: petriNetDataImm,
            },
          });
        })
        .catch(error => {
          console.error("Error:", error);
          dispatch({
            type: actionTypes.connections.setLoadingPetriNetData,
            payload: {
              connectionUri: connectionUri,
              isLoadingPetriNetData: false,
            },
          });
        });
    }

    if (toAutoClaim) {
      const theirConnectionUri = event.getSender();
      const ownedNeedUri = event.getReceiverNeed();
      const ownNodeUri = event.getReceiverNode();
      const theirNeedUri = event.getSenderNeed();
      const theirNodeUri = event.getSenderNode();

      let referencedContentUris = new Map().set("claims", [
        { "@id": event.getIsRemoteResponseTo() },
      ]);

      /*
      This Dispatch is similar to the one we use in connection-actions.js ->
        function connectionsChatMessage(...)
        The part where we iterate over all the references and send the dispatches to mark the
        message appropriately. usually we need to check whether the messageUri to be marked is
        the remoteMessageUri or the ownMessageUri, but since the autoClaim will only be executed
        on ownMessages we do not need this check here
       */
      /*TODO:
       Since we set a messageToBe (successfully) claimed before we even know if the transition was successful
       we might need to rethink this implementation in favor of a dirtyState somehow, and remove the dirty state on success
       of the message(if(toRefreshData)-part above)... but for now and because
       connectionsChateMessage does not do this either it will do...*/
      dispatch({
        type: actionTypes.messages.messageStatus.markAsClaimed,
        payload: {
          messageUri: event.getIsRemoteResponseTo(),
          connectionUri: connectionUri,
          needUri: ownedNeedUri,
          claimed: true,
        },
      });

      buildChatMessage({
        chatMessage: undefined,
        additionalContent: undefined,
        referencedContentUris: referencedContentUris,
        connectionUri,
        ownedNeedUri,
        theirNeedUri,
        ownNodeUri,
        theirNodeUri,
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
            type: actionTypes.connections.sendChatMessageRefreshDataOnSuccess,
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
    }

    if (toDispatchList) {
      toDispatchList.forEach(d => {
        if (d.type) {
          dispatch(d);
        } else {
          // if an adHocConnection was successfully created, go to the correct connectionUri
          if (d.connectionUri === "responseEvent::receiverUri") {
            dispatch(
              actionCreators.router__stateGoCurrent({
                connectionUri,
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
        eventUri: messageUri,
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

/**
 * @param needUri
 * @param remoteNeedUri
 * @param connectionUri
 * @return {*}
 */
function getConnectionRelatedData(needUri, remoteNeedUri, connectionUri) {
  const remoteNeedP = won.getNeed(remoteNeedUri);
  const ownedNeedP = won.getNeed(needUri);
  const connectionP = won.getConnectionWithEventUris(connectionUri, {
    requesterWebId: needUri,
  });
  const eventsP = won
    .getEventsOfConnection(connectionUri, { requesterWebId: needUri })
    .then(eventsLookup => {
      const eventList = [];
      for (let [, event] of entries(eventsLookup)) {
        eventList.push(event);
      }
      return eventList;
    });

  const remotePersonaP = remoteNeedP.then(remoteNeed => {
    const remoteHeldBy = remoteNeed && remoteNeed["won:heldBy"];
    const remotePersonaUri = remoteHeldBy && remoteHeldBy["@id"];

    if (remotePersonaUri) {
      return won.getNeed(remotePersonaUri);
    } else {
      return Promise.resolve(undefined);
    }
  });

  const ownPersonaP = ownedNeedP.then(ownedNeed => {
    const ownHeldBy = ownedNeed && ownedNeed["won:heldBy"];
    const ownPersonaUri = ownHeldBy && ownHeldBy["@id"];

    if (ownPersonaUri) {
      return won.getNeed(ownPersonaUri);
    } else {
      return Promise.resolve(undefined);
    }
  });

  return Promise.all([
    remoteNeedP,
    ownedNeedP,
    connectionP,
    eventsP,
    remotePersonaP,
    ownPersonaP,
  ]).then(results => {
    return {
      remoteNeed: results[0],
      ownedNeed: results[1],
      connection: results[2],
      events: results[3],
      remotePersona: results[4],
      ownPersona: results[5],
    };
  });
}
