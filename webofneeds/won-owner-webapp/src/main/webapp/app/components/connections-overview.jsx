import React, { useState } from "react";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import {
  get,
  sortByDate,
  generateLink,
  filterConnectionsBySearchValue,
  extractAtomUriFromConnectionUri,
} from "../utils.js";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";
import WonTitlePicker from "./details/picker/title-picker.jsx";

import "~/style/_connections-overview.scss";

export default function WonConnectionsOverview() {
  const history = useHistory();
  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(generalSelectors.getAtoms);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  let allChatConnections = filterConnectionsBySearchValue(
    useSelector(generalSelectors.getAllChatConnections),
    storedAtoms,
    searchText,
    externalDataState,
    true,
    true
  );

  const connections = sortByDate(allChatConnections) || [];

  const connectionElements =
    connections &&
    connections.map(conn => {
      const connUri = get(conn, "uri");
      const atomUri = extractAtomUriFromConnectionUri(connUri);
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
