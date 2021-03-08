import React, { useState } from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { useHistory } from "react-router-dom";
import vocab from "../../service/vocab.js";
import {
  getUri,
  generateLink,
  filterConnectionsBySearchValue,
  filterAtomsBySearchValue,
} from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";

import WonConnectionSelectionItem from "../connection-selection-item.jsx";
import WonTitlePicker from "../details/picker/title-picker.jsx";
import WonSocketAddAtom from "../socket-add-atom.jsx";
import WonSocketAddButton from "../socket-add-button.jsx";

import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

import "~/style/_atom-content-chats.scss";

export default function AtomContentChats({
  atom,
  relevantConnections,
  showAddPicker,
  toggleAddPicker,
}) {
  const history = useHistory();
  const accountState = useSelector(generalSelectors.getAccountState);
  const isAtomOwned = accountUtils.isAtomOwned(accountState, getUri(atom));

  const [showSuggestions, toggleSuggestions] = useState(false);
  const [showClosed, toggleClosed] = useState(false);
  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(generalSelectors.getAtoms);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const filteredStoredAtoms = filterAtomsBySearchValue(
    storedAtoms,
    searchText,
    externalDataState
  );

  const reactions = atomUtils.getReactions(
    atom,
    vocab.CHAT.ChatSocketCompacted
  );

  // If the atom is not owned, we will show Our ChatConnections to this atom instead
  const chatConnections = filterConnectionsBySearchValue(
    relevantConnections,
    storedAtoms,
    searchText,
    externalDataState,
    false,
    true
  );

  const activeChatConnections = chatConnections.filter(
    conn =>
      !(connectionUtils.isSuggested(conn) || connectionUtils.isClosed(conn))
  );
  const suggestedChatConnections = chatConnections.filter(
    connectionUtils.isSuggested
  );
  const closedChatConnections = chatConnections.filter(
    connectionUtils.isClosed
  );

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
          connectionElements.push(
            <div className="acc__item" key={connUri}>
              <WonConnectionSelectionItem
                connection={conn}
                toLink={generateLink(
                  history.location,
                  {
                    postUri: getUri(atom),
                    connectionUri: connUri,
                  },
                  "/connections",
                  false
                )}
                flip={!isAtomOwned}
              />
            </div>
          );
        });

    return connectionElements;
  }

  function generateHeaderItem(label, size, toggleFunction, isExpanded) {
    return (
      <div
        className="acc__segment__header clickable"
        onClick={() => toggleFunction(!isExpanded)}
      >
        <div className="acc__segment__header__title">
          {label}
          <span className="acc__segment__header__title__count">
            {"(" + size + ")"}
          </span>
        </div>
        <div className="acc__segment__header__carret" />
        <svg
          className={
            "acc__segment__header__carret " +
            (isExpanded
              ? " acc__segment__header__carret--expanded "
              : " acc__segment__header__carret--collapsed ")
          }
        >
          <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
        </svg>
      </div>
    );
  }

  return (
    <won-atom-content-chats>
      <div className="acc__search">
        <WonTitlePicker
          onUpdate={setSearchText}
          initialValue={searchText.value}
          detail={{ placeholder: "Filter Chats" }}
        />
      </div>
      {!showAddPicker ? (
        activeChatConnections.size > 0 || reactions ? (
          <div className="acc__segment">
            <div className="acc__segment__content borderTop">
              {atomUtils.isActive(atom) && reactions ? (
                <WonSocketAddButton
                  senderReactions={reactions}
                  isAtomOwned={isAtomOwned}
                  targetAtom={atom}
                  onClick={() => toggleAddPicker(!showAddPicker)}
                  targetSocketType={vocab.CHAT.ChatSocketCompacted}
                />
              ) : (
                undefined
              )}
              {generateConnectionItems(activeChatConnections)}
            </div>
          </div>
        ) : (
          <div className="acc__segment">
            <div className="acc__segment__content">
              <div className="acc__empty">
                {searchText.value.trim().length > 0
                  ? `No ${wonLabelUtils.getSocketItemsLabel(
                      vocab.CHAT.ChatSocketCompacted
                    )} found for '${searchText.value.trim()}'`
                  : `No ${wonLabelUtils.getSocketItemsLabel(
                      vocab.CHAT.ChatSocketCompacted
                    )}`}
              </div>
            </div>
          </div>
        )
      ) : (
        undefined
      )}
      {!showAddPicker && suggestedChatConnections.size > 0 ? (
        <div className="acc__segment">
          {generateHeaderItem(
            "Suggestions",
            suggestedChatConnections.size,
            toggleSuggestions,
            showSuggestions
          )}
          {showSuggestions ? (
            <div className="acc__segment__content">
              {generateConnectionItems(suggestedChatConnections)}
            </div>
          ) : (
            undefined
          )}
        </div>
      ) : (
        undefined
      )}
      {!showAddPicker && closedChatConnections.size > 0 ? (
        <div className="acc__segment">
          {generateHeaderItem(
            "Closed",
            closedChatConnections.size,
            toggleClosed,
            showClosed
          )}
          {showClosed ? (
            <div className="acc__segment__content">
              {generateConnectionItems(closedChatConnections)}
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
          addToSocketType={vocab.CHAT.ChatSocketCompacted}
          reactions={reactions}
          accountState={accountState}
          onClose={() => toggleAddPicker(!showAddPicker)}
        />
      ) : (
        undefined
      )}
    </won-atom-content-chats>
  );
}

AtomContentChats.propTypes = {
  atom: PropTypes.object.isRequired,
  relevantConnections: PropTypes.object.isRequired,
  showAddPicker: PropTypes.bool.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};
