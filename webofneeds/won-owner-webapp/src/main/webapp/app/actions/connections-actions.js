/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import Immutable from "immutable";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../api/owner-api.js";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors.js";

import { get, getIn, is } from "../utils.js";

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

import * as processUtils from "../redux/utils/process-utils.js";

export function connectionsChatMessageClaimOnSuccess(
  chatMessage,
  additionalContent,
  connectionUri
) {
  return (dispatch, getState) => {
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const theirAtomUri = getIn(getState(), [
      "atoms",
      get(ownedAtom, "uri"),
      "connections",
      connectionUri,
      "targetAtomUri",
    ]);
    const theirAtom = getIn(getState(), ["atoms", theirAtomUri]);
    const theirConnectionUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetConnectionUri",
    ]);

    buildChatMessage({
      chatMessage: chatMessage,
      additionalContent: additionalContent,
      referencedContentUris: undefined,
      connectionUri,
      ownedAtomUri: get(ownedAtom, "uri"),
      theirAtomUri: theirAtomUri,
      ownNodeUri: get(ownedAtom, "nodeUri"),
      theirNodeUri: get(theirAtom, "nodeUri"),
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
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const theirAtomUri = getIn(getState(), [
      "atoms",
      get(ownedAtom, "uri"),
      "connections",
      connectionUri,
      "targetAtomUri",
    ]);
    const theirAtom = getIn(getState(), ["atoms", theirAtomUri]);
    const theirConnectionUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetConnectionUri",
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
          const correctUri = get(msg, "remoteUri") || get(msg, "uri");
          if (correctUri) contentUris.push({ "@id": correctUri });
          //THE PARTS BELOW SHOULD NOT BE CALLED WITHIN THIS DISPATCH
          switch (key) {
            case "retracts":
              dispatch({
                type: actionTypes.messages.messageStatus.markAsRetracted,
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
                type: actionTypes.messages.messageStatus.markAsRejected,
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
                  actionTypes.messages.messageStatus.markAsCancellationPending,
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
                type: actionTypes.messages.messageStatus.markAsAccepted,
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
                type: actionTypes.messages.messageStatus.markAsClaimed,
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
                type: actionTypes.messages.messageStatus.markAsProposed,
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
      connectionUri,
      ownedAtomUri: get(ownedAtom, "uri"),
      theirAtomUri: theirAtomUri,
      ownNodeUri: get(ownedAtom, "nodeUri"),
      theirNodeUri: get(theirAtom, "nodeUri"),
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
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const theirAtomUri = getIn(getState(), [
      "atoms",
      get(ownedAtom, "uri"),
      "connections",
      connectionUri,
      "targetAtomUri",
    ]);
    const theirAtom = getIn(getState(), ["atoms", theirAtomUri]);
    const theirConnectionUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetConnectionUri",
    ]);

    const openMsg = await buildOpenMessage(
      connectionUri,
      get(ownedAtom, "uri"),
      theirAtomUri,
      get(ownedAtom, "nodeUri"),
      get(theirAtom, "nodeUri"),
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
    const connectToAtom = getIn(state, ["atoms", connectToAtomUri]);

    const nodeUri = getIn(state, ["config", "defaultNodeUri"]);

    // create new atom
    const { message, eventUri, atomUri } = await buildCreateMessage(
      atomDraft,
      nodeUri
    );

    // create the new atom
    dispatch({
      type: actionTypes.atoms.create, // TODO custom action
      payload: { eventUri, message, atomUri, atom: atomDraft },
    });

    dispatch(
      actionCreators.router__stateGo("connections", {
        useCase: undefined,
        useCaseGroup: undefined,
        fromAtomUri: undefined,
        viewConnUri: undefined,
        mode: undefined,
      })
    );

    // add persona if present
    if (personaUri) {
      const persona = getIn(state, ["atoms", personaUri]);
      ownerApi
        .serverSideConnect(
          atomUtils.getSocketUri(persona, won.HOLD.HolderSocketCompacted),
          `${atomUri}#holdableSocket`,
          false,
          true
        )
        .then(async response => {
          if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(`Could not connect identity: ${errorMsg}`);
          }
        });
    }

    if (generalSelectors.isAtomOwned(state, connectToAtomUri)) {
      const connectToSocketUri = connectToSocketType
        ? atomUtils.getSocketUri(connectToAtom, connectToSocketType)
        : atomUtils.getDefaultSocketUri(connectToAtom);

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

      const atomDraftSocketUri = getSocketFromDraft(atomDraft);

      if (atomDraftSocketUri && connectToSocketUri) {
        ownerApi
          .serverSideConnect(
            connectToSocketUri,
            `${atomUri}${atomDraftSocketUri}`,
            false,
            true
          )
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
      // establish connection
      const cnctMsg = buildConnectMessage({
        ownedAtomUri: atomUri,
        theirAtomUri: connectToAtomUri,
        ownNodeUri: nodeUri,
        theirNodeUri: get(connectToAtom, "nodeUri"),
        connectMessage: "",
      });

      const connectToSocketUri = connectToSocketType
        ? atomUtils.getSocketUri(connectToAtom, connectToSocketType)
        : atomUtils.getDefaultSocketUri(connectToAtom);

      won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent => {
        // connect action to be dispatched when the
        // ad hoc atom has been created:
        //TODO: FIGURE OUT WHICH SOCKETS WILL BE CONNECTED
        const connectAction = {
          type: actionTypes.atoms.connect,
          payload: {
            eventUri: cnctMsg.eventUri,
            message: cnctMsg.message,
            optimisticEvent: optimisticEvent,
            targetSocketUri: connectToSocketUri,
          },
        };

        // register the connect action to be dispatched when
        // atom creation is successful
        dispatch({
          type: actionTypes.messages.dispatchActionOn.registerSuccessOwn,
          payload: {
            eventUri: eventUri,
            actionToDispatch: connectAction,
          },
        });
      });
    }
  });
}

