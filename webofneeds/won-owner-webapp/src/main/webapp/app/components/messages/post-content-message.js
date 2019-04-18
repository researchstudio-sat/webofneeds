import angular from "angular";

import squareImageModule from "../square-image.js";
import postContentModule from "../post-content.js";
import postMenuModule from "../post-menu.js";

import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { classOnComponentRoot } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

import "style/_post-content-message.scss";

function genComponentConf() {
  let template = `
        <div class="won-cm__center">
            <div class="won-cm__center__bubble">
                <won-post-menu post-uri="self.postUri"></won-post-menu>
                <won-post-content post-uri="self.postUri"></won-post-content>
            </div>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      window.pcm4dbg = this;

      const selectFromState = state => {
        const post = this.postUri && state.getIn(["atoms", this.postUri]);

        return {
          post,
          postLoading:
            !post ||
            getIn(state, ["process", "atoms", post.get("uri"), "loading"]),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.postUri", "self.connectionUri"],
        this
      );
      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      postUri: "=",
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.postContentMessage", [
    squareImageModule,
    postMenuModule,
    postContentModule,
  ])
  .directive("wonPostContentMessage", genComponentConf).name;
