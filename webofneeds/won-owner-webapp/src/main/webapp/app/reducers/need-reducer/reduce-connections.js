import won from "../../won-es6.js";
import { parseConnection } from "./parse-connection.js";
import { markUriAsRead, markConnUriAsClosed } from "../../won-localstorage.js";

import { markNeedAsRead } from "./reduce-needs.js";

export function storeConnectionsData(state, connectionsToStore) {
  if (connectionsToStore && connectionsToStore.size > 0) {
    connectionsToStore.forEach(connection => {
      state = addConnectionFull(state, connection);
    });
  }
  return state;
}

/**
 * Add's the connection to the needs connections.
 *
 * @param state
 * @param connection
 * @param newConnection
 * @return {*}
 */
export function addConnectionFull(state, connection) {
  let parsedConnection = parseConnection(connection);

  if (parsedConnection) {
    // console.log("parsedConnection: ", parsedConnection.toJS(), "immutable
    // ", parsedConnection);

    const needUri = parsedConnection.get("belongsToUri");
    let connections = state.getIn([needUri, "connections"]);

    if (connections) {
      const connectionUri = parsedConnection.getIn(["data", "uri"]);

      if (parsedConnection.getIn(["data", "unread"])) {
        //If there is a new message for the connection we will set the connection to newConnection
        state = state.setIn(
          [needUri, "lastUpdateDate"],
          parsedConnection.getIn(["data", "lastUpdateDate"])
        );
        state = state.setIn([needUri, "unread"], true);
      }

      if (parsedConnection.getIn(["data", "state"]) === won.WON.Closed) {
        markConnUriAsClosed(connectionUri);
      }

      return state.mergeDeepIn(
        [needUri, "connections", connectionUri],
        parsedConnection.get("data")
      );
    } else {
      console.error(
        "Couldnt add valid connection - missing need data in state",
        needUri,
        "parsedConnection: ",
        parsedConnection.toJS()
      );
    }
  } else {
    // console.log("No connection parsed, add no connection to this state:
    // ", state);
  }
  return state;
}

export function markConnectionAsRead(state, connectionUri, needUri) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);

  markUriAsRead(connectionUri);

  if (!connection) {
    console.error(
      "no connection with connectionUri: <",
      connectionUri,
      "> found within needUri: <",
      needUri,
      ">"
    );
    return state;
  }

  state = state.setIn([needUri, "connections", connectionUri, "unread"], false);

  if (
    state.getIn([needUri, "connections"]).filter(conn => conn.get("unread"))
      .size == 0
  ) {
    state = markNeedAsRead(state, needUri);
  }

  return state;
}

export function setConnectionLoading(state, connectionUri, isLoading) {
  const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
  const needUri = need && need.get("uri");
  const connection = need && need.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "no connection with connectionUri: <",
      connectionUri,
      "> found within needUri: <",
      needUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [needUri, "connections", connectionUri, "isLoading"],
    isLoading
  );
}

export function markConnectionAsRated(state, connectionUri) {
  let need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
  let connection = need && need.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "no connection with connectionUri: <",
      connectionUri,
      "> found within needUri: <",
      need.get("uri"),
      ">"
    );
    return state;
  }

  return state.setIn(
    [need.get("uri"), "connections", connectionUri, "isRated"],
    true
  );
}

/**
 * Get the need for a given connectionUri
 *
 * @param state
 *            to retrieve data from
 * @param connectionUri
 *            to find corresponding need for
 */
export function selectNeedByConnectionUri(allNeedsInState, connectionUri) {
  return allNeedsInState
    .filter(need => need.getIn(["connections", connectionUri]))
    .first();
}

export function changeConnectionState(state, connectionUri, newState) {
  const need = selectNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.error("no need found for connectionUri", connectionUri);
    return state;
  }

  const needUri = need.get("uri");

  if (newState === won.WON.Closed) {
    markConnUriAsClosed(connectionUri);
  }

  return state
    .setIn([needUri, "connections", connectionUri, "state"], newState)
    .setIn([needUri, "connections", connectionUri, "unread"], true);
}

export function changeConnectionStateByFun(state, connectionUri, fun) {
  const need = selectNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.error("no need found for connectionUri", connectionUri);
    return state;
  }

  const needUri = need.get("uri");
  const connectionState = state.getIn([
    needUri,
    "connections",
    connectionUri,
    "state",
  ]);

  return changeConnectionState(state, connectionUri, fun(connectionState));
}

export function updateAgreementStateData(state, connectionUri, agreementData) {
  const need = selectNeedByConnectionUri(state, connectionUri);

  if (!need || !agreementData) {
    console.error("no need found for connectionUri", connectionUri);
    return state;
  }

  const needUri = need.get("uri");

  return state
    .setIn(
      [needUri, "connections", connectionUri, "agreementData"],
      agreementData
    )
    .setIn([needUri, "connections", connectionUri, "isLoading"], false);
}

export function setShowAgreementData(state, connectionUri, showAgreementData) {
  const need = selectNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.error("no need found for connectionUri", connectionUri);
    return state;
  }

  const needUri = need.get("uri");

  return state.setIn(
    [needUri, "connections", connectionUri, "showAgreementData"],
    showAgreementData
  );
}
