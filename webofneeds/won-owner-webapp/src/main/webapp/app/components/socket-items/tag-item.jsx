import React from "react";
import PropTypes from "prop-types";
import { generateLink, getUri } from "../../utils";
import vocab from "../../service/vocab";
import VisibilitySensor from "react-visibility-sensor";
import * as connectionUtils from "../../redux/utils/connection-utils";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view.jsx";
import WonAtomTagHeader from "../atom-tag-header.jsx";
import WonTagSocketActions from "../socket-actions/tag-actions.jsx";
import { actionCreators } from "../../actions/actions";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";

import "~/style/_tag-item.scss";

export default function WonTagItem({
  connection,
  atom,
  isOwned,
  targetAtom,
  flip,
}) {
  const dispatch = useDispatch();
  const history = useHistory();

  const addActionButtons = isOwned || flip;
  let headerClassName = "";

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

  switch (connectionUtils.getState(connection)) {
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
          "ti " +
          (connectionUtils.isUnread(connection)
            ? " won-unread "
            : "" + headerClassName)
        }
      >
        <WonAtomContextSwipeableView
          className={headerClassName}
          actionButtons={
            addActionButtons ? (
              <WonTagSocketActions connection={connection} />
            ) : (
              undefined
            )
          }
        >
          <WonAtomTagHeader
            atom={flip ? atom : targetAtom}
            toLink={generateLink(
              history.location,
              {
                postUri: flip ? getUri(atom) : getUri(targetAtom),
                connectionUri: getUri(connection),
                tab: undefined,
              },
              "/post"
            )}
          />
        </WonAtomContextSwipeableView>
      </div>
    </VisibilitySensor>
  );
}
WonTagItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  flip: PropTypes.bool,
};
