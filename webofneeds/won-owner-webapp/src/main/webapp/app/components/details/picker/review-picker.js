import angular from "angular";
import { attach, delay } from "../../../utils.js";
import wonInput from "../../../directives/input.js";

import "style/_reviewpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="reviewp__input">
        <select
            class="reviewp__input__rating"
            ng-model="self.selectedRating"
            ng-disabled="self.detail.rating.length <= 1"
            won-input="::self.updateRating()">
            <option ng-repeat="rating in self.detail.rating" value="{{rating.value}}">{{rating.label}}</option>
        </select>
        <input
            type="text"
            class="reviewp__input__inner"
            placeholder="{{self.detail.placeholder}}"
            ng-blur="::self.updateText(true)"
            won-input="::self.updateText(false)"
            ng-class="{'reviewp__input__inner--withreset' : self.showResetButton}"/>
        <div class="reviewp__input__reset clickable">
          <svg class="reviewp__input__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetText(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.reviewp4dbg = this;

      this.addedText = this.initialValue && this.initialValue.text;
      this.selectedRating = this.initialValue && this.initialValue.rating;

      if (!this.selectedRating) {
        this.selectedRating = "3";
      }

      this.showResetButton = false;

      delay(0).then(() => this.showInitialText());
    }

    /**
     * Checks validity and uses callback method
     */
    update(text, rating) {
      const trimmedText = text.trim();
      if (rating) {
        this.onUpdate({
          value: {
            text: trimmedText === "" ? undefined : trimmedText,
            rating: rating,
          },
        });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    getDefaultRating() {
      let defaultRating;

      this.detail &&
        this.detail.rating.forEach(rating => {
          if (rating.default) defaultRating = rating.value;
        });

      return defaultRating;
    }

    showInitialText() {
      this.addedText = this.initialValue && this.initialValue.text;
      this.selectedRating =
        (this.initialValue && this.initialValue.rating) ||
        this.getDefaultRating();

      if (this.initialValue && this.initialValue.text) {
        this.text().value = this.initialValue.text;
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    rating() {
      if (!this._rating) {
        this._rating = this.$element[0].querySelector(
          ".reviewp__input__rating"
        );
      }
      return this._rating;
    }

    updateText() {
      const text = this.text().value;

      this.update(text, this.selectedRating);
      this.showResetButton = true;
    }

    updateRating() {
      this.selectedRating = this.rating().value;
      this.update(this.addedText, this.selectedRating);
    }

    resetText(resetInput) {
      this.addedText = undefined;
      this.selectedRating = this.getDefaultRating();

      if (resetInput) {
        this.text().value = "";
        this.rating().value = this.selectedRating;
        this.showResetButton = false;
      }
      this.update(this.addedText, this.selectedRating);
    }

    text() {
      if (!this._textInput) {
        this._textInput = this.$element[0].querySelector(
          ".reviewp__input__inner"
        );
      }
      return this._textInput;
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
  .module("won.owner.components.reviewPicker", [wonInput])
  .directive("wonReviewPicker", genComponentConf).name;
