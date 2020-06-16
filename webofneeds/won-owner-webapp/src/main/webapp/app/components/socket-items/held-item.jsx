import React from "react";
import PropTypes from "prop-types";
import { get } from "../../utils";
import VisibilitySensor from "react-visibility-sensor";
import * as connectionUtils from "../../redux/utils/connection-utils";
import { actionCreators } from "../../actions/actions";
import { useDispatch } from "react-redux";

import "~/style/_socket-item.scss";
import WonAtomCard from "../atom-card";

export default function WonHeldItem({
  connection,
  atom,
  isOwned,
  targetAtom,
  flip,
  currentLocation,
}) {
  const dispatch = useDispatch();

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
        <WonAtomCard
          atom={flip ? atom : targetAtom}
          showIndicators={isOwned}
          showHolder={false}
          currentLocation={currentLocation}
        />
      </div>
    </VisibilitySensor>
  );
}
WonHeldItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  currentLocation: PropTypes.object,
  flip: PropTypes.bool,
};
