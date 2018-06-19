import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";
import Immutable from "immutable";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <h1>Person Detail Picker</h1>
      <div ng-repeat="detail in self.personDetails" class="pp__detail">
          <div class="pp__detail__label">
              {{detail.fieldname}}
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
  // DONE add details to RDF
  // TODO: display details in info

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.pp4dbg = this;

      // TODO: require name!
      this.addedPerson = Immutable.Map();
      // Map([
      //   ["name", undefined],
      //   ["title", undefined],
      //   ["company", undefined],
      //   ["position", undefined],
      //   ["skills", undefined],
      //   ["bio", undefined],
      // ]);
      this.personDetails = [
        { fieldname: "Name", name: "name", value: undefined }, // s:name
        { fieldname: "Academic Title", name: "title", value: undefined },
        { fieldname: "Company", name: "company", value: undefined },
        { fieldname: "Position", name: "position", value: undefined },
        { fieldname: "Skills Title", name: "skills", value: undefined },
        { fieldname: "Bio", name: "bio", value: undefined },
      ];
      this.visibleResetButtons = new Set();

      delay(0).then(() => this.showInitialPerson());
    }

    showInitialPerson() {
      if (this.initialPerson && this.initialPerson.size > 0) {
        for (let [detail, value] of this.initialPerson.entries()) {
          this.addedPerson = this.addedPerson.set(detail, value);

          // TODO: how does this die when skills is an array?
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
      // split between skills and everything else because skills should be a list
      if (detail.value) {
        if (detail.name !== "skills") {
          this.addedPerson = this.addedPerson.set(detail.name, detail.value);
        } else if (detail.value.length > 0) {
          const skills = Immutable.fromJS(detail.value.split(/[\s,]+/) || []); // TODO: maybe split just for ,
          this.addedPerson = this.addedPerson.set(detail.name, skills);
        }

        this.onPersonUpdated({ person: this.addedPerson });

        if (detail.value.length > 0) {
          this.visibleResetButtons.add(detail.name);
        } else {
          this.visibleResetButtons.delete(detail.name);
        }
      } else {
        this.addedPerson = this.addedPerson.set(detail.name, undefined);
        this.onPersonUpdated({ person: this.addedPerson });
        this.visibleResetButtons.delete(detail.name);
      }
    }

    showResetButton(detail) {
      return this.visibleResetButtons.has(detail);
    }

    resetPersonDetail(detail) {
      this.addedPerson = this.addedPerson.set(detail, undefined);
      this.visibleResetButtons.delete(detail);
      let personIndex = this.personDetails.findIndex(
        personDetail => personDetail.name === detail
      );
      if (personIndex !== -1) {
        this.personDetails[personIndex].value = "";
      }
      //this.textfieldna().value = "";
      this.onPersonUpdated({ person: this.addedPerson });
      //this.showResetButton = false;
    }

    // textfieldnNg() {
    //   return this.domCache.ng(".tp__input__inner");
    // }

    // textfieldn() {
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
