/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach } from "../../utils.js";
import won from "../../won-es6.js";
import { actionCreators } from "../../actions/actions.js";
import sendRequestModule from "../send-request.js";
import visitorTitleBarModule from "../visitor-title-bar.js";
import { getPostUriFromRoute } from "../../selectors/general-selectors.js";

import * as srefUtils from "../../sref-utils.js";

import "style/_post-visitor.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.p4dbg = this;
    this.WON = won.WON;
    Object.assign(this, srefUtils); // bind srefUtils to scope

    const selectFromState = state => {
      const postUri = getPostUriFromRoute(state);
      const post = state.getIn(["needs", postUri]);

      const isOwnPost = post && post.get("isOwned");

      return {
        postUri,
        isOwnPost: isOwnPost,
        post,
        won: won.WON,
        showModalDialog: state.getIn(["view", "showModalDialog"]),
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);
  }

  goToOwnPost() {
    this.router__stateGoAbs("connections", {
      connectionUri: undefined,
      postUri: this.postUri,
    });
    //TODO: REDIRECT TO THE OVERVIEW INSTEAD OF POST-VIEW (this does not work yet as the postUri is not set yet during the time of the function execution
  }
}

Controller.$inject = serviceDependencies;

export default angular
  .module("won.owner.components.post", [
    sendRequestModule,
    ngAnimate,
    visitorTitleBarModule,
  ])
  .controller("PostController", Controller).name;
