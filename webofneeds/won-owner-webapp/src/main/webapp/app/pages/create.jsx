/** @jsx h */

import angular from "angular";
import PageCreate from "./react/create.jsx";
import { attach } from "../cstm-ng-utils.js";
import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageCreate" props="{}" />
  </container>
);

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class CreateController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.PageCreate = PageCreate;
  }
}

CreateController.$inject = [];

export default {
  module: angular
    .module("won.owner.components.create", [])
    .controller("CreateController", [...serviceDependencies, CreateController])
    .name,
  controller: "CreateController",
  template: template,
};
