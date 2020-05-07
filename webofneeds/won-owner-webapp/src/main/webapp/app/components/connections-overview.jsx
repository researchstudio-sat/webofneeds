import React, { useState } from "react";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
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
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import WonAtomIcon from "./atom-icon.jsx";

import "~/style/_connections-overview.scss";

export default function WonConnectionsOverview() {
  const history = useHistory();
  const dispatch = useDispatch();
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

  function showAtomDetails(atomUri) {
    dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({ atomUri: atomUri, selectTab: "DETAIL" })
      )
    );
    history.push(
      generateLink(
        history.location,
        { postUri: atomUri, tab: "DETAIL" },
        "/post"
      )
    );
  }

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
          <div className="co__item__own">
            <WonAtomIcon
              atomUri={atomUri}
              onClick={() => showAtomDetails(atomUri)}
            />
          </div>
          <div
            className={
              "co__item__remote " +
              (connectionUtils.isUnread(conn) ? " won-unread " : "")
            }
          >
            <WonConnectionSelectionItem
              connection={conn}
              toLink={generateLink(history.location, {
                connectionUri: connUri,
              })}
            />
          </div>
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
