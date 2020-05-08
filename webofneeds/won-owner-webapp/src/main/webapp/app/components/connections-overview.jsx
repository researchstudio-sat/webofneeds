import React, { useState } from "react";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import {
  get,
  getIn,
  sortByDate,
  getQueryParams,
  generateLink,
  filterConnectionsBySearchValue,
} from "../utils.js";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";
import WonTitlePicker from "./details/picker/title-picker.jsx";

import "~/style/_connections-overview.scss";

export default function WonConnectionsOverview() {
  const history = useHistory();
  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(state => generalSelectors.getAtoms(state));

  let allChatConnections = filterConnectionsBySearchValue(
    useSelector(state => generalSelectors.getAllChatConnections(state)),
    storedAtoms,
    searchText,
    true
  );

  const { connectionUri } = getQueryParams(history.location);

  const connUriInRoute = connectionUri;

  const isConnUriInRoutePresent = !!get(allChatConnections, connUriInRoute);

  const connectionInRoute = useSelector(state => {
    const atom = generalSelectors.getOwnedAtomByConnectionUri(
      state,
      connUriInRoute
    );
    return getIn(atom, ["connections", connUriInRoute]);
  });

  // If the connection from the Uri was not present and there is a connection within the uri we add it to our map
  if (!isConnUriInRoutePresent && connectionInRoute) {
    allChatConnections = allChatConnections.set(
      connUriInRoute,
      connectionInRoute
    );
  }

  const connections = sortByDate(allChatConnections) || [];

  const connectionElements =
    connections &&
    connections.map(conn => {
      const connUri = get(conn, "uri");
      const atomUri = connUri.split("/c")[0];
      return (
        <div className="co__item" key={connUri}>
          <WonConnectionSelectionItem
            connection={conn}
            senderAtom={get(storedAtoms, atomUri)}
            toLink={generateLink(history.location, {
              connectionUri: connUri,
            })}
          />
        </div>
      );
    });

  return (
    <won-connections-overview>
      <div className="co__search">
        <WonTitlePicker
          onUpdate={setSearchText}
          initialValue={searchText.value}
          detail={{ placeholder: "Filter Chats" }}
        />
      </div>
      {connectionElements}
    </won-connections-overview>
  );
}
