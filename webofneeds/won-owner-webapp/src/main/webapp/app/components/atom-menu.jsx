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
    const atom = getIn(state, ["atoms", this.atomUri]);
    const isPersona = atomUtils.isPersona(atom);
    const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);

    const hasHolderSocket = atomUtils.hasHolderSocket(atom);
    const hasGroupSocket = atomUtils.hasGroupSocket(atom);
    const hasReviewSocket = atomUtils.hasReviewSocket(atom);
    const hasBuddySocket = atomUtils.hasBuddySocket(atom);
    const reviewCount =
      hasReviewSocket && getIn(atom, ["rating", "reviewCount"]);

    const groupMembers = hasGroupSocket && get(atom, "groupMembers");
    const groupChatConnections =
      isOwned &&
      hasGroupSocket &&
      connectionSelectors.getGroupChatConnectionsByAtomUri(state, this.atomUri);
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
      this.atomUri
    );
    const heldByUri = atomUtils.getHeldByUri(atom);
    const isHeld = atomUtils.isHeld(atom);
    const persona = getIn(state, ["atoms", heldByUri]);
    const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
    const personaAggregateRating =
      personaHasReviewSocket && getIn(persona, ["rating", "aggregateRating"]);

    const suggestions =
      isOwned &&
      connectionSelectors.getSuggestedConnectionsByAtomUri(state, this.atomUri);

    const buddyConnections =
      isOwned &&
      connectionSelectors.getBuddyConnectionsByAtomUri(
        state,
        this.atomUri,
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
      atomLoading: !atom || processUtils.isAtomLoading(process, this.atomUri),
      atomFailedToLoad:
        atom && processUtils.hasAtomFailedToLoad(process, this.atomUri),
      shouldShowRdf: viewUtils.showRdf(viewState),
      visibleTab: viewUtils.getVisibleTabByAtomUri(viewState, this.atomUri),
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
        key="detail"
        className={this.generateAtomItemCssClasses(
          this.isSelectedTab("DETAIL")
        )}
        onClick={() => this.selectTab("DETAIL")}
      >
        <span className="atom-menu__item__label">Detail</span>
      </div>
    );

    if (this.state.isHeld) {
      buttons.push(
        <div
          key="heldby"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HELDBY")
          )}
          onClick={() => this.selectTab("HELDBY")}
        >
          <span className="atom-menu__item__label">Persona</span>
          {this.state.personaAggregateRatingString && (
            <span className="atom-menu__item__rating">
              (★ {this.state.personaAggregateRatingString})
            </span>
          )}
        </div>
      );
    } else if (this.state.isHoldable && this.state.isOwned) {
      buttons.push(
        <div
          key="heldby"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HELDBY")
          )}
          onClick={() => this.selectTab("HELDBY")}
        >
          <span className="atom-menu__item__label">+ Persona</span>
          {this.state.personaAggregateRatingString && (
            <span className="atom-menu__item__rating">
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
              key="participants"
              className={this.generateAtomItemCssClasses(
                this.isSelectedTab("PARTICIPANTS"),
                false,
                this.state.hasUnreadGroupChatRequests
              )}
              onClick={() => this.selectTab("PARTICIPANTS")}
            >
              <span className="atom-menu__item__unread" />
              <span className="atom-menu__item__label">Group Members</span>
              {this.state.connectedGroupChatConnectionsSize && (
                <span className="atom-menu__item__count">
                  ({this.state.connectedGroupChatConnectionsSize})
                </span>
              )}
            </div>
          )
        : buttons.push(
            <div
              key="participants"
              className={this.generateAtomItemCssClasses(
                this.isSelectedTab("PARTICIPANTS"),
                !this.state.groupMembers
              )}
              onClick={() => this.selectTab("PARTICIPANTS")}
            >
              <span className="atom-menu__item__label">Group Members</span>
              <span className="atom-menu__item__count">
                ({this.state.groupMembersSize})
              </span>
            </div>
          );
    }

    this.state.isOwned &&
      this.state.hasChatSocket &&
      buttons.push(
        <div
          key="suggestions"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("SUGGESTIONS"),
            !this.state.hasSuggestions,
            this.state.hasUnreadSuggestions
          )}
          onClick={() => this.selectTab("SUGGESTIONS")}
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">Suggestions</span>
          <span className="atom-menu__item__count">
            ({this.state.suggestionsSize})
          </span>
        </div>
      );

    this.state.hasHolderSocket &&
      buttons.push(
        <div
          key="holds"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("HOLDS"),
            !this.state.hasHeldAtoms,
            this.state.hasUnreadSuggestedConnectionsInHeldAtoms
          )}
          onClick={() => this.selectTab("HOLDS")}
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">Posts</span>
          <span className="atom-menu__item__count">
            ({this.state.heldAtomsSize})
          </span>
        </div>
      );

    this.state.hasBuddySocket &&
      buttons.push(
        <div
          key="buddies"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("BUDDIES"),
            !this.state.hasBuddies,
            this.state.hasUnreadBuddyConnections
          )}
          onClick={() => this.selectTab("BUDDIES")}
        >
          <span className="atom-menu__item__unread" />
          <span className="atom-menu__item__label">Buddies</span>
          <span className="atom-menu__item__count">
            ({this.state.buddyCount})
          </span>
        </div>
      );
    this.state.hasReviewSocket &&
      buttons.push(
        <div
          key="reviews"
          className={this.generateAtomItemCssClasses(
            this.isSelectedTab("REVIEWS"),
            !this.state.hasReviews,
            false
          )}
          onClick={() => this.selectTab("REVIEWS")}
        >
          <span className="atom-menu__item__label">Reviews</span>
          {this.state.hasReviews && (
            <span className="atom-menu__item__rating">
              ({this.state.reviewCount})
            </span>
          )}
        </div>
      );
    this.state.shouldShowRdf &&
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
    this.state.atomLoading && cssClassNames.push("won-is-loading");
    this.state.atomFailedToLoad && cssClassNames.push("won-failed-to-load");

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
    return tabName === this.state.visibleTab;
  }

  selectTab(tabName) {
    this.props.ngRedux.dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: this.atomUri,
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
