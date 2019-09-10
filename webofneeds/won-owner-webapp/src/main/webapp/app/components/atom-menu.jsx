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
import Immutable from "immutable";

import "~/style/_atom-menu.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isPersona = atomUtils.isPersona(atom);
  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const hasHolderSocket = atomUtils.hasHolderSocket(atom);
  const hasGroupSocket = atomUtils.hasGroupSocket(atom);
  const hasReviewSocket = atomUtils.hasReviewSocket(atom);
  const hasBuddySocket = atomUtils.hasBuddySocket(atom);
  const reviewCount = hasReviewSocket
    ? getIn(atom, ["rating", "reviewCount"])
    : 0;

  const groupMembers = hasGroupSocket ? get(atom, "groupMembers") : 0;
  const groupChatConnections =
    isOwned &&
    hasGroupSocket &&
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

  const heldAtoms = hasHolderSocket && get(atom, "holds");

  const hasUnreadSuggestedConnectionsInHeldAtoms = generalSelectors.hasUnreadSuggestedConnectionsInHeldAtoms(
    state,
    ownProps.atomUri
  );
  const heldByUri = atomUtils.getHeldByUri(atom);
  const isHeld = atomUtils.isHeld(atom);
  const persona = getIn(state, ["atoms", heldByUri]);
  const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
  const personaAggregateRating =
    personaHasReviewSocket && getIn(persona, ["rating", "aggregateRating"]);

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
    personaHasReviewSocket,
    personaAggregateRatingString:
      personaAggregateRating && personaAggregateRating.toFixed(1),
    hasHeldAtoms: heldAtomsSize > 0,
    hasUnreadSuggestedConnectionsInHeldAtoms,
    heldAtomsSize,
    hasHolderSocket,
    hasGroupSocket,
    hasReviewSocket,
    hasBuddySocket,
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
    visibleTab: viewUtils.getVisibleTabByAtomUri(viewState, ownProps.atomUri),
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

    if (this.props.isHeld) {
      buttons.push(
        <div
          key="heldby"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HELDBY")
          )}
          onClick={() => this.selectTab("HELDBY")}
        >
          <span className="atom-menu__item__label">Persona</span>
          {this.props.personaAggregateRatingString && (
            <span className="atom-menu__item__rating">
              (★ {this.props.personaAggregateRatingString + ")"}
            </span>
          )}
        </div>
      );
    } else if (this.props.isHoldable && this.props.isOwned) {
      buttons.push(
        <div
          key="heldby"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HELDBY")
          )}
          onClick={() => this.selectTab("HELDBY")}
        >
          <span className="atom-menu__item__label">+ Persona</span>
          {this.props.personaAggregateRatingString && (
            <span className="atom-menu__item__rating">
              {"(★ " + this.props.personaAggregateRatingString + ")"}
            </span>
          )}
        </div>
      );
    }

    if (this.props.hasGroupSocket) {
      this.props.isOwned
        ? buttons.push(
            <div
              key="participants"
              className={this.generateAtomItemCssClasses(
                this.isSelectedTab("PARTICIPANTS"),
                false,
                this.props.hasUnreadGroupChatRequests
              )}
              onClick={() => this.selectTab("PARTICIPANTS")}
            >
              <span className="atom-menu__item__unread" />
              <span className="atom-menu__item__label">Group Members</span>
              {!!this.props.connectedGroupChatConnectionsSize && (
                <span className="atom-menu__item__count">
                  {"(" + this.props.connectedGroupChatConnectionsSize + ")"}
                </span>
              )}
            </div>
          )
        : buttons.push(
            <div
              key="participants"
              className={this.generateAtomItemCssClasses(
                this.isSelectedTab("PARTICIPANTS"),
                !this.props.groupMembers
              )}
              onClick={() => this.selectTab("PARTICIPANTS")}
            >
              <span className="atom-menu__item__label">Group Members</span>
              <span className="atom-menu__item__count">
                {"(" + this.props.groupMembersSize + ")"}
              </span>
            </div>
          );
    }

    this.props.isOwned &&
      this.props.hasChatSocket &&
      buttons.push(
        <div
          key="suggestions"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("SUGGESTIONS"),
            !this.props.hasSuggestions,
            this.props.hasUnreadSuggestions
          )}
          onClick={() => this.selectTab("SUGGESTIONS")}
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">Suggestions</span>
          <span className="atom-menu__item__count">
            {"(" + this.props.suggestionsSize + ")"}
          </span>
        </div>
      );

    this.props.hasHolderSocket &&
      buttons.push(
        <div
          key="holds"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HOLDS"),
            !this.props.hasHeldAtoms,
            this.props.hasUnreadSuggestedConnectionsInHeldAtoms
          )}
          onClick={() => this.selectTab("HOLDS")}
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">Posts</span>
          <span className="atom-menu__item__count">
            {"(" + this.props.heldAtomsSize + ")"}
          </span>
        </div>
      );

    this.props.hasBuddySocket &&
      buttons.push(
        <div
          key="buddies"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("BUDDIES"),
            !this.props.hasBuddies,
            this.props.hasUnreadBuddyConnections
          )}
          onClick={() => this.selectTab("BUDDIES")}
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">Buddies</span>
          <span className="atom-menu__item__count">
            {"(" + this.props.buddyCount + ")"}
          </span>
        </div>
      );
    this.props.hasReviewSocket &&
      buttons.push(
        <div
          key="reviews"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("REVIEWS"),
            !this.props.hasReviews,
            false
          )}
          onClick={() => this.selectTab("REVIEWS")}
        >
          <span className="atom-menu__item__label">Reviews</span>
          {this.props.hasReviews && (
            <span className="atom-menu__item__rating">
              {"(" + this.props.reviewCount + ")"}
            </span>
          )}
        </div>
      );
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
  personaHasReviewSocket: PropTypes.bool,
  personaAggregateRatingString: PropTypes.string,
  hasHeldAtoms: PropTypes.bool,
  hasUnreadSuggestedConnectionsInHeldAtoms: PropTypes.bool,
  heldAtomsSize: PropTypes.number,
  hasHolderSocket: PropTypes.bool,
  hasGroupSocket: PropTypes.bool,
  hasReviewSocket: PropTypes.bool,
  hasBuddySocket: PropTypes.bool,
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
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomMenu);
