/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import imageDropzoneModule from "./image-dropzone.js";
import descriptionPickerModule from "./details/description-picker.js";
import locationPickerModule from "./details/location-picker.js";
import matchingContextPicker from "./details/matching-context-picker.js";
import personPickerModule from "./details/person-picker.js";
import routePickerModule from "./details/route-picker.js";
import tagsPickerModule from "./details/tags-picker.js";
import ttlPickerModule from "./details/ttl-picker.js";
import createIsseeksModule from "./create-isseeks.js";
import { postTitleCharacterLimit } from "config";
import { get, getIn, attach, deepFreeze } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import won from "../won-es6.js";
import { connect2Redux } from "../won-utils.js";

const postTypeTexts = [
  {
    type: won.WON.BasicNeedTypeDemand,
    text: "I'm looking for",
    helpText:
      "Select this if you are looking for things or services other people offer",
  },
  {
    type: won.WON.BasicNeedTypeSupply,
    text: "I'm offering",
    helpText:
      "Use this if you are offering an item or a service. You will find people who said" +
      " that they're looking for something.",
  },
  {
    type: won.WON.BasicNeedTypeDotogether,
    text: "I want to find someone to",
    helpText:
      "Select this if you are looking to find other people who share your interest. You will be matched" +
      " with other people who chose this option as well.",
  },
  {
    type: won.WON.BasicNeedTypeCombined,
    text: "I'm offering and looking for",
    helpText:
      "Select this if you are looking for things or services other people offer + Use this if you are offering an item or a service. You will find people who said" +
      " that they're looking for something.",
  },
];

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

//All deatils, except tags, because tags are saved in an array
const keySet = deepFreeze(
  new Set([
    "description",
    "location",
    "matchingContext",
    "thumbnail",
    "travelAction",
    "ttl",
  ])
);

function genComponentConf() {
  const template = `
        <div class="cp__header">
            <a class="cp__header__back clickable show-in-responsive"
                ng-click="self.router__stateGoCurrent({showCreateView: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="cp__header__back__icon">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <span class="cp__header__title" ng-if="self.isPost">Post</span>
            <span class="cp__header__title" ng-if="self.isSearch">Search</span>
        </div>
        <div class="cp__content">
            <won-create-isseeks ng-if="self.isPost" is-or-seeks="::'Description'" on-update="::self.updateDraft(draft, 'is')" on-scroll="::self.scrollToBottom(element)"></won-create-isseeks>
            <won-create-isseeks ng-if="self.isSearch" is-or-seeks="::'Search'" on-update="::self.updateDraft(draft, 'seeks')" on-scroll="::self.scrollToBottom(element)"></won-create-isseeks>
            <!-- TODO: decide on whether to re-add stuff like an additional search/description window -->
            <won-labelled-hr label="::'done?'" class="cp__content__labelledhr show-in-responsive"></won-labelled-hr>
            <button type="submit" class="won-button--filled red cp__content__publish show-in-responsive"
                    ng-disabled="!self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
        <div class="cp__footer hide-in-responsive" >
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <button type="submit" class="won-button--filled red cp__footer__publish"
                    ng-disabled="!self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      this.SEARCH = "search";
      this.POST = "post";

      this.focusedElement = null;
      //TODO debug; deleteme
      window.cnc4dbg = this;

      this.postTypeTexts = postTypeTexts;
      this.characterLimit = postTitleCharacterLimit;
      this.draftIs = {
        title: "",
        type: postTypeTexts[3].type,
        description: "",
        tags: undefined,
        person: undefined,
        location: undefined,
        travelAction: undefined,
        thumbnail: undefined,
        matchingContext: undefined,
      };
      this.draftSeeks = {
        title: "",
        type: postTypeTexts[3].type,
        description: "",
        tags: undefined,
        person: undefined,
        location: undefined,
        travelAction: undefined,
        thumbnail: undefined,
        matchingContext: undefined,
      };

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));
      this.draftObject = { is: this.draftIs, seeks: this.draftSeeks };

      this.pendingPublishing = false;
      this.details = { is: [], seeks: [] };
      this.isNew = true;

      const selectFromState = state => {
        const showCreateView = getIn(state, [
          "router",
          "currentParams",
          "showCreateView",
        ]);
        const isSearch = showCreateView === this.SEARCH;
        const isPost = showCreateView && !isSearch;

        return {
          connectionHasBeenLost:
            state.getIn(["messages", "reconnecting"]) ||
            state.getIn(["messages", "lostConnection"]),
          showCreateView,
          isSearch,
          isPost,
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    onResize() {
      //TODO: delete
      //console.log("ResizeEvent: ", window.screen.height);
      if (this.focusedElement) {
        if (this.windowHeight < window.screen.height) {
          this.windowHeight < window.screen.height;
          this.scrollToBottom(this.focusedElement);
        } else {
          this.windowHeight = window.screen.height;
        }
      }
    }

    scrollToBottom(element) {
      this._programmaticallyScrolling = true;

      if (element) {
        let heightHeader =
          this.$element[0].querySelector(".cp__header").offsetHeight + 10;
        let scrollTop = this.$element[0].querySelector(element).offsetTop;
        this.scrollContainer().scrollTop = scrollTop - heightHeader;

        this.focusedElement = element;
        //TODO: debug: delete me
        //console.log("ScrollTop: ", this.scrollContainer().scrollTop);
      }
    }

    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".cp__content");
      }
      return this._scrollContainer;
    }

    isValid() {
      const draft = this.draftObject;
      const hasContent = get(draft, "is") || get(draft, "seeks");
      const title =
        getIn(draft, ["is", "title"]) || getIn(draft, ["seeks", "title"]);
      const hasValidTitle = title && title.length < this.characterLimit;
      const hasTTL =
        getIn(draft, ["is", "ttl"]) || getIn(draft, ["seeks", "ttl"]);
      return (
        !this.connectionHasBeenLost && hasContent && (hasValidTitle || hasTTL)
      );
    }

    updateDraft(updatedDraft, isSeeks) {
      if (this.isNew) {
        this.isNew = false;
      }

      this.draftObject[isSeeks] = updatedDraft;
    }

    publish() {
      // Post both needs
      if (!this.pendingPublishing) {
        this.pendingPublishing = true;
        this.needs__create(
          this.draftObject,
          this.$ngRedux.getState().getIn(["config", "defaultNodeUri"])
        );
      }
    }

    hasSearchString(object) {
      if (object.is || !object.seeks) {
        return false;
      } else {
        for (const key of keySet) {
          if (object.seeks[key]) {
            return false;
          }
        }
        // Handle tags list
        if (object.seeks.tags.length > 0) {
          return false;
        }
      }
      return true;
    }

    createWhatsNew() {
      if (!this.pendingPublishing) {
        this.pendingPublishing = true;
        this.needs__whatsNew();
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
      /*scope-isolation*/
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.createPost", [
    labelledHrModule,
    imageDropzoneModule,
    descriptionPickerModule,
    locationPickerModule,
    matchingContextPicker,
    personPickerModule,
    routePickerModule,
    tagsPickerModule,
    ttlPickerModule,
    createIsseeksModule,
    ngAnimate,
  ])
  .directive("wonCreatePost", genComponentConf).name;
