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
            ng-click="self.reset(detail)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
      </div>
    `;

  // TODO: rewrite this from tag picker to person picker
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
      // this.showResetButton = false;

      delay(0).then(() => this.showInitialPerson());
    }

    showInitialPerson() {
      // let _tagsForTextfield = "";

      // TODO: if person is an object, go through all values and display them
      // TODO: add textfields for every single detail?
      if (this.initialPerson && this.initialPerson.size > 0) {
        for (let [detail, value] of this.initialPerson.entries()) {
          this.addedPerson.set(detail, value);
        }
      }
      //this.textfield().value = _tagsForTextfield.trim();

      this.$scope.$apply();
    }

    updateDetails(detail) {
      if (detail.value) {
        this.addedPerson.set(detail.name, detail.value);
        this.onPersonUpdated({ person: this.addedPerson });

        console.log("detail name: ", detail.name);
        console.log("detail value: ", detail.value);
        console.log("detail.value.length: ", detail.value.length);
        if (detail.value.length > 0) {
          this.visibleResetButtons.add(detail.name);
          console.log(this.visibleResetButtons);
        } else {
          this.visibleResetButtons.delete(detail.name);
        }
      }
    }

    // updateTags() {
    //   // TODO: do something with text that does not start with #
    //   const text = this.textfield().value;

    //   if (text && text.trim().length > 0) {
    //     this.addedPerson = extractHashtags(text);
    //     this.onPersonUpdated({ person: this.addedPerson });
    //     this.showResetButton = true;
    //   } else {
    //     this.resetPerson();
    //   }
    // }

    showResetButton(detail) {
      return this.visibleResetButtons.has(detail);
    }

    resetPerson() {
      this.addedPerson = undefined;
      //this.textfield().value = "";
      this.onPersonUpdated({ person: undefined });
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
