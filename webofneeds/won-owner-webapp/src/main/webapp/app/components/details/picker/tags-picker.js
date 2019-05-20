import angular from "angular";
import { attach, delay, dispatchEvent, is } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";

import "~/style/_tagspicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- TAGS INPUT -->
      <div class="tp__input">
        <input 
          class="tp__input__inner"
          type="text"
          placeholder="{{self.detail.placeholder}}"
          won-input="::self.updateTags()"
          ng-class="{'tp__input__inner--withreset' : self.showResetButton}"
        />
        <svg class="tp__input__icon clickable" 
          style="--local-primary:var(--won-primary-color);"
          ng-if="self.showResetButton"
          ng-click="self.resetTags()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
      </div>

      <!-- LIST OF ADDED TAGS -->
      <!-- TODO: make tags individually deletable -->
      <!-- TODO: add # to tag text -->
      <div class="tp__taglist">
        <span class="tp__taglist__tag" ng-repeat="tag in self.addedTags">{{tag}}</span>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.tp4dbg = this;

      this.addedTags = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialTags());
    }

    /**
     * Checks validity and uses callback method
     */
    update(tags) {
      const payload = {
        value:
          tags && tags.length > 0
            ? // check if there are tags
              tags
            : undefined,
      };
      this.onUpdate(payload);
      dispatchEvent(this.$element[0], "update", payload);
    }

    showInitialTags() {
      this.addedTags = this.initialValue;
      let _tagsForTextfield = "";
      if (this.initialValue && this.initialValue.length > 0) {
        this.initialValue.forEach(function(tag) {
          _tagsForTextfield += tag + " ";
        });
        this.showResetButton = true;
      }
      this.textfield().value = _tagsForTextfield.trim();

      this.$scope.$apply();
    }

    updateTags() {
      // TODO: do something with text that does not start with #
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedTags = extractHashtags(text);
        this.update(this.addedTags);
        this.showResetButton = true;
      } else {
        this.resetTags();
      }
    }

    resetTags() {
      this.addedTags = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    textfieldNg() {
      return this.domCache.ng(".tp__input__inner");
    }

    textfield() {
      return this.domCache.dom(".tp__input__inner");
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
  .module("won.owner.components.tagsPicker", [wonInput])
  .directive("wonTagsPicker", genComponentConf).name;

function extractHashtags(str) {
  if (!str) {
    return [];
  }
  if (!is("String", str)) {
    "utils.js: extractHashtags expects a string but got: " + str;
  }
  const seperatorChars = ",;#";
  const tags = str
    .split(new RegExp(`[${seperatorChars}]+`, "i"))
    .map(t => t.trim())
    .filter(t => t); // filter empty after trimming

  return Array.from(new Set(tags)); // filter out duplicates and return
}
