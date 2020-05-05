/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
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

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isPersona = atomUtils.isPersona(atom);
  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const reviewCount = getIn(atom, ["rating", "reviewCount"]) || 0;

  const groupMembers = get(atom, "groupMembers") || 0;
  const groupChatConnections =
    isOwned &&
    atomUtils.hasGroupSocket(atom) &&
    connectionSelectors.getGroupChatConnectionsByAtomUri(
      state,
      ownProps.atomUri
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

  const heldAtoms = get(atom, "holds");

  const hasUnreadSuggestedConnectionsInHeldAtoms = generalSelectors.hasUnreadSuggestedConnectionsInHeldAtoms(
    state,
    ownProps.atomUri
  );
  const heldByUri = atomUtils.getHeldByUri(atom);
  const isHeld = atomUtils.isHeld(atom);
  const holder = getIn(state, ["atoms", heldByUri]);
  const isHeldByServiceAtom = atomUtils.isServiceAtom(holder);
  const holderHasReviewSocket = atomUtils.hasReviewSocket(holder);
  const holderAggregateRating =
    holderHasReviewSocket && getIn(holder, ["rating", "aggregateRating"]);

  const suggestions =
    isOwned &&
    connectionSelectors.getSuggestedConnectionsByAtomUri(
      state,
      ownProps.atomUri
    );

  const buddyConnections =
    isOwned &&
    connectionSelectors.getBuddyConnectionsByAtomUri(
      state,
      ownProps.atomUri,
      true,
      false
    );

  const buddies = isOwned
    ? buddyConnections.filter(conn => connectionUtils.isConnected(conn))
    : get(atom, "buddies");

  const viewState = get(state, "view");
  const process = get(state, "process");

  const suggestionsSize = suggestions ? suggestions.size : 0;
  const groupMembersSize = groupMembers ? groupMembers.size : 0;
  const heldAtomsSize = heldAtoms ? heldAtoms.size : 0;

  return {
    atomUri: ownProps.atomUri,
    atom,
    isPersona,
    isHoldable: atomUtils.hasHoldableSocket(atom),
    isOwned,
    isHeld,
    isHeldByServiceAtom,
    holderHasReviewSocket,
    holderAggregateRatingString:
      holderAggregateRating && holderAggregateRating.toFixed(1),
    hasHeldAtoms: heldAtomsSize > 0,
    hasUnreadSuggestedConnectionsInHeldAtoms,
    heldAtomsSize,
    hasUnreadBuddyConnections:
      !!buddyConnections &&
      !!buddyConnections.find(conn => connectionUtils.isUnread(conn)),
    hasBuddies: buddyConnections
      ? buddyConnections.size > 0
      : buddies
        ? buddies.size > 0
        : false,
    buddyCount: buddies ? buddies.size : 0,
    hasReviews: reviewCount > 0,
    reviewCount,
    hasChatSocket: atomUtils.hasChatSocket(atom),
    groupMembers: groupMembersSize > 0,
    groupMembersSize,
    connectedGroupChatConnectionsSize: connectedGroupChatConnections
      ? connectedGroupChatConnections.size
      : 0,
    hasUnreadGroupChatRequests: nonClosedNonConnectedGroupChatConnections
      ? nonClosedNonConnectedGroupChatConnections.filter(conn =>
          get(conn, "unread")
        ).size > 0
      : false,
    hasSuggestions: suggestionsSize > 0,
    hasUnreadSuggestions:
      suggestionsSize > 0
        ? !!suggestions.find(conn => get(conn, "unread"))
        : false,
    suggestionsSize,
    atomLoading: !atom || processUtils.isAtomLoading(process, ownProps.atomUri),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, ownProps.atomUri),
    shouldShowRdf: viewUtils.showRdf(viewState),
    socketTypeArray: atomUtils.getSocketTypeArray(atom),
    visibleTab: viewUtils.getVisibleTabByAtomUri(
      viewState,
      ownProps.atomUri,
      ownProps.defaultTab
    ),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    selectTab: (atomUri, tab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
        )
      );
    },
  };
};

