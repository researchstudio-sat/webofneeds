/** @jsx h */
/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import { attach } from "../cstm-ng-utils.js";

import PagePost from "./react/post.jsx";
import { h } from "preact";

const template = (
  <container>
    <won-preact component="self.PagePost" props="{}" />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.PagePost = PagePost;
  }
}

Controller.$inject = serviceDependencies;

export default {
  module: angular
    .module("won.owner.components.post", [])
    .controller("PostController", Controller).name,
  controller: "PostController",
  template: template,
};
