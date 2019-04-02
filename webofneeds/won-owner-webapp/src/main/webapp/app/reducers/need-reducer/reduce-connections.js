import won from "../../won-es6.js";
import Immutable from "immutable";
import { parseConnection } from "./parse-connection.js";
import { markUriAsRead } from "../../won-localstorage.js";

import { markNeedAsRead } from "./reduce-needs.js";
import { getIn } from "../../utils.js";

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
function addConnectionFull(state, connection) {
  let parsedConnection = parseConnection(connection);

  if (parsedConnection) {
    const needUri = parsedConnection.get("belongsToUri");
    const need = state.get(needUri);

    if (need) {
      const remoteNeedUri = parsedConnection.getIn(["data", "remoteNeedUri"]);
      const connectionUri = parsedConnection.getIn(["data", "uri"]);

      const facetUri = parsedConnection.get("facetUri");
      const realFacet = need.getIn(["content", "facets", facetUri]);

      parsedConnection = parsedConnection.setIn(["data", "facet"], realFacet);

      if (realFacet === won.WON.HolderFacetCompacted) {
        const holdsUri = remoteNeedUri;
        console.debug(
          "Handling a holderFacet-connection within need: ",
          needUri,
          " setting holds to holdsUri: ",
          holdsUri
        );

        if (holdsUri) {
          const currentHolds = state.getIn([needUri, "holds"]);
          if (currentHolds && !currentHolds.includes(holdsUri)) {
            state = state.updateIn([needUri, "holds"], holdsList =>
              holdsList.push(holdsUri)
            );
          }
        }
      } else if (realFacet === won.WON.HoldableFacetCompacted) {
        //holdableFacet Connection from need to persona -> need to add heldBy remoteNeedUri to the need
        const heldByUri = remoteNeedUri;
        console.debug(
          "Handling a holdableFacet-connection within need: ",
          needUri,
          " setting heldBy to heldByUri: ",
          heldByUri
        );

        if (heldByUri) {
          state = state.setIn([needUri, "heldBy"], heldByUri);
        }
      }

      const remoteNeed = state.get(remoteNeedUri);

      if (remoteNeed) {
        const remoteFacetUri = parsedConnection.get("remoteFacetUri");
        const realRemoteFacet = remoteNeed.getIn([
          "content",
          "facets",
          remoteFacetUri,
        ]);

        if (realRemoteFacet) {
          parsedConnection = parsedConnection.setIn(
            ["data", "remoteFacet"],
            realRemoteFacet
          );
        }
      }

      if (parsedConnection.getIn(["data", "unread"])) {
        //If there is a new message for the connection we will set the connection to newConnection
        state = state.setIn(
          [needUri, "lastUpdateDate"],
          parsedConnection.getIn(["data", "lastUpdateDate"])
        );
        state = state.setIn([needUri, "unread"], true);
      }

      return state.mergeDeepIn(
        [needUri, "connections", connectionUri],
        parsedConnection.get("data")
      );
    } else {
      console.error(
        "Couldn't add valid connection - missing need data in state",
        needUri,
        "parsedConnection: ",
        parsedConnection.toJS()
      );
    }
  }
  return state;
}

export function markConnectionAsRead(state, connectionUri, needUri) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);

  markUriAsRead(connectionUri);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
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

export function markConnectionAsRated(state, connectionUri) {
  let need = connectionUri && getNeedByConnectionUri(state, connectionUri);
  let connection = need && need.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within needUri: <",
      need && need.get("uri"),
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
export function getNeedByConnectionUri(allNeedsInState, connectionUri) {
  return allNeedsInState.find(need =>
    need.getIn(["connections", connectionUri])
  );
}

export function changeConnectionState(state, connectionUri, newState) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  const connection = getIn(need, ["connections", connectionUri]);
  if (
    newState === won.WON.Closed &&
    connection.get("facet") == won.WON.HolderFacetCompacted
  ) {
    state = state.updateIn([needUri, "holds"], holds =>
      holds.delete(connection.get("remoteNeedUri"))
    );
  }

  if (
    newState === won.WON.Closed &&
    connection.get("facet") == won.WON.HoldableFacetCompacted
  ) {
    state = state.deleteIn([needUri, "heldBy"]);
  }

  return state
    .setIn([needUri, "connections", connectionUri, "state"], newState)
    .setIn([needUri, "connections", connectionUri, "unread"], true);
}

export function changeConnectionStateByFun(state, connectionUri, fun) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
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

export function updatePetriNetStateData(state, connectionUri, petriNetData) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need || !petriNetData) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") or no petriNetData set in params -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  return state.setIn(
    [needUri, "connections", connectionUri, "petriNetData"],
    petriNetData
  );
}

export function updateAgreementStateData(state, connectionUri, agreementData) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need || !agreementData) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") or no agreementData set in params -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  return state.setIn(
    [needUri, "connections", connectionUri, "agreementData"],
    agreementData
  );
}

export function setShowAgreementData(state, connectionUri, showAgreementData) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  return state.setIn(
    [needUri, "connections", connectionUri, "showAgreementData"],
    showAgreementData
  );
}

export function setShowPetriNetData(state, connectionUri, showPetriNetData) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  return state.setIn(
    [needUri, "connections", connectionUri, "showPetriNetData"],
    showPetriNetData
  );
}

export function setMultiSelectType(
  state,
  connectionUri,
  multiSelectType = undefined
) {
  const need = getNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  let messages = state.getIn([
    needUri,
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
      [needUri, "connections", connectionUri, "messages"],
      messages
    );
    state = state.setIn(
      [needUri, "connections", connectionUri, "multiSelectType"],
      undefined
    );
  } else {
    state = state.setIn(
      [needUri, "connections", connectionUri, "multiSelectType"],
      multiSelectType
    );
  }

  return state;
}

export function addConnectionsToLoad(state, needUri, connections) {
  let newState = state;
  needUri &&
    connections &&
    connections.forEach(conn => {
      newState = addConnectionToLoad(newState, needUri, conn);
    });
  return newState;
}

export function addConnectionToLoad(state, needUri, conn) {
  const storedNeed = state.get(needUri);
  const isConnectionPresent =
    storedNeed &&
    !!storedNeed.getIn(["connections", conn.get("connectionUri")]);

  const facetTypeToCompacted = facetType => {
    if (facetType === won.WON.ChatFacet) {
      return won.WON.ChatFacetCompacted;
    } else if (facetType === won.WON.GroupFacet) {
      return won.WON.GroupFacetCompacted;
    } else if (facetType === won.WON.ReviewFacet) {
      return won.WON.ReviewFacetCompacted;
    } else if (facetType === won.WON.HolderFacet) {
      return won.WON.HolderFacetCompacted;
    } else if (facetType === won.WON.HoldableFacet) {
      return won.WON.HoldableFacetCompacted;
    } else {
      console.warn(
        "Unknown facetType: ",
        facetType,
        " - can't compact, return as is"
      );
      return facetType;
    }
  };

  if (storedNeed && !isConnectionPresent) {
    const connection = Immutable.fromJS({
      uri: conn.get("connectionUri"),
      state: conn.get("connectionState"),
      facet: facetTypeToCompacted(conn.get("facetType")),
      messages: Immutable.Map(),
      agreementData: undefined,
      remoteNeedUri: undefined,
      remoteConnectionUri: undefined,
      creationDate: undefined,
      lastUpdateDate: undefined,
      unread: undefined,
      isRated: false,
      showAgreementData: false,
    });

    return state.mergeDeepIn(
      [needUri, "connections", conn.get("connectionUri")],
      connection
    );
  }

  return state;
}
