/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postIsOrSeeksInfoModule from "./post-is-or-seeks-info.js";
import postHeaderModule from "./post-header.js";
import postShareLinkModule from "./post-share-link.js";
import labelledHrModule from "./labelled-hr.js";
import postContextDropdownModule from "./post-context-dropdown.js";
import trigModule from "./trig.js";
import { attach } from "../utils.js";
import won from "../won-es6.js";
import { labels, relativeTime } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import {
  selectOpenPostUri,
  selectLastUpdateTime,
  selectOpenConnectionUri,
} from "../selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="post-info__header" ng-if="self.includeHeader">
            <a class="post-info__header__back clickable show-in-responsive"
               ng-class="{'show-in-responsive': !self.fromConnection}"
               ng-click="self.router__stateGoCurrent({postUri : null})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.post.get('uri')"
                timestamp="self.createdTimestamp"
                hide-image="::false">
            </won-post-header>
            <won-post-context-dropdown ng-if="self.post.get('ownNeed')"></won-post-context-dropdown>
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
                <div class="post-info__content__general__item__label" ng-show="self.post.get('type')">
                  Type
                </div>
                <div class="post-info__content__general__item__value" ng-show="self.post.get('type')">
                  {{self.labels.type[self.post.get('type')]}}{{self.post.get('matchingContexts')? ' in '+ self.post.get('matchingContexts').join(', ') : '' }}
                </div>
              </div>
              <won-post-share-link
                ng-if="!(self.post.get('state') === self.WON.InactiveCompacted || self.post.get('isWhatsAround') || self.post.get('isWhatsNew'))"
                post-uri="self.post.get('uri')">
              </won-post-share-link>
            </div>

            <won-gallery ng-if="self.post.get('hasImages')">
            </won-gallery>

            <won-post-is-or-seeks-info branch="::'is'" ng-if="self.hasIsBranch"></won-post-is-or-seeks-info>
            <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.hasIsBranch && self.hasSeeksBranch"></won-labelled-hr>
            <won-post-is-or-seeks-info branch="::'seeks'" ng-if="self.hasSeeksBranch"></won-post-is-or-seeks-info>
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
                      <span class="rdflink__label">RDF on Server</span>
              </a>
              <won-trig
                ng-if="self.post.get('jsonld')"
                jsonld="self.post.get('jsonld')">
              </won-trig>
            </div>
        </div>
        <div class="post-info__footer" ng-if="!self.isLoading()">
            <button class="won-button--filled red post-info__footer__button"
                ng-if="self.post.get('ownNeed') && self.post.get('isWhatsNew')"
                ng-click="self.createWhatsAround()"
                ng-disabled="self.pendingPublishing">
                <svg class="won-button-icon" style="--local-primary:white;">
                    <use xlink:href="#ico36_location_current" href="#ico36_location_current"></use>
                </svg>
                <span ng-if="!self.pendingPublishing">What's in your Area?</span>
                <span ng-if="self.pendingPublishing">Finding out what's going on&hellip;</span>
            </button>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      this.is = "is";
      this.seeks = "seeks";
      this.labels = labels;

      this.pendingPublishing = false;

      window.pi4dbg = this;

      const selectFromState = state => {
        const postUri = selectOpenPostUri(state);
        const openConnectionUri = selectOpenConnectionUri(state);
        const post = state.getIn(["needs", postUri]);
        const is = post ? post.get("is") : undefined;

        //TODO it will be possible to have more than one seeks
        const seeks = post ? post.get("seeks") : undefined;

        return {
          WON: won.WON,
          hasIsBranch: !!is,
          hasSeeksBranch: !!seeks,
          post,
          friendlyTimestamp:
            post &&
            relativeTime(selectLastUpdateTime(state), post.get("creationDate")),
          createdTimestamp: post && post.get("creationDate"),
          shouldShowRdf: state.get("showRdf"),
          fromConnection: !!openConnectionUri,
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.includeHeader"],
        this
      );

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    isLoading() {
      return !this.post || this.post.get("isLoading");
    }

    createWhatsAround() {
      if (!this.pendingPublishing) {
        this.pendingPublishing = true;
        this.needs__whatsAround();
      }
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
      includeHeader: "=",
    },
  };
}

export default angular
  .module("won.owner.components.postInfo", [
    postIsOrSeeksInfoModule,
    postHeaderModule,
    postShareLinkModule,
    labelledHrModule,
    postContextDropdownModule,
    trigModule,
  ])
  .directive("wonPostInfo", genComponentConf).name;
