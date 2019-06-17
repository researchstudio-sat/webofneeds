/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import inviewModule from "angular-inview";
import postIsOrSeeksInfoModule from "./post-is-or-seeks-info.js";
import labelledHrModule from "./labelled-hr.js";
import postContentGeneral from "./post-content-general.js";
import postContentPersona from "./post-content-persona.js";
import postContentParticipants from "./post-content-participants.js";
import atomContentBuddies from "./atom-content-buddies.js";
import postHeaderModule from "./post-header.js";
import trigModule from "./trig.js";
import { attach, getIn, get } from "../utils.js";
import won from "../won-es6.js";
import { connect2Redux } from "../won-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import {
  getOwnedPersonas,
  getConnectionUriFromRoute,
  isAtomOwned,
} from "../redux/selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import ngAnimate from "angular-animate";
import { Elm } from "../../elm/AddPersona.elm";

import "~/style/_post-content.scss";
import "~/style/_rdflink.scss";
import elmModule from "./elm.js";

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
              Failed To Load - Atom might have been deleted
          </span>
          <div class="post-failedtoload__actions">
            <button class="post-failedtoload__actions__button red won-button--outlined thin"
                ng-click="self.tryReload()">
                Try Reload
            </button>
          </div>
        </div>
        <div class="post-content" ng-if="!self.postLoading && !self.postFailedToLoad">
          <div class="post-content__updateindicator" ng-if="self.postProcessingUpdate">
            <svg class="hspinner post-content__updateindicator__spinner">
              <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
            <span class="post-content__updateindicator__label">Processing changes...</span>
          </div>
          <!-- GENERAL INFORMATION -->
          <won-post-content-general ng-if="self.isSelectedTab('DETAIL')" post-uri="self.postUri"></won-post-content-general>
          <!-- DETAIL INFORMATION -->
          <won-post-is-or-seeks-info branch="::'content'" ng-if="self.isSelectedTab('DETAIL') && self.hasContent" post-uri="self.postUri"></won-post-is-or-seeks-info>
          <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.isSelectedTab('DETAIL') && self.hasContent && self.hasSeeksBranch"></won-labelled-hr>
          <won-post-is-or-seeks-info branch="::'seeks'" ng-if="self.isSelectedTab('DETAIL') && self.hasSeeksBranch" post-uri="self.postUri"></won-post-is-or-seeks-info>

          <!-- PERSONA INFORMATION -->
          <won-post-content-persona ng-if="self.isSelectedTab('HELDBY') && self.post.get('heldBy')" holds-uri="self.postUri"></won-post-content-persona>
          <won-elm module="self.addPersonaModule" ng-if="self.isSelectedTab('HELDBY') && self.isOwned && self.hasHoldableSocket && !self.post.get('heldBy')" props="{post: self.post.toJS(), personas: self.personas.toJS()}"></won-elm>
          
          <!-- PARTICIPANT INFORMATION -->
          <won-post-content-participants ng-if="self.isSelectedTab('PARTICIPANTS')" post-uri="self.postUri"></won-post-content-participants>
          
          <!-- BUDDY INFORMATION -->
          <won-atom-content-buddies ng-if="self.isSelectedTab('BUDDIES')" atom-uri="self.postUri"></won-atom-content-buddies>

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
                  atom-uri="::conn.get('targetAtomUri')">
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
                No Suggestions for this Atom.
            </div>
          </div>

          <!-- OTHER ATOMS -->
          <div class="post-content__members" ng-if="self.isSelectedTab('HOLDS')">
            <div
              class="post-content__members__member"
              ng-if="self.hasHeldPosts"
              ng-repeat="heldPostUri in self.heldPostsArray track by heldPostUri">
              <div class="post-content__members__member__indicator"></div>
              <won-post-header
                class="clickable"
                ng-click="self.router__stateGoCurrent({viewAtomUri: heldPostUri, viewConnUri: undefined})"
                atom-uri="::heldPostUri">
              </won-post-header>
            </div>
            <div class="post-content__members__empty"
                ng-if="!self.hasHeldPosts">
                This Persona does not have any Atoms.
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
      this.won = won;
      window.postcontent4dbg = this;

      this.addPersonaModule = Elm.AddPersona;

      const selectFromState = state => {
        const openConnectionUri = getConnectionUriFromRoute(state);
        const post = getIn(state, ["atoms", this.postUri]);
        const isPersona = atomUtils.isPersona(post);
        const isOwned = isAtomOwned(state, this.postUri);
        const content = get(post, "content");

        //TODO it will be possible to have more than one seeks
        const seeks = get(post, "seeks");

        const hasContent = this.hasVisibleDetails(content);
        const hasSeeksBranch = this.hasVisibleDetails(seeks);

        const heldPosts = isPersona && get(post, "holds");

        const suggestions = connectionSelectors.getSuggestedConnectionsByAtomUri(
          state,
          this.postUri
        );

        const viewState = get(state, "view");
        const process = get(state, "process");

        return {
          hasContent,
          hasSeeksBranch,
          post,
          isPersona,
          isOwned,
          hasHeldPosts: isPersona && heldPosts && heldPosts.size > 0,
          heldPostsArray: isPersona && heldPosts && heldPosts.toArray(),
          hasChatSocket: atomUtils.hasChatSocket(post),
          hasHoldableSocket: atomUtils.hasHoldableSocket(post),
          hasSuggestions: isOwned && suggestions && suggestions.size > 0,
          suggestionsArray: isOwned && suggestions && suggestions.toArray(),
          postLoading:
            !post || processUtils.isAtomLoading(process, this.postUri),
          postFailedToLoad:
            post && processUtils.hasAtomFailedToLoad(process, this.postUri),
          postProcessingUpdate:
            post && processUtils.isAtomProcessingUpdate(process, this.postUri),
          createdTimestamp: post && post.get("creationDate"),
          shouldShowRdf: viewUtils.showRdf(viewState),
          fromConnection: !!openConnectionUri,
          openConnectionUri,
          visibleTab: viewUtils.getVisibleTabByAtomUri(viewState, this.postUri),
          personas: getOwnedPersonas(state),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }

    tryReload() {
      if (this.postUri && this.postFailedToLoad) {
        this.atoms__fetchUnloadedAtom(this.postUri);
      }
    }

    closeConnection(conn, rateBad = false) {
      if (!conn) {
        return;
      }

      const connUri = conn.get("uri");

      if (rateBad) {
        this.connections__rate(connUri, won.WONCON.binaryRatingBad);
      }

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          atomUri: this.postUri,
        });
      }

      this.connections__close(connUri);
    }

    sendRequest(conn, message = "") {
      if (!conn) {
        return;
      }

      const connUri = get(conn, "uri");
      const targetAtomUri = get(conn, "targetAtomUri");

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          atomUri: this.postUri,
        });
      }

      this.connections__rate(connUri, won.WONCON.binaryRatingGood);
      this.atoms__connect(this.postUri, connUri, targetAtomUri, message);
      this.router__stateGo("connections", {
        connectionUri: connUri,
        viewAtomUri: undefined,
        viewConnUri: undefined,
      });
    }

    addPersona(persona) {
      this.personas__connect(this.postUri, persona);
    }

    isSelectedTab(tabName) {
      return tabName === this.visibleTab;
    }

    markAsRead(conn) {
      if (conn && conn.get("unread")) {
        const payload = {
          connectionUri: conn.get("uri"),
          atomUri: this.postUri,
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
          atomUri: this.postUri,
        });
      }

      this.router__stateGoCurrent({
        viewConnUri: connUri,
        viewAtomUri: undefined,
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
            detailKey != "sockets" &&
            detailKey != "defaultSocket"
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
    postContentParticipants,
    postHeaderModule,
    atomContentBuddies,
    trigModule,
    inviewModule.name,
    elmModule,
  ])
  .directive("wonPostContent", genComponentConf).name;
