/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import {
  get,
  sortByDate,
  filterConnectionsBySearchValue,
  filterAtomsBySearchValue,
} from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils";
import * as accountUtils from "../redux/utils/account-utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import WonSocketAddAtom from "./socket-add-atom.jsx";

import "~/style/_atom-content-socket.scss";
import * as generalSelectors from "../redux/selectors/general-selectors";

export default function WonAtomContentSocket({
  atom,
  socketType,
  ItemComponent,
}) {
  const accountState = useSelector(generalSelectors.getAccountState);
  const isAtomOwned = accountUtils.isAtomOwned(accountState, get(atom, "uri"));

  const [showRequestReceived, toggleRequestReceived] = useState(false);
  const [showRequestSent, toggleRequestSent] = useState(false);
  const [showSuggestions, toggleSuggestions] = useState(false);
  const [showClosed, toggleClosed] = useState(false);
  const [searchText, setSearchText] = useState({ value: "" });
  const [showAddPicker, toggleAddPicker] = useState(false);

  const storedAtoms = useSelector(generalSelectors.getAtoms);

  const filteredStoredAtoms = filterAtomsBySearchValue(storedAtoms, searchText);

  const allAtomConnections = atomUtils.getConnections(atom, socketType);

  const reactions = atomUtils.getReactions(atom, socketType);

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
        <ItemComponent
          connection={conn}
          atom={atom}
          targetAtom={get(storedAtoms, get(conn, "targetAtomUri"))}
          isOwned={isAtomOwned}
        />
      </React.Fragment>
    ));
  }

  function generateHeaderItem(label, size, toggleFunction, isExpanded) {
    return (
      <div
        className="acs__segment__header clickable"
        onClick={() => toggleFunction(!isExpanded)}
      >
        <div className="acs__segment__header__title">
          {label}
          <span className="acs__segment__header__title__count">
            {"(" + size + ")"}
          </span>
        </div>
        <div className="acs__segment__header__carret" />
        <svg
          className={
            "acs__segment__header__carret " +
            (isExpanded
              ? " acs__segment__header__carret--expanded "
              : " acs__segment__header__carret--collapsed ")
          }
        >
          <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
        </svg>
      </div>
    );
  }

  function generateAddItem() {
    if (reactions) {
      const onClick = () => {
        //TODO: SHOW UI-ELEMENTS AFTER CLICK
        console.debug(
          "Clicked Add Reactions: ",
          JSON.stringify(reactions.toJS())
        );
        toggleAddPicker(!showAddPicker); //TODO: SHOULD NOT BE A TOGGLE IN MY OPINION
      };

      //TODO: DO NOT ALLOW REACTIONS ON INACTIVE ATOMS
      //TODO: LABEL BASED ON OWNERSHIP
      //TODO: SHOW OPTIONS FOR ALL POSSIBLE USECASES (SIMILAR TO THE ATOM-FOOTER
      //TODO: SHOW CREATE NEW OPTION FOR ALL POSSIBLE USECASES

      return (
        <div className="socketadd clickable" onClick={onClick}>
          <svg className="socketadd__icon" title="Create a new post">
            <use xlinkHref={ico36_plus} href={ico36_plus} />
          </svg>
          <span className="socketadd__label">Add</span>
        </div>
      );
    } else {
      return undefined;
    }
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
      {!showAddPicker ? (
        activeConnections.size > 0 || reactions ? (
          <div className="acs__segment">
            <div className="acs__segment__content">
              {generateConnectionItems(activeConnections)}
              {generateAddItem()}
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
        )
      ) : (
        undefined
      )}
      {!showAddPicker && requestReceivedConnections.size > 0 ? (
        <div className="acs__segment">
          {generateHeaderItem(
            "Incoming Requests",
            requestReceivedConnections.size,
            toggleRequestReceived,
            showRequestReceived
          )}
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
      {!showAddPicker && requestSentConnections.size > 0 ? (
        <div className="acs__segment">
          {generateHeaderItem(
            "Sent Requests",
            requestSentConnections.size,
            toggleRequestSent,
            showRequestSent
          )}
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
      {!showAddPicker && suggestedConnections.size > 0 ? (
        <div className="acs__segment">
          {generateHeaderItem(
            "Suggestions",
            suggestedConnections.size,
            toggleSuggestions,
            showSuggestions
          )}
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
      {!showAddPicker && closedConnections.size > 0 ? (
        <div className="acs__segment">
          {generateHeaderItem(
            "Closed",
            closedConnections.size,
            toggleClosed,
            showClosed
          )}
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
      {showAddPicker ? (
        <WonSocketAddAtom
          addToAtom={atom}
          storedAtoms={filteredStoredAtoms}
          addToSocketType={socketType}
          reactions={reactions}
          accountState={accountState}
          onClose={() => toggleAddPicker(!showAddPicker)}
        />
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
