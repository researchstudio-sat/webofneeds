import * as ownerApi from "../api/owner-api.js";
import Immutable from "immutable";
import { actionTypes } from "../actions/actions.js";
import * as atomUtils from "./utils/atom-utils.js";
import { parseMetaAtom } from "../reducers/atom-reducer/parse-atom.js";
import { is, get, getIn, numOfEvts2pageSize } from "../utils.js";
import won from "../won-es6";

export function fetchOwnedData(dispatch) {
  return ownerApi
    .getOwnedMetaAtoms()
    .then(metaAtoms => {
      const atomsImm = Immutable.fromJS(metaAtoms);
      dispatch({
        type: actionTypes.atoms.storeOwnedMetaAtoms,
        payload: Immutable.fromJS({
          metaAtoms: atomsImm ? atomsImm : Immutable.Map(),
        }),
      });

      const activeAtomsImm =
        atomsImm &&
        atomsImm.filter(metaAtom =>
          atomUtils.isActive(parseMetaAtom(metaAtom))
        );

      return [...activeAtomsImm.keys()];
    })
    .then(activeAtomUris => fetchDataForOwnedAtoms(activeAtomUris, dispatch));
}

export async function fetchDataForOwnedAtoms(ownedAtomUris, dispatch) {
  if (!is("Array", ownedAtomUris) || ownedAtomUris.length === 0) {
    return;
  }

  return urisToLookupMap(ownedAtomUris, uri =>
    fetchOwnedAtomAndDispatch(uri, dispatch)
  )
    .then(() =>
      urisToLookupMap(ownedAtomUris, atomUri =>
        fetchConnectionsOfAtomAndDispatch(atomUri, dispatch)
      )
    )
    .then(atomConnectionMap => {
      const theirAtomUris = Immutable.fromJS(atomConnectionMap)
        .filter(connections => connections.size > 0)
        .flatMap(entry => entry)
        .map(conn => conn.get("targetAtom"))
        .toSet()
        .toArray();

      dispatch({
        type: actionTypes.atoms.storeTheirUrisInLoading,
        payload: Immutable.fromJS({ uris: theirAtomUris }),
      });
      return theirAtomUris;
    })
    .then(theirAtomUris =>
      urisToLookupMap(theirAtomUris, uri =>
        fetchTheirAtomAndDispatch(uri, dispatch)
      )
    );
}

export function fetchActiveConnectionAndDispatch(connUri, atomUri, dispatch) {
  return won
    .getConnectionWithEventUris(connUri, { requesterWebId: atomUri })
    .then(connection => {
      dispatch({
        type: actionTypes.connections.storeActive,
        payload: Immutable.fromJS({ connections: { [connUri]: connection } }),
      });
      return connection;
    })
    .catch(() => {
      dispatch({
        type: actionTypes.connections.storeUriFailed,
        payload: Immutable.fromJS({ uri: connUri }),
      });
      return;
    });
}

export function fetchTheirAtomAndDispatch(atomUri, dispatch) {
  return won
    .getAtom(atomUri)
    .then(atom => {
      if (atom["hold:heldBy"] && atom["hold:heldBy"]["@id"]) {
        const personaUri = atom["hold:heldBy"]["@id"];
        dispatch({
          type: actionTypes.personas.storeTheirUrisInLoading,
          payload: Immutable.fromJS({ uris: [personaUri] }),
        });
        return won
          .getAtom(personaUri)
          .then(personaAtom => {
            dispatch({
              type: actionTypes.personas.storeTheirs,
              payload: Immutable.fromJS({
                atoms: { [personaUri]: personaAtom },
              }),
            });
            return atom;
          })
          .catch(err => {
            const errResponse = err && err.response;
            const isDeleted = !!(errResponse && errResponse.status == 410);

            dispatch({
              type: isDeleted
                ? actionTypes.personas.removeDeleted
                : actionTypes.personas.storeUriFailed,
              payload: Immutable.fromJS({ uri: personaUri }),
            });
            return atom;
          });
      } else {
        return atom;
      }
    })
    .then(atom => {
      dispatch({
        type: actionTypes.atoms.storeTheirs,
        payload: Immutable.fromJS({ atoms: { [atomUri]: atom } }),
      });
      return atom;
    })
    .catch(err => {
      const errResponse = err && err.response;
      const isDeleted = !!(errResponse && errResponse.status == 410);

      dispatch({
        type: isDeleted
          ? actionTypes.atoms.removeDeleted
          : actionTypes.atoms.storeUriFailed,
        payload: Immutable.fromJS({ uri: atomUri }),
      });
      return;
    });
}