export function connectionsConnectAdHoc(theirAtomUri, textMessage, persona) {
  return (dispatch, getState) =>
    connectAdHoc(theirAtomUri, textMessage, persona, dispatch, getState); // moved to separate function to make transpilation work properly
}
function connectAdHoc(
  theirAtomUri,
  textMessage,
  personaUri,
  dispatch,
  getState
) {
  ensureLoggedIn(dispatch, getState).then(async () => {
    const state = getState();
    const theirAtom = getIn(state, ["atoms", theirAtomUri]);
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

    // create new atom
    const { message, eventUri, atomUri } = await buildCreateMessage(
      adHocDraft,
      nodeUri
    );

    // add persona
    if (personaUri) {
      const persona = getIn(state, ["atoms", personaUri]);
      const response = await ownerApi.serverSideConnect(
        atomUtils.getSocketUri(persona, won.HOLD.HolderSocketCompacted),
        `${atomUri}#holdableSocket`,
        false,
        true
      );
      if (!response.ok) {
        const errorMsg = await response.text();
        throw new Error(`Could not connect identity: ${errorMsg}`);
      }
    }

    // establish connection
    const cnctMsg = buildConnectMessage({
      ownedAtomUri: atomUri,
      theirAtomUri: theirAtomUri,
      ownNodeUri: nodeUri,
      theirNodeUri: get(theirAtom, "nodeUri"),
      connectMessage: textMessage,
    });

    won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent => {
      // connect action to be dispatched when the
      // ad hoc atom has been created:
      //TODO: FIGURE OUT WHICH SOCKETS WILL BE CONNECTED
      const connectAction = {
        type: actionTypes.atoms.connect,
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
          },
        },
      });

      // register the connect action to be dispatched when
      // atom creation is successful
      dispatch({
        type: actionTypes.messages.dispatchActionOn.registerSuccessOwn,
        payload: {
          eventUri: eventUri,
          actionToDispatch: connectAction,
        },
      });

      // create the new atom
      dispatch({
        type: actionTypes.atoms.create, // TODO custom action
        payload: { eventUri, message, atomUri, atom: adHocDraft },
      });
    });
  });
}

