/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { get, sortByDate, filterConnectionsBySearchValue } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils";
import * as accountUtils from "../redux/utils/account-utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import WonTitlePicker from "./details/picker/title-picker.jsx";

import "~/style/_atom-content-socket.scss";
import * as generalSelectors from "../redux/selectors/general-selectors";

export default function WonAtomContentSocket({
  atom,
  socketType,
  ItemComponent,
}) {
  const socketUri = atomUtils.getSocketUri(atom, socketType);
  const accountState = useSelector(state => get(state, "account"));
  const isAtomOwned = accountUtils.isAtomOwned(accountState, get(atom, "uri"));

  const [showRequestReceived, toggleRequestReceived] = useState(false);
  const [showRequestSent, toggleRequestSent] = useState(false);
  const [showSuggestions, toggleSuggestions] = useState(false);
  const [showClosed, toggleClosed] = useState(false);
  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(state => generalSelectors.getAtoms(state));

  const connections = filterConnectionsBySearchValue(
    get(atom, "connections").filter(
      conn => get(conn, "socketUri") === socketUri
    ),
    storedAtoms,
    searchText
  );

  // If an atom is owned we display all connStates, if the atom is not owned we only display connected states
  const activeConnections = connections.filter(conn =>
    connectionUtils.isConnected(conn)
  );

  const requestSentConnections = isAtomOwned
    ? connections.filter(conn => connectionUtils.isRequestSent(conn))
    : Immutable.Map();

  const requestReceivedConnections = isAtomOwned
    ? connections.filter(conn => connectionUtils.isRequestReceived(conn))
    : Immutable.Map();

  const suggestedConnections = isAtomOwned
    ? connections.filter(conn => connectionUtils.isSuggested(conn))
    : Immutable.Map();
  const closedConnections = isAtomOwned
    ? connections.filter(conn => connectionUtils.isClosed(conn))
    : Immutable.Map();

  function generateConnectionItems(connections) {
    const connectionsArray = sortByDate(connections) || [];

    return connectionsArray.map((conn, index) => (
      <React.Fragment key={get(conn, "uri") + "-" + index}>
        <ItemComponent connection={conn} atom={atom} isOwned={isAtomOwned} />
      </React.Fragment>
    ));
  }

  return (
    <won-atom-content-socket>
      <div className="acs__search">
        <WonTitlePicker
          onUpdate={setSearchText}
          initialValue={searchText.value}
          detail={{ placeholder: "Filter Connections" }}
        />
      </div>
      {activeConnections.size > 0 ? (
        <div className="acs__segment">
          <div className="acs__segment__content">
            {generateConnectionItems(activeConnections)}
          </div>
        </div>
      ) : (
        <div className="acs__segment">
          <div className="acs__segment__content">
            <div className="acs__empty">
              {searchText.value.trim().length > 0
                ? "No Results"
                : "No Connections"}
            </div>
          </div>
        </div>
      )}
      {requestReceivedConnections.size > 0 ? (
        <div className="acs__segment">
          <div
            className="acs__segment__header clickable"
            onClick={() => toggleRequestReceived(!showRequestReceived)}
          >
            <div className="acs__segment__header__title">
              Incoming Requests
              <span className="acs__segment__header__title__count">
                {"(" + requestReceivedConnections.size + ")"}
              </span>
            </div>
            <div className="acs__segment__header__carret" />
            <svg
              className={
                "acs__segment__header__carret " +
                (showRequestReceived
                  ? " acs__segment__header__carret--expanded "
                  : " acs__segment__header__carret--collapsed ")
              }
            >
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          </div>
          {showRequestReceived ? (
            <div className="acs__segment__content">
              {generateConnectionItems(requestReceivedConnections)}
            </div>
          ) : (
            undefined
          )}
        </div>
      ) : (
        undefined
      )}
      {requestSentConnections.size > 0 ? (
        <div className="acs__segment">
          <div
            className="acs__segment__header clickable"
            onClick={() => toggleRequestSent(!showRequestSent)}
          >
            <div className="acs__segment__header__title">
              Sent Requests
              <span className="acs__segment__header__title__count">
                {"(" + requestSentConnections.size + ")"}
              </span>
            </div>
            <div className="acs__segment__header__carret" />
            <svg
              className={
                "acs__segment__header__carret " +
                (showRequestSent
                  ? " acs__segment__header__carret--expanded "
                  : " acs__segment__header__carret--collapsed ")
              }
            >
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          </div>
          {showRequestSent ? (
            <div className="acs__segment__content">
              {generateConnectionItems(requestSentConnections)}
            </div>
          ) : (
            undefined
          )}
        </div>
      ) : (
        undefined
      )}
      {suggestedConnections.size > 0 ? (
        <div className="acs__segment">
          <div
            className="acs__segment__header clickable"
            onClick={() => toggleSuggestions(!showSuggestions)}
          >
            <div className="acs__segment__header__title">
              Suggestions
              <span className="acs__segment__header__title__count">
                {"(" + suggestedConnections.size + ")"}
              </span>
            </div>
            <div className="acs__segment__header__carret" />
            <svg
              className={
                "acs__segment__header__carret " +
                (showSuggestions
                  ? " acs__segment__header__carret--expanded "
                  : " acs__segment__header__carret--collapsed ")
              }
            >
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          </div>
          {showSuggestions ? (
            <div className="acs__segment__content">
              {generateConnectionItems(suggestedConnections)}
            </div>
          ) : (
            undefined
          )}
        </div>
      ) : (
        undefined
      )}
      {closedConnections.size > 0 ? (
        <div className="acs__segment">
          <div
            className="acs__segment__header clickable"
            onClick={() => toggleClosed(!showClosed)}
          >
            <div className="acs__segment__header__title">
              Closed
              <span className="acs__segment__header__title__count">
                {"(" + closedConnections.size + ")"}
              </span>
            </div>
            <div className="acs__segment__header__carret" />
            <svg
              className={
                "acs__segment__header__carret " +
                (showClosed
                  ? " acs__segment__header__carret--expanded "
                  : " acs__segment__header__carret--collapsed ")
              }
            >
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          </div>
          {showClosed ? (
            <div className="acs__segment__content">
              {generateConnectionItems(closedConnections)}
            </div>
          ) : (
            undefined
          )}
        </div>
      ) : (
        undefined
      )}
    </won-atom-content-socket>
  );
}

WonAtomContentSocket.propTypes = {
  atom: PropTypes.object.isRequired,
  socketType: PropTypes.string.isRequired,
  ItemComponent: PropTypes.func.isRequired,
};
