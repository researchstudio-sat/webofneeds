/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import { actionTypes, actionCreators } from "./actions.js";
import { getIn } from "../utils.js";

import Immutable from "immutable";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import { isFetchMessageEffectsNeeded } from "../won-message-utils.js";
import * as stateStore from "../redux/state-store.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as ownerApi from "../api/owner-api.js";
import { extractAtomUriBySocketUri, get } from "../utils";
import * as processUtils from "../redux/utils/process-utils";

export function successfulCloseAtom(event) {
  return (dispatch, getState) => {
    //TODO MAYBE DELETE THIS FUNCTION, I THINK IT SERVES NO PURPOSE
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
export function successfulReopenAtom(event) {
  return (dispatch, getState) => {
    //TODO MAYBE DELETE THIS FUNCTION, I THINK IT SERVES NO PURPOSE
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
  return (dispatch, getState) => {
    const atomUri = event.getAtom();

    stateStore
      .fetchAtomAndDispatch(atomUri, dispatch, getState)
      .then(() => dispatch({ type: actionTypes.messages.closeAtom.failed }));
  };
}

export function failedReopenAtom(event) {
  return (dispatch, getState) => {
    const atomUri = event.getAtom();

    stateStore
      .fetchAtomAndDispatch(atomUri, dispatch, getState)
      .then(() => dispatch({ type: actionTypes.messages.reopenAtom.failed }));
  };
}

/*
         recipientAtom: "https://192.168.124.53:8443/won/resource/atom/1741189480636743700"
         senderAtom: "https://192.168.124.53:8443/won/resource/atom/1741189480636743700"
         has....Connection
         event.uri


         vocab.WONMSG.recipientAtom = vocab.WONMSG.baseUri + "recipientAtom";
         vocab.WONMSG.recipientAtomCompacted = vocab.WONMSG.prefix + ":recipientAtom";
         vocab.WONMSG.recipient = vocab.WONMSG.baseUri + "recipient"; // connection if connection event
         vocab.WONMSG.recipientCompacted = vocab.WONMSG.prefix + ":recipient";
         vocab.WONMSG.recipientNode = vocab.WONMSG.baseUri + "recipientNode";
         vocab.WONMSG.recipientNodeCompacted = vocab.WONMSG.prefix + ":recipientNode";
         vocab.WONMSG.senderAtom = vocab.WONMSG.baseUri + "senderAtom";
         vocab.WONMSG.senderAtomCompacted = vocab.WONMSG.prefix + ":senderAtom";
         vocab.WONMSG.sender = vocab.WONMSG.baseUri + "sender";
         vocab.WONMSG.senderCompacted = vocab.WONMSG.prefix + ":sender";
         vocab.WONMSG.senderNode = vocab.WONMSG.baseUri + "senderNode";
         vocab.WONMSG.senderNodeCompacted = vocab.WONMSG.prefix + ":senderNode";
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
    const atomURI = event.getAtom();

    won.getAtom(atomURI).then(atom => {
      dispatch(
        actionCreators.atoms__createSuccessful({
          eventUri: event.getIsResponseTo(),
          atomUri: event.getAtom(),
          atom: atom,
        })
      );
    });
  };
}

export function successfulEdit(event) {
  return (dispatch, getState) => {
    console.debug("Received success replace message:", event);
    //const state = getState();
    //load the edited data into the local rdf store and publish AtomEditEvent when done
    const atomURI = event.getAtom();

    const processState = get(getState(), "process");

    if (processUtils.isAtomLoading(processState, atomURI)) {
      console.debug(
        "successfulEdit: Atom is currently loading DO NOT FETCH AGAIN"
      );
    } else {
      stateStore
        .fetchAtomAndDispatch(atomURI, dispatch, getState, true)
        .then(() => {
          dispatch(
            actionCreators.atoms__editSuccessful({
              eventUri: event.getIsResponseTo(),
              atomUri: event.getAtom(),
              //atom: atom,
            })
          );
        });
    }
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

export function processChangeNotificationMessage(wonMessage) {
  return (dispatch, getState) => {
    const atomUriToLoad = extractAtomUriBySocketUri(
      wonMessage.getSenderSocket()
    );

    /*
    Workaround, there is a possibility of a racecondition between the functions processChangeNotificationMessage, and successfulEdit
    e.g the atom could be fetched into the store twice or multiple times (e.g if an atom gets changed that has connections to multiple
    of your own needs, and thus the processChangeNotificationMessage would be called multiple times in a row. this way we ensure that
    there can only be one running fetch/storeClear at a time
    */
    const processState = get(getState(), "process");
    const isAtomLoading = processUtils.isAtomLoading(
      processState,
      atomUriToLoad
    );
    const isAtomProcessingUpdate = processUtils.isAtomProcessingUpdate(
      processState,
      atomUriToLoad
    );
    if (!isAtomLoading && !isAtomProcessingUpdate) {
      stateStore.fetchAtomAndDispatch(atomUriToLoad, dispatch, getState, true);
    } else {
      console.debug(
        "Omit fetch for processChangeNotificationMessage, fetch is currently in progress for atom: ",
        atomUriToLoad,
        " / isAtomLoading: ",
        isAtomLoading,
        "isAtomProcessingUpdate: ",
        isAtomProcessingUpdate
      );
    }

    dispatch({
      type: actionTypes.messages.processChangeNotificationMessage,
      payload: wonMessage,
    });
  };
}

export function processConnectionMessage(wonMessage) {
  return (dispatch, getState) => {
    if (isFetchMessageEffectsNeeded(wonMessage)) {
      const state = getState();
      const senderSocketUri = wonMessage.getSenderSocket();
      const targetSocketUri = wonMessage.getTargetSocket();

      const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);
      const targetAtomUri = extractAtomUriBySocketUri(targetSocketUri);

      const isSentEvent = generalSelectors.isAtomOwned(senderAtomUri)(state);
      const isReceivedEvent = generalSelectors.isAtomOwned(targetAtomUri)(
        state
      );

      let sentEventPromise;
      if (isSentEvent) {
        const senderAtom = generalSelectors.getAtom(senderAtomUri)(state);
        const senderConnection = atomUtils.getConnectionBySocketUris(
          senderAtom,
          senderSocketUri,
          targetSocketUri
        );

        const senderConnectionUri = get(senderConnection, "uri");

        sentEventPromise = processMessageEffectsAndMessage(
          wonMessage,
          senderAtomUri,
          senderConnectionUri,
          dispatch
        ).then(() => true);
      } else {
        sentEventPromise = Promise.resolve(false);
      }

      let receivedEventPromise;
      if (isReceivedEvent) {
        const targetAtom = generalSelectors.getAtom(targetAtomUri)(state);
        const targetConnection = atomUtils.getConnectionBySocketUris(
          targetAtom,
          targetSocketUri,
          senderSocketUri
        );

        const targetConnectionUri = get(targetConnection, "uri");

        receivedEventPromise = processMessageEffectsAndMessage(
          wonMessage,
          targetAtomUri,
          targetConnectionUri,
          dispatch
        ).then(() => true);
      } else {
        receivedEventPromise = Promise.resolve(false);
      }

      Promise.all([sentEventPromise, receivedEventPromise]).then(
        ([isSentEvent, isReceivedEvent]) => {
          isReceivedEvent &&
            console.debug("connection Message is Relevant for receiver");
          isSentEvent &&
            console.debug("connection Message is Relevant for sender");

          dispatch({
            type: actionTypes.messages.processConnectionMessage,
            payload: wonMessage,
          });
        }
      );
    } else {
      dispatch({
        type: actionTypes.messages.processConnectionMessage,
        payload: wonMessage,
      });
    }
  };
}

