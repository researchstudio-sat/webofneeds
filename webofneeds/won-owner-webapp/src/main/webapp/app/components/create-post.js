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
import routePickerModule from "./details/route-picker.js";
import tagsPickerModule from "./details/tags-picker.js";
import ttlPickerModule from "./details/ttl-picker.js";
import createIsseeksModule from "./create-isseeks.js";
import { postTitleCharacterLimit } from "config";
import { getIn, attach } from "../utils.js";
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
            <won-create-isseeks ng-if="self.isPost" is-or-seeks="::'Description'" on-update="::self.updateDraft(draft, self.is)" on-scroll="::self.snapToBottom"></won-create-isseeks>
            <won-create-isseeks ng-if="self.isSearch" is-or-seeks="::'Search'" on-update="::self.updateDraft(draft, self.seeks)" on-scroll="::self.snapToBottom"></won-create-isseeks>
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

      //TODO debug; deleteme
      window.cnc = this;

      this.postTypeTexts = postTypeTexts;
      this.characterLimit = postTitleCharacterLimit;
      this.draftIs = {
        title: "",
        type: postTypeTexts[3].type,
        description: "",
        tags: undefined,
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
        location: undefined,
        travelAction: undefined,
        thumbnail: undefined,
        matchingContext: undefined,
      };

      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));
      this.draftObject = { is: this.draftIs, seeks: this.draftSeeks };

      this.is = "is";
      this.seeks = "seeks";

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
          connectionHasBeenLost: state.getIn(["messages", "lostConnection"]),
          showCreateView,
          isSearch,
          isPost,
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    snapToBottom() {
      this._snapBottom = true;
      this.scrollToBottom();
    }
    unsnapFromBottom() {
      this._snapBottom = false;
    }
    updateScrollposition() {
      if (this._snapBottom) {
        this.scrollToBottom();
      }
    }
    scrollToBottom() {
      this._programmaticallyScrolling = true;

      this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
    }
    onScroll() {
      if (!this._programmaticallyScrolling) {
        //only unsnap if the user scrolled themselves
        this.unsnapFromBottom();
      }

      const sc = this.scrollContainer();
      const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
      if (isAtBottom) {
        this.snapToBottom();
      }

      this._programmaticallyScrolling = false;
    }
    scrollContainerNg() {
      return angular.element(this.scrollContainer());
    }
    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".cp__content");
      }
      return this._scrollContainer;
    }

    isValid() {
      return (
        !this.connectionHasBeenLost &&
        (this.draftObject[this.is] || this.draftObject[this.seeks]) &&
        (this.draftObject[this.is].title ||
          this.draftObject[this.seeks].title) &&
        (this.draftObject[this.is].title.length < this.characterLimit ||
          this.draftObject[this.seeks].title.length < this.characterLimit)
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

        const tmpList = [this.is, this.seeks];
        const newObject = {
          is: this.draftObject.is,
          seeks: this.draftObject.seeks,
        };

        for (const tmp of tmpList) {
          if (newObject[tmp].title === "") {
            delete newObject[tmp];
          }
        }

        this.needs__create(
          newObject,
          this.$ngRedux.getState().getIn(["config", "defaultNodeUri"])
        );
      }
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
    routePickerModule,
    tagsPickerModule,
    ttlPickerModule,
    createIsseeksModule,
    ngAnimate,
  ])
  .directive("wonCreatePost", genComponentConf).name;
