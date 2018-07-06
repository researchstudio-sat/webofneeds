import angular from "angular";

import "ng-redux";
import won from "../won-es6.js";
import { postTitleCharacterLimit } from "config";
import { attach, clone, delay, dispatchEvent } from "../utils.js";

//TODO: can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  //"$ngRedux",
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
            placeholder="{{self.titlePlaceholder? self.titlePlaceholder : 'What? (Short title shown in lists)'}}"
            ng-blur="::self.updateTitle()"
            ng-keyup="::self.updateTitle()"/>
    </div>
    <!-- Mandatory Input Fields -->

    <!-- DETAILS Picker -->
    <div class="cis__addDetail" ng-if="self.hasDetails()">
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
        <div class="cis__detail__items" ng-if="self.showDetail">
          <div class="cis__detail__items__item" ng-repeat="detail in self.detailList">
              <!-- HEADER -->
              <div class="cis__detail__items__item__header"
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
              <div class="cis__detail__items__item__component"
                ng-click="self.onScroll({element: '.cis__details'})"
                ng-if="self.openDetail === detail.identifier"
                detail-element="{{detail.component}}"
                on-update="::self.updateDetail(identifier, value)"
                initial-value="::self.draftObject[detail.identifier]"
                identifier="detail.identifier">
              </div>
          </div>
        </div>
    </div>
    <!-- /DETAIL Picker/ -->
  `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.won = won;

      //TODO: debug; deleteme
      window.cis4dbg = this;

      this.characterLimit = postTitleCharacterLimit;
      this.details = new Set();

      this.showDetail = false;
      this.openDetail = undefined;

      delay(0).then(() => this.loadInitialDraft());
    }

    hasDetails() {
      return (
        this.detailList &&
        this.detailList !== {} &&
        Object.keys(this.detailList).length > 0
      );
    }

    loadInitialDraft() {
      this.draftObject = clone(this.initialDraft);
      for (const draftDetail in this.initialDraft) {
        this.details.add(draftDetail);
        this.draftObject[draftDetail] = this.initialDraft[draftDetail];
      }
    }

    updateDraft() {
      for (const detail in this.detailList) {
        if (!this.details.has(detail)) {
          this.draftObject[detail] = undefined;
        }
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
      detailList: "=",
      initialDraft: "=",
      onUpdate: "&", // Usage: on-update="::myCallback(draft)"
      onScroll: "&",
      titlePlaceholder: "=",
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
