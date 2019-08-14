/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import { delay } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = ``;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.focusedElement = null;

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));

      this.$scope.$watch(
        () => this.isFromAtomToLoad,
        () => delay(0).then(() => this.ensureFromAtomIsLoaded())
      );

      this.$scope.$watch(
        () => this.showCreateInput,
        () => delay(0).then(() => this.loadInitialDraft())
      );
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      /*scope-isolation*/
    },
    template: template,
  };
}
export default //.controller('CreateAtomController', [...serviceDependencies, CreateAtomController])
angular
  .module("won.owner.components.createPost", [ngAnimate])
  .directive("wonCreatePost", genComponentConf).name;
