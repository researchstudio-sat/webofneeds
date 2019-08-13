/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";

import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import Immutable from "immutable";

import "~/style/_atom-menu.scss";

export default class WonAtomMenu extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const post = getIn(state, ["atoms", this.atomUri]);
    const isPersona = atomUtils.isPersona(post);
    const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);

    const hasHolderSocket = atomUtils.hasHolderSocket(post);
    const hasGroupSocket = atomUtils.hasGroupSocket(post);
    const hasReviewSocket = atomUtils.hasReviewSocket(post);
    const hasBuddySocket = atomUtils.hasBuddySocket(post);
    const reviewCount =
      hasReviewSocket && getIn(post, ["rating", "reviewCount"]);

    const groupMembers = hasGroupSocket && get(post, "groupMembers");
    const groupChatConnections =
      isOwned &&
      hasGroupSocket &&
      connectionSelectors.getGroupChatConnectionsByAtomUri(state, this.postUri);
    const connectedGroupChatConnections =
      groupChatConnections &&
      groupChatConnections.filter(conn => connectionUtils.isConnected(conn));
    const nonClosedNonConnectedGroupChatConnections =
      groupChatConnections &&
      groupChatConnections.filter(
        conn =>
          !(connectionUtils.isConnected(conn) || connectionUtils.isClosed(conn))
      );

    const heldPosts = hasHolderSocket && get(post, "holds");

    const hasUnreadSuggestedConnectionsInHeldAtoms = generalSelectors.hasUnreadSuggestedConnectionsInHeldAtoms(
      state,
      this.postUri
    );
    const heldByUri = atomUtils.getHeldByUri(post);
    const isHeld = atomUtils.isHeld(post);
    const persona = getIn(state, ["atoms", heldByUri]);
    const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
    const personaAggregateRating =
      personaHasReviewSocket && getIn(persona, ["rating", "aggregateRating"]);

    const suggestions =
      isOwned &&
      connectionSelectors.getSuggestedConnectionsByAtomUri(state, this.postUri);

    const buddyConnections =
      isOwned &&
      connectionSelectors.getBuddyConnectionsByAtomUri(
        state,
        this.postUri,
        true,
        false
      );

    const buddies = isOwned
      ? buddyConnections.filter(conn => connectionUtils.isConnected(conn))
      : get(post, "buddies");

    const viewState = get(state, "view");
    const process = get(state, "process");

    const suggestionsSize = suggestions ? suggestions.size : 0;
    const groupMembersSize = groupMembers ? groupMembers.size : 0;
    const heldPostsSize = heldPosts ? heldPosts.size : 0;

    return {
      post,
      isPersona,
      isHoldable: atomUtils.hasHoldableSocket(post),
      isOwned,
      isHeld,
      personaHasReviewSocket,
      personaAggregateRatingString:
        personaAggregateRating && personaAggregateRating.toFixed(1),
      hasHeldPosts: heldPostsSize > 0,
      hasUnreadSuggestedConnectionsInHeldAtoms,
      heldPostsSize,
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
      hasChatSocket: atomUtils.hasChatSocket(post),
      groupMembers: groupMembersSize > 0,
      groupMembersSize,
      connectedGroupChatConnectionsSize:
        connectedGroupChatConnections && connectedGroupChatConnections.size,
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
      postLoading: !post || processUtils.isAtomLoading(process, this.postUri),
      postFailedToLoad:
        post && processUtils.hasAtomFailedToLoad(process, this.postUri),
      shouldShowRdf: viewUtils.showRdf(viewState),
      visibleTab: viewUtils.getVisibleTabByAtomUri(viewState, this.postUri),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const buttons = [];

    buttons.push(
      <div
        className={this.generateAtomItemCssClasses(
          this.isSelectedTab("DETAIL")
        )}
        onClick={() => this.selectTab("DETAIL")}
      >
        <span className="post-menu__item__label">Detail</span>
      </div>
    );

    if (this.state.isHeld) {
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HELDBY")
          )}
          onClick={() => this.selectTab("HELDBY")}
        >
          <span className="post-menu__item__label">Persona</span>
          {this.state.personaAggregateRatingString && (
            <span className="post-menu__item__rating">
              (★ {this.state.personaAggregateRatingString})
            </span>
          )}
        </div>
      );
    } else if (this.state.isHoldable && this.state.isOwned) {
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HELDBY")
          )}
          onClick={() => this.selectTab("HELDBY")}
        >
          <span className="post-menu__item__label">+ Persona</span>
          {this.state.personaAggregateRatingString && (
            <span className="post-menu__item__rating">
              (★ {this.state.personaAggregateRatingString})
            </span>
          )}
        </div>
      );
    }

    if (this.state.hasGroupSocket) {
      this.state.isOwned
        ? buttons.push(
            <div
              className={this.generateAtomItemCssClasses(
                this.isSelectedTab("PARTICIPANTS"),
                false,
                this.state.hasUnreadGroupChatRequests
              )}
              onClick={() => this.selectTab("PARTICIPANTS")}
            >
              <span className="post-menu__item__unread" />
              <span className="post-menu__item__label">Group Members</span>
              {this.state.connectedGroupChatConnectionsSize && (
                <span className="post-menu__item__count">
                  ({this.state.connectedGroupChatConnectionsSize})
                </span>
              )}
            </div>
          )
        : buttons.push(
            <div
              className={this.generateAtomItemCssClasses(
                this.isSelectedTab("PARTICIPANTS"),
                !this.state.groupMembers
              )}
              onClick={() => this.selectTab("PARTICIPANTS")}
            >
              <span className="post-menu__item__label">Group Members</span>
              <span className="post-menu__item__count">
                ({this.state.groupMembersSize})
              </span>
            </div>
          );
    }

    this.state.isOwned &&
      this.state.hasChatSocket &&
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("SUGGESTIONS"),
            !this.state.hasSuggestions,
            this.state.hasUnreadSuggestions
          )}
          onClick={() => this.selectTab("SUGGESTIONS")}
        >
          <span className="post-menu__item__unread" />
          <span className="post-menu__item__label">Suggestions</span>
          <span className="post-menu__item__count">
            ({this.state.suggestionsSize})
          </span>
        </div>
      );

    this.state.hasHolderSocket &&
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HOLDS"),
            !this.state.hasHeldPosts,
            this.state.hasUnreadSuggestedConnectionsInHeldAtoms
          )}
          onClick={() => this.selectTab("HOLDS")}
        >
          <span className="post-menu__item__unread" />
          <span className="post-menu__item__label">Posts</span>
          <span className="post-menu__item__count">
            ({this.state.heldPostsSize})
          </span>
        </div>
      );

    this.state.hasBuddySocket &&
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("BUDDIES"),
            !this.state.hasBuddies,
            this.state.hasUnreadBuddyConnections
          )}
          onClick={() => this.selectTab("BUDDIES")}
        >
          <span className="post-menu__item__unread" />
          <span className="post-menu__item__label">Buddies</span>
          <span className="post-menu__item__count">
            ({this.state.buddyCount})
          </span>
        </div>
      );
    this.state.hasReviewSocket &&
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("REVIEWS"),
            !this.state.hasReviews,
            false
          )}
          onClick={() => this.selectTab("REVIEWS")}
        >
          <span className="post-menu__item__label">Reviews</span>
          {this.state.hasReviews && (
            <span className="post-menu__item__rating">
              ({this.state.reviewCount})
            </span>
          )}
        </div>
      );
    this.state.shouldShowRdf &&
      buttons.push(
        <div
          className={this.generateAtomItemCssClasses(this.isSelectedTab("RDF"))}
          onClick={() => this.selectTab("RDF")}
        >
          <span className="post-menu__item__label">RDF</span>
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
    this.state.postLoading && cssClassNames.push("won-is-loading");
    this.state.postFailedToLoad && cssClassNames.push("won-failed-to-load");

    return cssClassNames.join(" ");
  }

  generateAtomItemCssClasses(
    selected = false,
    inactive = false,
    unread = false
  ) {
    const cssClassNames = ["post-menu__item"];

    selected && cssClassNames.push("post-menu__item--selected");
    inactive && cssClassNames.push("post-menu__item--inactive");
    unread && cssClassNames.push("post-menu__item--unread");

    return cssClassNames.join(" ");
  }

  isSelectedTab(tabName) {
    return tabName === this.state.visibleTab;
  }

  selectTab(tabName) {
    this.props.ngRedux.dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: get(this.state.post, "uri"),
          selectTab: tabName,
        })
      )
    );
  }
}
WonAtomMenu.propTypes = {
  atomUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
