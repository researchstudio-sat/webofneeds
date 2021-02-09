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

import "~/style/_atom-content-members.scss";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function WonAtomContentMembers({
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
  const [showMembers, toggleMembers] = useState(true);
  const [showRoles, toggleRoles] = useState(true);
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

  const activeRoles = connections
    .filter(conn => connectionUtils.isConnected(conn))
    .filter(conn => {
      const targetSocketType = atomUtils.getSocketType(
        get(storedAtoms, connectionUtils.getTargetAtomUri(conn)),
        connectionUtils.getTargetSocketUri(conn)
      );

      return (
        targetSocketType === vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted
      );
    });

  const activeMembers = connections
    .filter(conn => connectionUtils.isConnected(conn))
    .filter(conn => {
      const targetSocketType = atomUtils.getSocketType(
        get(storedAtoms, connectionUtils.getTargetAtomUri(conn)),
        connectionUtils.getTargetSocketUri(conn)
      );

      return (
        !targetSocketType ||
        targetSocketType === vocab.WXSCHEMA.MemberOfSocketCompacted
      );
    });

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
    const connectionElements = [];

    connections &&
      connections
        .toOrderedMap()
        .sortBy(conn => {
          return atomUtils.getTitle(
            get(storedAtoms, connectionUtils.getTargetAtomUri(conn))
          );
        })
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
          "acm__segment__header " + (toggleFunction ? " clickable " : "")
        }
        onClick={toggleFunction ? () => toggleFunction(!isExpanded) : undefined}
      >
        <div className="acm__segment__header__title">
          {label}
          <span className="acm__segment__header__title__count">
            {"(" + size + ")"}
          </span>
        </div>
        <div className="acm__segment__header__carret" />
        {toggleFunction ? (
          <svg
            className={
              "acm__segment__header__carret " +
              (isExpanded
                ? " acm__segment__header__carret--expanded "
                : " acm__segment__header__carret--collapsed ")
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
    <won-atom-content-members>
      <div className="acm__search">
        <WonTitlePicker
          onUpdate={setSearchText}
          initialValue={searchText.value}
          detail={{
            placeholder: `Filter ${
              reactions
                ? wonLabelUtils.getSocketItemLabels(
                    socketType,
                    reactions.keys()
                  )
                : wonLabelUtils.getSocketItemsLabel(socketType)
            }`,
          }}
        />
      </div>
      {!showAddPicker ? (
        activeRoles.size > 0 || activeMembers.size > 0 || reactions ? (
          <React.Fragment>
            {activeRoles.size > 0 ? (
              <div className="acm__segment">
                {activeMembers.size > 0 &&
                  generateHeaderItem(
                    "Roles",
                    activeRoles.size,
                    toggleRoles,
                    showRoles
                  )}
                {showRoles && (
                  <div
                    className={`acm__segment__content ${segmentContentClassName ||
                      ""}`}
                  >
                    {generateConnectionItems(activeRoles)}
                  </div>
                )}
              </div>
            ) : (
              undefined
            )}
            {activeMembers.size > 0 ? (
              <div className="acm__segment">
                {activeRoles.size > 0 &&
                  generateHeaderItem(
                    "Members",
                    activeMembers.size,
                    toggleMembers,
                    showMembers
                  )}
                {showMembers && (
                  <div
                    className={`acm__segment__content ${segmentContentClassName ||
                      ""}`}
                  >
                    {generateConnectionItems(activeMembers)}
                  </div>
                )}
              </div>
            ) : (
              undefined
            )}
            <div className="acm__segment">
              <div
                className={`acm__segment__content ${segmentContentClassName ||
                  ""}`}
              >
                {atomUtils.isActive(atom) &&
                reactions &&
                ((isAtomOwned || !vocab.refuseAddToNonOwned[socketType]) &&
                  (!vocab.socketCapacity[socketType] ||
                    relevantConnections.filter(connectionUtils.isConnected)
                      .size < vocab.socketCapacity[socketType])) ? (
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
          </React.Fragment>
        ) : (
          <div className="acm__segment">
            <div
              className={`acm__segment__content ${segmentContentClassName ||
                ""}`}
            >
              <div className="acm__empty">
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
        <div className="acm__segment">
          {generateHeaderItem(
            "Incoming Requests",
            requestReceivedConnections.size,
            toggleRequestReceived,
            showRequestReceived
          )}
          {showRequestReceived ? (
            <div
              className={`acm__segment__content ${segmentContentClassName ||
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
        <div className="acm__segment">
          {generateHeaderItem(
            isAtomOwned ? "Sent Requests" : "Pending Requests",
            requestSentConnections.size,
            toggleRequestSent,
            showRequestSent
          )}
          {showRequestSent ? (
            <div
              className={`acm__segment__content ${segmentContentClassName ||
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
        <div className="acm__segment">
          {generateHeaderItem(
            isAtomOwned ? "Suggestions" : "Suggested in",
            suggestedConnections.size,
            toggleSuggestions,
            showSuggestions
          )}
          {showSuggestions ? (
            <div
              className={`acm__segment__content ${segmentContentClassName ||
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
        <div className="acm__segment">
          {generateHeaderItem(
            "Closed",
            closedConnections.size,
            toggleClosed,
            showClosed
          )}
          {showClosed ? (
            <div
              className={`acm__segment__content ${segmentContentClassName ||
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
    </won-atom-content-members>
  );
}

WonAtomContentMembers.propTypes = {
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
