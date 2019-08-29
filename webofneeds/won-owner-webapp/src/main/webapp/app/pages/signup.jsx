/** @jsx h */

/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import { attach } from "../cstm-ng-utils.js";
import PageSignUp from "./react/signup.jsx";

import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageSignUp" />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$state" /*'$routeParams' /*injections as strings here*/,
  "$element",
];

class SignupController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.PageSignUp = PageSignUp;
  }
}

export default {
  module: angular
    .module("won.owner.components.signup", [])
    .controller("SignupController", [...serviceDependencies, SignupController])
    .name,
  controller: "SignupController",
  template: template,
};
