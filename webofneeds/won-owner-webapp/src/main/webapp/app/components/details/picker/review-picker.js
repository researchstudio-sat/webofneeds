import angular from "angular";
import { delay } from "../../../utils.js";
import { attach } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";

import "~/style/_reviewpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="reviewp__input">
        <select
            class="reviewp__input__rating"
            ng-model="self.selectedRating"
            ng-options="r.value as r.label for r in self.detail.rating"
            ng-change="::self.updateContent()"
            ng-disabled="self.detail.rating.length <= 1">
        </select>
        <input
            type="text"
            class="reviewp__input__inner"
            placeholder="{{self.detail.placeholder}}"
            ng-blur="::self.updateContent()"
            won-input="::self.updateContent()"
            ng-class="{'reviewp__input__inner--withreset' : self.showResetButton}"/>
        <div class="reviewp__input__reset clickable">
          <svg class="reviewp__input__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetText()">
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
      this.showResetButton = false;

      delay(0).then(() => this.showInitialValues());
    }

    /**
     * Checks validity and uses callback method
     */
    update(text, rating) {
      if (rating) {
        this.onUpdate({
          value: {
            text: text && text.trim(),
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

    showInitialValues() {
      const initialText = this.initialValue && this.initialValue.text;
      const initialRating = this.initialValue && this.initialValue.rating;

      if (initialRating) {
        this.selectedRating = initialRating;
      } else {
        this.selectedRating = this.getDefaultRating();
      }

      if (initialText) {
        this.showResetButton = true;
        this.addedText = initialText;
        this.text().value = this.addedText;
      }

      this.$scope.$apply();
    }

    updateContent() {
      this.addedText = this.text().value;
      this.showResetButton = !!this.addedText;

      this.update(this.addedText, this.selectedRating);
    }

    resetText() {
      this.addedText = undefined;
      this.text().value = "";
      this.showResetButton = false;

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
