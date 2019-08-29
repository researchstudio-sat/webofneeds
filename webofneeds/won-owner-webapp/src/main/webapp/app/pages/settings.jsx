/** @jsx h */

/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import { attach } from "../cstm-ng-utils.js";
import PageSettings from "./react/settings.jsx";

import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageSettings" props="{}" />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$state" /*'$routeParams' /*injections as strings here*/,
  "$element",
];

class SettingsController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.PageSettings = PageSettings;
  }
}

export default {
  module: angular
    .module("won.owner.components.settings", [])
    .controller("SettingsController", [
      ...serviceDependencies,
      SettingsController,
    ]).name,
  controller: "SettingsController",
  template: template,
};
