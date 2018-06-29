/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import imageDropzoneModule from "./image-dropzone.js";
import matchingContextModule from "./details/matching-context-picker.js"; // TODO: should be renamed
import createIsseeksModule from "./create-isseeks.js";
import { postTitleCharacterLimit } from "config";
import {
  get,
  getIn,
  attach,
  //  deepFreeze,
  delay,
} from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import won from "../won-es6.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";

// TODO: these should be replaced by importing defintions from config
import descriptionPickerModule from "./details/description-picker.js";
import locationPickerModule from "./details/location-picker.js";
import personPickerModule from "./details/person-picker.js";
import routePickerModule from "./details/route-picker.js";
import tagsPickerModule from "./details/tags-picker.js";
import ttlPickerModule from "./details/ttl-picker.js";

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
// const keySet = deepFreeze(
//   new Set([
//     "description",
//     "location",
//     "matchingContext",
//     "thumbnail",
//     "travelAction",
//     "ttl",
//   ])
// );

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

            <!-- ADD TITLE AND DETAILS -->
            <won-create-isseeks 
                ng-if="self.isPost" 
                is-or-seeks="::'Description'" 
                on-update="::self.updateDraft(draft, 'is')" 
                on-scroll="::self.scrollToBottom(element)">
            </won-create-isseeks>
            <won-create-isseeks 
                ng-if="self.isSearch" 
                is-or-seeks="::'Search'" 
                on-update="::self.updateDraft(draft, 'seeks')" 
                on-scroll="::self.scrollToBottom(element)">
            </won-create-isseeks>

            <!-- TUNE MATCHING -->
            <!-- 
              <won-labelled-hr label="::'tune matching?'" class="cp__content__labelledhr">
              </won-labelled-hr>
            -->
            <div class="cp__content__tuning">
                <div class="cp__content__tuning__title b detailPicker clickable"
                    ng-click="self.toggleTuningOptions()"
                    ng-class="{'closedDetailPicker': !self.showTuningOptions}">
                    <span>Tune Matching Behaviour</span>
                    <svg class="cp__content__tuning__title__carret" ng-show="!self.showTuningOptions">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg class="cp__content__tuning__title__carret" ng-show="self.showTuningOptions">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                </div>
                <div class="cp__content__tuning_matching-context">
                    <won-matching-context-picker
                      ng-if="self.showTuningOptions"
                      default-matching-context="::self.defaultMatchingContext"
                      initial-matching-context="::self.draftObject.matchingContext"
                      on-matching-context-updated="::self.updateMatchingContext(matchingContext)">
                    </won-matching-context-picker>
                </div>
            </div>

            <!-- PUBLISH BUTTON - RESPONSIVE MODE -->
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
        <!-- PUBLISH BUTTON - NON-RESPONSIVE MODE -->
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
        title: undefined,
        type: postTypeTexts[3].type, // TODO: do we use this information anywhere?
        // description: undefined,
        // tags: undefined,
        // person: undefined,
        // location: undefined,
        // travelAction: undefined,
        // thumbnail: undefined,
      };
      this.draftSeeks = {
        title: undefined,
        type: postTypeTexts[3].type,
        // description: undefined,
        // tags: undefined,
        // person: undefined,
        // location: undefined,
        // travelAction: undefined,
        // thumbnail: undefined,
      };

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));
      this.draftObject = {
        is: this.draftIs,
        seeks: this.draftSeeks,
        matchingContext: undefined,
      };

      this.showTuningOptions = false;

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

        // needed to be able to reset matching context to default
        // TODO: is there an easier way to do this?
        const defaultMatchingContextList = getIn(state, [
          "config",
          "theme",
          "defaultContext",
        ]);
        const defaultMatchingContext = defaultMatchingContextList
          ? defaultMatchingContextList.toJS()
          : [];

        return {
          connectionHasBeenLost: !selectIsConnected(state),
          showCreateView,
          isSearch,
          isPost,
          defaultMatchingContext: defaultMatchingContext,
        };
      };

      // TODO: think about how to deal with contexts predefined in usecases
      delay(0).then(() =>
        this.updateMatchingContext(this.defaultMatchingContext)
      );

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

    toggleTuningOptions() {
      // if (!this.showTuningOptions) {
      //   this.onScroll({ element: ".cis__addDetail__header.b" });
      // }
      this.showTuningOptions = !this.showTuningOptions;
    }

    isValid() {
      const draft = this.draftObject;
      const hasContent = get(draft, "is") || get(draft, "seeks");
      const hasValidIs = this.isOrSeeksIsValid(get(draft, "is"));
      const hasValidSeeks = this.isOrSeeksIsValid(get(draft, "seeks"));
      return (
        !this.connectionHasBeenLost &&
        hasContent &&
        (hasValidIs || hasValidSeeks)
      );
    }

    updateMatchingContext(matchingContext) {
      // also accepts []!
      if (matchingContext) {
        this.draftObject.matchingContext = matchingContext;
      } else {
        this.draftObject.matchingContext = undefined;
      }
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

        // TODO: This should be a usecase/done via a different component
        // Check if this is a search
        const searchString = this.checkForSearchString(this.draftObject);
        if (searchString) {
          this.draftObject.searchString = searchString;
          //delete this.draftObject.is;
        }

        const draft = this.getPublishObject(this.draftObject);

        this.needs__create(
          draft,
          this.$ngRedux.getState().getIn(["config", "defaultNodeUri"])
        );
      }
    }

    getPublishObject(draft) {
      if (!this.isOrSeeksIsValid(draft.is)) {
        delete draft.is;
      }
      if (!this.isOrSeeksIsValid(draft.seeks)) {
        delete draft.seeks;
      }
      return draft;
    }

    // returns true if the part has a valid title or any other detail
    isOrSeeksIsValid(isOrSeeks) {
      if (!isOrSeeks) {
        return false;
      }

      const title = get(isOrSeeks, "title");
      const hasValidTitle = title && title.length < this.characterLimit;

      let hasDetail = false;
      const details = Object.keys(isOrSeeks);
      for (let d of details) {
        if (isOrSeeks[d] && d !== "type") {
          hasDetail = true;
        }
      }
      return !!(hasValidTitle || hasDetail);
    }

    checkForSearchString(draft) {
      // draft has an is part -> not a pure search
      if (this.isOrSeeksIsValid(draft.is)) {
        return undefined;
      }

      // draft has no valid seeks part -> not a pure search
      if (!draft.seeks || !draft.seeks.title) {
        return undefined;
      }

      for (let detail of Object.keys(draft.seeks)) {
        if (detail !== "title" && detail !== "type" && draft.seeks[detail]) {
          return undefined;
        }
      }

      return draft.seeks.title;
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
    personPickerModule,
    routePickerModule,
    tagsPickerModule,
    ttlPickerModule,
    createIsseeksModule,
    matchingContextModule,
    ngAnimate,
  ])
  .directive("wonCreatePost", genComponentConf).name;
