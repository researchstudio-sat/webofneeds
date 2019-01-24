/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn } from "../../utils.js";
import won from "../../won-es6.js";
import { actionCreators } from "../../actions/actions.js";
import sendRequestModule from "../send-request.js";
import visitorTitleBarModule from "../visitor-title-bar.js";
import {
  getPostUriFromRoute,
  getViewNeedUriFromRoute,
} from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";

import * as srefUtils from "../../sref-utils.js";
import * as needUtils from "../../need-utils.js";

import "style/_post-visitor.scss";
import "style/_need-overlay.scss";

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
      const viewNeedUri = getViewNeedUriFromRoute(state);
      const post = state.getIn(["needs", postUri]);

      return {
        postUri,
        isOwnPost: needUtils.isOwned(post),
        isActive: needUtils.isActive(post),
        post,
        won: won.WON,
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showNeedOverlay: !!viewNeedUri,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        viewNeedUri,
        postLoading:
          !post || getIn(state, ["process", "needs", postUri, "loading"]),
        postFailedToLoad:
          post && getIn(state, ["process", "needs", postUri, "failedToLoad"]),
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);
  }

  tryReload() {
    if (this.postUri && this.postFailedToLoad) {
      this.needs__fetchUnloadedNeed(this.postUri);
    }
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
