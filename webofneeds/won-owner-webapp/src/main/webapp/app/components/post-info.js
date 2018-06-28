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
import { relativeTime } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectOpenPostUri, selectLastUpdateTime } from "../selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="post-info__header" ng-if="self.includeHeader">
            <a class="post-info__header__back clickable show-in-responsive"
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
            <won-gallery ng-show="self.post.get('hasImages')">
            </won-gallery>

            <!-- GENERAL Part -->
            <h2 class="post-info__heading" ng-show="self.friendlyTimestamp">
                Created
            </h2>
            <p class="post-info__details" ng-show="self.friendlyTimestamp">
                {{ self.friendlyTimestamp }}
            </p>
            <won-post-is-or-seeks-info is-or-seeks-part="self.isPart" ng-if="self.isPart"></won-post-is-or-seeks-info>
            <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.isPart && self.seeksPart"></won-labelled-hr>
            <won-post-is-or-seeks-info is-or-seeks-part="self.seeksPart" ng-if="self.seeksPart"></won-post-is-or-seeks-info>
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
            <won-post-share-link
                ng-if="!(self.post.get('state') === self.WON.InactiveCompacted || self.post.get('isWhatsAround') || self.post.get('isWhatsNew'))"
                post-uri="self.post.get('uri')">
            </won-post-share-link>
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

      this.pendingPublishing = false;

      window.pi4dbg = this;

      const selectFromState = state => {
        const postUri = selectOpenPostUri(state);
        const post = state.getIn(["needs", postUri]);
        const is = post ? post.get("is") : undefined;

        //TODO it will be possible to have more than one seeks
        const seeks = post ? post.get("seeks") : undefined;

        return {
          WON: won.WON,
          isPart: is
            ? {
                postUri: postUri,
                isOrSeeks: is,
                isString: "is",
                person: is && is.get("person"),
                location: is && is.get("location"),
                address:
                  is.get("location") && is.get("location").get("address"),
                travelAction: is && is.get("travelAction"),
                fromAddress:
                  is.get("travelAction") &&
                  is.get("travelAction").get("fromAddress"),
                toAddress:
                  is.get("travelAction") &&
                  is.get("travelAction").get("toAddress"),
              }
            : undefined,
          seeksPart: seeks
            ? {
                postUri: postUri,
                isOrSeeks: seeks,
                seeksString: "seeks",
                location: seeks && seeks.get("location"),
                person: seeks && seeks.get("person"),
                hasSearchString: !!seeks && seeks.get("searchString"),
                address:
                  seeks.get("location") && seeks.get("location").get("address"),
                travelAction: seeks && seeks.get("travelAction"),
                fromAddress:
                  seeks.get("travelAction") &&
                  seeks.get("travelAction").get("fromAddress"),
                toAddress:
                  seeks.get("travelAction") &&
                  seeks.get("travelAction").get("toAddress"),
              }
            : undefined,
          post,
          friendlyTimestamp:
            post &&
            relativeTime(selectLastUpdateTime(state), post.get("creationDate")),
          createdTimestamp: post && post.get("creationDate"),
          shouldShowRdf: state.get("showRdf"),
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
