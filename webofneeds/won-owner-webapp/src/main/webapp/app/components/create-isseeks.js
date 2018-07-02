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

const emptyDraft = deepFreeze({
  title: "",
  type: won.WON.BasicNeedTypeCombined,
  description: "",
  tags: undefined,
  location: undefined,
  travelAction: undefined,
  thumbnail: undefined,
  matchingContext: undefined,
});

// const availableDetails = {
//   description: {
//     name: "description",
//   },
//   location: {
//     name: "location",
//     title: "Location",
//     icon: "ico36_location_circle",
//     component: "won-location-picker", // put all the html here?
//   },
//   person: {},
//   route: {},
//   tags: {},
//   ttl: {},
// };

//TODO: can't inject $scope with the angular2-router, preventing redux-cleanup
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
            <div class="cis__addDetail__header a detailPicker clickable"
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
                        <span>Location</span>
                </div>

                <!-- PERSON PICKER -->
                <div class="cis__detail__items__item person"
                ng-click="self.toggleOpenDetail('person')"
                ng-class="{'picked' : self.openDetail === 'person'}">
                    <svg class="cis__circleicon" ng-show="!self.details.has('person')">
                        <!-- TODO: create and use a better icon -->
                        <use xlink:href="#ico36_person_single_circle" href="#ico36_person_single_circle"></use>
                    </svg>
                    <svg class="cis__circleicon" ng-show="self.details.has('person')">
                        <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                    </svg>
                    <span>Person</span>
                </div>

                <!-- ROUTE PICKER -->
                <div class="cis__detail__items__item route"
                    ng-click="self.toggleOpenDetail('travelAction')"
                    ng-class="{'picked' : self.openDetail === 'travelAction'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('travelAction')">
                            <use xlink:href="#ico36_location_circle" href="#ico36_location_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('travelAction')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Route (From - To)</span>
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
              ng-click="self.onScroll({element: '.cis__details'})"
              ng-if="self.openDetail === 'description'"
              initial-description="::self.draftObject.description"
              on-description-updated="::self.updateDetail('description', description)">
            </won-description-picker>

            <!-- LOCATION -->
            <won-location-picker
                ng-click="self.onScroll({element: '.cis__details'})"
                ng-if="self.openDetail === 'location'"
                initial-location="::self.draftObject.location"
                on-location-updated="::self.updateDetail('location', location)">
            </won-location-picker>

            <!-- PERSON -->
            <won-person-picker 
              ng-if="self.openDetail === 'person'"
              initial-person="::self.draftObject.person"
              on-person-updated="::self.updateDetail('person', person)">
            </won-person-picker>

            <!-- ROUTE -->
            <won-route-picker
              ng-click="self.onScroll({element: '.cis__details'})"
              ng-if="self.openDetail === 'travelAction'"
              initial-travel-action="::self.draftObject.travelAction"
              on-route-updated="::self.updateDetail('travelAction', travelAction)">
            </won-route-picker>

            <!-- TAGS -->
            <won-tags-picker
                ng-click="self.onScroll({element: '.cis__details'})"
                ng-click="self.onScroll()"
                ng-if="self.openDetail === 'tags'"
                initial-tags="::self.draftObject.tags"
                on-tags-updated="::self.updateDetail('tags', tags)">
            </won-tags-picker>

            <!-- TTL -->
            <won-ttl-picker
              ng-click="self.onScroll({element: '.cis__details'})"
              ng-click="self.onScroll()"
              ng-if="self.openDetail === 'ttl'"
              initial-ttl="::self.draftObject.ttl"
              on-ttl-updated="::self.updateDetail('ttl', ttl)">
            </won-ttl-picker>
        </div>
        <!-- /DETAILS/ -->
        
        <!-- MATCHING CONTEXT PICKER -->
        <div class="cis__addDetail">
            <div class="cis__addDetail__header b detailPicker clickable"
                ng-click="self.toggleMatchingContext()"
                ng-class="{'closedDetailPicker': !self.showMatchingContext}">
                <span>Tune Matching Behaviour</span>
                <svg class="cis__addDetail__header__carret" ng-show="!self.showMatchingContext">
                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                </svg>
                <svg class="cis__addDetail__header__carret" ng-show="self.showMatchingContext">
                    <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
            </div>
            <div class="cis__detail__matching-context">
              <won-matching-context-picker
                ng-click="self.onScroll({element: '.cis__detail__matching-context'})"
                ng-if="self.showMatchingContext"
                default-matching-context="::self.defaultMatchingContext"
                initial-matching-context="::self.draftObject.matchingContext"
                on-matching-context-updated="::self.updateMatchingContext(matchingContext)">
              </won-matching-context-picker>
            </div>
        </div>`;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.won = won;

      //TODO: debug; deleteme
      window.cis4dbg = this;

      this.characterLimit = postTitleCharacterLimit;
      // TODO: check if this is a good way to do this.
      this.defaultMatchingContextList = this.$ngRedux
        .getState()
        .getIn(["config", "theme", "defaultContext"]);
      this.defaultMatchingContext = this.defaultMatchingContextList
        ? this.defaultMatchingContextList.toJS()
        : [];

      this.openDetail = undefined;

      this.reset();

      if (
        this.defaultMatchingContext &&
        this.defaultMatchingContext.length > 0
      ) {
        this.details.add("matching-context");
        this.draftObject.matchingContext = this.defaultMatchingContext;
        //this.updateDraft();
      }

      //this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));
      const selectFromState = () => ({});

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    reset() {
      this.draftObject = clone(emptyDraft);
      this.details = new Set(); // remove all detail-cards

      this.showDetail = false; // and close selector
      this.showMatchingContext = false;
    }

    updateDraft() {
      // TODO: this should use a detail list instead
      if (!this.details.has("description")) {
        this.draftObject.description = undefined;
      }
      if (!this.details.has("location")) {
        this.draftObject.location = undefined;
      }
      if (!this.details.has("matching-context")) {
        this.draftObject.matchingContext = undefined;
      }
      if (!this.details.has("person")) {
        this.draftObject.person = undefined;
      }
      if (!this.details.has("travelAction")) {
        this.draftObject.travelAction = undefined;
      }
      if (!this.details.has("tags")) {
        this.draftObject.tags = undefined;
      }
      if (!this.details.has("ttl")) {
        this.draftObject.ttl = undefined;
      }

      this.onUpdate({ draft: this.draftObject });
      dispatchEvent(this.$element[0], "update", { draft: this.draftObject });
    }

    setDraft(updatedDraft) {
      Object.assign(this.draftObject, updatedDraft);
      this.updateDraft();
    }

    updateTitle() {
      const titleString = ((this.titleInput() || {}).value || "").trim();
      this.draftObject.title = titleString;
      this.updateDraft();
    }

    updateDetail(name, value) {
      if (value) {
        if (!this.details.has(name)) {
          this.details.add(name);
        }
        this.draftObject[name] = value;
      } else if (this.details.has(name)) {
        this.details.delete(name);
        this.draftObject[name] = undefined;
      }

      this.updateDraft();
    }

    // works differently to other details and should not be lumped in with them
    updateMatchingContext(matchingContext) {
      // also accepts []!
      if (matchingContext) {
        if (!this.details.has("matching-context")) {
          this.details.add("matching-context");
        }
        this.draftObject.matchingContext = matchingContext;
      } else if (this.details.has("matching-context")) {
        this.details.delete("matching-context");
        this.draftObject.matchingContext = undefined;
      }

      this.updateDraft();
    }

    updateScroll() {
      console.log("Scoll activity");
      this.onScroll();
    }

    pickImage(image) {
      this.draftObject.thumbnail = image;
    }

    toggleDetail() {
      if (!this.showDetail) {
        this.onScroll({ element: ".cis__addDetail__header.a" });
      }
      this.showDetail = !this.showDetail;
    }

    toggleMatchingContext() {
      if (!this.showMatchingContext) {
        this.onScroll({ element: ".cis__addDetail__header.b" });
      }
      this.showMatchingContext = !this.showMatchingContext;
    }

    toggleOpenDetail(detail) {
      // open clicked detail
      if (this.openDetail === detail) {
        this.openDetail = undefined;
      } else {
        this.openDetail = detail;
        //this.onScroll({ element: ".cis__addDetail__header.a" });
        this.onScroll({ element: ".cis__details" });
      }
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
      onScroll: "&",
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.createIsseek", [])
  .directive("wonCreateIsseeks", genComponentConf).name;
