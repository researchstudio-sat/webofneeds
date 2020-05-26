import vocab from "../../service/vocab.js";
import { parseConnection } from "./parse-connection.js";
import { markUriAsRead } from "../../won-localstorage.js";

import { markAtomAsRead } from "./reduce-atoms.js";
import { getIn, get } from "../../utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils";
import { addAtomStub } from "./reduce-atoms";

export function storeConnectionsData(state, connectionsToStore) {
  if (connectionsToStore && connectionsToStore.size > 0) {
    connectionsToStore.forEach(connection => {
      state = addConnectionFull(state, connection);
    });
  }
  return state;
}

/**
 * Adds the connection to the atoms connections.
 *
 * @param atomState
 * @param connection
 * @param newConnection
 * @return {*}
 */
function addConnectionFull(atomState, connection) {
  let parsedConnection = parseConnection(connection);

  if (parsedConnection) {
    const atomUri = get(parsedConnection, "belongsToUri");
    const atom = get(atomState, atomUri);

    if (atom) {
      const targetAtomUri = getIn(parsedConnection, ["data", "targetAtomUri"]);
      const connectionUri = getIn(parsedConnection, ["data", "uri"]);

      const socketUri = getIn(parsedConnection, ["data", "socketUri"]);
      const socketType = getIn(atom, ["content", "sockets", socketUri]);

      if (
        socketType === vocab.HOLD.HolderSocketCompacted &&
        connectionUtils.isConnected(get(parsedConnection, "data"))
      ) {
        const holdsUri = targetAtomUri;

        if (holdsUri) {
          atomState = atomState.updateIn([atomUri, "holds"], holds =>
            holds.add(holdsUri)
          );
        }
      } else if (
        socketType === vocab.HOLD.HoldableSocketCompacted &&
        connectionUtils.isConnected(get(parsedConnection, "data"))
      ) {
        //holdableSocket Connection from atom to persona -> need to add heldBy targetAtomUri to the atom
        const heldByUri = targetAtomUri;

        if (heldByUri) {
          atomState = atomState.setIn([atomUri, "heldBy"], heldByUri);
        }
      }

      const targetAtom = get(atomState, targetAtomUri);

      if (targetAtom) {
        const targetSocketUri = getIn(parsedConnection, [
          "data",
          "targetSocketUri",
        ]);
        const realTargetSocket = getIn(targetAtom, [
          "content",
          "sockets",
          targetSocketUri,
        ]);

        if (
          connectionUtils.isConnected(get(parsedConnection, "data")) &&
          socketType === vocab.BUDDY.BuddySocketCompacted &&
          realTargetSocket === vocab.BUDDY.BuddySocketCompacted
        ) {
          atomState = atomState.updateIn([atomUri, "buddies"], buddies =>
            buddies.add(targetAtomUri)
          );
          atomState = atomState.updateIn([targetAtomUri, "buddies"], buddies =>
            buddies.add(atomUri)
          );
        }
      }

      if (connectionUtils.isUnread(get(parsedConnection, "data"))) {
        //If there is a new message for the connection we will set the connection to newConnection
        atomState = atomState.setIn(
          [atomUri, "lastUpdateDate"],
          getIn(parsedConnection, ["data", "lastUpdateDate"])
        );
        atomState = atomState.setIn([atomUri, "unread"], true);
      }

      return atomState.mergeDeepIn(
        [atomUri, "connections", connectionUri],
        get(parsedConnection, "data")
      );
    } else {
      console.error(
        "Couldn't add valid connection - missing atom data in state",
        atomUri,
        "parsedConnection: ",
        parsedConnection.toJS()
      );
    }
  }
  return atomState;
}

export function markConnectionAsRead(state, connectionUri, atomUri) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);

  markUriAsRead(connectionUri);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return state;
  }

  state = state.setIn([atomUri, "connections", connectionUri, "unread"], false);

  if (
    state.getIn([atomUri, "connections"]).filter(conn => conn.get("unread"))
      .size == 0
  ) {
    state = markAtomAsRead(state, atomUri);
  }

  return state;
}

export function markConnectionAsRated(state, connectionUri) {
  let atom = connectionUri && getAtomByConnectionUri(state, connectionUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atom && atom.get("uri"),
      ">"
    );
    return state;
  }

  return state.setIn(
    [atom.get("uri"), "connections", connectionUri, "isRated"],
    true
  );
}

/**
 * Get the atom for a given connectionUri
 *
 * @param state
 *            to retrieve data from
 * @param connectionUri
 *            to find corresponding atom for
 */
export function getAtomByConnectionUri(allAtomsInState, connectionUri) {
  return allAtomsInState.find(atom =>
    atom.getIn(["connections", connectionUri])
  );
}

