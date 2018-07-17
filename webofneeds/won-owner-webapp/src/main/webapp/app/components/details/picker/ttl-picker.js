import angular from "angular";
import won from "../../../won-es6.js";
//import Immutable from "immutable";
import {
  attach,
  delay,
  //getIn
} from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- INPUT FIELD -->
      <div class="ttlp__input">
        <svg class="ttlp__input__icon clickable" 
          style="--local-primary:var(--won-primary-color);"
          ng-if="self.showResetButton"
          ng-click="self.resetTTL()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
        <textarea
          won-textarea-autogrow
          class="ttlp__input__inner won-txt won-txt--code"
          ng-keyup="::self.updateTTLBuffered()"
          ng-blur="::self.updateTTL()"
          placeholder="{{self.detail.placeholder}}"></textarea>
      </div>
      
      <!-- HELP TEXT -->
      <div class="ttlp__helptext">
        Expects valid turtle.
        <code><{{::self.won.WON.contentNodeBlankUri.is}}></code> and
        <code><{{::self.won.WON.contentNodeBlankUri.seeks}}></code> and
        will be replaced by the URI generated for this part (i.e. is/description 
        or seeks/searches) of the need. Use the URI, so your TTL can be found 
        when parsing the need. See <code>won.defaultTurtlePrefixes</code>
        for prefixes that will be added automatically. E.g.
        <code><{{::self.won.WON.contentNodeBlankUri.is}}> dc:description "hello world!".</code>
        For more information see the
        <a href="https://github.com/researchstudio-sat/webofneeds/blob/master/documentation/need-structure.md">
          documentation on the need-structure
        </a>.
      </div>

      <!-- ERROR TEXT -->
      <div class="ttlp__parse-error" ng-show="self.ttlParseError">
        {{self.ttlParseError}}
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);
      this.won = won;

      window.ttlp4dbg = this;

      this.ttlParseError = undefined;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialTTL());
    }

    /**
     * Checks validity and uses callback method
     */
    update(ttlString) {
      if (ttlString && ttlString.trim().length > 0) {
        // TODO: currently ignores if TTL is valid, see #1825
        this.onUpdate({ value: ttlString });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialTTL() {
      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue || "";
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateTTLBuffered() {
      if (this.textfield().value.length > 0) {
        this.showResetButton = true;
      }

      if (this._ttlUpdateTimeoutId) {
        clearTimeout(this._ttlUpdateTimeoutId);
      }
      this._ttlUpdateTimeoutId = setTimeout(() => this.updateTTL(), 4000);
    }

    updateTTL() {
      const ttlString = this.textfield().value;
      if (ttlString && ttlString.trim().length > 0) {
        this.update(ttlString);
      } else {
        this.resetTTL();
      }

      this.checkTTLparsability();
    }

    async checkTTLparsability() {
      // everything below is just for checking if the string is parsable
      // prints an error message if not
      await delay(200); // nicer deletion behaviour
      const ttlString = this.textfield().value;

      if (ttlString && ttlString.trim().length > 0) {
        try {
          await won.ttlToJsonLd(ttlString);
          this.ttlParseError = undefined;
          this.$scope.$apply();
        } catch (parseError) {
          this.ttlParseError = parseError.message;
          this.$scope.$apply();
        }
      }
    }

    resetTTL() {
      this.ttlParseError = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
      // make sure that the textfield updates its size:
      this.textfield().dispatchEvent(new Event("input"));
    }

    textfieldNg() {
      return this.domCache.ng(".ttlp__input__inner");
    }

    textfield() {
      return this.domCache.dom(".ttlp__input__inner");
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
  .module("won.owner.components.ttlPicker", [])
  .directive("wonTtlPicker", genComponentConf).name;
