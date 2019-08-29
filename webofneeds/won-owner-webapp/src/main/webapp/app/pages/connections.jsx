/** @jsx h */

import angular from "angular";
import PageConnections from "./react/connections.jsx";
import { attach } from "../cstm-ng-utils.js";
import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageConnections" props="{}" />
  </container>
);

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.PageConnections = PageConnections;
  }
}

ConnectionsController.$inject = [];

export default {
  module: angular
    .module("won.owner.components.connections", [])
    .controller("ConnectionsController", [
      ...serviceDependencies,
      ConnectionsController,
    ]).name,
  controller: "ConnectionsController",
  template: template,
};
