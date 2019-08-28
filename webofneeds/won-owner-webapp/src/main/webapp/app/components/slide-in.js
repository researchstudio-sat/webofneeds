/**
 * Created by quasarchimaere on 20.11.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";

import { connect2Redux } from "../configRedux.js";

function genSlideInConf() {
  let template = /*html*/ `
        
    `;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*injections as strings here*/,
    "$element",
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      connect2Redux(undefined, actionCreators, [], this);

      this.$scope.$watch("self.verificationToken", verificationToken =>
        this.verifyEmailAddress(verificationToken)
      );
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    scope: {}, //isolate scope to allow usage within other controllers/components
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
  };
}

export default angular
  .module("won.owner.components.slideIn", [ngAnimate])
  .directive("wonSlideIn", genSlideInConf).name;
