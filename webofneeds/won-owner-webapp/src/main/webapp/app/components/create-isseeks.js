/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";

import "ng-redux";
import won from "../won-es6.js";
import { postTitleCharacterLimit } from "config";
import { attach, deepFreeze, clone, dispatchEvent } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { name as descriptionPickerName } from "./description-picker.tsx";

const emptyDraft = deepFreeze({
  title: "",
  type: won.WON.BasicNeedTypeCombined,
  description: "",
  tags: [],
  location: undefined,
  thumbnail: undefined,
  matchingContext: undefined,
});

// const availableDetails = {
//   description: {},
//   location : {
//     detailName: "location",
//     detailTitle: "Location",
//     detailIcon: "ico36_location_circle",
//     detailComponent: "won-location-picker",
//   },
//   tags: {},
//   ttl: {},
// };

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*, '$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <!-- Mandatory Input Fields -->
        <div class="cis__mandatory">
            <input
                type="text"
                maxlength="{{self.characterLimit}}"
                class="cis__mandatory__title won-txt"
                placeholder="What? - Short title shown in lists"
                ng-blur="::self.updateTitle()"
                ng-keyup="::self.updateTitle()"/>
        </div>
        <!-- Mandatory Input Fields -->

        <!-- DETAILS Picker -->
        <div class="cis__addDetail">
            <div class="cis__addDetail__header detailPicker clickable"
                ng-click="self.toggleDetail()"
                ng-class="{'closedDetailPicker': !self.showDetail}">
                    <span>Add more detail</span>
                    <svg class="cis__addDetail__header__carret" ng-show="!self.showDetail">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg class="cis__addDetail__header__carret" ng-show="self.showDetail">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
            </div>
            <div class="cis__detail__items" ng-if="self.showDetail" >
                <!-- DESCRIPTION PICKER -->
                <div class="cis__detail__items__item description"
                    ng-click="self.toggleOpenDetail('description')"
                    ng-class="{'picked' : self.openDetail === 'description'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('description')">
                            <use xlink:href="#ico36_description_circle" href="#ico36_description_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('description')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Description</span>
                </div>

                
                <!-- LOCATION PICKER -->
                <div class="cis__detail__items__item location"
                    ng-click="self.toggleOpenDetail('location')"
                    ng-class="{'picked' : self.openDetail === 'location'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('location')">
                            <use xlink:href="#ico36_location_circle" href="#ico36_location_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('location')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Address or Location</span>
                </div>

                <!-- TAGS PICKER -->
                <div class="cis__detail__items__item tags"
                    ng-click="self.toggleOpenDetail('tags')"
                    ng-class="{'picked' : self.openDetail === 'tags'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('tags')">
                            <use xlink:href="#ico36_tags_circle" href="#ico36_tags_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('tags')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Tags</span>
                </div>

                <!-- TTL PICKER -->
                <div class="cis__detail__items__item ttl"
                    ng-click="self.toggleOpenDetail('ttl')"
                    ng-class="{'picked' : self.openDetail === 'ttl'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('ttl')">
                            <use xlink:href="#ico36_rdf_logo_circle" href="#ico36_rdf_logo_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('ttl')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Turtle (TTL)</span>
                </div>
            </div>
        </div>
        <!-- /DETAIL Picker/ -->

        <!-- DETAILS -->
        <!-- TODO: move details into the div opened by the detail picker? -->
        <div class="cis__details" ng-if="self.showDetail">
            <!-- DESCRIPTION -->
            <won-description-picker
                ng-if="self.openDetail === 'description'"
                initial-description="::self.draftObject.description"
                on-description-updated="::self.updateDescription(description)">
            </won-description-picker>

            <!-- LOCATION -->
            <won-location-picker 
                ng-if="self.openDetail === 'location'"
                initial-location="::self.draftObject.location"
                on-location-picked="::self.updateLocation(location)">
            </won-location-picker>

            <!-- TAGS -->
            <won-tags-picker
                ng-if="self.openDetail === 'tags'"
                initial-tags="::self.draftObject.tags"
                on-tags-updated="::self.updateTags(tags)">
            </won-tags-picker>

            <!-- TTL -->
            <won-ttl-picker
              ng-if="self.openDetail === 'ttl'"
              initial-ttl="::self.draftObject.ttl"
              on-ttl-updated="::self.updateTTL(ttl)">
            </won-ttl-picker>
        </div>
        <!-- /DETAILS/ -->
