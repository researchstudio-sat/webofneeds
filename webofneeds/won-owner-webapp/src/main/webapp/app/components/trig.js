import angular from "angular";
import { attach, trigPrefixesAndBody } from "../utils.js";
import won from "../won-es6.js";

const serviceDependencies = ["$scope"];
function genComponentConf() {
  let template = `
    <div 
      ng-click="self.showTrigPrefixes = !self.showTrigPrefixes" 
      ng-show="self.trigBody"
      class="trig">
        <code class="trig__prefixes" ng-show="!self.showTrigPrefixes">@prefix ...</code> <!-- no spaces or newlines within the code-tag, because it is preformatted -->
        <code class="trig__prefixes" ng-show="self.showTrigPrefixes">{{ self.trigPrefixes }}</code> <!-- no spaces or newlines within the code-tag, because it is preformatted -->
        <code class="trig__contentgraph">{{ self.trigBody }}</code> <!-- no spaces or newlines within the code-tag, because it is preformatted -->
    </div>
  `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      window.trg4dbg = this;

      this.$scope.$watch("self.trig", (newTrig, prevTrig) =>
        this.updatedTrig(newTrig, prevTrig)
      );
      this.$scope.$watch("self.jsonld", (newJsonld, prevJsonld) =>
        this.updatedJsonld(newJsonld, prevJsonld)
      );
    }
    updatedTrig(newTrig, prevTrig) {
      if (newTrig) {
        console.log("NOT YET IMPLEMENTED: ", newTrig, prevTrig);
      }
    }
    async updatedJsonld(newJsonld, prevJsonld) {
      // generate new trig, if the json-ld has changed or it hasn't been generated before
      if (newJsonld && (newJsonld != prevJsonld || !this.trig)) {
        // console.log("NOT YET IMPLEMENTED: ", newJsonld, prevJsonld);

        window.newJsonld = newJsonld;
        window.won4deleteme = won;

        // TODO parse to trig if possible, otherwise to ttl

        const trigString = await won.jsonLdToTrig(newJsonld.toJS());
        const { trigPrefixes, trigBody } = trigPrefixesAndBody(trigString);
        this.trigPrefixes = trigPrefixes;
        this.trigBody = trigBody;

        console.log(
          "parsed trig in <won-trig>: ",
          trigPrefixes,
          "\n\n\n",
          trigBody,
          newJsonld,
          prevJsonld
        );
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      /* pass in either a trig-string _or_ a jsonld-object */
      trig: "=", // as string
      jsonld: "=", // as immutable-js object
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.trig", [])
  .directive("wonTrig", genComponentConf).name;
