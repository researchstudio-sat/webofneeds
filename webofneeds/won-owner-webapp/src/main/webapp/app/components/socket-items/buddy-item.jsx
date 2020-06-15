/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import * as connectionUtils from "../../redux/utils/connection-utils";
import vocab from "../../service/vocab.js";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";

import "~/style/_socket-item.scss";
import WonAtomHeader from "../atom-header";
import WonBuddySocketActions from "../socket-actions/buddy-actions";

export default function WonBuddyItem({
  connection,
  atom,
  isOwned,
  targetAtom,
  flip,
}) {
  const dispatch = useDispatch();
  const history = useHistory();

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
              <WonBuddySocketActions connection={connection} />
            ) : (
              undefined
            )
          }
        >
          <WonAtomHeader
            atom={flip ? atom : targetAtom}
            toLink={generateLink(
              history.location,
              {
                postUri: flip ? get(atom, "uri") : get(targetAtom, "uri"),
                connectionUri: get(connection, "uri"),
              },
              "/post"
            )}
          />
        </WonAtomContextSwipeableView>
      </div>
    </VisibilitySensor>
  );
}
WonBuddyItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  flip: PropTypes.bool,
};
