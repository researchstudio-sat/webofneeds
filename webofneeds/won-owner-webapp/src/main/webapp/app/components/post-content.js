/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postIsOrSeeksInfoModule from "./post-is-or-seeks-info.js";
import labelledHrModule from "./labelled-hr.js";
import postContentGeneral from "./post-content-general.js";
import trigModule from "./trig.js";
import { attach } from "../utils.js";
import won from "../won-es6.js";
import { labels } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectOpenConnectionUri } from "../selectors/selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_post-content.scss";
import "style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="post-skeleton" ng-if="self.isLoading()">
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
        <div class="post-content" ng-if="!self.isLoading()">
          <won-post-content-general post-uri="self.post.get('uri')"></won-post-content-general>

          <won-gallery ng-if="self.post.get('hasImages')">
          </won-gallery>

          <!-- SEARCH STRING -->
          <won-title-viewer
            ng-if="self.isPureSearch && self.searchString"
            content="self.searchString"
            detail="::{ label: 'Searching for' }">
          </won-title-viewer>

          <!-- DETAIL INFORMATION -->
          <won-post-is-or-seeks-info branch="::'is'" ng-if="self.hasIsBranch" post-uri="self.post.get('uri')"></won-post-is-or-seeks-info>
          <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.hasIsBranch && self.hasSeeksBranch"></won-labelled-hr>
          <won-post-is-or-seeks-info branch="::'seeks'" ng-if="self.hasSeeksBranch && !self.isPureSearch" post-uri="self.post.get('uri')"></won-post-is-or-seeks-info>
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

      this.is = "is";
      this.seeks = "seeks";
      this.labels = labels;

      window.postcontent4dbg = this;

      const selectFromState = state => {
        const openConnectionUri = selectOpenConnectionUri(state);
        const post = state.getIn(["needs", this.postUri]);
        const is = post ? post.get("is") : undefined;

        //TODO it will be possible to have more than one seeks
        const seeks = post ? post.get("seeks") : undefined;

        const isPureSearch =
          post &&
          is === undefined &&
          seeks === undefined &&
          post.get("searchString");

        const searchString = post ? post.get("searchString") : undefined; //workaround to display searchString only in seeks

        return {
          WON: won.WON,
          hasIsBranch: !!is,
          hasSeeksBranch: !!seeks,
          post,
          createdTimestamp: post && post.get("creationDate"),
          shouldShowRdf: state.get("showRdf"),
          fromConnection: !!openConnectionUri,
          openConnectionUri,
          isPureSearch: isPureSearch,
          searchString: searchString,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    isLoading() {
      return !this.post || this.post.get("isLoading");
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
    postIsOrSeeksInfoModule,
    labelledHrModule,
    postContentGeneral,
    trigModule,
  ])
  .directive("wonPostContent", genComponentConf).name;