export function fetchDataForNonOwnedAtomOnly(atomUri, dispatch) {
  dispatch({
    type: actionTypes.atoms.storeTheirUrisInLoading,
    payload: Immutable.fromJS({ uris: [atomUri] }),
  });
  return fetchTheirAtomAndDispatch(atomUri, dispatch);
}

export function fetchWhatsNew(
  dispatch,
  getState,
  modifiedAfterDate = new Date(Date.now() - 30 /*Days before*/ * 86400000)
) {
  return ownerApi.getAllMetaAtoms(modifiedAfterDate).then(atoms => {
    const atomsImm = Immutable.fromJS(atoms);
    const atomUris = [...atomsImm.keys()];

    dispatch({
      type: actionTypes.atoms.storeWhatsNew,
      payload: Immutable.fromJS({ metaAtoms: atoms }),
    });
    return atomUris;
  });
}

export function fetchWhatsAround(
  dispatch,
  getState,
  modifiedAfterDate,
  location,
  maxDistance
) {
  return ownerApi
    .getAllMetaAtomsNear(modifiedAfterDate, location, maxDistance)
    .then(atoms => {
      const atomsImm = Immutable.fromJS(atoms);
      const atomUris = [...atomsImm.keys()];

      dispatch({
        type: actionTypes.atoms.storeWhatsAround,
        payload: Immutable.fromJS({
          metaAtoms: atoms,
          location: location,
          maxDistance: maxDistance,
        }),
      });
      return atomUris;
    });
}

export function fetchMessages(
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

function fetchConnectionsOfAtomAndDispatch(atomUri, dispatch) {
  return won
    .getConnectionUrisWithStateByAtomUri(atomUri, atomUri)
    .then(connectionsWithStateAndSocket => {
      dispatch({
        type: actionTypes.connections.storeMetaConnections,
        payload: Immutable.fromJS({
          atomUri: atomUri,
          connections: connectionsWithStateAndSocket,
        }),
      });
      const activeConnectionUris = connectionsWithStateAndSocket
        .filter(conn => conn.connectionState !== won.WON.Closed)
        .filter(conn => conn.connectionState !== won.WON.Suggested)
        .map(conn => conn.connectionUri);

      const targetAtomUris = connectionsWithStateAndSocket.map(
        conn => conn.targetAtomUri
      );

      console.debug("targetAtomUris: ", targetAtomUris);

      dispatch({
        type: actionTypes.connections.storeActiveUrisInLoading,
        payload: Immutable.fromJS({
          atomUri: atomUri,
          connUris: activeConnectionUris,
        }),
      });

      return activeConnectionUris;
    })
    .then(activeConnectionUris =>
      urisToLookupMap(activeConnectionUris, connUri =>
        fetchActiveConnectionAndDispatch(connUri, atomUri, dispatch)
      )
    );
}

function fetchOwnedAtomAndDispatch(atomUri, dispatch) {
  return won
    .getAtom(atomUri)
    .then(atom => {
      dispatch({
        type: actionTypes.atoms.storeOwned,
        payload: Immutable.fromJS({ atoms: { [atomUri]: atom } }),
      });
      return atom;
    })
    .catch(err => {
      const errResponse = err && err.response;
      const isDeleted = !!(errResponse && errResponse.status == 410);

      dispatch({
        type: isDeleted
          ? actionTypes.atoms.removeDeleted
          : actionTypes.atoms.storeUriFailed,
        payload: Immutable.fromJS({ uri: atomUri }),
      });
      return;
    });
}

/**
 * Helper Method to make sure we only load numberOfEvents messages into the store, seems that the cache is not doing what its supposed to do otherwise
 * FIXME: remove this once the fetchpaging works again (or at all)
 * @param state
 * @param connection
 * @param connectionUri
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
 * key-value-pair will not be contained in the result.
 * @param uris
 * @param asyncLookupFunction
 * @param excludeUris uris to exclude from lookup
 * @param abortOnError -> abort the whole crawl by breaking the promisechain instead of ignoring the failures
 * @return {*}
 */
function urisToLookupMap(
  uris,
  asyncLookupFunction,
  excludeUris = [],
  abortOnError = false
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
      if (abortOnError) {
        throw new Error(error);
      } else {
        console.error(
          `failed lookup for ${uri} in utils.js:urisToLookupMap ` +
            error.message,
          "\n\n",
          error.stack,
          "\n\n",
          urisAsArrayWithoutExcludes,
          "\n\n",
          uris,
          "\n\n",
          error
        );
        return undefined;
      }
    })
  );
  return Promise.all(asyncLookups).then(dataObjects => {
    const lookupMap = {};
    //make sure there's the same
    uris.forEach((uri, i) => {
      if (dataObjects[i]) {
        lookupMap[uri] = dataObjects[i];
      }
    });
    return lookupMap;
  });
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
