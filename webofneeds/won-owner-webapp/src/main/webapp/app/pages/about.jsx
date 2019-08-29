/** @jsx h */

import angular from "angular";
import PageAbout from "./react/about.jsx";

import { attach } from "../cstm-ng-utils.js";
import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageAbout" />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$state",
  "$scope" /*'$routeParams' /*injections as strings here*/,
  "$element",
];

class AboutController {
  constructor(/* arguments <- serviceDependencies */) {
    this.PageAbout = PageAbout;
    attach(this, serviceDependencies, arguments);
  }
}

export default {
  module: angular
    .module("won.owner.components.about", [])
    .controller("AboutController", [...serviceDependencies, AboutController])
    .name,
  controller: "AboutController",
  template: template,
};
