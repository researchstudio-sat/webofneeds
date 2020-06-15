import React from "react";
import PropTypes from "prop-types";
import { generateLink, get } from "../../utils";
import vocab from "../../service/vocab";
import VisibilitySensor from "react-visibility-sensor";
import * as connectionUtils from "../../redux/utils/connection-utils";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";
import { actionCreators } from "../../actions/actions";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";

import "~/style/_socket-item.scss";
import WonAtomHeader from "../atom-header";
import WonGenericSocketActions from "../socket-actions/generic-actions";

export default function WonGenericItem({
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
    case vocab.WON.RequestReceived:
      headerClassName = "status--received";
      break;

    case vocab.WON.RequestSent:
      headerClassName = "status--sent";
      break;

    case vocab.WON.Closed:
      headerClassName = "status--closed";
      break;

    case vocab.WON.Suggested:
      headerClassName = "status--suggested";
      break;

    default:
      break;
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
              <WonGenericSocketActions connection={connection} />
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
                postUri: flip
                  ? get(atom, "uri")
                  : get(connection, "targetAtomUri"),
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
WonGenericItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  flip: PropTypes.bool,
};
