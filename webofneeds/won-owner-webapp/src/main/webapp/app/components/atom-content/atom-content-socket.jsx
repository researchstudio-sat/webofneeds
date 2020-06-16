/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import {
  get,
  sortByDate,
  filterConnectionsBySearchValue,
  filterAtomsBySearchValue,
} from "../../utils.js";

import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import WonTitlePicker from "../details/picker/title-picker.jsx";
import WonSocketAddAtom from "../socket-add-atom.jsx";

import "~/style/_atom-content-socket.scss";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function WonAtomContentSocket({
  atom,
  socketType,
  ItemComponent,
  allowAdHoc,
  relevantConnections,
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

  const socketUri = atomUtils.getSocketUri(atom, socketType);
  const filteredStoredAtoms = filterAtomsBySearchValue(storedAtoms, searchText);

  const reactions = atomUtils.getReactions(atom, socketType);

  const connections = filterConnectionsBySearchValue(
    relevantConnections,
    storedAtoms,
    searchText
  );

  // If an atom is owned we display all connStates, if the atom is not owned we only display connected states
  const activeConnections = connections.filter(conn =>
    connectionUtils.isConnected(conn)
  );

  const requestSentConnections = connections.filter(conn =>
    connectionUtils.isRequestSent(conn)
  );

  const requestReceivedConnections = connections.filter(conn =>
    connectionUtils.isRequestReceived(conn)
  );

  const suggestedConnections = connections.filter(conn =>
    connectionUtils.isSuggested(conn)
  );

  const closedConnections = connections.filter(conn =>
    connectionUtils.isClosed(conn)
  );

  function generateConnectionItems(connections) {
    const connectionsArray = sortByDate(connections) || [];

    return connectionsArray.map((conn, index) => {
      const flip = get(conn, "targetSocketUri") === socketUri;

      return (
        <React.Fragment key={get(conn, "uri") + "-" + index}>
          <ItemComponent
            connection={conn}
            atom={
              flip ? get(storedAtoms, get(conn, "uri").split("/c")[0]) : atom
            }
            targetAtom={get(storedAtoms, get(conn, "targetAtomUri"))}
            isOwned={isAtomOwned}
            flip={get(conn, "targetSocketUri") === socketUri}
          />
        </React.Fragment>
      );
    });
  }

  function generateHeaderItem(label, size, toggleFunction, isExpanded) {
    return (
      <div
        className={
          "acs__segment__header " + (toggleFunction ? " clickable " : "")
        }
        onClick={toggleFunction ? () => toggleFunction(!isExpanded) : undefined}
      >
        <div className="acs__segment__header__title">
          {label}
          <span className="acs__segment__header__title__count">
            {"(" + size + ")"}
          </span>
        </div>
        <div className="acs__segment__header__carret" />
        {toggleFunction ? (
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
        ) : (
          undefined
        )}
      </div>
    );
  }

  function generateAddItem() {
    if (atomUtils.isActive(atom) && (reactions || allowAdHoc)) {
      const onClick = () => {
        toggleAddPicker(!showAddPicker);
      };

      return (
        <div className="socketadd clickable" onClick={onClick}>
          <svg className="socketadd__icon" title="Create a new post">
            <use xlinkHref={ico36_plus} href={ico36_plus} />
          </svg>
          <span className="socketadd__label">
            {isAtomOwned
              ? `Add ${
                  reactions
                    ? wonLabelUtils.getSocketItemLabels(reactions.keys())
                    : "Atom"
                }`
              : `Connect ${
                  reactions
                    ? wonLabelUtils.getSocketItemLabels(reactions.keys())
                    : "Atom"
                }`}
          </span>
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
          detail={{
            placeholder: `Filter ${wonLabelUtils.getSocketItemsLabel(
              socketType
            )}`,
          }}
        />
      </div>
      {!showAddPicker ? (
        activeConnections.size > 0 || reactions || allowAdHoc ? (
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
                  ? `No ${wonLabelUtils.getSocketItemsLabel(
                      socketType
                    )} found for '${searchText.value.trim()}'`
                  : `No ${wonLabelUtils.getSocketItemsLabel(socketType)}`}
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
            isAtomOwned ? "Sent Requests" : "Pending Requests",
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
            isAtomOwned ? "Suggestions" : "Suggested in",
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
          allowAdHoc={allowAdHoc}
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
  relevantConnections: PropTypes.object.isRequired,
  socketType: PropTypes.string.isRequired,
  ItemComponent: PropTypes.func.isRequired,
  allowAdHoc: PropTypes.bool,
};
