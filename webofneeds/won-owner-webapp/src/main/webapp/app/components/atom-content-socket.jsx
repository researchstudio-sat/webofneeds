/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { get, sortByDate, filterConnectionsBySearchValue } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils";
import * as accountUtils from "../redux/utils/account-utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import WonSuggestAtomPicker from "./details/picker/suggest-atom-picker.jsx";

import "~/style/_atom-content-socket.scss";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { actionCreators } from "../actions/actions";

export default function WonAtomContentSocket({
  atom,
  socketType,
  ItemComponent,
  suggestPicker,
}) {
  const dispatch = useDispatch();
  const accountState = useSelector(state => get(state, "account"));
  const isAtomOwned = accountUtils.isAtomOwned(accountState, get(atom, "uri"));

  const [showRequestReceived, toggleRequestReceived] = useState(false);
  const [showRequestSent, toggleRequestSent] = useState(false);
  const [showSuggestions, toggleSuggestions] = useState(false);
  const [showClosed, toggleClosed] = useState(false);
  const [showSuggestAtomExpanded, toggleSuggestAtomExpanded] = useState(false);
  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(state => generalSelectors.getAtoms(state));

  const allAtomConnections = atomUtils.getConnections(atom, socketType);

  let excludedFromRequestUris = [get(atom, "uri")];

  allAtomConnections &&
    allAtomConnections.map(conn =>
      excludedFromRequestUris.push(get(conn, "targetAtomUri"))
    );

  const connections = filterConnectionsBySearchValue(
    allAtomConnections,
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
      {isAtomOwned && suggestPicker ? (
        <React.Fragment>
          <WonLabelledHr
            label={suggestPicker.label}
            arrow={showSuggestAtomExpanded ? "up" : "down"}
            onClick={() => toggleSuggestAtomExpanded(!showSuggestAtomExpanded)}
          />
          {showSuggestAtomExpanded ? (
            <WonSuggestAtomPicker
              initialValue={undefined}
              onUpdate={({ value }) => {
                const targetAtomUri = value;
                if (!isAtomOwned) {
                  return;
                }

                const targetAtom = get(storedAtoms, targetAtomUri);

                const senderSocketUri = atomUtils.getSocketUri(
                  atom,
                  socketType
                );

                let targetSocketUri;
                for (const allowedSocket of suggestPicker.allowedSockets) {
                  //Use the first found socketUri of the allowedSockets (in order) to be used as the targetSocket for the connect
                  targetSocketUri = atomUtils.getSocketUri(
                    targetAtom,
                    allowedSocket
                  );
                  if (targetSocketUri) break;
                }

                if (!targetSocketUri) {
                  return;
                }

                const payload = {
                  caption: suggestPicker.modalCaption || "Connection",
                  text: suggestPicker.modalText || "Send Request?",
                  buttons: [
                    {
                      caption: "Yes",
                      callback: () => {
                        dispatch(
                          actionCreators.atoms__connectSockets(
                            senderSocketUri,
                            targetSocketUri,
                            ""
                          )
                        );
                        dispatch(actionCreators.view__hideModalDialog());
                      },
                    },
                    {
                      caption: "No",
                      callback: () => {
                        dispatch(actionCreators.view__hideModalDialog());
                      },
                    },
                  ],
                };
                dispatch(actionCreators.view__showModalDialog(payload));
              }}
              detail={{ placeholder: suggestPicker.placeholder }}
              excludedUris={excludedFromRequestUris}
              allowedSockets={suggestPicker.allowedSockets}
              excludedText={suggestPicker.excludedText}
              notAllowedSocketText={suggestPicker.notAllowedSocketText}
              noSuggestionsText={suggestPicker.noSuggestionsText}
            />
          ) : (
            undefined
          )}
        </React.Fragment>
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
  suggestPicker: PropTypes.object,
};
