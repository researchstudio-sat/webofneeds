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
  getUri,
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
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);
  const isAtomOwned = accountUtils.isAtomOwned(accountState, getUri(atom));

  const [showRequestReceived, toggleRequestReceived] = useState(false);
  const [showRequestSent, toggleRequestSent] = useState(false);
  const [showSuggestions, toggleSuggestions] = useState(false);
  const [showClosed, toggleClosed] = useState(false);
  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(generalSelectors.getAtoms);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const socketUri = atomUtils.getSocketUri(atom, socketType);
  const filteredStoredAtoms = filterAtomsBySearchValue(
    storedAtoms,
    searchText,
    externalDataState
  );

  const reactions = atomUtils.getReactions(atom, socketType);

  const connections = filterConnectionsBySearchValue(
    relevantConnections,
    storedAtoms,
    searchText,
    externalDataState
  );

  // If an atom is owned we display all connStates, if the atom is not owned we only display connected states
  const activeConnections = connections.filter(connectionUtils.isConnected);

  const requestSentConnections = connections.filter(
    connectionUtils.isRequestSent
  );

  const requestReceivedConnections = connections.filter(
    connectionUtils.isRequestReceived
  );

  const suggestedConnections = connections.filter(connectionUtils.isSuggested);

  const closedConnections = connections.filter(connectionUtils.isClosed);

  function generateConnectionItems(connections) {
    const connectionElements = [];

    connections &&
      connections
        .toOrderedMap()
        .sortBy(conn => {
          const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
          return lastUpdateDate && lastUpdateDate.getTime();
        })
        .reverse()
        .map((conn, connUri) => {
          const flip = connectionUtils.hasTargetSocketUri(conn, socketUri);
          connectionElements.push(
            <ItemComponent
              key={connUri}
              connection={conn}
              currentLocation={currentLocation}
              atom={
                flip
                  ? get(storedAtoms, extractAtomUriFromConnectionUri(connUri))
                  : atom
              }
              targetAtom={get(
                storedAtoms,
                connectionUtils.getTargetAtomUri(conn)
              )}
              isOwned={isAtomOwned}
              flip={flip}
            />
          );
        });

    return connectionElements;
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
                  targetAtom={atom}
                  targetSocketType={socketType}
                  className={addButtonClassName}
                />
              ) : (
                undefined
              )}
              {generateConnectionItems(activeConnections)}
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
