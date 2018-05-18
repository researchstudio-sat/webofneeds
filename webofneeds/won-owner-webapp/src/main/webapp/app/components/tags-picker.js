import angular from "angular";
import { attach, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { doneTypingBufferNg, DomCache } from "../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element", "$sce"];
function genComponentConf() {
  let template = `
    <div class="cis__addDetail__header tags" ng-click="self.resetTags()">
      <svg class="cis__circleicon">
        <use xlink:href="#ico36_tags_circle" href="#ico36_tags_circle"></use>
      </svg>
      Remove All Tags
      </div>
      <div class="tp__taglist">
        <span class="tp__taglist__tag" ng-repeat="tag in self.addedTags">#{{tag}}</span>
      </div>
      <input class="tp__input"
        placeholder="e.g. #couch #free" type="text"
        ng-keyup="::self.doneTyping()"
      />
      <!-- ng-keyup="::self.updateTags()" -->

      <input type="text" class="tp__textbox" placeholder="e.g. #couch #free"/>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.tp4dbg = this;

      this.addedTags = this.initialTags;

      // doneTypingBufferNg(
      //     e => this.doneTyping(e),
      //     this.textfieldNg(), 1000
      // );
    }

    doneTyping() {
      const text = this.textfield().value;

      if (!text || text.trim().length === 0) {
        // do stuff
      } else {
        // do stuff
        // if last character was a space, add tag
        if (text.endsWith(" ")) {
          this.addTag(text.trim());
        }
      }
    }

    addTag(tag) {
      // TODO: check for duplicates
      this.addedTags.push(tag);
      console.log("TAGLIST: ", this.addedTags);
      //this.onTagsUpdated({tags: this.addedTags});
    }

    resetTags() {
      this.addedTags = [];
      console.log("TAGLIST: ", this.addedTags);
      //this.onTagsUpdated({tags: this.addedTags});
    }

    textfieldNg() {
      return this.domCache.ng(".tp__input");
    }

    textfield() {
      return this.domCache.dom(".tp__input");
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
