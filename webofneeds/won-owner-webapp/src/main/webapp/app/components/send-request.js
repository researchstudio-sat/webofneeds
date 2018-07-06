import angular from "angular";
import "ng-redux";
import connectionHeaderModule from "./connection-header.js";
import feedbackGridModule from "./feedback-grid.js";
import postIsOrSeeksInfoModule from "./post-is-or-seeks-info.js";
import postShareLinkModule from "./post-share-link.js";
import labelledHrModule from "./labelled-hr.js";
import chatTextFieldSimpleModule from "./chat-textfield-simple.js";
import connectionContextDropdownModule from "./connection-context-dropdown.js";
import won from "../won-es6.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import {
  selectOpenPostUri,
  selectNeedByConnectionUri,
  selectLastUpdateTime,
} from "../selectors.js";
import { connect2Redux } from "../won-utils.js";
import { labels, relativeTime } from "../won-label-utils.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="post-info__header" ng-if="self.includeHeader">
            <a class="post-info__header__back clickable show-in-responsive"
               ng-click="self.router__stateGoCurrent({connectionUri : undefined, sendAdHocRequest: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-connection-header
                connection-uri="self.connection.get('uri')"
                timestamp="self.connection.get('lastUpdateDate')"
                hide-image="::false">
            </won-connection-header>
            <won-connection-context-dropdown ng-if="self.connection && self.connection.get('isRated')"></won-connection-context-dropdown>
        </div>
        <div class="post-info__content" ng-if="self.isLoading()">
            <h2 class="post-info__heading"></h2>
            <p class="post-info__details"></p>
            <h2 class="post-info__heading"></h2>
            <p class="post-info__details"></p>
            <h2 class="post-info__heading"></h2>
            <p class="post-info__details"></p>
            <p class="post-info__details"></p>
            <p class="post-info__details"></p>
            <p class="post-info__details"></p>
            <p class="post-info__details"></p>
            <h2 class="post-info__heading"></h2>
            <div class="post-info__details"></div>
        </div>
        <div class="post-info__content" ng-if="!self.isLoading()">
            <div class="post-info__content__general">
              <div class="post-info__content__general__item">
                <div class="post-info__content__general__item__label" ng-show="self.friendlyTimestamp">
                  Created
                </div>
                <div class="post-info__content__general__item__value" ng-show="self.friendlyTimestamp">
                  {{ self.friendlyTimestamp }}
                </div>
              </div>
              <div class="post-info__content__general__item">
                <div class="post-info__content__general__item__label" ng-show="self.displayedPost.get('type')">
                  Type
                </div>
                <div class="post-info__content__general__item__value" ng-show="self.displayedPost.get('type')">
                  {{self.labels.type[self.displayedPost.get('type')]}}{{self.displayedPost.get('matchingContexts')? ' in '+ self.displayedPost.get('matchingContexts').join(', ') : '' }}
                </div>
              </div>
            </div>

            <won-gallery ng-show="self.displayedPost.get('hasImages')">
            </won-gallery>

            <won-post-is-or-seeks-info branch="::'is'" ng-if="self.hasIsBranch"></won-post-is-or-seeks-info>
            <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.hasIsBranch && self.hasSeeksBranch"></won-labelled-hr>
            <won-post-is-or-seeks-info branch="::'seeks'" ng-if="self.hasSeeksBranch"></won-post-is-or-seeks-info>
            <a class="rdflink clickable"
               ng-if="self.shouldShowRdf && self.connection"
               target="_blank"
               href="{{ self.connectionUri }}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Connection</span>
            </a>
            <a class="rdflink clickable"
               ng-if="self.shouldShowRdf"
               target="_blank"
               href="{{ self.postUriToConnectTo }}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Post</span>
            </a>
        </div>
        <div class="post-info__footer" ng-if="!self.isLoading()">
            <won-post-share-link
                ng-if="self.displayedPost.get('state') !== self.WON.InactiveCompacted"
                post-uri="self.displayedPost && self.displayedPost.get('uri')">
            </won-post-share-link>
            <won-labelled-hr label="::'Or'" class="post-info__footer__labelledhr"></won-labelled-hr>

            <won-feedback-grid ng-if="self.connection && !self.connection.get('isRated')" connection-uri="self.connectionUri"></won-feedback-grid>

            <chat-textfield-simple
                placeholder="::'Message (optional)'"
                on-submit="::self.sendRequest(value)"
                allow-empty-submit="::true"
                submit-button-label="::'Ask to Chat'"
                ng-if="!self.connection || self.connection.get('isRated')"
            >
            </chat-textfield-simple>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.message = "";
      this.labels = labels;
      this.WON = won.WON;
      window.openMatch4dbg = this;

      const selectFromState = state => {
        const connectionUri = decodeURIComponent(
          getIn(state, ["router", "currentParams", "connectionUri"])
        );
        const ownNeed =
          connectionUri && selectNeedByConnectionUri(state, connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", connectionUri]);
        const postUriToConnectTo = !connection
          ? selectOpenPostUri(state)
          : connection && connection.get("remoteNeedUri");

        const displayedPost = state.getIn(["needs", postUriToConnectTo]);

        const is = displayedPost ? displayedPost.get("is") : undefined;
        //TODO it will be possible to have more than one seeks
        const seeks = displayedPost ? displayedPost.get("seeks") : undefined;

        return {
          connection,
          connectionUri,
          ownNeed,
          hasIsBranch: !!is,
          hasSeeksBranch: !!seeks,
          displayedPost,
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
          postUriToConnectTo,
          friendlyTimestamp:
            displayedPost &&
            relativeTime(
              selectLastUpdateTime(state),
              displayedPost.get("creationDate")
            ),
          shouldShowRdf: state.get("showRdf"),
          createdTimestamp: displayedPost && displayedPost.get("creationDate"),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    isLoading() {
      return !this.displayedPost || this.displayedPost.get("isLoading");
    }

    sendRequest(message) {
      const isOwnNeedWhatsX =
        this.ownNeed &&
        (this.ownNeed.get("isWhatsAround") || this.ownNeed.get("isWhatsNew"));

      if (!this.connection || isOwnNeedWhatsX) {
        this.router__stateGoResetParams("connections");

        if (isOwnNeedWhatsX) {
          //Close the connection if there was a present connection for a whatsaround need
          this.connections__close(this.connectionUri);
        }

        if (this.postUriToConnectTo) {
          this.connections__connectAdHoc(this.postUriToConnectTo, message);
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.needs__connect(
          this.ownNeed.get("uri"),
          this.connectionUri,
          this.ownNeed
            .getIn(["connections", this.connectionUri])
            .get("remoteNeedUri"),
          message
        );
        this.router__stateGoCurrent({ connectionUri: this.connectionUri });
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      includeHeader: "=", //only read once
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.sendRequest", [
    postIsOrSeeksInfoModule,
    connectionHeaderModule,
    feedbackGridModule,
    labelledHrModule,
    chatTextFieldSimpleModule,
    postShareLinkModule,
    connectionContextDropdownModule,
  ])
  .directive("wonSendRequest", genComponentConf).name;
