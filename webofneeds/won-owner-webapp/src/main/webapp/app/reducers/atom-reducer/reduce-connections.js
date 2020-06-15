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
      const connectionUri = getIn(parsedConnection, ["data", "uri"]);

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

export function markConnectionAsRead(state, connectionUri) {
  const atomUri = connectionUri && connectionUri.split("/c")[0];
  const connection = getIn(state, [atomUri, "connections", connectionUri]);

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
    getIn(state, [atomUri, "connections"]).find(conn => !get(conn, "unread"))
  ) {
    state = markAtomAsRead(state, atomUri);
  }

  return state;
}

export function markConnectionAsRated(state, connectionUri) {
  let atom = connectionUri && getAtomByConnectionUri(state, connectionUri);
  let connection = getIn(atom, ["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      get(atom, "uri"),
      ">"
    );
    return state;
  }

  return state.setIn(
    [get(atom, "uri"), "connections", connectionUri, "isRated"],
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
    getIn(atom, ["connections", connectionUri])
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

  const atomUri = get(atom, "uri");

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

  const atomUri = get(atom, "uri");
  const connectionState = getIn(state, [
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

  const atomUri = get(atom, "uri");

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
  const atomUri = get(atom, "uri");

  return state.setIn(
    [atomUri, "connections", connectionUri, "agreementData"],
    agreementData
  );
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
  const atomUri = get(atom, "uri");

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

  const atomUri = get(atom, "uri");

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

  const atomUri = get(atom, "uri");

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

  const atomUri = get(atom, "uri");

  let messages = getIn(state, [
    atomUri,
    "connections",
    connectionUri,
    "messages",
  ]);

  if (!multiSelectType) {
    messages = messages.map(msg => {
      msg = getIn(msg, ["viewState", "isSelected"])
        ? msg.setIn(["viewState", "isSelected"], false)
        : msg;
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