export function connectionsClose(connectionUri) {
  return (dispatch, getState) => {
    const ownedAtom = get(getState(), "atoms").find(atom =>
      getIn(atom, ["connections", connectionUri])
    );
    const theirAtomUri = getIn(getState(), [
      "atoms",
      get(ownedAtom, "uri"),
      "connections",
      connectionUri,
      "targetAtomUri",
    ]);
    const theirAtom = getIn(getState(), ["atoms", theirAtomUri]);
    const theirConnectionUri = getIn(ownedAtom, [
      "connections",
      connectionUri,
      "targetConnectionUri",
    ]);

    buildCloseMessage(
      connectionUri,
      get(ownedAtom, "uri"),
      theirAtomUri,
      get(ownedAtom, "nodeUri"),
      get(theirAtom, "nodeUri"),
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
  //Closes the 'targetConnection' again, if closeConnections(...) only closes the 'own' connection
  return dispatch => {
    const connectionUri = message.getSenderConnection();
    const targetAtomUri = message.getSenderAtom();
    const remoteNode = message.getSenderNode();
    const ownedAtomUri = message.getRecipientAtom();
    const ownNode = message.getRecipientNode();

    buildCloseMessage(
      connectionUri,
      targetAtomUri,
      ownedAtomUri,
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
        let msgToRateFor = { connection: connection };

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
 *   that the view atoms. Note that the
 *   actual number varies due the varying number
 *   of success-responses the server includes and
 *   because the API only accepts a count of
 *   events that include the latter.
 * @return {Function}
 */
export function showLatestMessages(connectionUriParam, numberOfEvents) {
  return (dispatch, getState) => {
    const state = getState();
    const connectionUri =
      connectionUriParam || generalSelectors.getConnectionUriFromRoute(state);
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

    const fetchParams = {
      requesterWebId: atomUri,
      pagingSize: numOfEvts2pageSize(numberOfEvents),
      deep: true,
    };
    return fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents,
      fetchParams
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

  const fetchParams = {
    requesterWebId: atomUri,
    pagingSize: numOfEvts2pageSize(numberOfEvents),
    deep: true,
  };

  fetchMessages(
    dispatch,
    state,
    connectionUri,
    atomUri,
    numberOfEvents,
    fetchParams
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
export function showMoreMessages(connectionUriParam, numberOfEvents) {
  return (dispatch, getState) => {
    const state = getState();
    const connectionUri =
      connectionUriParam || generalSelectors.getConnectionUriFromRoute(state);
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
    const messageHashValue =
      oldestMessageUri && oldestMessageUri.replace(/.*\/event\/(.*)/, "$1"); // everything following the `/event/`

    const fetchParams = {
      requesterWebId: atomUri,
      pagingSize: numOfEvts2pageSize(numberOfEvents),
      deep: true,
      resumebefore: messageHashValue,
    };

    return fetchMessages(
      dispatch,
      state,
      connectionUri,
      atomUri,
      numberOfEvents,
      fetchParams
    );
  };
}

async function fetchMessages(
  dispatch,
  state,
  connectionUri,
  atomUri,
  numberOfEvents,
  fetchParams
) {
  dispatch({
    type: actionTypes.connections.fetchMessagesStart,
    payload: Immutable.fromJS({ connectionUri: connectionUri }),
  });

  return won
    .getConnectionWithEventUris(connectionUri, fetchParams)
    .then(connection =>
      getMessageUrisToLoad(
        dispatch,
        state,
        connection,
        connectionUri,
        numberOfEvents
      )
    )
    .then(eventUris => {
      return urisToLookupSuccessAndFailedMap(
        eventUris,
        eventUri => won.getWonMessage(eventUri, { requesterWebId: atomUri }),
        []
      );
    })
    .then(events => storeMessages(dispatch, events, connectionUri));
}

async function getMessageUrisToLoad(
  dispatch,
  state,
  connection,
  connectionUri,
  numberOfEvents
) {
  console.debug(
    "getMessageUrisToLoad of connection(uri:",
    connectionUri,
    "): ",
    connection
  );
  const messagesToFetch = limitNumberOfEventsToFetchInConnection(
    state,
    connection,
    connectionUri,
    numberOfEvents
  );

  dispatch({
    type: actionTypes.connections.messageUrisInLoading,
    payload: Immutable.fromJS({
      connectionUri: connectionUri,
      uris: messagesToFetch,
    }),
  });

  return messagesToFetch;
}

/**
 * Helper function that stores dispatches the success and failed actions for a given set of messages
 * @param messages
 * @param connectionUri
 */
function storeMessages(dispatch, messages, connectionUri) {
  if (messages) {
    const messagesImm = Immutable.fromJS(messages);
    const successMessages = get(messagesImm, "success");
    const failedMessages = get(messagesImm, "failed");

    if (successMessages.size > 0) {
      dispatch({
        type: actionTypes.connections.fetchMessagesSuccess,
        payload: Immutable.fromJS({
          connectionUri: connectionUri,
          events: successMessages,
        }),
      });
    }

    if (failedMessages.size > 0) {
      dispatch({
        type: actionTypes.connections.fetchMessagesFailed,
        payload: Immutable.fromJS({
          connectionUri: connectionUri,
          events: failedMessages,
        }),
      });
    }

    /*If neither succes nor failed has any elements we simply say that fetching Ended, that way
    we can ensure that there is not going to be a lock on the connection because loadingMessages was complete but never
    reset its status
    */
    if (successMessages.size == 0 && failedMessages.size == 0) {
      dispatch({
        type: actionTypes.connections.fetchMessagesEnd,
        payload: Immutable.fromJS({ connectionUri: connectionUri }),
      });
    }
  }
}

function numOfEvts2pageSize(numberOfEvents) {
  // `*3*` to compensate for the *roughly* 2 additional success events per chat message
  return numberOfEvents * 3;
}

/**
 * Helper Method to make sure we only load numberOfEvents messages into the store, seems that the cache is not doing what its supposed to do otherwise
 * FIXME: remove this once the fetchpaging works again (or at all)
 * @param state
 * @param connection
 * @param numberOfEvents
 * @returns {Array}
 */
function limitNumberOfEventsToFetchInConnection(
  state,
  connection,
  connectionUri,
  numberOfEvents
) {
  const connectionImm = Immutable.fromJS(connection);

  const allMessagesToLoad = getIn(state, [
    "process",
    "connections",
    connectionUri,
    "messages",
  ]).filter(msg => get(msg, "toLoad") && !get(msg, "failedToLoad"));
  let messagesToFetch = [];

  const fetchedConnectionEvents =
    connectionImm &&
    get(connectionImm, "hasEvents") &&
    get(connectionImm, "hasEvents").filter(eventUri => !!eventUri); //Filter out undefined/null values

  if (fetchedConnectionEvents && fetchedConnectionEvents.size > 0) {
    fetchedConnectionEvents.map(eventUri => {
      if (
        allMessagesToLoad.has(eventUri) &&
        messagesToFetch.length < numOfEvts2pageSize(numberOfEvents)
      ) {
        messagesToFetch.push(eventUri);
      }
    });
  } else {
    allMessagesToLoad.map((messageStatus, messageUri) => {
      messagesToFetch.push(messageUri);
    });
  }

  return messagesToFetch;
}

/**
 * Takes a single uri or an array of uris, performs the lookup function on each
 * of them seperately, collects the results and builds an map/object
 * with the uris as keys and the results as values.
 * If any call to the asyncLookupFunction fails, the corresponding
 * key-value-pair will not be contained in the success-result but rather in the failed-results.
 * @param uris
 * @param asyncLookupFunction
 * @param excludeUris uris to exclude from lookup
 * @return {*}
 */
function urisToLookupSuccessAndFailedMap(
  uris,
  asyncLookupFunction,
  excludeUris = []
) {
  //make sure we have an array and not a single uri.
  const urisAsArray = is("Array", uris) ? uris : [uris];
  const excludeUrisAsArray = is("Array", excludeUris)
    ? excludeUris
    : [excludeUris];

  const urisAsArrayWithoutExcludes = urisAsArray.filter(
    uri => excludeUrisAsArray.indexOf(uri) < 0
  );

  const asyncLookups = urisAsArrayWithoutExcludes.map(uri =>
    asyncLookupFunction(uri).catch(error => {
      return error;
    })
  );
  return Promise.all(asyncLookups).then(dataObjects => {
    const lookupMap = { success: {}, failed: {} };
    //make sure there's the same
    uris.forEach((uri, i) => {
      if (dataObjects[i] instanceof Error) {
        lookupMap["failed"][uri] = dataObjects[i];
      } else if (dataObjects[i]) {
        lookupMap["success"][uri] = dataObjects[i];
      }
    });
    return lookupMap;
  });
}
