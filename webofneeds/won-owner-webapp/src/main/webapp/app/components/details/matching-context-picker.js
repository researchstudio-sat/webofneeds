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
          placeholder="Enter Matching Context..."
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

      <!--
          <div class="mcp__contextlist">
              <span class="mcp__contextlist__context" ng-repeat="context in self.tempMatchingContext">{{context}} </span>
          </div>
          <input 
            placeholder="{{self.tempMatchingString ? self.tempMatchingString : 'e.g. \\'sports fitness\\''}}"
          />
      -->
    `;

  // TODO: get default contexts -> see create-post
  // TODO: check how hard checkboxes would be to implement
  // TODO: check with tags picker to use list of contexts instead of just string
  // --> can use array, is converted to a list in need reducer
  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.mcp4dbg = this;

      this.addedMatchingContext = this.initialMatchingContext;
      this.showResetButton = false;

      // this.defaultContext = this.$ngRedux
      //   .getState()
      //   .getIn(["config", "theme", "defaultContext"]);
      // this.tempMatchingContext = this.defaultContext
      //   ? this.defaultContext.toJS()
      //   : [];

      delay(0).then(() => this.showInitialMatchingContext());

      // this.tempMatchingString = this.tempMatchingContext
      //   ? this.tempMatchingContext.join(" ")
      //   : "";
    }

    // S|TART stuff from create-posts.js

    // updateDraft(updatedDraft, isSeeks) {
    //   if (this.isNew) {
    //     this.isNew = false;
    //     if (!this.defaultContext) {
    //       this.defaultContext = this.$ngRedux
    //         .getState()
    //         .getIn(["config", "theme", "defaultContext"]);
    //       this.tempMatchingContext = this.defaultContext
    //         ? this.defaultContext.toJS()
    //         : [];
    //       this.tempMatchingString = this.tempMatchingContext
    //         ? this.tempMatchingContext.join(" ")
    //         : "";
    //     }
    //   }

    //   this.draftObject[isSeeks] = updatedDraft;
    // }

    // mergeMatchingContext() {
    //   const list = this.tempMatchingString
    //     ? this.tempMatchingString.match(/(\S+)/gi)
    //     : [];

    //   return list.reduce(function(a, b) {
    //     if (a.indexOf(b) < 0) a.push(b);
    //     return a;
    //   }, []);
    // }

    // addMatchingContext() {
    //   this.tempMatchingContext = this.mergeMatchingContext();
    // }

    // publish() {
    //   // Post both needs
    //   if (!this.pendingPublishing) {
    //     this.pendingPublishing = true;

    //     const tmpList = [this.is, this.seeks];
    //     const newObject = {
    //       is: this.draftObject.is,
    //       seeks: this.draftObject.seeks,
    //     };

    //     for (const tmp of tmpList) {
    //       if (this.tempMatchingContext.length > 0) {
    //         newObject[tmp].matchingContext = this.tempMatchingContext;
    //       }
    //
    //       if (newObject[tmp].title === "") {
    //         delete newObject[tmp];
    //       }
    //     }

    //     this.needs__create(
    //       newObject,
    //       this.$ngRedux.getState().getIn(["config", "defaultNodeUri"])
    //     );
    //   }
    // }

    // END stuff from create-posts.js

    showInitialMatchingContext() {
      // TODO: change for matching contexts
      this.addedMatchingContext = this.initialMatchingContext;

      if (this.initialMatchingContext) {
        this.textfield().value = this.initialMatchingContext.toString();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateMatchingContext() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedMatchingContext = text.trim().split(/\s+/); // split on whitespace
        this.onMatchingContextUpdated({
          matchingContext: this.addedMatchingContext,
        });
        this.showResetButton = true;
      } else {
        this.resetMatchingContext();
      }
    }

    resetMatchingContext() {
      this.addedMatchingContext = undefined;
      this.textfield().value = "";
      this.onMatchingContextUpdated({ matchingContext: undefined });
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
      initialMatchingContext: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.matchingContextPicker", [])
  .directive("wonMatchingContextPicker", genComponentConf).name;
