/** @jsx h */
/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { delay, get, getIn } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import won from "../won-es6.js";

import { actionCreators } from "../actions/actions.js";
import WonAtomInfo from "../components/atom-info.jsx";
import WonAtomMessages from "../components/atom-messages.jsx";
import WonModalDialog from "../components/modal-dialog.jsx";
import WonToasts from "../components/toasts.jsx";
import WonMenu from "../components/menu.jsx";
import WonSlideIn from "../components/slide-in.jsx";
import WonFooter from "../components/footer.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { h } from "preact";

import "~/style/_post.scss";
import "~/style/_connection-overlay.scss";
import * as accountUtils from "../redux/utils/account-utils";

const template = (
  <container>
    <won-preact
      className="modalDialog"
      component="self.WonModalDialog"
      props="{}"
      ng-if="self.showModalDialog"
    />
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-preact
        component="self.WonAtomMessages"
        props="{connectionUri: self.viewConnUri}"
        className="atomMessages"
      />
    </div>
    <won-topnav page-title="self.atomTitle" />
    <won-preact
      className="menu"
      component="self.WonMenu"
      props="{}"
      ng-if="self.isLoggedIn"
    />
    <won-preact className="toasts" component="self.WonToasts" props="{}" />
    <won-preact
      className="slideIn"
      component="self.WonSlideIn"
      props="{}"
      ng-if="self.showSlideIns"
    />
    <main className="postcontent">
      <won-preact
        class="atomInfo"
        component="self.WonAtomInfo"
        props="{atomUri: self.atomUri}"
        ng-if="!(self.atomLoading || self.atomFailedToLoad) && self.atom"
      />
      <div className="pc__loading" ng-if="self.atomLoading">
        <svg className="pc__loading__spinner hspinner">
          <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
        </svg>
        <span className="pc__loading__label">Loading...</span>
      </div>
      <div className="pc__failed" ng-if="self.atomFailedToLoad">
        <svg className="pc__failed__icon">
          <use
            xlinkHref="#ico16_indicator_error"
            href="#ico16_indicator_error"
          />
        </svg>
        <span className="pc__failed__label">
          Failed To Load - Atom might have been deleted
        </span>
        <div className="pc__failed__actions">
          <button
            className="pc__failed__actions__button red won-button--outlined thin"
            ng-click="self.tryReload()"
          >
            Try Reload
          </button>
        </div>
      </div>
    </main>
    <won-preact className="footer" component="self.WonFooter" props="{}" />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    window.p4dbg = this;
    this.WON = won.WON;
    this.WonAtomInfo = WonAtomInfo;
    this.WonAtomMessages = WonAtomMessages;
    this.WonModalDialog = WonModalDialog;
    this.WonToasts = WonToasts;
    this.WonMenu = WonMenu;
    this.WonSlideIn = WonSlideIn;
    this.WonFooter = WonFooter;

    const selectFromState = state => {
      const atomUri = generalSelectors.getPostUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const atom = getIn(state, ["atoms", atomUri]);

      const process = get(state, "process");
      const accountState = get(state, "account");

      return {
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        atomUri,
        isOwnedAtom: generalSelectors.isAtomOwned(state, atomUri),
        atom,
        atomTitle: get(atom, "humanReadable"),
        won: won.WON,
        showSlideIns:
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showConnectionOverlay: !!viewConnUri,
        viewConnUri,
        atomLoading: !atom || processUtils.isAtomLoading(process, atomUri),
        atomToLoad: !atom || processUtils.isAtomToLoad(process, atomUri),
        atomFailedToLoad:
          atom && processUtils.hasAtomFailedToLoad(process, atomUri),
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);

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

export default {
  module: angular
    .module("won.owner.components.post", [ngAnimate])
    .controller("PostController", Controller).name,
  controller: "PostController",
  template: template,
};
