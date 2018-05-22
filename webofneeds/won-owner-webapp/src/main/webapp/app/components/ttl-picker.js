import angular from "angular";
import won from "../won-es6.js";
//import Immutable from "immutable";
import {
  attach,
  //delay,
  //getIn
} from "../utils.js";
import { doneTypingBufferNg, DomCache } from "../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element", "$sce"];
function genComponentConf() {
  let template = `
    <div class="cis__ttl" ng-if="self.openDetail === 'ttl'">
      <div class="cis__addDetail__header ttl" ng-click="self.details.delete('ttl') && self.updateDraft()">
        <svg class="cis__circleicon nonHover">
          <use xlink:href="#ico36_rdf_logo_circle" href="#ico36_rdf_logo_circle"></use>
        </svg>
        <svg class="cis__circleicon hover">
          <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
        </svg>
        <span>Remove Turtle (TTL)</span>
      </div>

      <textarea
        won-textarea-autogrow
        class="cis__ttl__text won-txt won-txt--code"
        ng-blur="::self.updateTTL()"
        ng-keyup="::self.updateTTLBuffered()"
        placeholder="Enter TTL...">
      </textarea>
      
      <div class="cis__ttl__helptext">
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

      <div class="cis__ttl__parse-error" ng-show="self.ttlParseError">{{self.ttlParseError}}</div>
    </div>`;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.ttlp4dbg = this;

      this.savedTTL = this.initialTTL;
      this.showResetButton = false;

      doneTypingBufferNg(e => this.doneTyping(e), this.textfieldNg(), 300);
    }

    resetTTL() {}

    doneTyping() {
      const text = this.textfield().value;

      this.showResetButton = false;
      this.$scope.$apply(() => {
        this.resetLocation();
      });

      if (!text) {
        // do something
      } else {
        // do something
      }
    }

    updateTTLBuffered() {
      if (this._ttlUpdateTimeoutId) {
        clearTimeout(this._ttlUpdateTimeoutId);
      }
      this._ttlUpdateTimeoutId = setTimeout(() => this.updateTTL(), 4000);
    }

    updateTTL() {
      //await won.ttlToJsonLd(won.defaultTurtlePrefixes + '\n' + $0.value)
      const ttlString = (this.ttlInput() || {}).value || "";

      this.draftObject.ttl = ttlString;

      won
        .ttlToJsonLd(ttlString)
        .then(parsedJsonLd => {
          this.$scope.$apply(() => (this.ttlParseError = ""));
          return parsedJsonLd;
        })
        .catch(parseError => {
          this.$scope.$apply(() => (this.ttlParseError = parseError.message));
        });

      if (ttlString && !this.details.has("ttl")) {
        this.details.add("ttl");
      }
    }

    textfieldNg() {
      return this.domCache.ng("#ttlp__searchbox__inner");
    }

    textfield() {
      return this.domCache.dom("#ttlp__searchbox__inner");
    }

    ttlInputNg() {
      return angular.element(this.ttlInput());
    }
    ttlInput() {
      if (!this._ttlInput) {
        this._ttlInput = this.$element[0].querySelector(".cis__ttl__text");
      }
      return this._ttlInput;
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onTTLChanged: "&",
      initialTTL: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.ttlPicker", [])
  .directive("wonTTLPicker", genComponentConf).name;
