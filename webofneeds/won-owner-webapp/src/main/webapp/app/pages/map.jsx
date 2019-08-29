/** @jsx h */
import angular from "angular";
import { attach } from "../cstm-ng-utils.js";
import { h } from "preact";

import PageMap from "./react/map.jsx";

const template = (
  <container>
    <won-preact component="self.PageMap" props="{}" />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.PageMap = PageMap;
  }
}

Controller.$inject = serviceDependencies;

export default {
  module: angular
    .module("won.owner.components.map", [])
    .controller("MapController", Controller).name,
  controller: "MapController",
  template: template,
};
