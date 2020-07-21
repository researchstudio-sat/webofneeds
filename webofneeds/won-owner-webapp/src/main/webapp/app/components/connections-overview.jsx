import React, { useState } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import {
  get,
  generateLink,
  filterConnectionsBySearchValue,
  extractAtomUriFromConnectionUri,
} from "../utils.js";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";
import WonTitlePicker from "./details/picker/title-picker.jsx";

import "~/style/_connections-overview.scss";

export default function WonConnectionsOverview({
  chatConnections,
  storedAtoms,
}) {
  const history = useHistory();
  const [searchText, setSearchText] = useState({ value: "" });

  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  let allChatConnections = filterConnectionsBySearchValue(
    chatConnections,
    storedAtoms,
    searchText,
    externalDataState,
    true,
    true
  );

  const generateConnectionElements = () => {
    let connectionElements = [];
    if (allChatConnections) {
      allChatConnections.map((conn, connUri) => {
        const atomUri = extractAtomUriFromConnectionUri(connUri);
        connectionElements.push(
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
    }
    return connectionElements;
  };

  return (
    <won-connections-overview>
      <div className="co__search">
        <WonTitlePicker
          onUpdate={setSearchText}
          initialValue={searchText.value}
          detail={{ placeholder: "Filter Chats" }}
        />
      </div>
      {generateConnectionElements()}
    </won-connections-overview>
  );
}
WonConnectionsOverview.propTypes = {
  chatConnections: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
};
