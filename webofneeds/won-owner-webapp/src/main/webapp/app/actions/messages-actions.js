/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import { actionTypes, actionCreators } from "./actions.js";
import { get, getIn } from "../utils.js";

import Immutable from "immutable";
import { getOwnMessageUri } from "../message-utils.js";
import * as generalSelectors from "../selectors/general-selectors.js";

import {
  fetchDataForOwnedAtoms,
  fetchDataForNonOwnedAtomOnly,
  fetchMessageEffects,
  fetchPetriNetUris,
  isFetchMessageEffectsNeeded,
  buildChatMessage,
  fetchTheirAtomAndDispatch,
  fetchActiveConnectionAndDispatch,
} from "../won-message-utils.js";

export function successfulCloseAtom(event) {
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
export function failedCloseAtom(event) {
  return dispatch => {
    const atomUri = event.getRecipientAtom();
    /*
        * TODO not sure if it's necessary to invalidate
        * the cache here as the previous action will just have
        * been an optimistic update of the state. Invalidation
        * should happen in the action that causes the interaction
        * with the server.
        */
    won
      .invalidateCacheForAtom(atomUri) // mark atom and it's connection container dirty
      .then(() => won.getConnectionUrisWithStateByAtomUri(atomUri))
      .then(connectionsWithStateAndSocket =>
        Promise.all(
          connectionsWithStateAndSocket.map(
            conn => won.invalidateCacheForNewMessage(conn.connectionUri) // mark connections dirty
          )
        )
      )
      .then(() =>
        // as the atom and it's connections have been marked dirty
        // they will be reloaded on this action.
        fetchDataForOwnedAtoms([atomUri], dispatch)
      )
      .then(allThatData =>
        dispatch({
          type: actionTypes.messages.closeAtom.failed,
          payload: allThatData,
        })
      );
  };
}

/*
         recipientAtom: "https://192.168.124.53:8443/won/resource/atom/1741189480636743700"
         senderAtom: "https://192.168.124.53:8443/won/resource/atom/1741189480636743700"
         has....Connection
         event.uri


         won.WONMSG.recipientAtom = won.WONMSG.baseUri + "recipientAtom";
         won.WONMSG.recipientAtomCompacted = won.WONMSG.prefix + ":recipientAtom";
         won.WONMSG.recipient = won.WONMSG.baseUri + "recipient"; // connection if connection event
         won.WONMSG.recipientCompacted = won.WONMSG.prefix + ":recipient";
         won.WONMSG.recipientNode = won.WONMSG.baseUri + "recipientNode";
         won.WONMSG.recipientNodeCompacted = won.WONMSG.prefix + ":recipientNode";
         won.WONMSG.senderAtom = won.WONMSG.baseUri + "senderAtom";
         won.WONMSG.senderAtomCompacted = won.WONMSG.prefix + ":senderAtom";
         won.WONMSG.sender = won.WONMSG.baseUri + "sender";
         won.WONMSG.senderCompacted = won.WONMSG.prefix + ":sender";
         won.WONMSG.senderNode = won.WONMSG.baseUri + "senderNode";
         won.WONMSG.senderNodeCompacted = won.WONMSG.prefix + ":senderNode";
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
      //when a connection is closed by the node (e.g. when you close/deactivate an atom all its corresponding connections will be closed)
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
    //TODO: if negative, use alternative atom URI and send again
    //fetch atom data and store in local RDF store
    //get URI of newly created atom from message

    //load the data into the local rdf store and publish AtomCreatedEvent when done
    const atomURI = event.getRecipientAtom();

    won.getAtom(atomURI).then(atom => {
      dispatch(
        actionCreators.atoms__createSuccessful({
          eventUri: event.getIsResponseTo(),
          atomUri: event.getSenderAtom(),
          atom: atom,
        })
      );
    });
  };
}

export function successfulEdit(event) {
  return dispatch => {
    console.debug("Received success replace message:", event);
    //const state = getState();
    //load the edited data into the local rdf store and publish AtomEditEvent when done
    const atomURI = event.getRecipientAtom();

    won
      //.invalidateCacheForAtom(atomURI)
      .clearStoreWithPromise()
      .then(() => fetchDataForOwnedAtoms([atomURI], dispatch))
      .then(() => {
        dispatch(
          actionCreators.atoms__editSuccessful({
            eventUri: event.getIsResponseTo(),
            atomUri: event.getSenderAtom(),
            //atom: atom,
          })
        );
      });
  };
}

export function processOpenMessage(event) {
  return (dispatch, getState) => {
    const recipientAtomUri = event.getRecipientAtom();
    const receiverConnectionUri = event.getReceiver();

    const senderAtomUri = event.getSenderAtom();
    const senderConnectionUri = event.getSender();

    const state = getState();
    const senderAtom = getIn(state, ["atoms", senderAtomUri]);
    const recipientAtom = getIn(state, ["atoms", recipientAtomUri]);

    const isOwnSenderAtom = generalSelectors.isAtomOwned(state, senderAtomUri);
    const isOwnRecipientAtom = generalSelectors.isAtomOwned(
      state,
      recipientAtomUri
    );

    //check if the two connections are relevant to be stored within the state (if connUri is present, and if Atom belongs to self)
    const isSenderConnectionRelevant = senderConnectionUri && isOwnSenderAtom;
    const isReceiverConnectionRelevant =
      receiverConnectionUri && isOwnRecipientAtom;

    let senderAtomP;
    if (isOwnSenderAtom) {
      //We know that all own atoms are already stored within the state, so we do not have to retrieve it
      senderAtomP = Promise.resolve(won.invalidateCacheForAtom(senderAtomUri));
    } else {
      senderAtomP = fetchTheirAtomAndDispatch(senderAtomUri, dispatch);
    }

    let recipientAtomP;
    if (isOwnRecipientAtom) {
      //We know that all own atoms are already stored within the state, so we do not have to retrieve it
      recipientAtomP = Promise.resolve(
        won.invalidateCacheForAtom(recipientAtomUri)
      );
    } else {
      recipientAtomP = fetchTheirAtomAndDispatch(recipientAtomUri, dispatch);
    }

    let senderConnectionP;
    if (!isSenderConnectionRelevant) {
      console.debug(
        "senderConnection not relevant, resolve promise with undefined -> ignore the connection"
      );
      senderConnectionP = Promise.resolve(false);
    } else if (getIn(senderAtom, ["connections", senderConnectionUri])) {
      senderConnectionP = won
        .invalidateCacheForNewConnection(senderConnectionUri, senderAtomUri)
        .then(() => true);
    } else {
      senderConnectionP = fetchActiveConnectionAndDispatch(
        senderConnectionUri,
        senderAtomUri,
        dispatch
      ).then(() => true);
    }

    let receiverConnectionP;
    if (!isReceiverConnectionRelevant) {
      console.debug(
        "receiverConnection not relevant, resolve promise with undefined -> ignore the connection"
      );
      receiverConnectionP = Promise.resolve(true);
    } else if (getIn(recipientAtom, ["connections", receiverConnectionUri])) {
      receiverConnectionP = won
        .invalidateCacheForNewConnection(
          receiverConnectionUri,
          recipientAtomUri
        )
        .then(() => true);
    } else {
      receiverConnectionP = fetchActiveConnectionAndDispatch(
        receiverConnectionUri,
        recipientAtomUri,
        dispatch
      ).then(() => true);
    }

    Promise.all([
      senderConnectionP,
      receiverConnectionP,
      senderAtomP,
      recipientAtomP,
    ]).then(
      ([
        senderConnectionRelevant,
        receiverConnectionRelevant,
        senderAtom,
        recipientAtom,
      ]) => {
        if (receiverConnectionRelevant) {
          console.debug("Change ReceiverConnectionState ", recipientAtom);
          dispatch({
            type: actionTypes.messages.openMessageReceived,
            payload: {
              updatedConnectionUri: receiverConnectionUri,
              ownedAtomUri: recipientAtomUri,
              message: event,
            },
          });
        }

        if (senderConnectionRelevant) {
          console.debug("Change SenderConnectionState ", senderAtom);
          dispatch({
            type: actionTypes.messages.openMessageSent,
            payload: {
              senderConnectionUri: senderConnectionUri,
              senderAtomUri: senderAtomUri,
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

export function processChangeNotificationMessage(event) {
  return (dispatch, getState) => {
    console.debug("processChangeNotificationMessage for: ", event);
    const atomUriToLoad = event.getSenderAtom();

    won
      //.invalidateCacheForAtom(atomURI)
      .clearStoreWithPromise()
      .then(() => {
        if (generalSelectors.isAtomOwned(getState(), atomUriToLoad)) {
          fetchDataForOwnedAtoms([atomUriToLoad], dispatch);
        } else {
          fetchDataForNonOwnedAtomOnly(atomUriToLoad, dispatch);
        }
      });

    dispatch({
      type: actionTypes.messages.processChangeNotificationMessage,
      payload: event,
    });
  };
}

export function processConnectionMessage(event) {
  return (dispatch, getState) => {
    if (isFetchMessageEffectsNeeded(event)) {
      const _atomUri = event.getSenderAtom();
      const isSentEvent = generalSelectors.isAtomOwned(getState(), _atomUri);

      let connectionUri;
      let atomUri;

      if (isSentEvent) {
        connectionUri = event.getSender();
        atomUri = event.getSenderAtom();
      } else {
        connectionUri = event.getReceiver();
        atomUri = event.getRecipientAtom();
      }

      const messages = getState().getIn([
        "atoms",
        atomUri,
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
                        atomUri: atomUri,
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
                        atomUri: atomUri,
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
                        atomUri: atomUri,
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
                        atomUri: atomUri,
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
                        atomUri: atomUri,
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
                        atomUri: atomUri,
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
    const recipientAtomUri = event.getRecipientAtom();

    const senderAtomUri = event.getSenderAtom();
    const senderConnectionUri = event.getSender();

    const state = getState();
    const senderAtom = getIn(state, ["atoms", senderAtomUri]);
    const recipientAtom = getIn(state, ["atoms", recipientAtomUri]);
    const isOwnSenderAtom = generalSelectors.isAtomOwned(state, senderAtomUri);
    const isOwnRecipientAtom = generalSelectors.isAtomOwned(
      state,
      recipientAtomUri
    );

    let senderAtomP;
    if (isOwnSenderAtom) {
      //We know that all own atoms are already stored within the state, so we do not have to retrieve it
      senderAtomP = Promise.resolve(won.invalidateCacheForAtom(senderAtomUri));
    } else {
      senderAtomP = fetchTheirAtomAndDispatch(senderAtomUri, dispatch);
    }

    let recipientAtomP;
    if (isOwnRecipientAtom) {
      //We know that all own atoms are already stored within the state, so we do not have to retrieve it
      recipientAtomP = Promise.resolve(
        won.invalidateCacheForAtom(recipientAtomUri)
      );
    } else {
      recipientAtomP = fetchTheirAtomAndDispatch(recipientAtomUri, dispatch);
    }

    let senderCP;
    if (!senderConnectionUri || !isOwnSenderAtom) {
      console.debug(
        "senderConnectionUri was null or senderAtom is not ownedAtom, resolve promise with undefined -> ignore the connection"
      );
      senderCP = Promise.resolve(false);
    } else if (
      senderAtom &&
      senderAtom.getIn(["connections", senderConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      senderCP = Promise.resolve(
        won.invalidateCacheForNewConnection(senderConnectionUri, senderAtomUri)
      ).then(() => true);
    } else {
      senderCP = fetchActiveConnectionAndDispatch(
        senderConnectionUri,
        senderAtomUri,
        dispatch
      ).then(() => true);
    }

    let receiverCP;
    if (!receiverConnectionUri || !isOwnRecipientAtom) {
      console.debug(
        "receiverConnectionUri was null or recipientAtom is not ownedAtom, resolve promise with undefined -> ignore the connection"
      );
      receiverCP = Promise.resolve(false);
    } else if (
      recipientAtom &&
      recipientAtom.getIn(["connections", receiverConnectionUri])
    ) {
      // already in state. invalidate the version in the rdf-store.
      receiverCP = won
        .invalidateCacheForNewConnection(
          receiverConnectionUri,
          recipientAtomUri
        )
        .then(() => true);
    } else {
      receiverCP = fetchActiveConnectionAndDispatch(
        receiverConnectionUri,
        recipientAtomUri,
        dispatch
      ).then(() => true);
    }

    //we have to retrieve the personas too
    Promise.all([senderCP, receiverCP, senderAtomP, recipientAtomP]).then(
      ([
        senderConnectionRelevant,
        receiverConnectionRelevant,
        senderAtom,
        recipientAtom,
      ]) => {
        if (receiverConnectionRelevant) {
          console.debug("Change ReceiverConnectionState ", recipientAtom);
          dispatch({
            type: actionTypes.messages.connectMessageReceived,
            payload: {
              updatedConnectionUri: receiverConnectionUri,
              ownedAtomUri: recipientAtomUri,
              message: event,
            },
          });
        }

        if (senderConnectionRelevant) {
          console.debug("Change SenderConnectionState ", senderAtom);
          dispatch({
            type: actionTypes.messages.connectMessageSent,
            payload: {
              senderConnectionUri: senderConnectionUri,
              senderAtomUri: senderAtomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
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
      "atoms",
      event.atomUri,
      "connections",
      event.connectionUri,
      "messages",
    ]);
    const messageUri = getOwnMessageUri(messages, event.messageUri);

    const payload = {
      messageUri: messageUri,
      connectionUri: event.connectionUri,
      atomUri: event.atomUri,
      cancellationPending: event.cancellationPending,
    };

    dispatch({
      type: actionTypes.messages.messageStatus.markAsCancellationPending,
      payload: payload,
    });
  };
}

export function atomMessageReceived(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' atom in the state - otherwise we'll ignore the hint
    const atom = getState().getIn(["atoms", event.getRecipientAtom()]);
    if (!atom) {
      console.debug(
        "ignoring atomMessage for an atom that is not ours:",
        event.getRecipientAtom()
      );
    }
    dispatch({
      type: actionTypes.messages.atomMessageReceived,
      payload: {
        atomUri: event.getRecipientAtom(),
        humanReadable: atom.get("humanReadable"),
        message: event.getTextMessage(),
      },
    });
  };
}

export function processSocketHintMessage(event) {
  return (dispatch, getState) => {
    const recipientAtomUri = event.getRecipientAtom();
    //const targetSocketUri = event.getHintTargetSocket(); //we currently dont need to know the targetSocketUri of the message (is known by fetching the connection)

    const currentState = getState();
    const recipientConnUri = event.getReceiver();
    const recipientAtom = getIn(currentState, ["atoms", recipientAtomUri]);

    if (!recipientAtom) {
      console.debug(
        "ignoring hint for an atom that is not yet in the state (could be a targetAtom, or a non stored ownedAtom):",
        recipientAtomUri
      );
    } else if (!recipientConnUri) {
      console.debug("ignoring hint without a receiver(Connection)Uri:", event);
    } else {
      won
        .invalidateCacheForNewConnection(recipientConnUri, recipientAtomUri)
        .then(() => {
          return fetchActiveConnectionAndDispatch(
            recipientConnUri,
            recipientAtomUri,
            dispatch
          );
        })
        .then(connection => {
          const targetAtomUri = connection && connection.targetAtom;
          const targetAtom = getIn(currentState, ["atoms", targetAtomUri]);

          if (targetAtom) {
            return Promise.resolve(won.invalidateCacheForAtom(targetAtomUri));
          } else {
            return fetchTheirAtomAndDispatch(targetAtomUri, dispatch);
          }
        });
    }
  };
}

export function processAtomHintMessage(event) {
  //TODO: Needs refactoring as atomHints are completely different and without a connection since the split into two different hintTypes
  return (dispatch, getState) => {
    //first check if we really have the 'own' atom in the state - otherwise we'll ignore the hint
    const ownedAtomUri = event.getRecipientAtom();
    const targetAtomUri = event.getHintTargetAtom();

    const currentState = getState();
    const ownedAtom = getIn(currentState, ["atoms", ownedAtomUri]);
    const targetAtom = getIn(currentState, ["atoms", targetAtomUri]);

    const ownedConnectionUri = event.getReceiver();

    if (!ownedAtom) {
      console.debug(
        "ignoring hint for an atom that is not yet in the state (could be a targetAtom, or a non stored ownedAtom):",
        ownedAtomUri
      );
    } else if (get(targetAtom, "state") === won.WON.InactiveCompacted) {
      console.debug("ignoring hint for an inactive atom:", targetAtomUri);
    } else {
      won
        .invalidateCacheForNewConnection(ownedConnectionUri, ownedAtomUri)
        .then(() => {
          if (targetAtom) {
            return Promise.resolve(won.invalidateCacheForAtom(targetAtomUri));
          } else {
            return fetchTheirAtomAndDispatch(targetAtomUri, dispatch);
          }
        })
        .then(() =>
          fetchActiveConnectionAndDispatch(
            ownedConnectionUri,
            ownedAtomUri,
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
      const ownedAtomUri = event.getRecipientAtom();
      const ownNodeUri = event.getRecipientNode();
      const theirAtomUri = event.getSenderAtom();
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
          atomUri: ownedAtomUri,
          claimed: true,
        },
      });

      buildChatMessage({
        chatMessage: undefined,
        additionalContent: undefined,
        referencedContentUris: referencedContentUris,
        connectionUri,
        ownedAtomUri,
        theirAtomUri,
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
