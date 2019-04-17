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
import postMessagesModule from "../post-messages.js";
import groupPostMessagesModule from "../group-post-messages.js";
import visitorTitleBarModule from "../visitor-title-bar.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as processUtils from "../../process-utils.js";
import * as srefUtils from "../../sref-utils.js";

import "style/_post-visitor.scss";
import "style/_atom-overlay.scss";
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
      const atomUri = generalSelectors.getPostUriFromRoute(state);
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const atom = getIn(state, ["atoms", atomUri]);

      const process = get(state, "process");

      return {
        atomUri,
        isOwnedAtom: generalSelectors.isAtomOwned(state, atomUri),
        atom,
        won: won.WON,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showAtomOverlay: !!viewAtomUri,
        showConnectionOverlay: !!viewConnUri,
        viewAtomUri,
        viewConnUri,
        atomLoading: !atom || processUtils.isAtomLoading(process, atomUri),
        atomToLoad: !atom || processUtils.isAtomToLoad(process, atomUri),
        atomFailedToLoad:
          atom && processUtils.hasAtomFailedToLoad(process, atomUri),
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);

    this.$scope.$watch(
      () =>
        this.atomUri && (!this.atom || (this.atomToLoad && !this.atomLoading)),
      () => delay(0).then(() => this.ensureAtomIsLoaded())
    );
  }

  ensureAtomIsLoaded() {
    if (
      this.atomUri &&
      (!this.atom || (this.atomToLoad && !this.atomLoading))
    ) {
      this.atoms__fetchUnloadedAtom(this.atomUri);
    }
  }

  tryReload() {
    if (this.atomUri && this.atomFailedToLoad) {
      this.atoms__fetchUnloadedAtom(this.atomUri);
    }
  }
}

Controller.$inject = serviceDependencies;

export default angular
  .module("won.owner.components.post", [
    sendRequestModule,
    ngAnimate,
    visitorTitleBarModule,
    postMessagesModule,
    groupPostMessagesModule,
  ])
  .controller("PostController", Controller).name;
