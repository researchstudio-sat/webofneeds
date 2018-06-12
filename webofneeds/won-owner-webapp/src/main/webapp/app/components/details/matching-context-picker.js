import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="mcp__input">
        <input 
          class="mcp__input__inner"
          type="text"
          placeholder="self.defaultPlaceholder"
          ng-keyup="::self.updateMatchingContext()"
          ng-class="{'mcp__input__inner--withreset' : self.showResetButton}"
        />
        <svg class="mcp__input__icon clickable" 
          style="--local-primary:var(--won-primary-color);"
          ng-if="self.showResetButton"
          ng-click="self.resetMatchingContext()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
      </div>
      
      <div class="mcp__helptext">
        <span>use whitespace to separate context names</span>
      </div>

      <div class="mcp__contextlist">
          <span class="mcp__contextlist__context" ng-repeat="context in self.addedContexts">{{context}}</span>
      </div>
    `;

  // TODO: check how hard checkboxes would be to implement
  // TODO: "restore default" - thingy?
  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.mcp4dbg = this;

      this.addedContexts = this.initialMatchingContext;
      this.defaultContexts = this.defaultMatchingContext;
      this.defaultPlaceholder = "e.g. 'sports fitness'";

      this.showResetButton = false;

      delay(0).then(() => this.showInitialMatchingContext());
    }

    showInitialMatchingContext() {
      this.addedContexts = this.initialMatchingContext;
      this.defaultContexts = this.defaultMatchingContext;
      this.defaultPlaceholder =
        this.defaultContexts && this.defaultContexts.length > 0
          ? this.defaultContexts.join(" ")
          : "e.g. 'sports fitness'";

      if (
        this.initialMatchingContext &&
        this.initialMatchingContext.length > 0
      ) {
        this.textfield().value = this.initialMatchingContext.join(" ");
        this.showResetButton = true;
      } else if (
        !this.initialMatchingContext &&
        this.defaultContexts.length > 0
      ) {
        this.textfield().value = this.defaultContexts.join(" ");
        this.addedContexts = this.defaultContexts;
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateMatchingContext() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        // split on whitespace
        let contextArray = text.trim().split(/\s+/);
        // remove duplicates
        this.addedContexts = contextArray.reduce(function(a, b) {
          if (a.indexOf(b) < 0) a.push(b);
          return a;
        }, []);
        this.onMatchingContextUpdated({
          matchingContext: this.addedContexts,
        });
        this.showResetButton = true;
      } else {
        this.resetMatchingContext();
      }
    }

    resetMatchingContext() {
      this.addedContexts = [];
      this.textfield().value = "";
      this.onMatchingContextUpdated({ matchingContext: [] });
      this.showResetButton = false;
    }

    textfieldNg() {
      return this.domCache.ng(".mcp__input__inner");
    }

    textfield() {
      return this.domCache.dom(".mcp__input__inner");
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onMatchingContextUpdated: "&",
      defaultMatchingContext: "=",
      initialMatchingContext: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.matchingContextPicker", [])
  .directive("wonMatchingContextPicker", genComponentConf).name;
