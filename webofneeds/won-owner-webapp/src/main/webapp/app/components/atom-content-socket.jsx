/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_outgoing from "~/images/won-icons/ico36_outgoing.svg";
import VisibilitySensor from "react-visibility-sensor";

import {
  get,
  generateLink,
  sortByDate,
  filterConnectionsBySearchValue,
} from "../utils.js";
import { actionCreators } from "../actions/actions.js";

import * as atomUtils from "../redux/utils/atom-utils";
import * as accountUtils from "../redux/utils/account-utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import vocab from "../service/vocab.js";
import WonAtomContextSwipeableView from "./atom-context-swipeable-view";
import WonTitlePicker from "./details/picker/title-picker.jsx";

import "~/style/_atom-content-socket.scss";
import * as generalSelectors from "../redux/selectors/general-selectors";

export default function WonAtomContentSocket({ atom, socketType }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const socketUri = atomUtils.getSocketUri(atom, socketType);
  const accountState = useSelector(state => get(state, "account"));
  const isAtomOwned = accountUtils.isAtomOwned(accountState, get(atom, "uri"));

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
  const activeConnections = isAtomOwned
    ? connections.filter(
        conn =>
          !(connectionUtils.isSuggested(conn) || connectionUtils.isClosed(conn))
      )
    : connections.filter(conn => connectionUtils.isConnected(conn));
  const suggestedConnections = isAtomOwned
    ? connections.filter(conn => connectionUtils.isSuggested(conn))
    : Immutable.Map();
  const closedConnections = isAtomOwned
    ? connections.filter(conn => connectionUtils.isClosed(conn))
    : Immutable.Map();

  function markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      setTimeout(() => {
        dispatch(
          actionCreators.connections__markAsRead({
            connectionUri: get(conn, "uri"),
            atomUri: get(atom, "uri"),
          })
        );
      }, 1500);
    }
  }

  function closeConnection(
    conn,
    dialogText = "Do you want to remove the Connection?"
  ) {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Connect",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");

            if (connectionUtils.isUnread(conn)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: get(conn, "uri"),
                  atomUri: get(atom, "uri"),
                })
              );
            }

            dispatch(actionCreators.connections__close(connUri));
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
  }

  function openRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    if (connectionUtils.isUnread(conn)) {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: get(conn, "uri"),
          atomUri: get(atom, "uri"),
        })
      );
    }

    const senderSocketUri = get(conn, "socketUri");
    const targetSocketUri = get(conn, "targetSocketUri");
    dispatch(
      actionCreators.atoms__connectSockets(
        senderSocketUri,
        targetSocketUri,
        message
      )
    );
  }

  function sendRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Connect",
      text: "Do you want to send a Request?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");
            const senderSocketUri = get(conn, "socketUri");
            const targetSocketUri = get(conn, "targetSocketUri");

            if (connectionUtils.isUnread(conn)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: get(conn, "uri"),
                  atomUri: get(atom, "uri"),
                })
              );
            }

            dispatch(
              actionCreators.connections__rate(
                connUri,
                vocab.WONCON.binaryRatingGood
              )
            );

            dispatch(
              actionCreators.atoms__connectSockets(
                senderSocketUri,
                targetSocketUri,
                message
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
  }

  function generateConnectionItems(connections) {
    const connectionsArray = sortByDate(connections) || [];

    return connectionsArray.map(conn => {
      let actionButtons;
      let headerClassName;

      switch (get(conn, "state")) {
        case vocab.WON.RequestReceived:
          headerClassName = "status--received";
          actionButtons = isAtomOwned ? (
            <div className="acs__item__actions">
              <svg
                className="acs__item__actions__icon request won-icon"
                onClick={() => openRequest(conn)}
              >
                <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
              </svg>
              <svg
                className="acs__item__actions__icon primary won-icon"
                onClick={() => closeConnection(conn, "Reject Request?")}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            </div>
          ) : (
            undefined
          );
          break;

        case vocab.WON.RequestSent:
          headerClassName = "status--sent";
          actionButtons = isAtomOwned ? (
            <div className="acs__item__actions">
              <svg
                className="acs__item__actions__icon disabled won-icon"
                disabled={true}
              >
                <use xlinkHref={ico36_outgoing} href={ico36_outgoing} />
              </svg>
              <svg
                className="acs__item__actions__icon secondary won-icon"
                onClick={() => closeConnection(conn, "Cancel Request?")}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            </div>
          ) : (
            undefined
          );
          break;

        case vocab.WON.Connected:
          actionButtons = isAtomOwned ? (
            <div className="acs__item__actions">
              <svg
                className="acs__item__actions__icon secondary won-icon"
                onClick={() => closeConnection(conn)}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            </div>
          ) : (
            undefined
          );
          break;

        case vocab.WON.Closed:
          headerClassName = "status--closed";
          actionButtons = isAtomOwned ? (
            <div className="acs__item__actions">Member has been removed</div>
          ) : (
            undefined
          );
          break;

        case vocab.WON.Suggested:
          headerClassName = "status--suggested";
          actionButtons = isAtomOwned ? (
            <div className="acs__item__actions">
              <svg
                className="acs__item__actions__icon request won-icon"
                onClick={() => sendRequest(conn)}
              >
                <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
              </svg>
              <svg
                className="acs__item__actions__icon primary won-icon"
                onClick={() => closeConnection(conn, "Remove Suggestion?")}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            </div>
          ) : (
            undefined
          );
          break;

        default:
          actionButtons = isAtomOwned ? (
            <div className="acs__item__actions">Unknown State</div>
          ) : (
            undefined
          );
      }

      return (
        <VisibilitySensor
          key={get(conn, "uri")}
          onChange={isVisible => {
            isVisible && connectionUtils.isUnread(conn) && markAsRead(conn);
          }}
          intervalDelay={2000}
        >
          <div
            className={
              "acs__item " +
              (connectionUtils.isUnread(conn) ? " won-unread " : "")
            }
          >
            <WonAtomContextSwipeableView
              className={headerClassName}
              actionButtons={actionButtons}
              atomUri={get(conn, "targetAtomUri")}
              toLink={generateLink(
                history.location,
                {
                  postUri: get(conn, "targetAtomUri"),
                },
                "/post"
              )}
            />
          </div>
        </VisibilitySensor>
      );
    });
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
};
