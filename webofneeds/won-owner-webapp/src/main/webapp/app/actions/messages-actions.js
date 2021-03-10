/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import { actionTypes, actionCreators } from "./actions.js";
import { getIn, getUri, get, extractAtomUriBySocketUri } from "../utils.js";

import Immutable from "immutable";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import { parseAtomContent } from "../reducers/atom-reducer/parse-atom.js";

import { isFetchMessageEffectsNeeded } from "../won-message-utils.js";
import * as stateStore from "../redux/state-store.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as ownerApi from "../api/owner-api.js";
import * as processUtils from "../redux/utils/process-utils";
import paWorker from "workerize-loader?[name].[contenthash:8]!../../parseAtom-worker.js";
import fakeNames from "~/app/fakeNames.json";
import vocab from "~/app/service/vocab";

const parseAtomWorker = paWorker();

export const successfulCloseAtom = wonMessage => (dispatch, getState) => {
  //TODO MAYBE DELETE THIS FUNCTION, I THINK IT SERVES NO PURPOSE
  //TODO maybe refactor these response message handling
  if (
    generalSelectors.isWaitingForAnswer(wonMessage.getIsRemoteResponseTo())(
      getState()
    )
  ) {
    //dispatch(actionCreators.connections__denied(wonMessage));
  }
};
export const successfulReopenAtom = wonMessage => (dispatch, getState) => {
  //TODO MAYBE DELETE THIS FUNCTION, I THINK IT SERVES NO PURPOSE
  //TODO maybe refactor these response message handling
  if (
    generalSelectors.isWaitingForAnswer(wonMessage.getIsRemoteResponseTo())(
      getState()
    )
  ) {
    //dispatch(actionCreators.connections__denied(wonMessage));
  }
};
export const failedCloseAtom = wonMessage => (dispatch, getState) =>
  stateStore
    .fetchAtomAndDispatch(wonMessage.getAtom(), dispatch, getState)
    .then(() => dispatch({ type: actionTypes.messages.closeAtom.failed }));

export const failedReopenAtom = wonMessage => (dispatch, getState) =>
  stateStore
    .fetchAtomAndDispatch(wonMessage.getAtom(), dispatch, getState)
    .then(() => dispatch({ type: actionTypes.messages.reopenAtom.failed }));

export const successfulCloseConnection = wonMessage => (dispatch, getState) => {
  const state = getState();
  //TODO maybe refactor these response message handling
  if (
    generalSelectors.isWaitingForAnswer(wonMessage.getIsRemoteResponseTo())(
      state
    )
  ) {
    dispatch({
      type: actionTypes.messages.close.success,
      payload: wonMessage,
    });
  } else if (
    generalSelectors.isWaitingForAnswer(wonMessage.getIsRemoteResponseTo())(
      state
    )
  ) {
    dispatch({
      type: actionTypes.messages.close.success,
      payload: wonMessage,
    });
  } else {
    //when a connection is closed by the node (e.g. when you close/deactivate an atom all its corresponding connections will be closed)
    dispatch({
      type: actionTypes.messages.close.success,
      payload: wonMessage,
    });
  }
};

export const failedCreate = wonMessage => dispatch => {
  console.error("Failed To Create Atom, Cause in WonMessage: ", wonMessage);

  dispatch(
    actionCreators.atoms__createFailure({
      messageUri: wonMessage.getIsResponseTo(),
      uri: wonMessage.getAtom(),
    })
  );
};

export const successfulCreate = wonMessage => (dispatch, getState) => {
  //load the data into the local rdf store and publish AtomCreatedEvent when done
  const atomUri = wonMessage.getAtom();
  stateStore
    .determineRequestCredentials(
      dispatch,
      getState(),
      atomUri,
      processSelectors.getAtomRequests(atomUri)(getState())
    )
    .then(requestCredentials =>
      won
        .fetchAtom(atomUri, requestCredentials)
        .then(atom => parseAtomWorker.parse(atom, fakeNames, vocab))
        .then(partiallyParsedAtom => {
          const parsedAtomImm = parseAtomContent(partiallyParsedAtom);
          if (parsedAtomImm) {
            dispatch(
              actionCreators.atoms__createSuccessful({
                messageUri: wonMessage.getIsResponseTo(),
                atomUri: atomUri,
                atom: parsedAtomImm,
              })
            );
          }
        })
        .catch(error => {
          if (error.status && error.status === 410) {
            dispatch({
              type: actionTypes.atoms.delete,
              payload: Immutable.fromJS({ uri: atomUri }),
            });
          } else {
            dispatch({
              type: actionTypes.atoms.storeUriFailed,
              payload: Immutable.fromJS({
                uri: atomUri,
                request: {
                  code: error.status,
                  message: error.message,
                  requestCredentials: requestCredentials,
                },
              }),
            });
          }
        })
    );
};

