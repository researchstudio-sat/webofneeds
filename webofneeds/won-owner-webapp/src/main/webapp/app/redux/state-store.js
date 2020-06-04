import * as ownerApi from "../api/owner-api.js";
import Immutable from "immutable";
import { actionTypes } from "../actions/actions.js";
import * as atomUtils from "./utils/atom-utils.js";
import * as processUtils from "./utils/process-utils.js";
import { parseMetaAtom } from "../reducers/atom-reducer/parse-atom.js";
import { get, getIn, is } from "../utils.js";
import won from "../won-es6";
import vocab from "../service/vocab.js";

export function fetchOwnedData(dispatch, getState) {
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
    .then(activeAtomUris =>
      fetchDataForOwnedAtoms(activeAtomUris, dispatch, getState)
    );
}

/**
 * fetches Data incl. connections for given array of atomUris
 * @param ownedAtomUris atomUris to be fetched
 * @param dispatch redux dispatcher
 * @param getState redux state
 * @param forceFetch bool if true, then fetch will be executed even if atom is already in the state (useful on edit)
 * @returns {Promise<void>|*}
 */
export function fetchDataForOwnedAtoms(
  ownedAtomUris,
  dispatch,
  getState,
  forceFetch = false
) {
  if (!is("Array", ownedAtomUris) || ownedAtomUris.length === 0) {
    return Promise.resolve();
  }

  return urisToLookupMap(ownedAtomUris, uri =>
    fetchOwnedAtomAndDispatch(uri, dispatch, getState, forceFetch)
  ).then(() =>
    urisToLookupMap(ownedAtomUris, atomUri =>
      fetchConnectionsOfAtomAndDispatch(atomUri, dispatch)
    )
  );
}

export function fetchActiveConnectionAndDispatch(connUri, atomUri, dispatch) {
  return won
    .getConnection(connUri, { requesterWebId: atomUri })
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
    });
}

export function fetchConnectionUriBySocketUris(
  senderSocketUri,
  targetSocketUri,
  atomUri
) {
  return won
    .getConnectionUrisBySocket(senderSocketUri, targetSocketUri, {
      requesterWebId: atomUri,
    })
    .catch(() => {
      console.error(
        "Fetch of ConnectionUri of sockets",
        senderSocketUri,
        "<->",
        targetSocketUri,
        "failed"
      );
    });
}

export function fetchActiveConnectionAndDispatchBySocketUris(
  senderSocketUri,
  targetSocketUri,
  atomUri,
  dispatch
) {
  return won
    .getConnectionBySocket(senderSocketUri, targetSocketUri, {
      requesterWebId: atomUri,
    })
    .then(conn => {
      dispatch({
        type: actionTypes.connections.storeActive,
        payload: Immutable.fromJS({ connections: { [conn.uri]: conn } }),
      });
      return conn;
    })
    .catch(() => {
      /*dispatch({
        type: actionTypes.connections.storeUriFailed,
        payload: Immutable.fromJS({ uri: connUri }),
      });*/
      console.error(
        "Store Connection of sockets",
        senderSocketUri,
        "<->",
        targetSocketUri,
        "failed"
      );
    });
}

/**
 * Fetches an atom (incl. the persona that holds it), the fetch is omitted if:
 * - the atom is already loaded
 * - the atom is currently loading
 * Omit fetching the persona attached to the atom if:
 * - there is no attached persona
 * - the persona is already loaded
 * - the persona is currently loading
 *
 * If update is set to true, the loaded status of an atom does NOT omit the fetch
 * @param {String} atomUri
 * @param dispatch
 * @param {function} getState
 * @param {boolean} update, defaults to false
 * @returns {*}
 */
