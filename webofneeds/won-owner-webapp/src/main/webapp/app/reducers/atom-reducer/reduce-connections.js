import { parseConnection } from "./parse-connection.js";
import { markUriAsRead } from "../../won-localstorage.js";

import { markAtomAsRead } from "./reduce-atoms.js";
import {
  getIn,
  get,
  getUri,
  extractAtomUriFromConnectionUri,
} from "../../utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils";
import * as messageUtils from "../../redux/utils/message-utils";
import { addAtomStub } from "./reduce-atoms";
import * as atomUtils from "~/app/redux/utils/atom-utils";

export function storeConnectionsData(allAtomsInState, connectionsToStore) {
  if (connectionsToStore && connectionsToStore.size > 0) {
    connectionsToStore.forEach(connection => {
      allAtomsInState = addConnectionFull(allAtomsInState, connection);
    });
  }
  return allAtomsInState;
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
      const parsedConnectionData = get(parsedConnection, "data");
      const connectionUri = getUri(parsedConnectionData);

      if (connectionUtils.isUnread(parsedConnectionData)) {
        //If there is a new message for the connection we will set the connection to newConnection
        atomState = atomState.setIn(
          [atomUri, "lastUpdateDate"],
          get(parsedConnectionData, "lastUpdateDate")
        );
        atomState = atomState.setIn([atomUri, "unread"], true);
      }

      return atomState.mergeDeepIn(
        [atomUri, "connections", connectionUri],
        parsedConnectionData
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

export function markConnectionAsRead(allAtomsInState, connectionUri) {
  const atomUri = extractAtomUriFromConnectionUri(connectionUri);
  const connection = getIn(allAtomsInState, [
    atomUri,
    "connections",
    connectionUri,
  ]);

  markUriAsRead(connectionUri);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return allAtomsInState;
  }

  allAtomsInState = allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "unread"],
    false
  );

  if (
    getIn(allAtomsInState, [atomUri, "connections"]).find(
      conn => !connectionUtils.isUnread(conn)
    )
  ) {
    allAtomsInState = markAtomAsRead(allAtomsInState, atomUri);
  }

  return allAtomsInState;
}

export function markConnectionAsRated(allAtomsInState, connectionUri) {
  let atom =
    connectionUri && getAtomByConnectionUri(allAtomsInState, connectionUri);
  let connection = atomUtils.getConnection(atom, connectionUri);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      getUri(atom),
      ">"
    );
    return allAtomsInState;
  }

  return allAtomsInState.setIn(
    [getUri(atom), "connections", connectionUri, "isRated"],
    true
  );
}

/**
 * Get the atom for a given connectionUri
 *
 * @param allAtomsInState
 *            to retrieve data from
 * @param connectionUri
 *            to find corresponding atom for
 */
export function getAtomByConnectionUri(allAtomsInState, connectionUri) {
  return (
    get(allAtomsInState, extractAtomUriFromConnectionUri(connectionUri)) ||
    allAtomsInState.find(atom => atomUtils.getConnection(atom, connectionUri))
  );
}

export function changeConnectionState(
  allAtomsInState,
  connectionUri,
  newState
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return allAtomsInState;
  }

  const atomUri = getUri(atom);

  return allAtomsInState
    .setIn([atomUri, "connections", connectionUri, "state"], newState)
    .setIn([atomUri, "connections", connectionUri, "unread"], true);
}

export function changeConnectionStateByFun(
  allAtomsInState,
  connectionUri,
  fun
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return allAtomsInState;
  }

  const atomUri = getUri(atom);
  const connectionState = getIn(allAtomsInState, [
    atomUri,
    "connections",
    connectionUri,
    "state",
  ]);

  return changeConnectionState(
    allAtomsInState,
    connectionUri,
    fun(connectionState)
  );
}

