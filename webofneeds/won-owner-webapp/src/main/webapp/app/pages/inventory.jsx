/** @jsx h */

import angular from "angular";
import { attach } from "../cstm-ng-utils.js";
import PageInventory from "./react/inventory.jsx";

import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PageInventory" />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.PageInventory = PageInventory;
  }
}

Controller.$inject = [];

export default {
  module: angular
    .module("won.owner.components.inventory", [])
    .controller("InventoryController", [...serviceDependencies, Controller])
    .name,
  controller: "InventoryController",
  template: template,
};