export function fetchAtomAndDispatch(
  atomUri,
  dispatch,
  getState,
  update = false
) {
  const processState = get(getState(), "process");

  if (!update && processUtils.isAtomLoaded(processState, atomUri)) {
    console.debug("Omit Fetch of Atom<", atomUri, ">, it is already loaded...");
    return Promise.resolve();
  } else if (processUtils.isAtomLoading(processState, atomUri)) {
    console.debug(
      "Omit Fetch of Atom<",
      atomUri,
      ">, it is currently loading..."
    );
    return Promise.resolve();
  }
  console.debug("Proceed Fetch of Atom<", atomUri, ">");

  dispatch({
    type: actionTypes.atoms.storeUriInLoading,
    payload: Immutable.fromJS({ uri: atomUri }),
  });

  return won
    .getAtom(atomUri)
    .then(atom => {
      if (
        atom[vocab.HOLD.heldByCompacted] &&
        atom[vocab.HOLD.heldByCompacted]["@id"]
      ) {
        const personaUri = atom[vocab.HOLD.heldByCompacted]["@id"];
        if (processUtils.isAtomLoaded(processState, personaUri)) {
          console.debug(
            "Omit Fetch of Persona<",
            personaUri,
            "> attached to Atom, it is already loaded"
          );
          return atom;
        } else if (processUtils.isAtomLoading(processState, personaUri)) {
          console.debug(
            "Omit Fetch of Persona<",
            personaUri,
            "> attached to Atom, it is currently loading..."
          );
          return atom;
        } else {
          dispatch({
            type: actionTypes.personas.storeUriInLoading,
            payload: Immutable.fromJS({ uri: personaUri }),
          });
          return won
            .getAtom(personaUri)
            .then(personaAtom => {
              dispatch({
                type: actionTypes.personas.store,
                payload: Immutable.fromJS({
                  atoms: { [personaUri]: personaAtom },
                }),
              });
              return atom;
            })
            .then(atom => {
              //Fetch All MetaConnections Of NonOwnedPersonaAndDispatch //TODO: ENHANCE LOADING PROCESS BY LOADING CONNECTIONS ONLY ON POST VIEW
              fetchConnectionsOfNonOwnedAtomAndDispatch(personaUri, dispatch);
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
        }
      } else {
        return atom;
      }
    })
    .then(atom => {
      dispatch({
        type: actionTypes.atoms.store,
        payload: Immutable.fromJS({ atoms: { [atomUri]: atom } }),
      });
      return atom;
    })
    .then(atom => {
      //Fetch All MetaConnections Of NonOwnedAtomAndDispatch //TODO: ENHANCE LOADING PROCESS BY LOADING CONNECTIONS ONLY ON POST VIEW
      fetchConnectionsOfNonOwnedAtomAndDispatch(atomUri, dispatch);
      return atom;
    })
    .catch(() => {
      dispatch({
        type: actionTypes.atoms.storeUriFailed,
        payload: Immutable.fromJS({ uri: atomUri }),
      });
    });
}

export function fetchPersonas(dispatch /*, getState,*/) {
  return ownerApi.getAllActiveMetaPersonas().then(atoms => {
    const atomsImm = Immutable.fromJS(atoms);
    const atomUris = [...atomsImm.keys()];

    dispatch({
      type: actionTypes.atoms.storeMetaAtoms,
      payload: Immutable.fromJS({ metaAtoms: atoms }),
    });

    return atomUris;
  });
}

export function fetchWhatsNew(
  dispatch,
  getState,
  createdAfterDate = new Date(Date.now() - 30 /*Days before*/ * 86400000)
) {
  return ownerApi.getAllMetaAtoms(createdAfterDate).then(atoms => {
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
  createdAfterDate,
  location,
  maxDistance
) {
  return ownerApi
    .getAllMetaAtomsNear(createdAfterDate, location, maxDistance)
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
  numberOfMessages,
  resumeAfter /*msgUri: load numberOfEvents before this msgUri*/
) {
  const fetchParams = {
    requesterWebId: atomUri,
    pagingSize: numberOfMessages * 3, // `*3*` to compensate for the *roughly* 2 additional success messages per chat message
    deep: true,
    resumeafter: resumeAfter,
  };

  dispatch({
    type: actionTypes.connections.fetchMessagesStart,
    payload: Immutable.fromJS({ connectionUri: connectionUri }),
  });

  const connectionContainerUri = getIn(state, [
    "atoms",
    atomUri,
    "connections",
    connectionUri,
    "messageContainerUri",
  ]);

  return won
    .getMessagesOfConnection(connectionUri, connectionContainerUri, fetchParams)
    .then(({ nextPage, messages }) => {
      const lookupMap = { success: {}, failed: {} };
      const loadingArray = [];
      messages.map(message => {
        loadingArray.push(message.msgUri);

        if (message.wonMessage) {
          lookupMap["success"][message.msgUri] = message.wonMessage;
        } else {
          lookupMap["failed"][message.msgUri] = undefined;
        }
      });

      return { nextPage: nextPage, messages: lookupMap };
    })
    .then(({ nextPage, messages }) => {
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
              nextPage: nextPage,
            }),
          });
        }

        if (failedMessages.size > 0) {
          dispatch({
            type: actionTypes.connections.fetchMessagesFailed,
            payload: Immutable.fromJS({
              connectionUri: connectionUri,
              events: failedMessages,
              nextPage: nextPage,
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
            payload: Immutable.fromJS({
              connectionUri: connectionUri,
              nextPage: nextPage,
            }),
          });
        }
      }
    });
}

