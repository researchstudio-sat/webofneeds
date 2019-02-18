/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn, get, delay } from "../../utils.js";
import { connect2Redux } from "../../won-utils.js";
import won from "../../won-es6.js";
import { actionCreators } from "../../actions/actions.js";
import sendRequestModule from "../send-request.js";
import visitorTitleBarModule from "../visitor-title-bar.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as needUtils from "../../need-utils.js";
import * as processUtils from "../../process-utils.js";
import * as srefUtils from "../../sref-utils.js";

import "style/_post-visitor.scss";
import "style/_need-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.p4dbg = this;
    this.WON = won.WON;
    Object.assign(this, srefUtils); // bind srefUtils to scope

    const selectFromState = state => {
      const needUri = generalSelectors.getPostUriFromRoute(state);
      const viewNeedUri = generalSelectors.getViewNeedUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const need = getIn(state, ["needs", needUri]);

      const process = get(state, "process");

      return {
        needUri,
        isOwnedNeed: needUtils.isOwned(need),
        need,
        won: won.WON,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showNeedOverlay: !!viewNeedUri,
        showConnectionOverlay: !!viewConnUri,
        viewNeedUri,
        viewConnUri,
        needLoading: !need || processUtils.isNeedLoading(process, needUri),
        needToLoad: !need || processUtils.isNeedToLoad(process, needUri),
        needFailedToLoad:
          need && processUtils.hasNeedFailedToLoad(process, needUri),
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);

    this.$scope.$watch(
      () =>
        this.needUri && (!this.need || (this.needToLoad && !this.needLoading)),
      () => delay(0).then(() => this.ensureNeedIsLoaded())
    );
  }

  ensureNeedIsLoaded() {
    if (
      this.needUri &&
      (!this.need || (this.needToLoad && !this.needLoading))
    ) {
      this.needs__fetchUnloadedNeed(this.needUri);
    }
  }

  tryReload() {
    if (this.needUri && this.needFailedToLoad) {
      this.needs__fetchUnloadedNeed(this.needUri);
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