`;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.won = won;

      //TODO debug; deleteme
      window.cis4dbg = this;

      this.characterLimit = postTitleCharacterLimit;

      this.openDetail = undefined;

      this.reset();

      const selectFromState = () => ({});

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    reset() {
      this.draftObject = clone(emptyDraft);
      this.details = new Set(); // remove all detail-cards

      this.showDetail = false; // and close selector
    }

    updateDraft() {
      if (!this.details.has("location")) {
        this.draftObject.location = undefined;
      }
      if (!this.details.has("tags")) {
        this.draftObject.tags = [];
      }
      if (!this.details.has("ttl")) {
        this.draftObject.ttl = undefined;
      }
      if (!this.details.has("description")) {
        this.draftObject.description = undefined;
      }

      this.onUpdate({ draft: this.draftObject });
      dispatchEvent(this.$element[0], "update", { draft: this.draftObject });
    }

    setDraft(updatedDraft) {
      Object.assign(this.draftObject, updatedDraft);
      this.updateDraft();
    }

    updateTitle() {
      const titleString = (this.titleInput() || {}).value || "";
      this.draftObject.title = titleString;
      this.updateDraft();
    }

    updateDescription(description) {
      this.draftObject.description = description;

      this.details.add("description");
    }

    updateLocation(location) {
      if (location) {
        if (!this.details.has("location")) {
          this.details.add("location");
        }
        this.draftObject.location = location;
      } else if (this.details.has("location")) {
        this.details.delete("location");
        this.draftObject.location = undefined;
      }

      this.updateDraft();
    }

    updateTags(tags) {
      if (tags && tags.length > 0) {
        if (!this.details.has("tags")) {
          this.details.add("tags");
        }
        this.draftObject.tags = tags;
      } else if (this.details.has("tags")) {
        this.details.delete("tags");
        this.draftObject.tags = [];
      }

      this.updateDraft();
    }

    updateTTL(ttl) {
      if (ttl && ttl.length > 0) {
        if (!this.details.has("ttl")) {
          this.details.add("ttl");
        }
        this.draftObject.ttl = ttl;
      } else if (this.details.has("ttl")) {
        this.details.delete("ttl");
        this.draftObject.ttl = [];
      }

      this.updateDraft();
    }

    pickImage(image) {
      this.draftObject.thumbnail = image;
    }

    toggleDetail() {
      this.showDetail = !this.showDetail;
    }

    toggleOpenDetail(detail) {
      // open clicked detail
      if (this.openDetail === detail) {
        this.openDetail = undefined;
      } else {
        this.openDetail = detail;
      }
    }

    getArrayFromSet(set) {
      return Array.from(set);
    }

    titleInputNg() {
      return angular.element(this.titleInput());
    }
    titleInput() {
      if (!this._titleInput) {
        this._titleInput = this.$element[0].querySelector(
          ".cis__mandatory__title"
        );
      }
      return this._titleInput;
    }

    descriptionInputNg() {
      return angular.element(this.descriptionInput());
    }
    descriptionInput() {
      if (!this._descriptionInput) {
        this._descriptionInput = this.$element[0].querySelector(
          ".cis__description__text"
        );
      }
      return this._descriptionInput;
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      isOrSeeks: "=",
      /*
             * Usage:
             *  on-update="::myCallback(draft)"
             */
      onUpdate: "&",
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.createIsseek", [descriptionPickerName])
  .directive("wonCreateIsseeks", genComponentConf).name;
