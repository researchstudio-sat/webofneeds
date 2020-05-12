/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useDispatch, useSelector } from "react-redux";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import vocab from "../service/vocab.js";
import Immutable from "immutable";

import "~/style/_atom-menu.scss";
import { getSocketTypeArray } from "../redux/utils/atom-utils";

export default function WonAtomMenu({ atom, defaultTab }) {
  const atomUri = get(atom, "uri");
  const dispatch = useDispatch();
  const isOwned = useSelector(state =>
    generalSelectors.isAtomOwned(state, atomUri)
  );

  const reviewCount = getIn(atom, ["rating", "reviewCount"]) || 0;

  const groupMembers = get(atom, "groupMembers") || 0;
  const groupChatConnections = useSelector(
    state =>
      isOwned &&
      atomUtils.hasGroupSocket(atom) &&
      connectionSelectors.getGroupChatConnectionsByAtomUri(state, atomUri)
  );
  const connectedGroupChatConnections =
    groupChatConnections &&
    groupChatConnections.filter(conn => connectionUtils.isConnected(conn));
  const nonClosedNonConnectedGroupChatConnections =
    groupChatConnections &&
    groupChatConnections.filter(
      conn =>
        !(connectionUtils.isConnected(conn) || connectionUtils.isClosed(conn))
    );

  const heldByUri = atomUtils.getHeldByUri(atom);
  const isHeld = atomUtils.isHeld(atom);
  const holder = useSelector(state => getIn(state, ["atoms", heldByUri]));
  const holderHasReviewSocket = atomUtils.hasReviewSocket(holder);
  const holderAggregateRating =
    holderHasReviewSocket && getIn(holder, ["rating", "aggregateRating"]);

  const buddyConnections = useSelector(
    state =>
      isOwned &&
      generalSelectors.getBuddyConnectionsByAtomUri(state, atomUri, true, false)
  );

  const buddies = isOwned
    ? buddyConnections.filter(conn => connectionUtils.isConnected(conn))
    : get(atom, "buddies");

  const viewState = useSelector(state => get(state, "view"));
  const process = useSelector(state => get(state, "process"));

  const groupMembersSize = groupMembers ? groupMembers.size : 0;
  const isHoldable = atomUtils.hasHoldableSocket(atom);
  const holderAggregateRatingString =
    holderAggregateRating && holderAggregateRating.toFixed(1);
  const hasUnreadBuddyConnections =
    !!buddyConnections &&
    !!buddyConnections.find(conn => connectionUtils.isUnread(conn));
  const hasBuddies = buddyConnections
    ? buddyConnections.size > 0
    : buddies
      ? buddies.size > 0
      : false;
  const buddyCount = buddies ? buddies.size : 0;
  const hasReviews = reviewCount > 0;
  const hasGroupMembers = groupMembersSize > 0;
  const connectedGroupChatConnectionsSize = connectedGroupChatConnections
    ? connectedGroupChatConnections.size
    : 0;
  const hasUnreadGroupChatRequests = nonClosedNonConnectedGroupChatConnections
    ? nonClosedNonConnectedGroupChatConnections.filter(conn =>
        get(conn, "unread")
      ).size > 0
    : false;
  const atomLoading = !atom || processUtils.isAtomLoading(process, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, atomUri);
  const shouldShowRdf = viewUtils.showRdf(viewState);
  const socketTypeArray = isOwned
    ? getSocketTypeArray(atom)
    : getSocketTypeArray(atom).filter(
        socketType => socketType !== vocab.CHAT.ChatSocketCompacted
      ); //filter the chat Socket so we do not display it as a menu item for non owned atoms

  const visibleTab = viewUtils.getVisibleTabByAtomUri(
    viewState,
    atomUri,
    defaultTab
  );

  function generateParentCssClasses() {
    const cssClassNames = [];
    atomLoading && cssClassNames.push("won-is-loading");
    atomFailedToLoad && cssClassNames.push("won-failed-to-load");

    return cssClassNames.join(" ");
  }

  function generateAtomItemCssClasses(
    selected = false,
    inactive = false,
    unread = false
  ) {
    const cssClassNames = ["atom-menu__item"];

    selected && cssClassNames.push("atom-menu__item--selected");
    inactive && cssClassNames.push("atom-menu__item--inactive");
    unread && cssClassNames.push("atom-menu__item--unread");

    return cssClassNames.join(" ");
  }

  const buttons = [];

  buttons.push(
    <div
      key="detail"
      className={generateAtomItemCssClasses(visibleTab === "DETAIL")}
      onClick={() =>
        dispatch(
          actionCreators.atoms__selectTab(
            Immutable.fromJS({ atomUri: atomUri, selectTab: "DETAIL" })
          )
        )
      }
    >
      <span className="atom-menu__item__label">Detail</span>
    </div>
  );

  // Add generic Tabs based on available Sockets
  socketTypeArray.map((socketType, index) => {
    let label = wonLabelUtils.labels.socketTabs[socketType] || socketType;
    let countLabel;

    const selected = visibleTab === socketType;
    let inactive = false;
    let unread = false;

    switch (socketType) {
      case vocab.GROUP.GroupSocketCompacted:
        if (isOwned) {
          unread = hasUnreadGroupChatRequests;
          countLabel = "(" + connectedGroupChatConnectionsSize + ")";
        } else {
          inactive = !hasGroupMembers;
          countLabel = "(" + groupMembersSize + ")";
        }
        break;

      case vocab.HOLD.HoldableSocketCompacted:
        if (isHeld) {
          countLabel =
            holderAggregateRatingString &&
            "(★ " + holderAggregateRatingString + ")";
        } else if (isHoldable && isOwned) {
          countLabel =
            holderAggregateRatingString &&
            "(★ " + holderAggregateRatingString + ")";
          label = "+ " + label;
        } else {
          // if there is currently no holder in a non-owned atom we set the label to undefined to remove the tab from being displayed
          label = undefined;
        }
        break;

      case vocab.BUDDY.BuddySocketCompacted:
        inactive = !hasBuddies;
        unread = hasUnreadBuddyConnections;
        countLabel = "(" + buddyCount + ")";
        break;

      case vocab.REVIEW.ReviewSocketCompacted:
        inactive = !hasReviews;
        countLabel = hasReviews && "(" + reviewCount + ")";
        break;

      default: {
        const activeConnections = isOwned
          ? atomUtils.getNonClosedConnectionsOfAtom(atom, socketType)
          : atomUtils.getConnectedConnectionsOfAtom(atom, socketType);

        inactive = !activeConnections || activeConnections.size === 0;
        countLabel =
          activeConnections && activeConnections.size > 0
            ? "(" + activeConnections.size + ")"
            : undefined;
        unread =
          activeConnections &&
          !!activeConnections.find(conn => connectionUtils.isUnread(conn));
        break;
      }
    }

    if (label) {
      buttons.push(
        <div
          key={socketType + "-" + index}
          className={generateAtomItemCssClasses(selected, inactive, unread)}
          onClick={() =>
            dispatch(
              actionCreators.atoms__selectTab(
                Immutable.fromJS({ atomUri: atomUri, selectTab: socketType })
              )
            )
          }
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">{label}</span>
          {countLabel ? (
            <span className="atom-menu__item__count">{countLabel}</span>
          ) : (
            undefined
          )}
        </div>
      );
    }
  });

  shouldShowRdf &&
    buttons.push(
      <div
        key="rdf"
        className={generateAtomItemCssClasses(visibleTab === "RDF")}
        onClick={() =>
          dispatch(
            actionCreators.atoms__selectTab(
              Immutable.fromJS({ atomUri: atomUri, selectTab: "RDF" })
            )
          )
        }
      >
        <span className="atom-menu__item__label">RDF</span>
      </div>
    );

  return (
    <won-atom-menu class={generateParentCssClasses()}>{buttons}</won-atom-menu>
  );
}

WonAtomMenu.propTypes = {
  atom: PropTypes.object.isRequired,
  defaultTab: PropTypes.string,
};
