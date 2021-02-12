import React, { useState } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

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

  let connectionElements = [];
  let closedConnectionElements = [];

  if (allChatConnections) {
    allChatConnections.map((conn, connUri) => {
      const atomUri = extractAtomUriFromConnectionUri(connUri);

      (connectionUtils.isClosed(conn)
        ? closedConnectionElements
        : connectionElements
      ).push(
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
      {closedConnectionElements.length > 0 ? (
        <React.Fragment>
          <div className="co__divider">
            {"Closed (" + closedConnectionElements.length + ")"}
          </div>
          {closedConnectionElements}
        </React.Fragment>
      ) : (
        undefined
      )}
    </won-connections-overview>
  );
}
WonConnectionsOverview.propTypes = {
  chatConnections: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
};