export const failedEdit = wonMessage => dispatch => {
  console.error("Failed To Edit Atom, Cause in WonMessage: ", wonMessage);

  dispatch(
    actionCreators.atoms__editFailure({
      messageUri: wonMessage.getIsResponseTo(),
      uri: wonMessage.getAtom(),
    })
  );
};

export const successfulEdit = wonMessage => (dispatch, getState) => {
  console.debug("Received success replace message:", wonMessage);
  //const state = getState();
  //load the edited data into the local rdf store and publish AtomEditEvent when done
  const atomUri = wonMessage.getAtom();

  const processState = get(getState(), "process");

  if (processUtils.isAtomLoading(processState, atomUri)) {
    console.debug(
      "successfulEdit: Atom is currently loading DO NOT FETCH AGAIN"
    );
  } else {
    stateStore
      .fetchAtomAndDispatch(atomUri, dispatch, getState, true)
      .then(() => {
        dispatch(
          actionCreators.atoms__editSuccessful({
            messageUri: wonMessage.getIsResponseTo(),
            atomUri: atomUri,
            //atom: atom,
          })
        );
      });
  }
};

export const processAgreementMessage = wonMessage => dispatch => {
  dispatch({
    type: actionTypes.messages.processAgreementMessage,
    payload: wonMessage,
  });
};

