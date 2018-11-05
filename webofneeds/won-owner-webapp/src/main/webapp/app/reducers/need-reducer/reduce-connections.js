import won from "../../won-es6.js";
import Immutable from "immutable";
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
    const needUri = parsedConnection.get("belongsToUri");
    const need = state.get(needUri);

    if (need) {
      const connectionUri = parsedConnection.getIn(["data", "uri"]);

      const facetUri = parsedConnection.get("facetUri");
      const realFacet = need.getIn(["hasFacets", facetUri]);

      parsedConnection = parsedConnection.setIn(["data", "facet"], realFacet);

      if (realFacet === won.WON.HolderFacetCompacted) {
        const holdsUri = parsedConnection.getIn(["data", "remoteNeedUri"]);
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
        const heldByUri = parsedConnection.getIn(["data", "remoteNeedUri"]);
        console.debug(
          "Handling a holdableFacet-connection within need: ",
          needUri,
          " setting heldBy to heldByUri: ",
          heldByUri
        );

        if (heldByUri) {
          state = state.setIn([needUri, "heldBy"], heldByUri);
        }
      } else if (realFacet !== won.WON.ChatFacetCompacted) {
        console.warn("Unknown Facet(", realFacet, ") do not add Connection");
        return state;
      }

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

export function setConnectionLoadingMessages(
  state,
  connectionUri,
  isLoadingMessages
) {
  const need =
    connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
  const needUri = need && need.get("uri");
  const connection = need && need.getIn(["connections", connectionUri]);

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

  return state.setIn(
    [needUri, "connections", connectionUri, "isLoadingMessages"],
    isLoadingMessages
  );
}

export function setConnectionLoadingAgreementData(
  state,
  connectionUri,
  isLoadingAgreementData
) {
  const need =
    connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
  const needUri = need && need.get("uri");
  const connection = need && need.getIn(["connections", connectionUri]);

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

  return state.setIn(
    [needUri, "connections", connectionUri, "isLoadingAgreementData"],
    isLoadingAgreementData
  );
}

export function setConnectionLoadingPetriNetData(
  state,
  connectionUri,
  isLoadingPetriNetData
) {
  const need =
    connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
  const needUri = need && need.get("uri");
  const connection = need && need.getIn(["connections", connectionUri]);

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

  return state
    .setIn(
      [needUri, "connections", connectionUri, "petriNetData", "isDirty"],
      isLoadingPetriNetData //Assuming that there is currently a load of petriNetData in progress we flag it dirty just in case
    )
    .setIn(
      [needUri, "connections", connectionUri, "isLoadingPetriNetData"],
      isLoadingPetriNetData
    );
}

export function setPetriNetDataDirty(
  state,
  connectionUri,
  isPetriNetDataDirty
) {
  const need =
    connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
  const needUri = need && need.get("uri");
  const connection = need && need.getIn(["connections", connectionUri]);

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

  return state.setIn(
    [needUri, "connections", connectionUri, "petriNetData", "isDirty"],
    isPetriNetDataDirty
  );
}

export function markConnectionAsRated(state, connectionUri) {
  let need = connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
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
export function getOwnedNeedByConnectionUri(allNeedsInState, connectionUri) {
  return allNeedsInState
    .filter(need => need.getIn(["connections", connectionUri]))
    .first();
}

export function changeConnectionState(state, connectionUri, newState) {
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

  if (!need) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") -> return unaltered state"
    );
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
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

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
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

  if (!need || !petriNetData) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") or no petriNetData set in params -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  return state
    .setIn(
      [needUri, "connections", connectionUri, "petriNetData", "data"],
      petriNetData
    )
    .setIn(
      [needUri, "connections", connectionUri, "petriNetData", "isLoaded"],
      true
    )
    .setIn(
      [needUri, "connections", connectionUri, "petriNetData", "isDirty"],
      false
    )
    .setIn(
      [needUri, "connections", connectionUri, "isLoadingPetriNetData"],
      false
    );
}

export function updateAgreementStateData(state, connectionUri, agreementData) {
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

  if (!need || !agreementData) {
    console.warn(
      "No need found for connection(",
      connectionUri,
      ") or no agreementData set in params -> return unaltered state"
    );
    return state;
  }

  const needUri = need.get("uri");

  return state
    .setIn(
      [needUri, "connections", connectionUri, "agreementData"],
      agreementData
    )
    .setIn(
      [needUri, "connections", connectionUri, "agreementData", "isLoaded"],
      true
    )
    .setIn(
      [needUri, "connections", connectionUri, "isLoadingAgreementData"],
      false
    );
}

export function setShowAgreementData(state, connectionUri, showAgreementData) {
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

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
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

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
  const need = getOwnedNeedByConnectionUri(state, connectionUri);

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

export function addActiveConnectionsToNeedInLoading(state, needUri, connUris) {
  needUri &&
    connUris &&
    connUris.size > 0 &&
    console.debug("addActiveConnectionsToNeedInLoading: ", connUris);
  let newState = state;
  needUri &&
    connUris &&
    connUris.forEach(connUri => {
      newState = addActiveConnectionToNeed(newState, needUri, connUri);
    });
  return newState;
}

export function addActiveConnectionToNeed(state, needUri, connUri) {
  const storedNeed = state.get(needUri);
  const isConnectionPresent =
    storedNeed && !!storedNeed.getIn(["connections", connUri]);

  if (storedNeed && !isConnectionPresent) {
    const connection = Immutable.fromJS({
      uri: connUri,
      state: undefined,
      messages: Immutable.Map(),
      agreementData: undefined,
      remoteNeedUri: undefined,
      remoteConnectionUri: undefined,
      creationDate: undefined,
      lastUpdateDate: undefined,
      unread: undefined,
      isRated: false,
      isLoadingMessages: false,
      isLoading: true,
      showAgreementData: false,
    });

    return state.mergeDeepIn([needUri, "connections", connUri], connection);
  }

  return state;
}
