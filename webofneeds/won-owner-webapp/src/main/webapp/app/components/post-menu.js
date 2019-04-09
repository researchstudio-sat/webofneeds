/**
 * Created by quasarchimaere on 19.02.2019.
 */

import angular from "angular";
import Immutable from "immutable";
import { attach, getIn, get } from "../utils.js";
import { labels } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import * as needUtils from "../need-utils.js";
import * as viewUtils from "../view-utils.js";
import * as processUtils from "../process-utils.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import * as connectionUtils from "../connection-utils.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_post-menu.scss";

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
              ng-if="self.hasGroupFacet && !self.isOwned"
              ng-click="self.selectTab('PARTICIPANTS')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('PARTICIPANTS'),
                'post-menu__item--inactive': !self.hasGroupMembers,
              }">
              <span class="post-menu__item__label">Group Members</span>
              <span class="post-menu__item__count">({{self.groupMembersSize}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasGroupFacet && self.isOwned"
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
              ng-if="self.isOwned && self.hasChatFacet"
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
              ng-if="self.hasHolderFacet"
              ng-click="self.selectTab('HOLDS')"
              ng-class="{
                'post-menu__item--selected': self.isSelectedTab('HOLDS'),
                'post-menu__item--inactive': !self.hasHeldPosts
              }">
              <span class="post-menu__item__label">Posts of this Persona</span>
              <span class="post-menu__item__count">({{self.heldPostsSize}})</span>
            </div>
            <div class="post-menu__item"
              ng-if="self.hasReviewFacet"
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
        const post = getIn(state, ["needs", this.postUri]);
        const isPersona = needUtils.isPersona(post);
        const isOwned = needUtils.isOwned(post);

        const hasHolderFacet = needUtils.hasHolderFacet(post);
        const hasGroupFacet = needUtils.hasGroupFacet(post);
        const hasReviewFacet = needUtils.hasReviewFacet(post);
        const reviewCount =
          hasReviewFacet && getIn(post, ["rating", "reviewCount"]);

        const groupMembers = hasGroupFacet && get(post, "groupMembers");
        const groupChatConnections =
          isOwned &&
          hasGroupFacet &&
          connectionSelectors.getGroupChatConnectionsByNeedUri(
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

        const heldPosts = hasHolderFacet && get(post, "holds");
        const heldByUri =
          needUtils.hasHoldableFacet(post) && get(post, "heldBy");
        const hasHeldBy = !!heldByUri; //aka Persona that holds this post
        const persona = hasHeldBy && getIn(state, ["needs", heldByUri]);
        const personaHasReviewFacet = needUtils.hasReviewFacet(persona);
        const personaAggregateRating =
          personaHasReviewFacet &&
          getIn(persona, ["rating", "aggregateRating"]);

        const suggestions =
          isOwned &&
          connectionSelectors.getSuggestedConnectionsByNeedUri(
            state,
            this.postUri
          );

        const viewState = get(state, "view");
        const process = get(state, "process");

        const suggestionsSize = suggestions ? suggestions.size : 0;
        const groupMembersSize = groupMembers ? groupMembers.size : 0;
        const heldPostsSize = heldPosts ? heldPosts.size : 0;

        return {
          post,
          isPersona,
          isHoldable: needUtils.hasHoldableFacet(post),
          isOwned,
          hasHeldBy,
          personaHasReviewFacet,
          personaAggregateRatingString:
            personaAggregateRating && personaAggregateRating.toFixed(1),
          hasHeldPosts: heldPostsSize > 0,
          heldPostsSize,
          hasHolderFacet,
          hasGroupFacet,
          hasReviewFacet,
          hasReviews: reviewCount > 0,
          reviewCount,
          hasChatFacet: needUtils.hasChatFacet(post),
          hasGroupMembers: groupMembersSize > 0,
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
            !post || processUtils.isNeedLoading(process, this.postUri),
          postFailedToLoad:
            post && processUtils.hasNeedFailedToLoad(process, this.postUri),
          shouldShowRdf: viewUtils.showRdf(viewState),
          visibleTab: viewUtils.getVisibleTabByNeedUri(viewState, this.postUri),
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
      this.needs__selectTab(
        Immutable.fromJS({ needUri: get(this.post, "uri"), selectTab: tabName })
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
