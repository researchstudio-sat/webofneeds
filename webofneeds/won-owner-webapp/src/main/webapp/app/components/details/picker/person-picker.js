import angular from "angular";
import { delay } from "../../../utils.js";
import { attach, DomCache } from "../../../cstm-ng-utils.js";
import Immutable from "immutable";
import wonInput from "../../../directives/input.js";

import "~/style/_personpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div ng-repeat="dtl in self.personDetails" class="pp__detail">
          <div class="pp__detail__label">
              {{dtl.fieldname}}
          </div>
          <input
            type="text"
            id="{{dtl.name}}"
            class="pp__detail__input"
            ng-model="dtl.value"
            won-input="::self.updateDetails(dtl)"
            ng-class="{'pp__detail__input--withreset' : self.showResetButton(dtl.name)}"
          />
          <svg class="pp__detail__icon clickable" 
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton(dtl.name)"
            ng-click="self.resetPersonDetail(dtl.name)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.pp4dbg = this;

      this.addedPerson = Immutable.Map();

      this.personDetails = [
        { fieldname: "Title", name: "title", value: undefined },
        { fieldname: "Name", name: "name", value: undefined },
        { fieldname: "Company", name: "company", value: undefined },
        { fieldname: "Position", name: "position", value: undefined },
      ];
      this.visibleResetButtons = new Set();

      delay(0).then(() => this.showInitialPerson());
    }

    /**
     * Checks validity and uses callback method
     */
    update(person) {
      let isEmpty = true;
      // check if person is empty
      if (person) {
        //check validity
        const personArray = Array.from(person.values());
        isEmpty = personArray.every(
          dtl => dtl === undefined || dtl === "" || (dtl && dtl.size === 0)
        );
      }
      if (person && !isEmpty) {
        this.onUpdate({ value: person });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialPerson() {
      if (this.initialValue && this.initialValue.size > 0) {
        for (let [dtl, value] of this.initialValue.entries()) {
          this.addedPerson = this.addedPerson.set(dtl, value);

          let personIndex = this.personDetails.findIndex(
            personDetail => personDetail.name === dtl
          );
          if (personIndex !== -1) {
            this.personDetails[personIndex].value = value;
          }
          if (value && value.length > 0) {
            this.visibleResetButtons.add(dtl);
          }
        }
      }

      this.$scope.$apply();
    }

    updateDetails(dtl) {
      // split between skills and everything else because skills should be a list
      if (dtl.value) {
        this.addedPerson = this.addedPerson.set(dtl.name, dtl.value);
        this.update(this.addedPerson);
        if (dtl.value.length > 0) {
          this.visibleResetButtons.add(dtl.name);
        } else {
          this.visibleResetButtons.delete(dtl.name);
        }
      } else {
        this.addedPerson = this.addedPerson.set(dtl.name, undefined);
        this.update(this.addedPerson);
        this.visibleResetButtons.delete(dtl.name);
      }
    }

    showResetButton(dtl) {
      return this.visibleResetButtons.has(dtl);
    }

    resetPersonDetail(dtl) {
      this.addedPerson = this.addedPerson.set(dtl, undefined);
      this.visibleResetButtons.delete(dtl);
      let personIndex = this.personDetails.findIndex(
        personDetail => personDetail.name === dtl
      );
      if (personIndex !== -1) {
        this.personDetails[personIndex].value = "";
      }
      this.update(this.addedPerson);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onUpdate: "&",
      initialValue: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.personPicker", [wonInput])
  .directive("wonPersonPicker", genComponentConf).name;
