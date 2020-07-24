import React from "react";
import PropTypes from "prop-types";
import { getUri } from "../../utils";
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
  //TODO: ADD ACTIONS
  function markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      setTimeout(() => {
        dispatch(
          actionCreators.connections__markAsRead({
            connectionUri: getUri(conn),
          })
        );
      }, 1500);
    }
  }

  const visibleAtom = flip ? atom : targetAtom;
  const visibleAtomUri = getUri(visibleAtom);

  return (
    <VisibilitySensor
      key={getUri(connection)}
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
          atomUri={visibleAtomUri}
          atom={visibleAtom}
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