export function changeConnectionState(allAtoms, connectionUri, newState) {
  const atom = getAtomByConnectionUri(allAtoms, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return allAtoms;
  }

  const atomUri = atom.get("uri");

  const targetAtomUri = getIn(atom, [
    "connections",
    connectionUri,
    "targetAtomUri",
  ]);
  const socketUri = getIn(atom, ["connections", connectionUri, "socketUri"]);
  const socketType = getIn(atom, ["content", "sockets", socketUri]);

  if (socketType === vocab.HOLD.HolderSocketCompacted) {
    if (newState === vocab.WON.Closed) {
      allAtoms = allAtoms.updateIn([atomUri, "holds"], holds =>
        holds.delete(targetAtomUri)
      );
    } else if (newState === vocab.WON.Connected) {
      allAtoms = allAtoms.updateIn([atomUri, "holds"], holds =>
        holds.add(targetAtomUri)
      );
    }
  } else if (socketType === vocab.HOLD.HoldableSocketCompacted) {
    if (newState === vocab.WON.Closed) {
      allAtoms = allAtoms.deleteIn([atomUri, "heldBy"]);
    } else if (newState === vocab.WON.Connected) {
      allAtoms = allAtoms.setIn([atomUri, "heldBy"], targetAtomUri);
    }
  }

  return allAtoms
    .setIn([atomUri, "connections", connectionUri, "state"], newState)
    .setIn([atomUri, "connections", connectionUri, "unread"], true);
}

export function changeConnectionStateByFun(state, connectionUri, fun) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const atomUri = atom.get("uri");
  const connectionState = state.getIn([
    atomUri,
    "connections",
    connectionUri,
    "state",
  ]);

  return changeConnectionState(state, connectionUri, fun(connectionState));
}

export function updatePetriNetStateData(state, connectionUri, petriNetData) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom || !petriNetData) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") or no petriNetData set in params -> return unaltered state"
    );
    return state;
  }

  const atomUri = atom.get("uri");

  return state.setIn(
    [atomUri, "connections", connectionUri, "petriNetData"],
    petriNetData
  );
}

export function updateAgreementStateData(state, connectionUri, agreementData) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom || !agreementData) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") or no agreementData set in params -> return unaltered state"
    );
    return state;
  }
}

export function updateAgreementStateDataset(
  state,
  connectionUri,
  agreementDataset
) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom || !agreementDataset) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") or no agreementData set in params -> return unaltered state"
    );
    return state;
  }
  const atomUri = atom.get("uri");

  return state.setIn(
    [atomUri, "connections", connectionUri, "agreementDataset"],
    agreementDataset
  );
}

export function setShowAgreementData(state, connectionUri, showAgreementData) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const atomUri = atom.get("uri");

  return state.setIn(
    [atomUri, "connections", connectionUri, "showAgreementData"],
    showAgreementData
  );
}

export function setShowPetriNetData(state, connectionUri, showPetriNetData) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const atomUri = atom.get("uri");

  return state.setIn(
    [atomUri, "connections", connectionUri, "showPetriNetData"],
    showPetriNetData
  );
}

export function setMultiSelectType(
  state,
  connectionUri,
  multiSelectType = undefined
) {
  const atom = getAtomByConnectionUri(state, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const atomUri = atom.get("uri");

  let messages = state.getIn([
    atomUri,
    "connections",
    connectionUri,
    "messages",
  ]);

  if (!multiSelectType) {
    messages = messages.map(msg => {
      msg = msg.setIn(["viewState", "isSelected"], false);
      return msg;
    });
    state = state.setIn(
      [atomUri, "connections", connectionUri, "messages"],
      messages
    );
    state = state.setIn(
      [atomUri, "connections", connectionUri, "multiSelectType"],
      undefined
    );
  } else {
    state = state.setIn(
      [atomUri, "connections", connectionUri, "multiSelectType"],
      multiSelectType
    );
  }

  return state;
}

export function addMetaConnections(state, atomUri, connections) {
  let newState = state;
  atomUri &&
    connections &&
    connections.forEach(conn => {
      newState = addMetaConnection(newState, atomUri, conn);
    });
  return newState;
}

function addMetaConnection(atomState, atomUri, conn) {
  const storedAtom = get(atomState, atomUri);
  const storedConnection = getIn(storedAtom, [
    "connections",
    get(conn, "connectionUri"),
  ]);

  const parsedMetaConnection = parseConnection(conn);

  if (!!storedAtom && !storedConnection && parsedMetaConnection) {
    const connectionUri = getIn(parsedMetaConnection, ["data", "uri"]);

    if (connectionUtils.isUnread(get(parsedMetaConnection, "data"))) {
      //If there is a new message for the connection we will set the connection to newConnection
      atomState = atomState.setIn(
        [atomUri, "lastUpdateDate"],
        getIn(parsedMetaConnection, ["data", "lastUpdateDate"])
      );
      atomState = atomState.setIn([atomUri, "unread"], true);
    }

    const targetAtomUri = get(conn, "targetAtom");
    atomState = addAtomStub(atomState, targetAtomUri);

    return atomState.mergeDeepIn(
      [atomUri, "connections", connectionUri],
      get(parsedMetaConnection, "data")
    );
  }

  return atomState;
}
