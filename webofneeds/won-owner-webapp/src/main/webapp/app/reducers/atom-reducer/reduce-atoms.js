import { markUriAsDeleted } from "../../won-localstorage.js";
import { parseMetaAtom } from "./parse-atom.js";
import Immutable from "immutable";
import { get, getIn, getUri } from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import vocab from "../../service/vocab.js";

export function addAtom(allAtomsInState, parsedAtom) {
  const parsedAtomUri = getUri(parsedAtom);

  if (parsedAtomUri) {
    let existingAtom = get(allAtomsInState, parsedAtomUri);

    if (existingAtom) {
      parsedAtom = parsedAtom.set(
        "connections",
        atomUtils.getConnections(existingAtom)
      );
    }

    return allAtomsInState.set(parsedAtomUri, parsedAtom);
  } else {
    console.error("Tried to add invalid atom-object: ", parsedAtom);
    return allAtomsInState;
  }
}

export function deleteAtom(allAtomsInState, deletedAtomUri) {
  markUriAsDeleted(deletedAtomUri);

  return allAtomsInState.delete(deletedAtomUri).map(atom => {
    const removeConnections = atom => {
      return atom.update(
        "connections",
        connections =>
          connections &&
          connections.filter((conn, connUri) => {
            if (connectionUtils.getTargetAtomUri(conn) !== deletedAtomUri) {
              return true;
            } else {
              markUriAsDeleted(connUri);
              return false;
            }
          })
      );
    };

    return removeConnections(atom);
  });
}

/**
 * Adds an atom-stub into the atom-redux-state, needed to get Posts that are not loaded/loading to show up as skeletons
 * Checks if stub/atom already exists, if so do nothing
 * @param allAtomsInState redux atom state
 * @param atomUri stub accessible under uri
 * @returns {*}
 */
export function addAtomStub(allAtomsInState, atomUri) {
  if (allAtomsInState && allAtomsInState.has(atomUri)) {
    return allAtomsInState;
  } else {
    return allAtomsInState.setIn(
      [atomUri],
      Immutable.fromJS({
        uri: atomUri,
        connections: Immutable.Map(),
        uriStub: true,
      })
    );
  }
}

/**
 * Adds atom-stubs into the atom-redux-state, needed to get Posts that are not loaded/loading to show up as skeletons
 * Checks if stub/atom already exists, if so do nothing
 * @param allAtomsInState redux atom state
 * @param atomUris stub accessible under uris
 * @returns {*}
 */
export function addAtomStubs(allAtomsInState, atomUris) {
  atomUris &&
    atomUris.forEach(atomUri => {
      allAtomsInState = addAtomStub(allAtomsInState, atomUri);
    });

  return allAtomsInState;
}

export function addMetaAtomStubs(allAtomsInState, metaAtoms) {
  metaAtoms &&
    metaAtoms.map(metaAtom => {
      allAtomsInState = addMetaAtomStub(allAtomsInState, metaAtom);
    });
  return allAtomsInState;
}

function addMetaAtomStub(allAtomsInState, metaAtom) {
  const parsedMetaAtom = parseMetaAtom(metaAtom);

  const parsedAtomUri = getUri(parsedMetaAtom);

  if (
    parsedAtomUri &&
    allAtomsInState &&
    (!allAtomsInState.has(parsedAtomUri) ||
      getIn(allAtomsInState, [parsedAtomUri, "uriStub"]))
  ) {
    return allAtomsInState.set(parsedAtomUri, parsedMetaAtom);
  }

  return allAtomsInState;
}

export function addAtomInCreation(allAtomsInState, atomInCreation, atomUri) {
  let atom = Immutable.fromJS(atomInCreation);

  if (atom) {
    return allAtomsInState.set(
      atomUri,
      atom
        .set("uri", atomUri)
        .set("state", vocab.WON.ActiveCompacted)
        .set("isBeingCreated", true)
        .set("connections", Immutable.Map())
        .set(
          "humanReadable",
          getIn(atom, ["content", "title"]) || getIn(atom, ["seeks", "title"])
        )
        .set("content", Immutable.Map())
    );
  } else {
    console.error("Tried to add invalid atom-object: ", atomInCreation);
    return allAtomsInState;
  }
}

export function markAtomAsRead(allAtomsInState, atomUri) {
  const atom = get(allAtomsInState, atomUri);

  if (!atom) {
    console.error("No atom with atomUri: <", atomUri, ">");
    return allAtomsInState;
  }
  return allAtomsInState.setIn([atomUri, "unread"], false);
}
