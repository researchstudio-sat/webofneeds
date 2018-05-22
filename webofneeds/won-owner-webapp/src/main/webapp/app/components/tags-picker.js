import angular from "angular";
import { attach, delay, extractHashtags } from "../utils.js";
import { DomCache } from "../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- TAGS INPUT -->
      <div class="tp__input">
        <input 
          class="tp__input__inner"
          type="text"
          placeholder="e.g. #couch #free"
          ng-keyup="::self.updateTags()"
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
        <span class="tp__taglist__tag" ng-repeat="tag in self.addedTags">#{{tag}}</span>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.tp4dbg = this;

      this.addedTags = this.initialTags;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialTags());
    }

    showInitialTags() {
      this.addedTags = this.initialTags;
      let _tagsForTextfield = "";
      if (this.initialTags) {
        this.initialTags.forEach(function(tag) {
          _tagsForTextfield += "#" + tag + " ";
        });
      }
      this.textfield().value = _tagsForTextfield.trim();

      this.$scope.$apply();
    }

    updateTags() {
      // TODO: do something with text that does not start with #
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedTags = extractHashtags(text);
        this.onTagsUpdated({ tags: this.addedTags });
        this.showResetButton = true;
      } else {
        this.resetTags();
      }
    }

    resetTags() {
      this.addedTags = undefined;
      this.textfield().value = "";
      this.onTagsUpdated({ tags: undefined });
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
      onTagsUpdated: "&",
      initialTags: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.tagsPicker", [])
  .directive("wonTagsPicker", genComponentConf).name;
