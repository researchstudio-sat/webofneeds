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
});

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
        <!-- DETAIL TOGGLES -->
        <div class="cis__detail__items" ng-if="self.showDetail" ng-repeat="detail in self.availableDetails">
            <div class="cis__detail__items__item"
                ng-click="self.toggleOpenDetail(detail.identifier)"
                ng-class="{'picked' : self.openDetail === detail.identifier}">
                <svg class="cis__circleicon" ng-show="!self.details.has(detail.identifier)">
                    <use xlink:href={{detail.icon}} href={{detail.icon}}></use>
                </svg>
                <svg class="cis__circleicon" ng-show="self.details.has(detail.identifier)">
                    <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                </svg>
                <span>{{detail.label}}</span>
            </div>

            <!-- COMPONENT -->
            <div 
              ng-click="self.onScroll({element: '.cis__details'})"
              ng-if="self.openDetail === detail.identifier"
              detail-element="{{detail.component}}"
              on-update="::self.updateDetail(identifier, value)"
              initial-value="::self.draftObject[detail.identifier]"
              identifier="detail.identifier">
            </div>
        </div>
    </div>
    <!-- /DETAIL Picker/ -->
  `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.won = won;

      // TODO: read this from config, see #1969 and subissues
      this.availableDetails = {
        description: {
          identifier: "description",
          label: "Description",
          icon: "#ico36_description_circle",
          component: "won-description-picker",
        },
        location: {
          identifier: "location",
          label: "Location",
          icon: "#ico36_location_circle",
          component: "won-location-picker",
        },
        person: {
          identifier: "person",
          label: "Person",
          icon: "#ico36_person_single_circle",
          component: "won-person-picker",
        },
        route: {
          identifier: "travelAction",
          label: "Route (From - To)",
          icon: "#ico36_location_circle", // TODO: find/create better icon
          component: "won-route-picker",
        },
        tags: {
          identifier: "tags",
          label: "Tags",
          icon: "#ico36_tags_circle",
          component: "won-tags-picker",
        },
        ttl: {
          identifier: "ttl",
          label: "Turtle (TTL)",
          icon: "#ico36_rdf_logo_circle",
          component: "won-ttl-picker",
        },
      };
      console.log(this.availableDetails);

      //TODO: debug; deleteme
      window.cis4dbg = this;

      this.characterLimit = postTitleCharacterLimit;

      this.openDetail = undefined;

      this.reset();

      //this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));
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
      // TODO: this should use a detail list instead
      if (!this.details.has("description")) {
        this.draftObject.description = undefined;
      }
      if (!this.details.has("location")) {
        this.draftObject.location = undefined;
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

    updateScroll() {
      // FIXME: broken due to HTML changes
      // console.log("Scoll activity");
      // this.onScroll();
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
  // this directive creates detail picker components with callbacks
  .directive("detailElement", [
    "$compile",
    function($compile) {
      return {
        restrict: "A",
        scope: {
          onUpdate: "&",
          initialValue: "=",
          identifier: "=",
        },
        link: function(scope, element, attrs) {
          const customTag = attrs.detailElement;
          if (!customTag) return;

          const customElem = angular.element(
            `<${customTag} initial-value="initialValue" on-update="internalUpdate(value)"></${customTag}>`
          );
          //customElem.attr("on-update", scope.onUpdate);

          scope.internalUpdate = function(value) {
            scope.onUpdate({
              identifier: scope.identifier,
              value: value,
            });
          };
          element.append($compile(customElem)(scope));
        },
      };
    },
  ])
  .directive("wonCreateIsseeks", genComponentConf).name;
