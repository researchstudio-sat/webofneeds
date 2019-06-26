import angular from "angular";

import "ng-redux";
import won from "../won-es6.js";
import { clone, delay, dispatchEvent } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";

import "~/style/_create-isseeks.scss";

//TODO: can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  //"$ngRedux",
  "$scope",
  "$element" /*, '$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
    <!-- DETAIL TOGGLES -->
    <div class="cis__detail__items">
      <div class="cis__detail__items__item" ng-repeat="detail in self.detailList"
          ng-if="detail.component"
          ng-class="{
            'cis__detail__items__item--won-expanded': self.openDetail === detail.identifier,
            'cis__detail__items__item--won-hasvalue': self.details.has(detail.identifier),
          }">
          <!-- HEADER -->
          <div class="cis__detail__items__item__header"
              ng-class="{
                'cis__detail__items__item__header--won-showvalue': self.details.has(detail.identifier) && self.openDetail !== detail.identifier,
                'cis__detail__items__item__header--won-showmandatoryindicator': detail.mandatory && !(self.details.has(detail.identifier) || self.openDetail === detail.identifier),
              }"
              ng-click="self.toggleOpenDetail($event, detail.identifier)">
              <svg class="cis__circleicon">
                  <use xlink:href={{detail.icon}} href={{detail.icon}}></use>
              </svg>
              <div class="cis__detail__items__item__header__label">
                {{detail.label}}
              </div>
              <div class="cis__detail__items__item__header__content"
                ng-if="self.details.has(detail.identifier) && self.openDetail !== detail.identifier">
                {{ self.generateHumanReadable(detail) }}
              </div>
              <div class="cis__mandatory" ng-if="detail.mandatory && !(self.details.has(detail.identifier) || self.openDetail === detail.identifier)" title="This is a mandatory Detail">
                <svg class="cis__mandatory__icon">
                    <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
                </svg>
              </div>
          </div>

          <!-- COMPONENT -->
          <div class="cis__detail__items__item__component"
            ng-if="self.openDetail === detail.identifier"
            detail-element="{{detail.component}}"
            on-update="::self.updateDetail(identifier, value)"
            initial-value="::self.draftObject[detail.identifier]"
            identifier="detail.identifier"
            detail="detail">
          </div>
      </div>
    </div>
  `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.won = won;

      //TODO: debug; deleteme
      window.cis4dbg = this;

      this.details = new Set();

      this.openDetail = undefined;

      delay(0).then(() => this.loadInitialDraft());
    }

    generateHumanReadable(detail) {
      return detail.generateHumanReadable({
        value: this.draftObject[detail.identifier],
        includeLabel: false,
      });
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

    pickImage(image) {
      this.draftObject.thumbnail = image;
    }

    toggleOpenDetail($event, detail) {
      // open clicked detail
      if (this.openDetail === detail) {
        this.openDetail = undefined;
      } else {
        this.openDetail = detail;
        const target = $event.currentTarget.parentElement;
        setTimeout(() => {
          this.onScroll({ element: target });
        }, 0);
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
      detailList: "=",
      initialDraft: "=",
      onUpdate: "&", // Usage: on-update="::myCallback(draft)"
      onScroll: "&",
    },
    template: template,
  };
}

export default //.controller('CreateAtomController', [...serviceDependencies, CreateAtomController])
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
          detail: "=",
        },
        link: function(scope, element, attrs) {
          const customTag = attrs.detailElement;
          if (!customTag) return;

          const customElem = angular.element(
            `<${customTag} initial-value="initialValue" on-update="internalUpdate(value)" detail="detail"></${customTag}>`
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
