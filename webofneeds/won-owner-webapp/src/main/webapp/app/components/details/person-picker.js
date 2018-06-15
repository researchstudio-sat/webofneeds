import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <h1>Person Detail Picker</h1>
      <div ng-repeat="detail in self.personDetails" class="pp__detail">
          <div class="pp__detail__label">
              {{detail.name}}
          </div>
          <input
            type="text"
            id="{{detail.name}}"
            class="pp__detail__input"
            ng-model="detail.value"
            ng-keyup="::self.updateDetails(detail)"
            ng-class="{'pp__detail__input--withreset' : self.showResetButton(detail.name)}"
          />
          <svg class="pp__detail__icon clickable" 
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton(detail.name)"
            ng-click="self.resetPersonDetail(detail.name)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
      </div>
    `;

  // DONE rewrite this from tag picker to person picker
  // DONE -> Map decide on some sort of data structure
  // DONE add to create-isseeks
  // TODO: styling
  // DONE add details to draft
  // TODO: add details to RDF
  // TODO: display details in info

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.pp4dbg = this;

      this.addedPerson = new Map();
      this.personDetails = [
        { name: "value1", value: undefined },
        { name: "value2", value: undefined },
        { name: "value3", value: undefined },
        { name: "value4", value: undefined },
        { name: "value5", value: undefined },
        { name: "value6", value: undefined },
        { name: "value7", value: undefined },
      ];
      this.visibleResetButtons = new Set();

      delay(0).then(() => this.showInitialPerson());
    }

    showInitialPerson() {
      if (this.initialPerson && this.initialPerson.size > 0) {
        for (let [detail, value] of this.initialPerson.entries()) {
          this.addedPerson.set(detail, value);
          let personIndex = this.personDetails.findIndex(
            personDetail => personDetail.name === detail
          );
          if (personIndex !== -1) {
            this.personDetails[personIndex].value = value;
          }
          if (value && value.length > 0) {
            this.visibleResetButtons.add(detail);
          }
        }
      }

      this.$scope.$apply();
    }

    updateDetails(detail) {
      if (detail.value) {
        this.addedPerson.set(detail.name, detail.value);
        this.onPersonUpdated({ person: this.addedPerson });

        if (detail.value.length > 0) {
          this.visibleResetButtons.add(detail.name);
        } else {
          this.visibleResetButtons.delete(detail.name);
        }
      } else {
        this.addedPerson.set(detail.name, undefined);
        this.onPersonUpdated({ person: this.addedPerson });
        this.visibleResetButtons.delete(detail.name);
      }
    }

    showResetButton(detail) {
      return this.visibleResetButtons.has(detail);
    }

    resetPersonDetail(detail) {
      this.addedPerson.set(detail, undefined);
      this.visibleResetButtons.delete(detail);
      let personIndex = this.personDetails.findIndex(
        personDetail => personDetail.name === detail
      );
      if (personIndex !== -1) {
        this.personDetails[personIndex].value = "";
      }
      //this.textfield().value = "";
      this.onPersonUpdated({ person: this.addedPerson });
      //this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".tp__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".tp__input__inner");
    // }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onPersonUpdated: "&",
      initialPerson: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.personPicker", [])
  .directive("wonPersonPicker", genComponentConf).name;
