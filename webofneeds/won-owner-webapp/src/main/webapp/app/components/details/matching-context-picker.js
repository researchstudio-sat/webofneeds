import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="mcp__checkboxes">
        <label ng-repeat="context in self.defaultBoxes">
            <input type="checkbox" ng-model="context.selected" ng-change="self.updateMatchingContext()"/> {{context.name}} 
        </label>
      </div>

      <div class="mcp__helptext">
        <span>use whitespace to separate context names</span>
      </div>

      <div class="mcp__input">
        <input 
          class="mcp__input__inner"
          type="text"
          placeholder="e.g. 'sports fitness'"
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
      
      <div class="mcp__contextlist">
          <span class="mcp__contextlist__context" ng-repeat="context in self.addedContext">{{context}}</span>
      </div>

      <!-- TODO: style this -->
      <button type="submit" class="won-button--outlined red mcp__restore-button"
          ng-if="self.defaultMatchingContext && self.defaultMatchingContext.length > 0"
          ng-click="::self.restoreDefault()">
          <span>Restore Default</span>
      </button>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.mcp4dbg = this;

      this.addedContext = [];
      // this.suggestedContext = this.suggestedMatchingContext;
      this.defaultBoxes = [];
      // this.suggestedBoxes = [];

      this.showResetButton = false;

      delay(0).then(() => this.showInitialMatchingContext());
    }

    showInitialMatchingContext() {
      if (this.initialMatchingContext) {
        let tempContext = [...this.initialMatchingContext];
        this.addedContext = this.initialMatchingContext;

        // deal with default contexts
        if (
          this.defaultMatchingContext &&
          this.defaultMatchingContext.length > 0
        ) {
          for (let context of this.defaultMatchingContext) {
            let isPresent = !!this.initialMatchingContext.includes(context);
            this.defaultBoxes.push({ name: context, selected: isPresent });
          }

          tempContext = this.initialMatchingContext.filter(
            context => !this.defaultMatchingContext.includes(context)
          );
        }

        if (tempContext.length > 0) {
          this.textfield().value = tempContext.join(" ");
          this.showResetButton = true;
        }
      }

      this.$scope.$apply();
    }

    updateMatchingContext() {
      const text = this.textfield().value;
      let checkboxContext = [];
      let textfieldContext = [];
      // get values from checkboxes
      for (let box of this.defaultBoxes) {
        if (box && box.selected) checkboxContext.push(box.name);
      }
      // get values from text field
      if (text && text.trim().length > 0) {
        this.showResetButton = true;
        textfieldContext = text.trim().split(/\s+/);
      }
      // - if textfield is empty -> hide resetbutton
      else {
        this.showResetButton = false;
      }
      // join & reduce
      let combinedContext = [...checkboxContext, ...textfieldContext].reduce(
        function(a, b) {
          if (a.indexOf(b) < 0) a.push(b);
          return a;
        },
        []
      );
      // if empty -> reset, else callback
      if (combinedContext.length > 0) {
        this.addedContext = combinedContext;
        this.onMatchingContextUpdated({ matchingContext: combinedContext });
      } else {
        this.resetMatchingContext();
      }
    }

    resetMatchingContext() {
      this.addedContext = [];
      this.textfield().value = "";
      this.onMatchingContextUpdated({ matchingContext: [] });
      this.showResetButton = false;
    }

    restoreDefault() {
      this.addedContext = this.defaultMatchingContext || [];
      this.textfield().value = "";
      this.showResetButton = false;
      this.onMatchingContextUpdated({
        matchingContext: this.addedContext,
      });

      for (let box of this.defaultBoxes) {
        box.selected = true;
      }
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