function processMessageEffectsAndMessage(
  wonMessage,
  atomUri,
  connectionUri,
  dispatch
) {
  if (!atomUri || !connectionUri) {
    return Promise.resolve();
  }

  dispatch({
    type: actionTypes.connections.setLoadingPetriNetData,
    payload: {
      connectionUri: connectionUri,
      loadingPetriNetData: true,
    },
  });

  const petriNetPromise = ownerApi
    .getPetriNetUris(connectionUri)
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

  const messageEffectsPromise = ownerApi
    .getMessageEffects(connectionUri, wonMessage.getMessageUri())
    .then(response => {
      for (const effect of response) {
        switch (effect.type) {
          case "ACCEPTS":
            if (effect.accepts) {
              let acceptedMessageUris = Array.isArray(effect.acceptedMessageUri)
                ? effect.acceptedMessageUri
                : [effect.acceptedMessageUri];
              acceptedMessageUris.forEach(acceptedMessageUri => {
                dispatch({
                  type: actionTypes.connections.agreementData.markAsAccepted,
                  payload: {
                    messageUri: acceptedMessageUri,
                    connectionUri: connectionUri,
                    atomUri: atomUri,
                    accepted: true,
                  },
                });
              });
              dispatch(
                actionCreators.connections__setLoadedAgreementDataset({
                  connectionUri: connectionUri,
                  loadedAgreementDataset: false,
                })
              );
            }
            break;
          case "CLAIMS":
            if (effect.claims) {
              let claimedMessageUris = Array.isArray(effect.claims)
                ? effect.claims
                : [effect.claims];

              claimedMessageUris.forEach(claimedMessageUri => {
                dispatch({
                  type: actionTypes.connections.agreementData.markAsClaimed,
                  payload: {
                    messageUri: claimedMessageUri,
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
                dispatch({
                  type: actionTypes.connections.agreementData.markAsProposed,
                  payload: {
                    messageUri: proposedMessageUri,
                    connectionUri: connectionUri,
                    atomUri: atomUri,
                    proposed: true,
                  },
                });
              });
            }

            if (effect.proposalType === "CANCELS") {
              let proposesToCancelUris = Array.isArray(effect.proposesToCancel)
                ? effect.proposesToCancel
                : [effect.proposesToCancel];

              proposesToCancelUris.forEach(proposesToCancelURI => {
                dispatch({
                  type:
                    actionTypes.connections.agreementData
                      .markAsCancellationPending,
                  payload: {
                    messageUri: proposesToCancelURI,
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
              let rejectedMessageUris = Array.isArray(effect.rejectedMessageUri)
                ? effect.rejectedMessageUri
                : [effect.rejectedMessageUri];

              rejectedMessageUris.forEach(rejectedMessageUri => {
                dispatch({
                  type: actionTypes.connections.agreementData.markAsRejected,
                  payload: {
                    messageUri: rejectedMessageUri,
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
                dispatch({
                  type: actionTypes.connections.agreementData.markAsRetracted,
                  payload: {
                    messageUri: retractedMessageUri,
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
    });

  return Promise.all([petriNetPromise, messageEffectsPromise]);
}

export function connectSuccessOwn(wonMessage) {
  return (dispatch, getState) => {
    const state = getState();
    const atomUri = extractAtomUriBySocketUri(wonMessage.getSenderSocket());

    const atom = generalSelectors.getAtom(atomUri)(state);

    const connection = getIn(atom, [
      "connections",
      "connectionFrom:" + wonMessage.getIsResponseTo(),
    ]);

    if (connection) {
      let connUriPromise;

      if (connectionUtils.isUsingTemporaryUri(connection)) {
        connUriPromise = stateStore.fetchConnectionUriBySocketUris(
          get(connection, "socketUri"),
          get(connection, "targetSocketUri"),
          atomUri
        );
      } else {
        connUriPromise = Promise.resolve(get(connection, "uri"));
      }

      connUriPromise.then(connUri => {
        dispatch({
          type: actionTypes.messages.connect.successOwn,
          payload: Immutable.fromJS({
            message: wonMessage,
            connUri: connUri,
            atomUri: atomUri,
          }),
        });
      });
    }
  };
}

export function connectSuccessRemote(wonMessage) {
  return (dispatch, getState) => {
    const state = getState();
    const atomUri = extractAtomUriBySocketUri(wonMessage.getTargetSocket());

    const atom = generalSelectors.getAtom(atomUri)(state);
    const targetSocketUri = wonMessage.getTargetSocket();
    const senderSocketUri = wonMessage.getSenderSocket();

    const connection =
      atomUtils.getConnectionBySocketUris(
        atom,
        targetSocketUri,
        senderSocketUri
      ) ||
      getIn(atom, [
        "connections",
        "connectionFrom:" + wonMessage.getIsResponseTo(),
      ]);

    if (connection) {
      let connUriPromise;

      if (connectionUtils.isUsingTemporaryUri(connection)) {
        connUriPromise = stateStore.fetchConnectionUriBySocketUris(
          targetSocketUri,
          senderSocketUri,
          atomUri
        );
      } else {
        connUriPromise = Promise.resolve(get(connection, "uri"));
      }

      connUriPromise.then(connUri => {
        dispatch({
          type: actionTypes.messages.connect.successRemote,
          payload: Immutable.fromJS({
            message: wonMessage,
            connUri: connUri,
            atomUri: atomUri,
          }),
        });
      });
    }
  };
}

export function processConnectMessage(wonMessage) {
  return (dispatch, getState) => {
    const senderSocketUri = wonMessage.getSenderSocket();
    const targetSocketUri = wonMessage.getTargetSocket();
    const state = getState();

    const recipientAtomUri = extractAtomUriBySocketUri(targetSocketUri);

    const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);

    const senderAtom = generalSelectors.getAtom(senderAtomUri)(state);
    const recipientAtom = generalSelectors.getAtom(recipientAtomUri)(state);
    const isOwnSenderAtom = generalSelectors.isAtomOwned(senderAtomUri)(state);
    const isOwnRecipientAtom = generalSelectors.isAtomOwned(recipientAtomUri)(
      state
    );

    const receiverConnectionUri = get(
      atomUtils.getConnectionBySocketUris(
        recipientAtom,
        targetSocketUri,
        senderSocketUri
      ),
      "uri"
    );
    const senderConnectionUri = get(
      atomUtils.getConnectionBySocketUris(
        senderAtom,
        senderSocketUri,
        targetSocketUri
      ),
      "uri"
    );

    let senderAtomP;
    if (isOwnSenderAtom) {
      //We know that all own atoms are already stored within the state, so we do not have to retrieve it
      senderAtomP = Promise.resolve(true);
    } else {
      senderAtomP = stateStore.fetchAtomAndDispatch(
        senderAtomUri,
        dispatch,
        getState
      );
    }

    let recipientAtomP;
    if (isOwnRecipientAtom) {
      //We know that all own atoms are already stored within the state, so we do not have to retrieve it
      recipientAtomP = Promise.resolve(true);
    } else {
      recipientAtomP = stateStore.fetchAtomAndDispatch(
        recipientAtomUri,
        dispatch,
        getState
      );
    }

    Promise.all([senderAtomP, recipientAtomP]).then(() => {
      let senderCP;
      if (isOwnSenderAtom) {
        if (!senderConnectionUri) {
          senderCP = stateStore
            .fetchActiveConnectionAndDispatchBySocketUris(
              senderSocketUri,
              targetSocketUri,
              senderAtomUri,
              dispatch
            )
            .then(() => true);
        } else {
          console.debug(
            "senderConnection relevant and we already have it, resolve with true -> handle the connection"
          );
          senderCP = Promise.resolve(true);
        }
      } else {
        console.debug(
          "senderAtom is not ownedAtom, resolve promise with undefined -> ignore the connection"
        );
        senderCP = Promise.resolve(false);
      }

      let receiverCP;
      if (isOwnRecipientAtom) {
        if (!receiverConnectionUri) {
          receiverCP = stateStore
            .fetchActiveConnectionAndDispatchBySocketUris(
              targetSocketUri,
              senderSocketUri,
              recipientAtomUri,
              dispatch
            )
            .then(() => true);
        } else {
          console.debug(
            "targetConnection relevant and we already have it, resolve with true -> handle the connection"
          );
          receiverCP = Promise.resolve(true);
        }
      } else {
        console.debug(
          "targetAtom is not ownedAtom, resolve promise with undefined -> ignore the connection"
        );
        receiverCP = Promise.resolve(false);
      }

      //we have to retrieve the personas too
      Promise.all([senderCP, receiverCP]).then(
        ([senderConnectionRelevant, receiverConnectionRelevant]) => {
          const newState = getState();

          if (receiverConnectionRelevant) {
            const newRecipientAtom = getIn(newState, [
              "atoms",
              recipientAtomUri,
            ]);
            const newReceiverConnection = atomUtils.getConnectionBySocketUris(
              newRecipientAtom,
              targetSocketUri,
              senderSocketUri
            );
            if (newReceiverConnection) {
              console.debug(
                "Change ReceiverConnectionState ",
                newRecipientAtom
              );
              dispatch({
                type: actionTypes.messages.connectMessageReceived,
                payload: {
                  updatedConnectionUri: get(newReceiverConnection, "uri"),
                  ownedAtomUri: recipientAtomUri,
                  message: wonMessage,
                },
              });
            } else {
              console.debug(
                "Dont change ReceiverConnectionState, receiverConnection is not present yet"
              );
            }
          }

          if (senderConnectionRelevant) {
            const newSenderAtom = getIn(newState, ["atoms", senderAtomUri]);
            const newSenderConnection = atomUtils.getConnectionBySocketUris(
              newSenderAtom,
              senderSocketUri,
              targetSocketUri
            );

            if (newSenderConnection) {
              console.debug("Change SenderConnectionState ", newSenderAtom);
              dispatch({
                type: actionTypes.messages.connectMessageSent,
                payload: {
                  updatedConnectionUri: get(newSenderConnection, "uri"),
                  senderAtomUri: senderAtomUri,
                  event: wonMessage,
                },
              });
            } else {
              console.debug(
                "Dont change SenderConnectionState, senderConnection is not present yet"
              );
            }
          }
        }
      );
    });
  };
}

export function updateMessageStatus(event) {
  return dispatch => {
    const payload = {
      messageUri: event.messageUri,
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

export function atomMessageReceived(wonMessage) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' atom in the state - otherwise we'll ignore the hint
    const targetAtomUri = extractAtomUriBySocketUri(
      wonMessage.getTargetSocket()
    );
    const atom = getState().getIn(["atoms", targetAtomUri]);
    if (!atom) {
      console.debug(
        "ignoring atomMessage for an atom that is not ours:",
        targetAtomUri
      );
    }
    dispatch({
      type: actionTypes.messages.atomMessageReceived,
      payload: {
        atomUri: targetAtomUri,
        humanReadable: atomUtils.getTitle(
          atom,
          generalSelectors.getExternalDataState(getState())
        ),
        message: wonMessage.getTextMessage(),
      },
    });
  };
}

export function processSocketHintMessage(wonMessage) {
  return (dispatch, getState) => {
    const targetSocketUri = wonMessage.getTargetSocket();
    const senderSocketUri = wonMessage.getHintTargetSocket();

    const state = getState();
    const targetAtomUri = extractAtomUriBySocketUri(targetSocketUri);

    const targetAtom = generalSelectors.getAtom(targetAtomUri)(state);
    const isOwnTargetAtom = generalSelectors.isAtomOwned(targetAtomUri)(state);

    if (!targetAtom) {
      console.debug(
        "ignoring hint for an atom that is not yet in the state (could be a targetAtom, or a non stored ownedAtom):",
        targetAtomUri
      );
    }

    const targetConnection = atomUtils.getConnectionBySocketUris(
      targetAtom,
      targetSocketUri,
      senderSocketUri
    );
    const targetConnectionUri = get(targetConnection, "uri");

    if (!targetConnectionUri && isOwnTargetAtom) {
      return stateStore
        .fetchActiveConnectionAndDispatchBySocketUris(
          targetSocketUri,
          senderSocketUri,
          targetAtomUri,
          dispatch
        )
        .then(() => true);
    } else if (!targetConnectionUri || !isOwnTargetAtom) {
      console.debug(
        "receiverConnectionUri was null or recipientAtom is not ownedAtom, resolve promise with undefined -> ignore the connection"
      );
      return Promise.resolve(false);
    } else if (targetConnection) {
      console.debug(
        "receiverConnection relevant, resolve with true -> handle the connection"
      );
      return Promise.resolve(true);
    } else {
      return stateStore
        .fetchActiveConnectionAndDispatch(
          targetConnectionUri,
          targetAtomUri,
          dispatch
        )
        .then(() => true);
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

    const ownedConnectionUri = event.getRecipientConnection();

    if (!ownedAtom) {
      console.debug(
        "ignoring hint for an atom that is not yet in the state (could be a targetAtom, or a non stored ownedAtom):",
        ownedAtomUri
      );
    } else if (atomUtils.isInactive(targetAtom)) {
      console.debug("ignoring hint for an inactive atom:", targetAtomUri);
    } else {
      Promise.resolve()
        .then(() => {
          if (targetAtom) {
            return Promise.resolve(true);
          } else {
            return stateStore.fetchAtomAndDispatch(
              targetAtomUri,
              dispatch,
              getState
            );
          }
        })
        .then(() =>
          stateStore.fetchActiveConnectionAndDispatch(
            ownedConnectionUri,
            ownedAtomUri,
            dispatch
          )
        );
    }
  };
}
