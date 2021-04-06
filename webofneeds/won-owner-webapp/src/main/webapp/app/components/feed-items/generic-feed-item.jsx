import React from "react";
import PropTypes from "prop-types";
import { generateLink, getUri } from "../../utils";

import VisibilitySensor from "react-visibility-sensor";
import * as wonLabelUtils from "~/app/won-label-utils";
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
  //isOwned,
  targetAtom,
  hideSenderAtom,
}) {
  const dispatch = useDispatch();
  const history = useHistory();

  //Remove actionButtons in feed for now
  const addActionButtons = false; //isOwned;
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

  const feedLabels = wonLabelUtils.generateFeedItemLabels(
    atomUtils.getSocketType(
      senderAtom,
      connectionUtils.getSocketUri(connection)
    ),
    atomUtils.getSocketType(
      targetAtom,
      connectionUtils.getTargetSocketUri(connection)
    ),
    targetAtom,
    connectionUtils.getState(connection)
  );

  const preItemLabel = feedLabels.prefix;
  const postItemLabel = feedLabels.postfix;

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
              <div className="fi__info">{preItemLabel}</div>
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
              <div className="fi__info">{postItemLabel}</div>
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
              <div className="fi__info">{preItemLabel}</div>
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
              <div className="fi__info">{postItemLabel}</div>
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
  //isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  hideSenderAtom: PropTypes.bool,
};
