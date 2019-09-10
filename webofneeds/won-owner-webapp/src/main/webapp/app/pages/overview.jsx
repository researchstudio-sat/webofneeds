/** @jsx h */

/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import { attach } from "../cstm-ng-utils.js";
import PageOverview from "./react/overview.jsx";
import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageOverview" />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.PageOverview = PageOverview;
  }
}

Controller.$inject = serviceDependencies;

export default {
  module: angular
    .module("won.owner.components.overview", [])
    .controller("OverviewController", Controller).name,
  controller: "OverviewController",
  template: template,
};