export const processChangeNotificationMessage = wonMessage => (
  dispatch,
  getState
) => {
  const atomUriToLoad = extractAtomUriBySocketUri(wonMessage.getSenderSocket());

  /*
    Workaround, there is a possibility of a racecondition between the functions processChangeNotificationMessage, and successfulEdit
    e.g the atom could be fetched into the store twice or multiple times (e.g if an atom gets changed that has connections to multiple
    of your own needs, and thus the processChangeNotificationMessage would be called multiple times in a row. this way we ensure that
    there can only be one running fetch/storeClear at a time
    */
  const processState = get(getState(), "process");
  const isAtomLoading = processUtils.isAtomLoading(processState, atomUriToLoad);
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

export const processConnectionMessage = wonMessage => (dispatch, getState) => {
  if (isFetchMessageEffectsNeeded(wonMessage)) {
    const state = getState();
    const senderSocketUri = wonMessage.getSenderSocket();
    const targetSocketUri = wonMessage.getTargetSocket();

    const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);
    const targetAtomUri = extractAtomUriBySocketUri(targetSocketUri);

    const isSentEvent = generalSelectors.isAtomOwned(senderAtomUri)(state);
    const isReceivedEvent = generalSelectors.isAtomOwned(targetAtomUri)(state);

    let sentEventPromise;
    if (isSentEvent) {
      const senderAtom = generalSelectors.getAtom(senderAtomUri)(state);
      const senderConnection = atomUtils.getConnectionBySocketUris(
        senderAtom,
        senderSocketUri,
        targetSocketUri
      );

      const senderConnectionUri = getUri(senderConnection);

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

      const targetConnectionUri = getUri(targetConnection);

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

const processMessageEffectsAndMessage = (
  wonMessage,
  atomUri,
  connectionUri,
  dispatch
) => {
  if (!atomUri || !connectionUri) {
    return Promise.resolve();
  }

  //AgreementDataset
  if (wonMessage.getAcceptsMessageUris()) {
    dispatch({
      type: actionTypes.connections.setLoadedAgreementDataset,
      payload: {
        connectionUri: connectionUri,
        loadedAgreementDataset: false,
      },
    });
  }

  dispatch({
    type: actionTypes.connections.setLoadingPetriNetData,
    payload: {
      connectionUri: connectionUri,
      loadingPetriNetData: true,
    },
  });

  const petriNetPromise = ownerApi
    .fetchPetriNetUris(connectionUri)
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
    .fetchMessageEffects(connectionUri, wonMessage.getMessageUri())
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
};

export const connectFailure = wonMessage => dispatch => {
  console.error(
    "connectFailure received, contents of wonMessage: ",
    wonMessage
  );
  dispatch({
    type: actionTypes.messages.connect.failure,
    payload: Immutable.fromJS({
      message: wonMessage,
    }),
  });
};

export const connectSuccessOwn = wonMessage => (dispatch, getState) => {
  const state = getState();
  const atomUri = extractAtomUriBySocketUri(wonMessage.getSenderSocket());

  const atom = generalSelectors.getAtom(atomUri)(state);

  const connection = atomUtils.getConnection(
    atom,
    "connectionFrom:" + wonMessage.getIsResponseTo()
  );

  if (connection) {
    let connUriPromise;

    if (connectionUtils.isUsingTemporaryUri(connection)) {
      connUriPromise = stateStore.fetchConnectionUriBySocketUris(
        connectionUtils.getSocketUri(connection),
        connectionUtils.getTargetSocketUri(connection),
        {
          requesterWebId: atomUri,
        }
      );
    } else {
      connUriPromise = Promise.resolve(getUri(connection));
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

export const connectSuccessRemote = wonMessage => (dispatch, getState) => {
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
    atomUtils.getConnection(
      atom,
      "connectionFrom:" + wonMessage.getIsResponseTo()
    );

  if (connection) {
    let connUriPromise;

    if (connectionUtils.isUsingTemporaryUri(connection)) {
      connUriPromise = stateStore.fetchConnectionUriBySocketUris(
        targetSocketUri,
        senderSocketUri,
        {
          requesterWebId: atomUri,
        }
      );
    } else {
      connUriPromise = Promise.resolve(getUri(connection));
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

export const processConnectMessage = wonMessage => (dispatch, getState) => {
  const senderSocketUri = wonMessage.getSenderSocket();
  const targetSocketUri = wonMessage.getTargetSocket();
  const state = getState();

  const recipientAtomUri = extractAtomUriBySocketUri(targetSocketUri);

  const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);
  const isOwnSenderAtom = generalSelectors.isAtomOwned(senderAtomUri)(state);
  const isOwnRecipientAtom = generalSelectors.isAtomOwned(recipientAtomUri)(
    state
  );

  //FIXME: ProcessConnectMessage could fetch atoms/connections in a way that makes the atom unloadable
  //e.g Atom is only accessible for Atoms with receivedRequests -> atom is fetched no connection is in the state yet so requesterWebId can't be found by a connection between the two
  // Current solution is to assume that the connectMessage already grants the receiver or the sender access to the atom, so we just try that uri as requesterWebId instead and override the Credentials

  //We know that all own atoms are already stored within the state, so we do not have to retrieve it
  const senderAtomP = isOwnSenderAtom
    ? Promise.resolve(true)
    : stateStore.fetchAtomAndDispatch(
        senderAtomUri,
        dispatch,
        getState,
        false,
        isOwnRecipientAtom ? { requesterWebId: recipientAtomUri } : undefined
      );

  //We know that all own atoms are already stored within the state, so we do not have to retrieve it
  const recipientAtomP = isOwnRecipientAtom
    ? Promise.resolve(true)
    : stateStore.fetchAtomAndDispatch(
        recipientAtomUri,
        dispatch,
        getState,
        false,
        isOwnSenderAtom ? { requesterWebId: senderAtomUri } : undefined
      );

  Promise.all([senderAtomP, recipientAtomP]).then(() => {
    let senderCP;
    if (isOwnSenderAtom) {
      const senderAtom = generalSelectors.getAtom(senderAtomUri)(state);

      const senderConnectionUri = getUri(
        atomUtils.getConnectionBySocketUris(
          senderAtom,
          senderSocketUri,
          targetSocketUri
        )
      );
      senderCP = senderConnectionUri
        ? Promise.resolve(true)
        : stateStore
            .fetchActiveConnectionAndDispatchBySocketUris(
              senderSocketUri,
              targetSocketUri,
              {
                requesterWebId: senderAtomUri,
              },
              dispatch
            )
            .then(() => true);
    } else {
      senderCP = Promise.resolve(false);
    }

    let receiverCP;
    if (isOwnRecipientAtom) {
      const recipientAtom = generalSelectors.getAtom(recipientAtomUri)(state);

      const receiverConnectionUri = getUri(
        atomUtils.getConnectionBySocketUris(
          recipientAtom,
          targetSocketUri,
          senderSocketUri
        )
      );

      receiverCP = receiverConnectionUri
        ? Promise.resolve(true)
        : stateStore
            .determineRequestCredentials(dispatch, state, recipientAtomUri)
            .then(requestCredentials =>
              stateStore.fetchActiveConnectionAndDispatchBySocketUris(
                targetSocketUri,
                senderSocketUri,
                requestCredentials,
                dispatch
              )
            )
            .then(() => true);
    } else {
      console.debug(
        "targetAtom is not ownedAtom, resolve promise with undefined -> ignore the connection"
      );
      receiverCP = Promise.resolve(false);
    }

    //we have to retrieve the personas too
    Promise.all([senderCP, receiverCP]).then(
      ([senderConnectionRelevant, receiverConnectionRelevant]) => {
        if (receiverConnectionRelevant) {
          const newRecipientAtom = generalSelectors.getAtom(recipientAtomUri)(
            getState()
          );
          const newReceiverConnection = atomUtils.getConnectionBySocketUris(
            newRecipientAtom,
            targetSocketUri,
            senderSocketUri
          );
          if (newReceiverConnection) {
            console.debug("Change ReceiverConnectionState ", newRecipientAtom);
            dispatch({
              type: actionTypes.messages.connectMessageReceived,
              payload: {
                updatedConnectionUri: getUri(newReceiverConnection),
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
          const newSenderAtom = generalSelectors.getAtom(senderAtomUri)(
            getState()
          );
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
                updatedConnectionUri: getUri(newSenderConnection),
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

export const updateMessageStatus = wonMessage => dispatch =>
  dispatch({
    type: actionTypes.messages.updateMessageStatus,
    payload: {
      messageUri: wonMessage.messageUri,
      connectionUri: wonMessage.connectionUri,
      atomUri: wonMessage.atomUri,
      messageStatus: wonMessage.messageStatus,
    },
  });

export const atomMessageReceived = wonMessage => (dispatch, getState) => {
  //first check if we really have the 'own' atom in the state - otherwise we'll ignore the hint
  const state = getState();
  const targetAtomUri = extractAtomUriBySocketUri(wonMessage.getTargetSocket());
  const atom = generalSelectors.getAtom(targetAtomUri)(state);
  if (!atom) {
    console.debug(
      "ignoring atomMessage for an atom that is not ours:",
      targetAtomUri
    );
  } else {
    dispatch({
      type: actionTypes.messages.atomMessageReceived,
      payload: {
        atomUri: targetAtomUri,
        humanReadable: atomUtils.getTitle(
          atom,
          generalSelectors.getExternalDataState(state)
        ),
        message: wonMessage.getTextMessage(),
      },
    });
  }
};

export const processSocketHintMessage = wonMessage => (dispatch, getState) => {
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
    return Promise.resolve(false);
  }

  const targetConnection = atomUtils.getConnectionBySocketUris(
    targetAtom,
    targetSocketUri,
    senderSocketUri
  );
  const targetConnectionUri = getUri(targetConnection);

  if (targetConnection) {
    console.debug(
      "receiverConnection already stored, resolve with true -> handle the connection"
    );
    return Promise.resolve(true);
  } else if (!isOwnTargetAtom) {
    console.debug(
      "recipientAtom is not ownedAtom, resolve promise with false -> ignore the connection"
    );
    return Promise.resolve(false);
  } else {
    return stateStore
      .determineRequestCredentials(dispatch, state, targetAtomUri)
      .then(requestCredentials =>
        (!targetConnectionUri
          ? stateStore.fetchActiveConnectionAndDispatchBySocketUris(
              targetSocketUri,
              senderSocketUri,
              requestCredentials,
              dispatch
            )
          : stateStore.fetchActiveConnectionAndDispatch(
              targetConnectionUri,
              requestCredentials,
              dispatch
            )
        ).then(() => true)
      );
  }
};

export const processAtomHintMessage = wonMessage =>
  //TODO: Needs refactoring as atomHints are completely different and without a connection since the split into two different hintTypes
  (dispatch, getState) => {
    //first check if we really have the 'own' atom in the state - otherwise we'll ignore the hint
    const ownedAtomUri = wonMessage.getRecipientAtom();
    const targetAtomUri = wonMessage.getHintTargetAtom();

    const currentState = getState();
    const ownedAtom = getIn(currentState, ["atoms", ownedAtomUri]);
    const targetAtom = getIn(currentState, ["atoms", targetAtomUri]);
    const ownedConnectionUri = wonMessage.getRecipientConnection();

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
          stateStore.determineRequestCredentials(
            dispatch,
            getState(),
            ownedAtomUri
          )
        )
        .then(requestCredentials =>
          stateStore.fetchActiveConnectionAndDispatch(
            ownedConnectionUri,
            requestCredentials,
            dispatch
          )
        );
    }
  };
