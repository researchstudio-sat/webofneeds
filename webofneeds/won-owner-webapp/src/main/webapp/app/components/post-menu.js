/**
 * Created by quasarchimaere on 19.02.2019.
 */

import angular from "angular";
import Immutable from "immutable";
import { attach, getIn, get } from "../utils.js";
import { labels } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import * as processUtils from "../process-utils.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as connectionUtils from "../connection-utils.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "~/style/_post-menu.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
            <div class="post-menu__item"
              ng-click="self.selectTab('DETAIL')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('DETAIL'),
              }">
              <span class="post-menu__item__label">Detail</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasHeldBy"
              ng-click="self.selectTab('HELDBY')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('HELDBY'),
              }">
              <span class="post-menu__item__label">Persona</span>
              <span class="post-menu__item__rating" ng-if="self.personaAggregateRatingString">(★ {{ self.personaAggregateRatingString }})</span>
            </div>
            <div class="post-menu__item"
              ng-if="!self.hasHeldBy && self.isHoldable && self.isOwned"
              ng-click="self.selectTab('HELDBY')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('HELDBY'),
              }">
              <span class="post-menu__item__label">+ Persona</span>
              <span class="post-menu__item__rating" ng-if="self.personaAggregateRatingString">(★ {{ self.personaAggregateRatingString }})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasGroupSocket && !self.isOwned"
              ng-click="self.selectTab('PARTICIPANTS')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('PARTICIPANTS'),
                'post-menu__item--inactive': !self.groupMembers,
              }">
              <span class="post-menu__item__label">Group Members</span>
              <span class="post-menu__item__count">({{self.groupMembersSize}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasGroupSocket && self.isOwned"
              ng-click="self.selectTab('PARTICIPANTS')"
              ng-class="{
                'post-menu__item--unread': self.hasUnreadGroupChatRequests,
                'post-menu__item--selected': self.isSelectedTab('PARTICIPANTS'),
              }">
              <span class="post-menu__item__unread"></span>
              <span class="post-menu__item__label">Group Members</span>
              <span class="post-menu__item__count" ng-if="self.connectedGroupChatConnectionsSize">({{self.connectedGroupChatConnectionsSize}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.isOwned && self.hasChatSocket"
              ng-click="self.selectTab('SUGGESTIONS')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('SUGGESTIONS'),
                'post-menu__item--inactive': !self.hasSuggestions,
                'post-menu__item--unread': self.hasUnreadSuggestions
              }">
              <span class="post-menu__item__unread"></span>
              <span class="post-menu__item__label">Suggestions</span>
              <span class="post-menu__item__count">({{self.suggestionsSize}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasHolderSocket"
              ng-click="self.selectTab('HOLDS')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('HOLDS'),
                'post-menu__item--inactive': !self.hasHeldPosts
              }">
              <span class="post-menu__item__label">Posts of this Persona</span>
              <span class="post-menu__item__count">({{self.heldPostsSize}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasBuddySocket"
              ng-click="self.selectTab('BUDDIES')"
              ng-class="{
                'post-menu__item--unread': self.hasBuddyRequests,
                'post-menu__item--selected': self.isSelectedTab('BUDDIES'),
                'post-menu__item--inactive': !self.hasBuddies
              }">
              <span class="post-menu__item__label">Buddies</span>
              <span class="post-menu__item__count">({{self.buddyCount}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasReviewSocket"
              ng-click="self.selectTab('REVIEWS')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('REVIEWS'),
                'post-menu__item--inactive': !self.hasReviews,
              }">
              <span class="post-menu__item__label">Reviews</span>
              <span class="post-menu__item__rating" ng-if="self.hasReviews">({{ self.reviewCount}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.shouldShowRdf"
              ng-click="self.selectTab('RDF')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('RDF'),
              }">
              <span class="post-menu__item__label">RDF</span>
            </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;

      window.postcontent4dbg = this;

      const selectFromState = state => {
        const post = getIn(state, ["atoms", this.postUri]);
        const isPersona = atomUtils.isPersona(post);
        const isOwned = generalSelectors.isAtomOwned(state, this.postUri);

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
          connectionSelectors.getGroupChatConnectionsByAtomUri(
            state,
            this.postUri
          );
        const connectedGroupChatConnections =
          groupChatConnections &&
          groupChatConnections.filter(conn =>
            connectionUtils.isConnected(conn)
          );
        const nonClosedNonConnectedGroupChatConnections =
          groupChatConnections &&
          groupChatConnections.filter(
            conn =>
              !(
                connectionUtils.isConnected(conn) ||
                connectionUtils.isClosed(conn)
              )
          );

        //TODO: BLARGH GROUPCHATCONN FILTER

        const heldPosts = hasHolderSocket && get(post, "holds");
        const heldByUri =
          atomUtils.hasHoldableSocket(post) && get(post, "heldBy");
        const hasHeldBy = !!heldByUri; //aka Persona that holds this post
        const persona = hasHeldBy && getIn(state, ["atoms", heldByUri]);
        const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
        const personaAggregateRating =
          personaHasReviewSocket &&
          getIn(persona, ["rating", "aggregateRating"]);

        const suggestions =
          isOwned &&
          connectionSelectors.getSuggestedConnectionsByAtomUri(
            state,
            this.postUri
          );

        const buddyConnections =
          isOwned &&
          connectionSelectors.getBuddyConnectionsByAtomUri(
            state,
            this.postUri,
            true,
            true
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
          hasHeldBy,
          personaHasReviewSocket,
          personaAggregateRatingString:
            personaAggregateRating && personaAggregateRating.toFixed(1),
          hasHeldPosts: heldPostsSize > 0,
          heldPostsSize,
          hasHolderSocket,
          hasGroupSocket,
          hasReviewSocket,
          hasBuddySocket,
          hasBuddyRequests:
            !!buddyConnections &&
            !!buddyConnections.find(conn =>
              connectionUtils.isRequestReceived(conn)
            ),
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
          postLoading:
            !post || processUtils.isAtomLoading(process, this.postUri),
          postFailedToLoad:
            post && processUtils.hasAtomFailedToLoad(process, this.postUri),
          shouldShowRdf: viewUtils.showRdf(viewState),
          visibleTab: viewUtils.getVisibleTabByAtomUri(viewState, this.postUri),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
      classOnComponentRoot(
        "won-failed-to-load",
        () => this.postFailedToLoad,
        this
      );
    }

    isSelectedTab(tabName) {
      return tabName === this.visibleTab;
    }

    selectTab(tabName) {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: get(this.post, "uri"), selectTab: tabName })
      );
    }
  }

  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
    scope: {
      postUri: "=",
    },
  };
}

export default angular
  .module("won.owner.components.postMenu", [])
  .directive("wonPostMenu", genComponentConf).name;