export function fetchConnectionsOfAtomAndDispatch(atomUri, dispatch) {
  return won
    .getConnectionUrisWithStateByAtomUri(atomUri)
    .then(connectionsWithStateAndSocket => {
      dispatch({
        type: actionTypes.connections.storeMetaConnections,
        payload: Immutable.fromJS({
          atomUri: atomUri,
          connections: connectionsWithStateAndSocket,
        }),
      });
      const activeConnectionUris = connectionsWithStateAndSocket
        .filter(
          conn =>
            conn.connectionState !== vocab.WON.Closed &&
            conn.connectionState !== vocab.WON.Suggested
        )
        .map(conn => conn.uri);

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

export function fetchConnectionsOfNonOwnedAtomAndDispatch(atomUri, dispatch) {
  return won
    .getConnectionUrisWithStateByAtomUri(atomUri, true)
    .then(connectionsWithStateAndSocket => {
      const connectedConnections = connectionsWithStateAndSocket.filter(
        conn => conn.connectionState === vocab.WON.Connected
      );

      dispatch({
        type: actionTypes.connections.storeMetaConnections,
        payload: Immutable.fromJS({
          atomUri: atomUri,
          connections: connectedConnections,
        }),
      });
    });
}

/**
 * fetches Data for atomUri
 * @param atomUri uri to be fetched
 * @param dispatch redux dispatcher
 * @param getState redux state
 * @param forceFetch bool if true, then fetch will be executed even if atom is already in the state (useful on edit)
 * @returns {Promise<void>|*}
 */
function fetchOwnedAtomAndDispatch(
  atomUri,
  dispatch,
  getState,
  forceFetch = false
) {
  const processState = get(getState(), "process");

  if (!forceFetch && processUtils.isAtomLoaded(processState, atomUri)) {
    console.debug("Omit Fetch of Atom<", atomUri, ">, it is already loaded...");
    return Promise.resolve();
  } else if (!forceFetch && processUtils.isAtomLoading(processState, atomUri)) {
    console.debug(
      "Omit Fetch of Atom<",
      atomUri,
      ">, it is currently loading..."
    );
    return Promise.resolve();
  }
  console.debug("Proceed Fetch of Atom<", atomUri, ">");

  dispatch({
    type: actionTypes.atoms.storeUriInLoading,
    payload: Immutable.fromJS({ uri: atomUri }),
  });

  return won
    .getAtom(atomUri)
    .then(atom => {
      dispatch({
        type: actionTypes.atoms.store,
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
    });
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
