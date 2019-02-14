/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postIsOrSeeksInfoModule from "./post-is-or-seeks-info.js";
import labelledHrModule from "./labelled-hr.js";
import postContentGeneral from "./post-content-general.js";
import postHeaderModule from "./post-header.js";
import trigModule from "./trig.js";
import { attach, getIn, get } from "../utils.js";
import won from "../won-es6.js";
import { labels } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import * as needUtils from "../need-utils.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import { getConnectionUriFromRoute } from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import ngAnimate from "angular-animate";

import "style/_post-content.scss";
import "style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="post-skeleton" ng-if="self.postLoading">
            <h2 class="post-skeleton__heading"></h2>
            <p class="post-skeleton__details"></p>
            <h2 class="post-skeleton__heading"></h2>
            <p class="post-skeleton__details"></p>
            <h2 class="post-skeleton__heading"></h2>
            <p class="post-skeleton__details"></p>
            <p class="post-skeleton__details"></p>
            <p class="post-skeleton__details"></p>
            <p class="post-skeleton__details"></p>
            <p class="post-skeleton__details"></p>
            <h2 class="post-skeleton__heading"></h2>
            <div class="post-skeleton__details"></div>
        </div>
        <div class="post-failedtoload" ng-if="self.postFailedToLoad">
          <svg class="post-failedtoload__icon">
              <use xlink:href="#ico16_indicator_error" href="#ico16_indicator_error"></use>
          </svg>
          <span class="post-failedtoload__label">
              Failed To Load - Need might have been deleted
          </span>
          <div class="post-failedtoload__actions">
            <button class="post-failedtoload__actions__button red won-button--outlined thin"
                ng-click="self.tryReload()">
                Try Reload
            </button>
        </div>
        </div>
        <div class="post-content" ng-if="!self.postLoading && !self.postFailedToLoad">
          <!-- DETAIL INFORMATION -->
          <won-post-is-or-seeks-info branch="::'content'" ng-if="self.hasContent" post-uri="self.post.get('uri')"></won-post-is-or-seeks-info>
          <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.hasContent && self.hasSeeksBranch"></won-labelled-hr>
          <won-post-is-or-seeks-info branch="::'seeks'" ng-if="self.hasSeeksBranch" post-uri="self.post.get('uri')"></won-post-is-or-seeks-info>

          <!-- PARTICIPANT INFORMATION -->
          <won-labelled-hr label="::'Group Members'" class="cp__labelledhr" ng-if="self.hasGroupMembers"></won-labelled-hr>
          <div class="post-content__members" ng-if="self.hasGroupMembers">
            <div
              class="post-content__members__member"
              ng-repeat="memberUri in self.groupMembersArray track by memberUri">
              <won-post-header
                class="clickable"
                ng-click="self.router__stateGoCurrent({viewNeedUri: memberUri})"
                need-uri="::memberUri">
              </won-post-header>
            </div>
          </div>
          <!-- SUGGESTIONS -->
          <won-labelled-hr label="::'Suggestions'" class="cp__labelledhr" ng-if="self.hasSuggestions"></won-labelled-hr>
          <div class="post-content__suggestions" ng-if="self.hasSuggestions">
            <div
              class="post-content__suggestions__suggestion"
              ng-repeat="conn in self.suggestionsArray"
              ng-class="{'won-unread': conn.get('unread')}">
                <won-post-header
                  class="clickable"
                  ng-click="self.connections__markAsRead({connectionUri: conn.get('uri'), needUri: self.post.get('uri')}) && self.router__stateGoCurrent({viewNeedUri: conn.get('remoteNeedUri')})"
                  need-uri="::conn.get('remoteNeedUri')">
                </won-post-header>
                <div class="post-content__suggestions__suggestion__actions">
                    <div
                      class="post-content__suggestions__suggestion__actions__button red won-button--outlined thin"
                      ng-click="self.sendRequest(conn.get('uri'), conn.get('remoteNeedUri'))">
                        Request
                    </div>
                    <div
                      class="post-content__suggestions__suggestion__actions__button red won-button--outlined thin"
                      ng-click="self.closeConnection(conn.get('uri'))">
                        Remove
                    </div>
                </div>
            </div>
          </div>

          <!-- OTHER NEEDS -->
          <won-labelled-hr label="::'Posts of this Persona'" class="cp__labelledhr" ng-if="self.isPersona && self.hasHeldPosts"></won-labelled-hr>
          <div class="post-content__members" ng-if="self.isPersona && self.hasHeldPosts">
            <div
              class="post-content__members__member"
              ng-repeat="heldPostUri in self.heldPostsArray track by heldPostUri">
              <won-post-header
                class="clickable"
                ng-click="self.router__stateGoCurrent({viewNeedUri: heldPostUri})"
                need-uri="::heldPostUri">
              </won-post-header>
            </div>
          </div>
          <!-- GENERAL INFORMATION -->
          <won-labelled-hr ng-click="self.toggleShowGeneral()" arrow="self.showGeneral ? 'up' : 'down'" label="::'General Information'" class="cp__labelledhr"></won-labelled-hr>
          <won-post-content-general class="post-collapsible-general" ng-if="self.showGeneral" post-uri="self.post.get('uri')"></won-post-content-general>
          <!-- RDF REPRESENTATION -->
          <div class="post-info__content__rdf" ng-if="self.shouldShowRdf">
            <h2 class="post-info__heading">
                RDF
            </h2>
            <a class="rdflink clickable"
              target="_blank"
              href="{{self.post.get('uri')}}">
                  <svg class="rdflink__small">
                      <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                  </svg>
                  <span class="rdflink__label">Post</span>
            </a>
            <a class="rdflink clickable"
             ng-if="self.openConnectionUri"
             target="_blank"
             href="{{ self.openConnectionUri }}">
                  <svg class="rdflink__small">
                      <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                  </svg>
                  <span class="rdflink__label">Connection</span>
            </a>
            <won-trig
              ng-if="self.post.get('jsonld')"
              jsonld="self.post.get('jsonld')">
            </won-trig>
          </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;

      window.postcontent4dbg = this;

      const selectFromState = state => {
        const openConnectionUri = getConnectionUriFromRoute(state);
        const post = getIn(state, ["needs", this.postUri]);
        const isPersona = needUtils.isPersona(post);
        const isOwned = needUtils.isOwned(post);
        const content = post ? post.get("content") : undefined;

        //TODO it will be possible to have more than one seeks
        const seeks = get(post, "seeks");

        const hasContent = this.hasVisibleDetails(content);
        const hasSeeksBranch = this.hasVisibleDetails(seeks);

        const groupMembers = get(post, "groupMembers");

        const heldPosts = get(post, "holds");

        const suggestions = connectionSelectors.getSuggestedConnectionsByNeedUri(
          state,
          this.postUri
        );

        return {
          WON: won.WON,
          hasContent,
          hasSeeksBranch,
          post,
          isPersona,
          hasHeldPosts: isPersona && heldPosts && heldPosts.size > 0,
          heldPostsArray: isPersona && heldPosts && heldPosts.toArray(),
          hasGroupMembers: groupMembers && groupMembers.size > 0,
          groupMembersArray: groupMembers && groupMembers.toArray(),
          hasSuggestions: isOwned && suggestions && suggestions.size > 0,
          suggestionsArray: isOwned && suggestions && suggestions.toArray(),
          postLoading:
            !post ||
            getIn(state, ["process", "needs", post.get("uri"), "loading"]),
          postFailedToLoad:
            post &&
            getIn(state, ["process", "needs", post.get("uri"), "failedToLoad"]),
          createdTimestamp: post && post.get("creationDate"),
          shouldShowRdf: state.getIn(["view", "showRdf"]),
          fromConnection: !!openConnectionUri,
          openConnectionUri,
          showGeneral: state.getIn([
            "view",
            "needs",
            this.postUri,
            "showGeneralInfo",
          ]),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }

    tryReload() {
      if (this.postUri && this.postFailedToLoad) {
        this.needs__fetchUnloadedNeed(this.postUri);
      }
    }

    closeConnection(connUri, rateBad = false) {
      rateBad && this.connections__rate(connUri, won.WON.binaryRatingBad);
      this.connections__close(connUri);
    }

    sendRequest(connUri, remoteNeedUri, message) {
      this.connections__rate(connUri, won.WON.binaryRatingGood);
      this.needs__connect(
        this.groupPostAdminUri,
        connUri,
        remoteNeedUri,
        message
      );
    }

    toggleShowGeneral() {
      this.needs__toggleGeneralInfo(this.postUri);
    }

    /**
     * This function checks if there is at least one detail present that is displayable
     */
    hasVisibleDetails(contentBranchImm) {
      return (
        contentBranchImm &&
        contentBranchImm.find(
          (detailValue, detailKey) =>
            detailKey != "type" &&
            detailKey != "facets" &&
            detailKey != "defaultFacet"
        )
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
  .module("won.owner.components.postContent", [
    ngAnimate,
    postIsOrSeeksInfoModule,
    labelledHrModule,
    postContentGeneral,
    postHeaderModule,
    trigModule,
  ])
  .directive("wonPostContent", genComponentConf).name;
