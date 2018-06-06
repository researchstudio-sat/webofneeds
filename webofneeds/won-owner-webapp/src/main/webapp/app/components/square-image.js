import angular from "angular";

import { attach, generateRgbColorArray } from "../utils.js";

import Identicon from "identicon.js";
window.Identicon4dbg = Identicon;

import shajs from "sha.js";
window.shajs4dbg = shajs;

const serviceDependencies = ["$scope"];
function genComponentConf() {
  let template = `
    <img class="image" ng-show="self.src" ng-src="{{self.src}}"/>

    <img class="image" 
      ng-if="!self.src && self.identiconSvg" 
      alt="Auto-generated title image for {{self.title}}"
      ng-src="data:image/svg+xml;base64,{{self.identiconSvg}}">
  `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      const unregister = this.$scope.$watch("self.uri", newVal => {
        if (newVal) unregister(); // only need to do this once
        this.updateIdenticon(newVal);
      });
    }

    updateIdenticon(input) {
      if (!input) return;
      // quick extra hash here as identicon.js only uses first 15
      // chars (which aren't very unique for our uris due to the base-url):
      const hash = new shajs.sha512().update(input).digest("hex");
      const rgbColorArray = generateRgbColorArray(hash);
      const idc = new Identicon(hash, {
        size: 100,
        foreground: [255, 255, 255, 255], // rgba white
        background: [...rgbColorArray, 255], // rgba
        margin: 0.2,
        format: "svg",
      });
      this.identiconSvg = idc.toString();
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      src: "=",
      title: "=",
      uri: "=", // only read once
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.squareImage", [])
  .directive("wonSquareImage", genComponentConf).name;