export function updatePetriNetStateData(
  allAtomsInState,
  connectionUri,
  petriNetData
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom || !petriNetData) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") or no petriNetData set in params -> return unaltered state"
    );
    return allAtomsInState;
  }

  const atomUri = getUri(atom);

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "petriNetData"],
    petriNetData
  );
}

export function updateAgreementStateData(
  allAtomsInState,
  connectionUri,
  agreementData
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom || !agreementData) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") or no agreementData set in params -> return unaltered state"
    );
    return allAtomsInState;
  }
  const atomUri = getUri(atom);

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "agreementData"],
    agreementData
  );
}

export function updateAgreementStateDataset(
  allAtomsInState,
  connectionUri,
  agreementDataset
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom || !agreementDataset) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") or no agreementDataset in params -> return unaltered state"
    );
    return allAtomsInState;
  }
  const atomUri = getUri(atom);

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "agreementDataset"],
    agreementDataset
  );
}

export function setShowAgreementData(
  allAtomsInState,
  connectionUri,
  showAgreementData
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return allAtomsInState;
  }

  const atomUri = getUri(atom);

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "showAgreementData"],
    showAgreementData
  );
}

export function setShowPetriNetData(
  allAtomsInState,
  connectionUri,
  showPetriNetData
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return allAtomsInState;
  }

  const atomUri = getUri(atom);

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "showPetriNetData"],
    showPetriNetData
  );
}

export function setMultiSelectType(
  allAtomsInState,
  connectionUri,
  multiSelectType = undefined
) {
  const atom = getAtomByConnectionUri(allAtomsInState, connectionUri);

  if (!atom) {
    console.warn(
      "No atom found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return allAtomsInState;
  }

  const atomUri = getUri(atom);

  let messages = getIn(allAtomsInState, [
    atomUri,
    "connections",
    connectionUri,
    "messages",
  ]);

  if (!multiSelectType) {
    allAtomsInState = allAtomsInState
      .setIn(
        [atomUri, "connections", connectionUri, "messages"],
        messageUtils.sortMessages(
          messages.map(msg => {
            msg = messageUtils.isMessageSelected(msg)
              ? msg.setIn(["viewState", "isSelected"], false)
              : msg;
            return msg;
          })
        )
      )
      .setIn(
        [atomUri, "connections", connectionUri, "multiSelectType"],
        undefined
      );
  } else {
    allAtomsInState = allAtomsInState.setIn(
      [atomUri, "connections", connectionUri, "multiSelectType"],
      multiSelectType
    );
  }

  return allAtomsInState;
}

export function addMetaConnections(allAtomsInState, atomUri, connections) {
  atomUri &&
    connections &&
    connections.forEach(conn => {
      allAtomsInState = addMetaConnection(allAtomsInState, atomUri, conn);
    });
  return allAtomsInState;
}

function addMetaConnection(atomState, atomUri, conn) {
  const storedAtom = get(atomState, atomUri);
  const storedConnection = getIn(storedAtom, [
    "connections",
    get(conn, "connectionUri"),
  ]);

  const parsedMetaConnection = parseConnection(conn);

  if (!!storedAtom && !storedConnection && parsedMetaConnection) {
    const parsedMetaConnectionData = get(parsedMetaConnection, "data");
    const connectionUri = getUri(parsedMetaConnectionData);

    if (connectionUtils.isUnread(parsedMetaConnectionData)) {
      //If there is a new message for the connection we will set the connection to newConnection
      atomState = atomState.setIn(
        [atomUri, "lastUpdateDate"],
        get(parsedMetaConnectionData, "lastUpdateDate")
      );
      atomState = atomState.setIn([atomUri, "unread"], true);
    }

    const targetAtomUri = get(conn, "targetAtom");
    atomState = addAtomStub(atomState, targetAtomUri);

    return atomState.mergeDeepIn(
      [atomUri, "connections", connectionUri],
      parsedMetaConnectionData
    );
  }

  return atomState;
}
