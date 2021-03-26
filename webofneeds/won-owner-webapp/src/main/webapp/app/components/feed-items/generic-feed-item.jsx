import React from "react";
import PropTypes from "prop-types";
import { generateLink, getUri } from "../../utils";

import vocab from "../../service/vocab";
import VisibilitySensor from "react-visibility-sensor";
import * as connectionUtils from "../../redux/utils/connection-utils";
import * as atomUtils from "~/app/redux/utils/atom-utils";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";
import WonAtomHeaderFeed from "../atom-header-feed";
import WonGenericSocketActions from "../socket-actions/generic-actions";

import { actionCreators } from "../../actions/actions";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";

import "~/style/_feed-item.scss";

export default function WonGenericFeedItem({
  connection,
  senderAtom,
  isOwned,
  targetAtom,
  hideSenderAtom,
}) {
  const dispatch = useDispatch();
  const history = useHistory();

  const addActionButtons = isOwned;
  let headerClassName;

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

  const targetAtomTypeLabel =
    targetAtom && atomUtils.generateTypeLabel(targetAtom);

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
          "fi " + (connectionUtils.isUnread(connection) ? " won-unread " : "")
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
          {hideSenderAtom ? (
            <React.Fragment>
              <div className="fi__info">{targetAtomTypeLabel}</div>
              <WonAtomHeaderFeed
                atom={targetAtom}
                toLink={generateLink(
                  history.location,
                  {
                    postUri: getUri(targetAtom),
                    connectionUri: getUri(connection),
                    tab: undefined,
                  },
                  "/post"
                )}
              />
              <div className="fi__info">
                {atomUtils.getSocketType(
                  senderAtom,
                  connectionUtils.getSocketUri(connection)
                ) +
                  " --> " +
                  atomUtils.getSocketType(
                    targetAtom,
                    connectionUtils.getTargetSocketUri(connection)
                  ) +
                  ":" +
                  connectionUtils.getState(connection)}
              </div>
            </React.Fragment>
          ) : (
            <React.Fragment>
              <WonAtomHeaderFeed
                atom={senderAtom}
                toLink={generateLink(
                  history.location,
                  {
                    postUri: getUri(senderAtom),
                    connectionUri: getUri(connection),
                    tab: undefined,
                  },
                  "/post"
                )}
              />
              <div className="fi__info">{targetAtomTypeLabel}</div>
              <WonAtomHeaderFeed
                atom={targetAtom}
                toLink={generateLink(
                  history.location,
                  {
                    postUri: getUri(targetAtom),
                    connectionUri: getUri(connection),
                    tab: undefined,
                  },
                  "/post"
                )}
              />
              <div className="fi__info">
                {atomUtils.getSocketType(
                  senderAtom,
                  connectionUtils.getSocketUri(connection)
                ) +
                  " --> " +
                  atomUtils.getSocketType(
                    targetAtom,
                    connectionUtils.getTargetSocketUri(connection)
                  ) +
                  ":" +
                  connectionUtils.getState(connection)}
              </div>
            </React.Fragment>
          )}
        </WonAtomContextSwipeableView>
      </div>
    </VisibilitySensor>
  );
}
WonGenericFeedItem.propTypes = {
  connection: PropTypes.object.isRequired,
  senderAtom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  hideSenderAtom: PropTypes.bool,
};