class WonAtomMenu extends React.Component {
  render() {
    const buttons = [];

    buttons.push(
      <div
        key="detail"
        className={this.generateAtomItemCssClasses(
          this.isSelectedTab("DETAIL")
        )}
        onClick={() => this.selectTab("DETAIL")}
      >
        <span className="atom-menu__item__label">Detail</span>
      </div>
    );

    // Add generic Tabs based on available Sockets
    this.props.socketTypeArray.map((socketType, index) => {
      let label = wonLabelUtils.labels.socketTabs[socketType] || socketType;
      let countLabel;

      const selected = this.isSelectedTab(socketType);
      let inactive = false;
      let unread = false;

      switch (socketType) {
        case vocab.GROUP.GroupSocketCompacted:
          if (this.props.isOwned) {
            unread = this.props.hasUnreadGroupChatRequests;
            countLabel =
              "(" + this.props.connectedGroupChatConnectionsSize + ")";
          } else {
            inactive = !this.props.groupMembers;
            countLabel = "(" + this.props.groupMembersSize + ")";
          }
          break;

        case vocab.HOLD.HoldableSocketCompacted:
          if (this.props.isHeld) {
            countLabel =
              this.props.holderAggregateRatingString &&
              "(★ " + this.props.holderAggregateRatingString + ")";
          } else if (this.props.isHoldable && this.props.isOwned) {
            countLabel =
              this.props.holderAggregateRatingString &&
              "(★ " + this.props.holderAggregateRatingString + ")";
            label = "+ " + label;
          }
          break;

        case vocab.HOLD.HolderSocketCompacted:
          inactive = !this.props.hasHeldAtoms;
          unread = this.props.hasUnreadSuggestedConnectionsInHeldAtoms;
          countLabel = "(" + this.props.heldAtomsSize + ")";
          break;

        case vocab.BUDDY.BuddySocketCompacted:
          inactive = !this.props.hasBuddies;
          unread = this.props.hasUnreadBuddyConnections;
          countLabel = "(" + this.props.buddyCount + ")";
          break;

        case vocab.REVIEW.ReviewSocketCompacted:
          inactive = !this.props.hasReviews;
          countLabel =
            this.props.hasReviews && "(" + this.props.reviewCount + ")";
          break;

        default: {
          const activeConnections = atomUtils.getActiveConnectionsOfAtom(
            this.props.atom,
            socketType
          );

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

      buttons.push(
        <div
          key={socketType + "-" + index}
          className={this.generateAtomItemCssClasses(
            selected,
            inactive,
            unread
          )}
          onClick={() => this.selectTab(socketType)}
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
    });

    this.props.shouldShowRdf &&
      buttons.push(
        <div
          key="rdf"
          className={this.generateAtomItemCssClasses(this.isSelectedTab("RDF"))}
          onClick={() => this.selectTab("RDF")}
        >
          <span className="atom-menu__item__label">RDF</span>
        </div>
      );

    return (
      <won-atom-menu class={this.generateParentCssClasses()}>
        {buttons}
      </won-atom-menu>
    );
  }

  generateParentCssClasses() {
    const cssClassNames = [];
    this.props.atomLoading && cssClassNames.push("won-is-loading");
    this.props.atomFailedToLoad && cssClassNames.push("won-failed-to-load");

    return cssClassNames.join(" ");
  }

  generateAtomItemCssClasses(
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

  isSelectedTab(tabName) {
    return tabName === this.props.visibleTab;
  }

  selectTab(tabName) {
    this.props.selectTab(this.props.atomUri, tabName);
  }
}
WonAtomMenu.propTypes = {
  atomUri: PropTypes.string.isRequired,
  selectTab: PropTypes.func,
  atom: PropTypes.object,
  isPersona: PropTypes.bool,
  isHoldable: PropTypes.bool,
  isOwned: PropTypes.bool,
  isHeld: PropTypes.bool,
  isHeldByServiceAtom: PropTypes.bool,
  holderHasReviewSocket: PropTypes.bool,
  holderAggregateRatingString: PropTypes.string,
  hasHeldAtoms: PropTypes.bool,
  hasUnreadSuggestedConnectionsInHeldAtoms: PropTypes.bool,
  heldAtomsSize: PropTypes.number,
  hasUnreadBuddyConnections: PropTypes.bool,
  hasBuddies: PropTypes.bool,
  buddyCount: PropTypes.number,
  hasReviews: PropTypes.bool,
  reviewCount: PropTypes.number,
  hasChatSocket: PropTypes.bool,
  groupMembers: PropTypes.bool,
  groupMembersSize: PropTypes.number,
  connectedGroupChatConnectionsSize: PropTypes.number,
  hasUnreadGroupChatRequests: PropTypes.bool,
  hasSuggestions: PropTypes.bool,
  hasUnreadSuggestions: PropTypes.bool,
  suggestionsSize: PropTypes.number,
  atomLoading: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  shouldShowRdf: PropTypes.bool,
  visibleTab: PropTypes.string,
  socketTypeArray: PropTypes.arrayOf(PropTypes.string),
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomMenu);
