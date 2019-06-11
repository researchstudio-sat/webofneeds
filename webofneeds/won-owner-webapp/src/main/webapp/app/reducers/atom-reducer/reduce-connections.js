import won from "../../won-es6.js";
import Immutable from "immutable";
import { parseConnection } from "./parse-connection.js";
import { markUriAsRead } from "../../won-localstorage.js";

import { markAtomAsRead } from "./reduce-atoms.js";
import { getIn, get } from "../../utils.js";
import * as connectionUtils from "../../connection-utils";

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
      const realSocket = getIn(atom, ["content", "sockets", socketUri]);

      parsedConnection = parsedConnection.setIn(["data", "socket"], realSocket);

      if (
        realSocket === won.HOLD.HolderSocketCompacted &&
        connectionUtils.isConnected(parsedConnection)
      ) {
        const holdsUri = targetAtomUri;

        if (holdsUri) {
          const currentHolds = getIn(atomState, [atomUri, "holds"]);
          if (currentHolds && !currentHolds.includes(holdsUri)) {
            atomState = atomState.updateIn([atomUri, "holds"], holdsList =>
              holdsList.push(holdsUri)
            );
          }
        }
      } else if (
        realSocket === won.HOLD.HoldableSocketCompacted &&
        connectionUtils.isConnected(parsedConnection)
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

        if (realTargetSocket) {
          parsedConnection = parsedConnection.setIn(
            ["data", "targetSocket"],
            realTargetSocket
          );
        }

        if (
          connectionUtils.isConnected(parsedConnection) &&
          realSocket === won.BUDDY.BuddySocketCompacted &&
          realTargetSocket === won.BUDDY.BuddySocketCompacted
        ) {
          const currentBuddies = getIn(atomState, [atomUri, "buddies"]);
          if (currentBuddies && !currentBuddies.includes(targetAtomUri)) {
            atomState = atomState.updateIn([atomUri, "buddies"], buddyList =>
              buddyList.push(targetAtomUri)
            );
          }
          const currentTargetBuddies = getIn(atomState, [
            targetAtomUri,
            "buddies",
          ]);
          if (currentTargetBuddies && !currentTargetBuddies.includes(atomUri)) {
            atomState = atomState.updateIn(
              [targetAtomUri, "buddies"],
              buddyList => buddyList.push(atomUri)
            );
          }
        }
      }

      if (getIn(parsedConnection, ["data", "unread"])) {
        //If there is a new message for the connection we will set the connection to newConnection
        atomState = atomState.setIn(
          [atomUri, "lastUpdateDate"],
          parsedConnection.getIn(["data", "lastUpdateDate"])
        );
        atomState = atomState.setIn([atomUri, "unread"], true);
      }

      return atomState.mergeDeepIn(
        [atomUri, "connections", connectionUri],
        parsedConnection.get("data")
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

export function changeConnectionState(state, connectionUri, newState) {
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

  const connection = getIn(atom, ["connections", connectionUri]);
  if (
    newState === won.WON.Closed &&
    connection.get("socket") === won.HOLD.HolderSocketCompacted
  ) {
    state = state.updateIn([atomUri, "holds"], holds =>
      holds.delete(connection.get("targetAtomUri"))
    );
  }

  if (
    newState === won.WON.Closed &&
    connection.get("socket") === won.HOLD.HoldableSocketCompacted
  ) {
    state = state.deleteIn([atomUri, "heldBy"]);
  }

  return state
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

  const atomUri = atom.get("uri");

  return state.setIn(
    [atomUri, "connections", connectionUri, "agreementData"],
    agreementData
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

export function addConnectionsToLoad(state, atomUri, connections) {
  let newState = state;
  atomUri &&
    connections &&
    connections.forEach(conn => {
      newState = addConnectionToLoad(newState, atomUri, conn);
    });
  return newState;
}

export function addConnectionToLoad(state, atomUri, conn) {
  const storedAtom = state.get(atomUri);
  const isConnectionPresent =
    storedAtom &&
    !!storedAtom.getIn(["connections", conn.get("connectionUri")]);

  const socketTypeToCompacted = socketType => {
    if (socketType === won.CHAT.ChatSocket) {
      return won.CHAT.ChatSocketCompacted;
    } else if (socketType === won.GROUP.GroupSocket) {
      return won.GROUP.GroupSocketCompacted;
    } else if (socketType === won.REVIEW.ReviewSocket) {
      return won.REVIEW.ReviewSocketCompacted;
    } else if (socketType === won.HOLD.HolderSocket) {
      return won.HOLD.HolderSocketCompacted;
    } else if (socketType === won.HOLD.HoldableSocket) {
      return won.HOLD.HoldableSocketCompacted;
    } else {
      console.warn(
        "Unknown socketType: ",
        socketType,
        " - can't compact, return as is"
      );
      return socketType;
    }
  };

  if (storedAtom && !isConnectionPresent) {
    const connection = Immutable.fromJS({
      uri: conn.get("connectionUri"),
      state: conn.get("connectionState"),
      socket: socketTypeToCompacted(conn.get("socketType")),
      messages: Immutable.Map(),
      agreementData: undefined,
      targetAtomUri: undefined,
      targetConnectionUri: undefined,
      creationDate: undefined,
      lastUpdateDate: undefined,
      unread: undefined,
      isRated: false,
      showAgreementData: false,
    });

    return state.mergeDeepIn(
      [atomUri, "connections", conn.get("connectionUri")],
      connection
    );
  }

  return state;
}
