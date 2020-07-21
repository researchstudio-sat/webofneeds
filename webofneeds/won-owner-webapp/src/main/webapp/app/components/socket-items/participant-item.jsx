/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import * as connectionUtils from "../../redux/utils/connection-utils";
import vocab from "../../service/vocab.js";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";

import "~/style/_socket-item.scss";

import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";
import { useHistory } from "react-router-dom";
import { useDispatch } from "react-redux";
import WonConnectionHeader from "../connection-header";
import WonParticipantSocketActions from "../socket-actions/participant-actions";

export default function WonParticipantItem({
  connection,
  atom,
  isOwned,
  targetAtom,
  flip,
}) {
  const history = useHistory();
  const dispatch = useDispatch();

  const addActionButtons = isOwned || flip;
  let headerClassName;

  function markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      setTimeout(() => {
        dispatch(
          actionCreators.connections__markAsRead({
            connectionUri: get(conn, "uri"),
          })
        );
      }, 1500);
    }
  }

  switch (get(connection, "state")) {
    case vocab.WON.RequestReceived: {
      headerClassName = "status--received";
      break;
    }
    case vocab.WON.Suggested: {
      headerClassName = "status--suggested";
      break;
    }
    case vocab.WON.RequestSent: {
      headerClassName = "status--sent";
      break;
    }
    case vocab.WON.Closed: {
      headerClassName = "status--closed";
      break;
    }
    default: {
      break;
    }
  }

  return (
    <VisibilitySensor
      key={get(connection, "uri")}
      onChange={isVisible => {
        isVisible &&
          connectionUtils.isUnread(connection) &&
          markAsRead(connection);
      }}
      intervalDelay={2000}
    >
      <div
        className={
          "si " + (connectionUtils.isUnread(connection) ? " won-unread " : "")
        }
      >
        <WonAtomContextSwipeableView
          className={headerClassName}
          actionButtons={
            addActionButtons ? (
              <WonParticipantSocketActions connection={connection} />
            ) : (
              undefined
            )
          }
        >
          <WonConnectionHeader
            connection={connection}
            toLink={generateLink(
              history.location,
              {
                postUri: flip ? get(atom, "uri") : get(targetAtom, "uri"),
                connectionUri: get(connection, "uri"),
                tab: undefined,
              },
              "/post"
            )}
            flip={flip}
            hideTimestamp={true}
            hideMessageIndicator={true}
          />
        </WonAtomContextSwipeableView>
      </div>
    </VisibilitySensor>
  );
}
WonParticipantItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  flip: PropTypes.bool,
};
