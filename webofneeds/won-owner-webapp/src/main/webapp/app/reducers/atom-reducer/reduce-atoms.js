import { parseAtom, parseMetaAtom } from "./parse-atom.js";
import Immutable from "immutable";
import { get, getIn } from "../../utils.js";
import vocab from "../../service/vocab.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

export function addAtom(atoms, jsonldAtom) {
  let newState;
  let parsedAtom = parseAtom(jsonldAtom);
  const parsedAtomUri = get(parsedAtom, "uri");

  if (parsedAtomUri) {
    let existingAtom = get(atoms, parsedAtomUri);

    if (existingAtom) {
      parsedAtom = parsedAtom.set(
        "connections",
        get(existingAtom, "connections")
      );

      const heldAtomUris = get(parsedAtom, "holds");
      if (heldAtomUris.size > 0) {
        heldAtomUris.map(atomUri => {
          atoms = addAtomStub(atoms, atomUri);
        });
      }

      const groupMemberUris = get(parsedAtom, "groupMembers");
      if (groupMemberUris.size > 0) {
        groupMemberUris.map(atomUri => {
          atoms = addAtomStub(atoms, atomUri);
        });
      }

      const buddyUris = get(parsedAtom, "buddies");
      if (buddyUris.size > 0) {
        buddyUris.map(atomUri => {
          atoms = addAtomStub(atoms, atomUri);
        });
      }
    }

    return atoms.set(parsedAtomUri, parsedAtom);
  } else {
    console.error("Tried to add invalid atom-object: ", jsonldAtom);
    newState = atoms;
  }

  return newState;
}

export function deleteAtom(atoms, deletedAtomUri) {
  return atoms.delete(deletedAtomUri).map(atom => {
    const removeHolder = atom => {
      if (atomUtils.getHeldByUri(atom) === deletedAtomUri) {
        return atom.delete("heldBy");
      } else return atom;
    };
    const removeHeld = atom => {
      return atom.updateIn(
        ["holds"],
        heldItems =>
          heldItems && heldItems.filter(heldItem => heldItem != deletedAtomUri)
      );
    };

    const removeGroupMembers = atom => {
      return atom.updateIn(
        ["groupMembers"],
        heldItems =>
          heldItems && heldItems.filter(heldItem => heldItem != deletedAtomUri)
      );
    };

    const removeBuddies = atom => {
      return atom.updateIn(
        ["buddies"],
        buddies =>
          buddies && buddies.filter(heldItem => heldItem != deletedAtomUri)
      );
    };

    const removeConnections = atom => {
      return atom.updateIn(
        ["connections"],
        connections =>
          connections &&
          connections.filter(
            conn => get(conn, "targetAtomUri") !== deletedAtomUri
          )
      );
    };

    return removeConnections(
      removeHeld(removeHolder(removeGroupMembers(removeBuddies(atom))))
    );
  });
}

/**
 * Adds an atom-stub into the atom-redux-state, needed to get Posts that are not loaded/loading to show up as skeletons
 * Checks if stub/atom already exists, if so do nothing
 * @param atoms redux atom state
 * @param atomUri stub accessible under uri
 * @returns {*}
 */
export function addAtomStub(atoms, atomUri) {
  if (atoms && atoms.has(atomUri)) {
    return atoms;
  } else {
    return atoms.setIn(
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
 * @param atoms redux atom state
 * @param atomUris stub accessible under uris
 * @returns {*}
 */
export function addAtomStubs(atoms, atomUris) {
  let newState = atoms;
  atomUris &&
    atomUris.forEach(atomUri => {
      newState = addAtomStub(newState, atomUri);
    });
  return newState;
}

export function addMetaAtomStubs(atoms, metaAtoms) {
  let newState = atoms;
  metaAtoms &&
    metaAtoms.map(metaAtom => {
      newState = addMetaAtomStub(newState, metaAtom);
    });
  return newState;
}

function addMetaAtomStub(atoms, metaAtom) {
  const parsedMetaAtom = parseMetaAtom(metaAtom);

  const parsedAtomUri = get(parsedMetaAtom, "uri");

  if (
    parsedAtomUri &&
    atoms &&
    (!atoms.has(parsedAtomUri) || getIn(atoms, [parsedAtomUri, "uriStub"]))
  ) {
    return atoms.set(parsedAtomUri, parsedMetaAtom);
  }

  return atoms;
}

export function addAtomInCreation(atoms, atomInCreation, atomUri) {
  let newState;
  let atom = Immutable.fromJS(atomInCreation);

  if (atom) {
    atom = atom
      .set("uri", atomUri)
      .set("state", vocab.WON.ActiveCompacted)
      .set("isBeingCreated", true)
      .set("connections", Immutable.Map());

    let title = undefined;

    if (atom.get("content")) {
      title = atom.getIn(["content", "title"]);
    }

    if (atom.get("seeks")) {
      title = atom.getIn(["seeks", "title"]);
    }

    atom = atom.set("humanReadable", title);
    atom = atom.set("content", Immutable.Map());
    newState = atoms.setIn([atomUri], atom);
  } else {
    console.error("Tried to add invalid atom-object: ", atomInCreation);
    newState = atoms;
  }
  return newState;
}

export function markAtomAsRead(state, atomUri) {
  const atom = state.get(atomUri);

  if (!atom) {
    console.error("No atom with atomUri: <", atomUri, ">");
    return state;
  }
  return state.setIn([atomUri, "unread"], false);
}
