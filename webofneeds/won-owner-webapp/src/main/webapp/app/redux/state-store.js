import * as ownerApi from "../owner-api.js";
import Immutable from "immutable";
import { actionTypes } from "../actions/actions.js";
import * as atomUtils from "./utils/atom-utils.js";
import { parseMetaAtom } from "../reducers/atom-reducer/parse-atom.js";
import { is } from "../utils.js";
import won from "../won-es6";
import * as connectionUtils from "./utils/connection-utils.js";

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
        .toSet();

      const theirAtomUrisArray = theirAtomUris.toArray();

      dispatch({
        type: actionTypes.atoms.storeTheirUrisInLoading,
        payload: Immutable.fromJS({ uris: theirAtomUrisArray }),
      });
      return theirAtomUrisArray;
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

export function fetchUnloadedData(dispatch) {
  return ownerApi
    .getOwnedMetaAtoms("INACTIVE")
    .then(metaAtoms => {
      console.debug("metaAtoms: ", metaAtoms);
      const atomsImm = Immutable.fromJS(metaAtoms);
      return [...atomsImm.keys()];
    })
    .then(atomUris => {
      dispatch({
        type: actionTypes.atoms.storeOwnedInactiveUrisInLoading,
        payload: Immutable.fromJS({ uris: atomUris }),
      });
      return fetchDataForOwnedAtoms(atomUris, dispatch);
    });
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

function fetchConnectionsOfAtomAndDispatch(atomUri, dispatch) {
  return won
    .getConnectionUrisWithStateByAtomUri(atomUri, atomUri)
    .then(connectionsWithStateAndSocket => {
      dispatch({
        type: actionTypes.connections.storeUrisToLoad,
        payload: Immutable.fromJS({
          atomUri: atomUri,
          connections: connectionsWithStateAndSocket,
        }),
      });

      const activeConnectionUris = connectionsWithStateAndSocket
        .filter(conn => !connectionUtils.isClosed(conn))
        .map(conn => conn.connectionUri);

      dispatch({
        type: actionTypes.connections.storeActiveUrisInLoading,
        payload: Immutable.fromJS({
          atomUri: atomUri,
          connUris: activeConnectionUris,
        }),
      });

      return urisToLookupMap(activeConnectionUris, connUri =>
        fetchActiveConnectionAndDispatch(connUri, atomUri, dispatch)
      );
    });
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

  const urisAsArrayWithoutExcludes = urisAsArray.filter(uri => {
    const exclude = excludeUrisAsArray.indexOf(uri) < 0;
    if (exclude) {
      return true;
    } else {
      return false;
    }
  });

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
