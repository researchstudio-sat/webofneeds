/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import { actionTypes, actionCreators } from "./actions.js";
import { get, getIn } from "../utils.js";

import Immutable from "immutable";
import { getOwnMessageUri } from "../message-utils.js";

import {
  fetchDataForOwnedNeeds,
  fetchMessageEffects,
  fetchPetriNetUris,
  isFetchMessageEffectsNeeded,
  buildChatMessage,
  fetchTheirNeedAndDispatch,
  fetchActiveConnectionAndDispatch,
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
      .then(() => won.getConnectionUrisWithStateByNeedUri(needUri))
      .then(connectionsWithStateAndFacet =>
        Promise.all(
          connectionsWithStateAndFacet.map(
            conn => won.invalidateCacheForNewMessage(conn.connectionUri) // mark connections dirty
          )
        )
      )
      .then(() =>
        // as the need and it's connections have been marked dirty
        // they will be reloaded on this action.
        fetchDataForOwnedNeeds([needUri], dispatch)
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
    const receiverNeedUri = event.getReceiverNeed();
    const receiverConnectionUri = event.getReceiver();

    const senderNeedUri = event.getSenderNeed();
    const senderConnectionUri = event.getSender();

    const currentState = getState();
    const senderNeed = getIn(currentState, ["needs", senderNeedUri]);
    const receiverNeed = getIn(currentState, ["needs", receiverNeedUri]);

    const isOwnSenderNeed = get(senderNeed, "isOwned");
    const isOwnReceiverNeed = get(receiverNeed, "isOwned");

    //check if the two connections are relevant to be stored within the state (if connUri is present, and if Need belongs to self)
    const isSenderConnectionRelevant = senderConnectionUri && isOwnSenderNeed;
    const isReceiverConnectionRelevant =
      receiverConnectionUri && isOwnReceiverNeed;

    let senderNeedP;
    if (isOwnSenderNeed) {
      //We know that all own needs are already stored within the state, so we do not have to retrieve it
      senderNeedP = Promise.resolve(won.invalidateCacheForNeed(senderNeedUri));
    } else {
      senderNeedP = fetchTheirNeedAndDispatch(senderNeedUri, dispatch);
    }

    let receiverNeedP;
    if (isOwnReceiverNeed) {
      //We know that all own needs are already stored within the state, so we do not have to retrieve it
      receiverNeedP = Promise.resolve(
        won.invalidateCacheForNeed(receiverNeedUri)
      );
    } else {
      receiverNeedP = fetchTheirNeedAndDispatch(receiverNeedUri, dispatch);
    }

    let senderConnectionP;
    if (!isSenderConnectionRelevant) {
      console.debug(
        "senderConnection not relevant, resolve promise with undefined -> ignore the connection"
      );
      senderConnectionP = Promise.resolve(false);
    } else if (getIn(senderNeed, ["connections", senderConnectionUri])) {
      senderConnectionP = won
        .invalidateCacheForNewConnection(senderConnectionUri, senderNeedUri)
        .then(() => true);
    } else {
      senderConnectionP = fetchActiveConnectionAndDispatch(
        senderConnectionUri,
        senderNeedUri,
        dispatch
      ).then(() => true);
    }

    let receiverConnectionP;
    if (!isReceiverConnectionRelevant) {
      console.debug(
        "receiverConnection not relevant, resolve promise with undefined -> ignore the connection"
      );
      receiverConnectionP = Promise.resolve(true);
    } else if (getIn(receiverNeed, ["connections", receiverConnectionUri])) {
      receiverConnectionP = won
        .invalidateCacheForNewConnection(receiverConnectionUri, receiverNeedUri)
        .then(() => true);
    } else {
      receiverConnectionP = fetchActiveConnectionAndDispatch(
        receiverConnectionUri,
        receiverNeedUri,
        dispatch
      ).then(() => true);
    }

    Promise.all([
      senderConnectionP,
      receiverConnectionP,
      senderNeedP,
      receiverNeedP,
    ]).then(
      ([
        senderConnectionRelevant,
        receiverConnectionRelevant,
        senderNeed,
        receiverNeed,
      ]) => {
        if (receiverConnectionRelevant) {
          console.debug("Change ReceiverConnectionState ", receiverNeed);
          dispatch({
            type: actionTypes.messages.openMessageReceived,
            payload: {
              updatedConnectionUri: receiverConnectionUri,
              ownedNeedUri: receiverNeedUri,
              message: event,
            },
          });
        }

        if (senderConnectionRelevant) {
          console.debug("Change SenderConnectionState ", senderNeed);
          dispatch({
            type: actionTypes.messages.openMessageSent,
            payload: {
              senderConnectionUri: senderConnectionUri,
              senderNeedUri: senderNeedUri,
              event: event,
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
          loadingPetriNetData: true,
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
              loadingPetriNetData: false,
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

    let senderNeedP;
    if (isOwnSenderNeed) {
      //We know that all own needs are already stored within the state, so we do not have to retrieve it
      senderNeedP = Promise.resolve(won.invalidateCacheForNeed(senderNeedUri));
    } else {
      senderNeedP = fetchTheirNeedAndDispatch(senderNeedUri, dispatch);
    }

    let receiverNeedP;
    if (isOwnReceiverNeed) {
      //We know that all own needs are already stored within the state, so we do not have to retrieve it
      receiverNeedP = Promise.resolve(
        won.invalidateCacheForNeed(receiverNeedUri)
      );
    } else {
      receiverNeedP = fetchTheirNeedAndDispatch(receiverNeedUri, dispatch);
    }

    let senderCP;
    if (!senderConnectionUri || !isOwnSenderNeed) {
      console.debug(
        "senderConnectionUri was null or senderNeed is not ownedNeed, resolve promise with undefined -> ignore the connection"
      );
      senderCP = Promise.resolve(false);
    } else if (
      senderNeed &&
      senderNeed.getIn(["connections", senderConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      senderCP = Promise.resolve(
        won.invalidateCacheForNewConnection(senderConnectionUri, senderNeedUri)
      ).then(() => true);
    } else {
      senderCP = fetchActiveConnectionAndDispatch(
        senderConnectionUri,
        senderNeedUri,
        dispatch
      ).then(() => true);
    }

    let receiverCP;
    if (!receiverConnectionUri || !isOwnReceiverNeed) {
      console.debug(
        "receiverConnectionUri was null or receiverNeed is not ownedNeed, resolve promise with undefined -> ignore the connection"
      );
      receiverCP = Promise.resolve(false);
    } else if (
      receiverNeed &&
      receiverNeed.getIn(["connections", receiverConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      receiverCP = won
        .invalidateCacheForNewConnection(receiverConnectionUri, receiverNeedUri)
        .then(() => true);
    } else {
      receiverCP = fetchActiveConnectionAndDispatch(
        receiverConnectionUri,
        receiverNeedUri,
        dispatch
      ).then(() => true);
    }

    //we have to retrieve the personas too
    Promise.all([senderCP, receiverCP, senderNeedP, receiverNeedP]).then(
      ([
        senderConnectionRelevant,
        receiverConnectionRelevant,
        senderNeed,
        receiverNeed,
      ]) => {
        if (receiverConnectionRelevant) {
          console.debug("Change ReceiverConnectionState ", receiverNeed);
          dispatch({
            type: actionTypes.messages.connectMessageReceived,
            payload: {
              updatedConnectionUri: receiverConnectionUri,
              ownedNeedUri: receiverNeedUri,
              message: event,
            },
          });
        }

        if (senderConnectionRelevant) {
          console.debug("Change SenderConnectionState ", senderNeed);
          dispatch({
            type: actionTypes.messages.connectMessageSent,
            payload: {
              senderConnectionUri: senderConnectionUri,
              senderNeedUri: senderNeedUri,
              event: event,
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
    const ownedNeedUri = event.getReceiverNeed();
    const remoteNeedUri = event.getMatchCounterpart();

    const currentState = getState();
    const ownedNeed = getIn(currentState, ["needs", ownedNeedUri]);
    const remoteNeed = getIn(currentState, ["needs", remoteNeedUri]);

    const ownedConnectionUri = event.getReceiver();

    if (!ownedNeed) {
      console.debug(
        "ignoring hint for a need that is not yet in the state (could be a remoteNeed, or a non stored ownedNeed):",
        ownedNeedUri
      );
    } else if (get(remoteNeed, "state") === won.WON.InactiveCompacted) {
      console.debug("ignoring hint for an inactive need:", remoteNeedUri);
    } else {
      won
        .invalidateCacheForNewConnection(ownedConnectionUri, ownedNeedUri)
        .then(() => {
          if (remoteNeed) {
            return Promise.resolve(won.invalidateCacheForNeed(remoteNeedUri));
          } else {
            return fetchTheirNeedAndDispatch(remoteNeedUri, dispatch);
          }
        })
        .then(() =>
          fetchActiveConnectionAndDispatch(
            ownedConnectionUri,
            ownedNeedUri,
            dispatch
          )
        );
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
          loadingPetriNetData: true,
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
              loadingPetriNetData: false,
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
