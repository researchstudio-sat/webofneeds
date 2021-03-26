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

  let statusItemLabel;
  let feedItemLabel;

  const senderSocketType = atomUtils.getSocketType(
    senderAtom,
    connectionUtils.getSocketUri(connection)
  );
  const targetSocketType = atomUtils.getSocketType(
    targetAtom,
    connectionUtils.getTargetSocketUri(connection)
  );
  const connectionState = connectionUtils.getState(connection);

  const targetAtomTypeLabel =
    targetAtom && atomUtils.generateTypeLabel(targetAtom);

  switch (connectionState) {
    case vocab.WON.RequestReceived:
      statusItemLabel = "Received Request from " + targetAtomTypeLabel;
      feedItemLabel = senderSocketType + " ---> " + targetSocketType;
      break;

    case vocab.WON.RequestSent:
      statusItemLabel = "Sent Request to " + targetAtomTypeLabel;
      feedItemLabel = senderSocketType + " ---> " + targetSocketType;
      break;

    case vocab.WON.Connected:
      statusItemLabel = "Added " + targetAtomTypeLabel;
      switch (senderSocketType) {
        case vocab.HOLD.HoldableSocketCompacted:
          feedItemLabel = "to Held Atoms";
          break;
        case vocab.PROJECT.ProjectSocketCompacted:
          feedItemLabel = "to Projects";
          break;
        case vocab.PROJECT.RelatedProjectSocketCompacted:
          feedItemLabel = "to Related Projects";
          break;
        case vocab.WXSCHEMA.MemberSocketCompacted:
          feedItemLabel = "as a Member";
          break;
        case vocab.WXSCHEMA.MemberOfSocketCompacted:
          statusItemLabel = "Joined " + targetAtomTypeLabel;
          feedItemLabel = "";
          break;
        case vocab.HOLD.HolderSocketCompacted:
          feedItemLabel = "as Holder";
          break;
        case vocab.BUDDY.BuddySocketCompacted:
          feedItemLabel = "as a Buddy";
          break;
        default:
          feedItemLabel = senderSocketType + " ---> " + targetSocketType;
          break;
      }
      break;

    case vocab.WON.Closed:
      statusItemLabel = "Removed " + targetAtomTypeLabel;
      switch (senderSocketType) {
        case vocab.HOLD.HoldableSocketCompacted:
          feedItemLabel = "from Held Atoms";
          break;
        case vocab.WXSCHEMA.MemberSocketCompacted:
          feedItemLabel = "from Members";
          break;
        case vocab.WXSCHEMA.MemberOfSocketCompacted:
          statusItemLabel = "Left " + targetAtomTypeLabel;
          feedItemLabel = "";
          break;
        case vocab.HOLD.HolderSocketCompacted:
          statusItemLabel = "Removed Holder " + targetAtomTypeLabel;
          feedItemLabel = "";
          break;
        case vocab.BUDDY.BuddySocketCompacted:
          feedItemLabel = "from Buddies";
          break;
        default:
          feedItemLabel = senderSocketType + " ---> " + targetSocketType;
          break;
      }
      break;

    case vocab.WON.Suggested:
      statusItemLabel = "Suggestion to add " + targetAtomTypeLabel;
      feedItemLabel = senderSocketType + " ---> " + targetSocketType;
      break;

    default:
      statusItemLabel = targetAtomTypeLabel;
      feedItemLabel = senderSocketType + " ---> " + targetSocketType;
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
              <div className="fi__info">{statusItemLabel}</div>
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
              <div className="fi__info">{feedItemLabel}</div>
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
              <div className="fi__info">{statusItemLabel}</div>
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
              <div className="fi__info">{feedItemLabel}</div>
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
