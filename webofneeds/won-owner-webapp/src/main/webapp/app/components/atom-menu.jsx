/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-menu.scss";

export default function WonAtomMenu({
  atom,
  visibleTab,
  setVisibleTab,
  toggleAddPicker,
  relevantConnectionsMap,
}) {
  const atomUri = get(atom, "uri");
  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  const reviewCount = getIn(atom, ["rating", "reviewCount"]) || 0;

  const heldByUri = atomUtils.getHeldByUri(atom);
  const isHeld = atomUtils.isHeld(atom);
  const holder = useSelector(generalSelectors.getAtom(heldByUri));
  const holderHasReviewSocket = atomUtils.hasReviewSocket(holder);
  const holderAggregateRating =
    holderHasReviewSocket && getIn(holder, ["rating", "aggregateRating"]);

  const viewState = useSelector(generalSelectors.getViewState);
  const process = useSelector(generalSelectors.getProcessState);

  const isHoldable = atomUtils.hasHoldableSocket(atom);
  const holderAggregateRatingString =
    holderAggregateRating && holderAggregateRating.toFixed(1);

  const hasReviews = reviewCount > 0;
  const atomLoading = !atom || processUtils.isAtomLoading(process, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, atomUri);
  const shouldShowRdf = viewUtils.showRdf(viewState);

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
      onClick={() => {
        setVisibleTab("DETAIL");
        toggleAddPicker(false);
      }}
    >
      <span className="atom-menu__item__label">Detail</span>
    </div>
  );

  // Add generic Tabs based on available Sockets
  relevantConnectionsMap.map((socketTypeConnections, socketType) => {
    let label = wonLabelUtils.getSocketTabLabel(socketType);
    let countLabel;

    const selected = visibleTab === socketType;
    let inactive = false; //TODO: Implement inactive based on connectionsCount and possibleReactions to socket
    let unread = false;

    switch (socketType) {
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

      case vocab.REVIEW.ReviewSocketCompacted:
        countLabel = hasReviews && "(" + reviewCount + ")";
        break;

      case vocab.CHAT.ChatSocketCompacted: {
        const socketUri = atomUtils.getSocketUri(atom, socketType);
        const activeConnections = socketTypeConnections
          .filter(
            conn =>
              // We filter out every chat connection that is not owned, otherwise the count would show non owned chatconnections of non owned atoms
              isOwned || connectionUtils.hasTargetSocketUri(conn, socketUri)
          )
          .filter(conn => !connectionUtils.isClosed(conn));
        countLabel =
          activeConnections && activeConnections.size > 0
            ? "(" + activeConnections.size + ")"
            : undefined;
        unread =
          activeConnections &&
          !!activeConnections.find(conn => connectionUtils.isUnread(conn));
        break;
      }

      case vocab.HOLD.HolderSocketCompacted: {
        //Holdertab should always just display the amount of connected items
        const activeConnections = socketTypeConnections.filter(conn =>
          connectionUtils.isConnected(conn)
        );

        countLabel =
          activeConnections && activeConnections.size > 0
            ? "(" + activeConnections.size + ")"
            : undefined;
        unread =
          activeConnections &&
          !!activeConnections.find(conn => connectionUtils.isUnread(conn));
        break;
      }

      default: {
        const activeConnections = socketTypeConnections.filter(
          conn => !connectionUtils.isClosed(conn)
        );

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
          key={socketType}
          className={generateAtomItemCssClasses(selected, inactive, unread)}
          onClick={() => {
            setVisibleTab(socketType);
            toggleAddPicker(false);
          }}
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
        onClick={() => {
          setVisibleTab("RDF");
          toggleAddPicker(false);
        }}
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
  relevantConnectionsMap: PropTypes.object.isRequired,
  visibleTab: PropTypes.string.isRequired,
  setVisibleTab: PropTypes.func.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};
