/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import inviewModule from "angular-inview";
import postIsOrSeeksInfoModule from "./post-is-or-seeks-info.js";
import labelledHrModule from "./labelled-hr.js";
import postContentGeneral from "./post-content-general.js";
import postContentPersona from "./post-content-persona.js";
import postHeaderModule from "./post-header.js";
import trigModule from "./trig.js";
import { attach, getIn, get } from "../utils.js";
import won from "../won-es6.js";
import { labels } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import * as needUtils from "../need-utils.js";
import * as viewUtils from "../view-utils.js";
import * as processUtils from "../process-utils.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import { getConnectionUriFromRoute } from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import ngAnimate from "angular-animate";

import "style/_post-content.scss";
import "style/_rdflink.scss";

const CONNECTION_READ_TIMEOUT = 1500;

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
          <!-- GENERAL INFORMATION -->
          <won-post-content-general ng-if="self.isSelectedTab('DETAIL')" post-uri="self.postUri"></won-post-content-general>
          <!-- DETAIL INFORMATION -->
          <won-post-is-or-seeks-info branch="::'content'" ng-if="self.isSelectedTab('DETAIL') && self.hasContent" post-uri="self.postUri"></won-post-is-or-seeks-info>
          <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.isSelectedTab('DETAIL') && self.hasContent && self.hasSeeksBranch"></won-labelled-hr>
          <won-post-is-or-seeks-info branch="::'seeks'" ng-if="self.isSelectedTab('DETAIL') && self.hasSeeksBranch" post-uri="self.postUri"></won-post-is-or-seeks-info>



          <!-- PERSONA INFORMATION -->
          <won-post-content-persona ng-if="self.isSelectedTab('HELDBY')" holds-uri="self.postUri"></won-post-content-persona>

          <!-- PARTICIPANT INFORMATION -->
          <div class="post-content__members" ng-if="self.isSelectedTab('PARTICIPANTS')">
            <div
                class="post-content__members__member"
                ng-if="self.hasGroupMembers"
                ng-repeat="memberUri in self.groupMembersArray track by memberUri">
                <won-post-header
                  class="clickable"
                  ng-click="self.router__stateGoCurrent({viewNeedUri: memberUri, viewConnUri: undefined})"
                  need-uri="::memberUri">
                </won-post-header>
            </div>
            <div class="post-content__members__empty"
                ng-if="!self.hasGroupMembers">
                No Groupmembers present.
            </div>
          </div>

          <!-- REVIEW INFORMATION -->
          <div class="post-content__reviews" ng-if="self.isSelectedTab('REVIEWS')">
            <div class="post-content__reviews__empty">
                No Reviews to display.
            </div>
          </div>

          <!-- SUGGESTIONS -->
          <div class="post-content__suggestions" ng-if="self.isSelectedTab('SUGGESTIONS')">
            <div
              class="post-content__suggestions__suggestion"
              ng-repeat="conn in self.suggestionsArray"
              ng-if="self.hasSuggestions"
              in-view="conn.get('unread') && $inview && self.markAsRead(conn)"
              ng-class="{'won-unread': conn.get('unread')}">
                <div class="post-content__suggestions__suggestion__indicator"></div>
                <won-post-header
                  class="clickable"
                  ng-click="self.viewSuggestion(conn)"
                  need-uri="::conn.get('remoteNeedUri')">
                </won-post-header>
                <div class="post-content__suggestions__suggestion__actions">
                    <div
                      class="post-content__suggestions__suggestion__actions__button red won-button--outlined thin"
                      ng-click="self.sendRequest(conn)">
                        Request
                    </div>
                    <div
                      class="post-content__suggestions__suggestion__actions__button red won-button--outlined thin"
                      ng-click="self.closeConnection(conn)">
                        Remove
                    </div>
                </div>
            </div>
            <div class="post-content__suggestions__empty"
                ng-if="!self.hasSuggestions">
                No Suggestions for this Need.
            </div>
          </div>

          <!-- OTHER NEEDS -->
          <div class="post-content__members" ng-if="self.isSelectedTab('HOLDS')">
            <div
              class="post-content__members__member"
              ng-if="self.hasHeldPosts"
              ng-repeat="heldPostUri in self.heldPostsArray track by heldPostUri">
              <won-post-header
                class="clickable"
                ng-click="self.router__stateGoCurrent({viewNeedUri: heldPostUri, viewConnUri: undefined})"
                need-uri="::heldPostUri">
              </won-post-header>
            </div>
            <div class="post-content__members__empty"
                ng-if="!self.hasHeldPosts">
                This Persona does not have any Needs.
            </div>
          </div>

          <!-- RDF REPRESENTATION -->
          <div class="post-info__content__rdf" ng-if="self.isSelectedTab('RDF')">
            <a class="rdflink clickable"
              target="_blank"
              href="{{self.postUri}}">
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
        const content = get(post, "content");

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

        const isOwnedNeedWhatsX =
          isOwned &&
          (needUtils.isWhatsAroundNeed(post) || needUtils.isWhatsNewNeed(post));

        const viewState = get(state, "view");
        const process = get(state, "process");

        return {
          hasContent,
          hasSeeksBranch,
          post,
          isOwnedNeedWhatsX,
          isPersona,
          isOwned,
          hasHeldPosts: isPersona && heldPosts && heldPosts.size > 0,
          heldPostsArray: isPersona && heldPosts && heldPosts.toArray(),
          hasGroupFacet: needUtils.hasGroupFacet(post),
          hasChatFacet: needUtils.hasChatFacet(post),
          hasGroupMembers: groupMembers && groupMembers.size > 0,
          groupMembersArray: groupMembers && groupMembers.toArray(),
          hasSuggestions: isOwned && suggestions && suggestions.size > 0,
          suggestionsArray: isOwned && suggestions && suggestions.toArray(),
          postLoading:
            !post || processUtils.isNeedLoading(process, this.postUri),
          postFailedToLoad:
            post && processUtils.hasNeedFailedToLoad(process, this.postUri),
          createdTimestamp: post && post.get("creationDate"),
          shouldShowRdf: viewUtils.showRdf(viewState),
          fromConnection: !!openConnectionUri,
          openConnectionUri,
          visibleTab: viewUtils.getVisibleTabByNeedUri(viewState, this.postUri),
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

    closeConnection(conn, rateBad = false) {
      if (!conn) {
        return;
      }

      const connUri = conn.get("uri");

      if (rateBad) {
        this.connections__rate(connUri, won.WON.binaryRatingBad);
      }

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          needUri: this.postUri,
        });
      }

      this.connections__close(connUri);
    }

    sendRequest(conn, message = "") {
      if (!conn) {
        return;
      }

      const connUri = get(conn, "uri");
      const remoteNeedUri = get(conn, "remoteNeedUri");

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          needUri: this.postUri,
        });
      }

      if (this.isOwnedNeedWhatsX) {
        this.connections__close(connUri);

        if (remoteNeedUri) {
          this.connections__connectAdHoc(remoteNeedUri, message);
        }
        //this.router__back();
      } else {
        this.connections__rate(connUri, won.WON.binaryRatingGood);
        this.needs__connect(this.postUri, connUri, remoteNeedUri, message);
      }
    }

    isSelectedTab(tabName) {
      return tabName === this.visibleTab;
    }

    markAsRead(conn) {
      if (conn && conn.get("unread")) {
        const payload = {
          connectionUri: conn.get("uri"),
          needUri: this.postUri,
        };

        const tmp_connections__markAsRead = this.connections__markAsRead;

        setTimeout(function() {
          tmp_connections__markAsRead(payload);
        }, CONNECTION_READ_TIMEOUT);
      }
    }

    viewSuggestion(conn) {
      if (!conn) {
        return;
      }

      const connUri = conn.get("uri");

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          needUri: this.postUri,
        });
      }

      this.router__stateGoCurrent({
        viewConnUri: connUri,
        viewNeedUri: undefined,
      });
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
    postContentPersona,
    postHeaderModule,
    trigModule,
    inviewModule.name,
  ])
  .directive("wonPostContent", genComponentConf).name;
