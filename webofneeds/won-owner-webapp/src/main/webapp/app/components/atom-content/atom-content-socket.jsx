/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import vocab from "../../service/vocab.js";
import {
  get,
  sortByDate,
  filterConnectionsBySearchValue,
  filterAtomsBySearchValue,
  extractAtomUriFromConnectionUri,
} from "../../utils.js";

import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import WonTitlePicker from "../details/picker/title-picker.jsx";
import WonSocketAddAtom from "../socket-add-atom.jsx";
import WonSocketAddButton from "../socket-add-button.jsx";

import "~/style/_atom-content-socket.scss";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function WonAtomContentSocket({
  atom,
  socketType,
  ItemComponent,
  relevantConnections,
  showAddPicker,
  toggleAddPicker,
  addButtonClassName,
  segmentContentClassName,
  setVisibleTab,
}) {
  const accountState = useSelector(generalSelectors.getAccountState);
  const isAtomOwned = accountUtils.isAtomOwned(accountState, get(atom, "uri"));

  const [showRequestReceived, toggleRequestReceived] = useState(false);
  const [showRequestSent, toggleRequestSent] = useState(false);
  const [showSuggestions, toggleSuggestions] = useState(false);
  const [showClosed, toggleClosed] = useState(false);
  const [searchText, setSearchText] = useState({ value: "" });

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
              flip
                ? get(
                    storedAtoms,
                    extractAtomUriFromConnectionUri(get(conn, "uri"))
                  )
                : atom
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
        activeConnections.size > 0 || reactions ? (
          <div className="acs__segment">
            <div
              className={`acs__segment__content ${segmentContentClassName ||
                ""}`}
            >
              {generateConnectionItems(activeConnections)}
              {atomUtils.isActive(atom) &&
              reactions &&
              ((isAtomOwned || !vocab.refuseAddToNonOwned[socketType]) &&
                (!vocab.socketCapacity[socketType] ||
                  relevantConnections.filter(connectionUtils.isConnected).size <
                    vocab.socketCapacity[socketType])) ? (
                <WonSocketAddButton
                  senderReactions={reactions}
                  isAtomOwned={isAtomOwned}
                  onClick={() => toggleAddPicker(!showAddPicker)}
                  targetSocketType={socketType}
                  className={addButtonClassName}
                />
              ) : (
                undefined
              )}
            </div>
          </div>
        ) : (
          <div className="acs__segment">
            <div
              className={`acs__segment__content ${segmentContentClassName ||
                ""}`}
            >
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
            <div
              className={`acs__segment__content ${segmentContentClassName ||
                ""}`}
            >
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
            <div
              className={`acs__segment__content ${segmentContentClassName ||
                ""}`}
            >
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
            <div
              className={`acs__segment__content ${segmentContentClassName ||
                ""}`}
            >
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
            <div
              className={`acs__segment__content ${segmentContentClassName ||
                ""}`}
            >
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
          onClose={() => {
            if (vocab.socketCapacity[socketType] === 1) {
              setVisibleTab("DETAIL");
            }
            toggleAddPicker(!showAddPicker);
          }}
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
  showAddPicker: PropTypes.bool.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
  addButtonClassName: PropTypes.string,
  segmentContentClassName: PropTypes.string,
  setVisibleTab: PropTypes.func.isRequired,
};
