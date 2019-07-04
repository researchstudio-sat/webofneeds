/** @jsx h */
/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { getIn, get, delay } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import won from "../won-es6.js";

import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import groupPostMessagesModule from "../components/group-post-messages.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { h } from "preact";

import "~/style/_post-visitor.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-post-messages connection-uri="self.viewConnUri" />
    </div>
    <won-topnav page-title="self.atomTitle" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main
      className="postcontent"
      ng-class="{'postcontent--won-loading': self.atomLoading, 'postcontent--won-failed': self.atomFailedToLoad}"
    >
      <won-post-info
        ng-if="!(self.atomLoading || self.atomFailedToLoad) && self.atom"
        atom-uri="self.atomUri"
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
    <won-footer />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.p4dbg = this;
    this.WON = won.WON;

    const selectFromState = state => {
      const atomUri = generalSelectors.getPostUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const atom = getIn(state, ["atoms", atomUri]);

      const process = get(state, "process");

      return {
        atomUri,
        isOwnedAtom: generalSelectors.isAtomOwned(state, atomUri),
        atom,
        atomTitle: get(atom, "humanReadable"),
        won: won.WON,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
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
    .module("won.owner.components.post", [
      ngAnimate,
      postMessagesModule,
      groupPostMessagesModule,
    ])
    .controller("PostController", Controller).name,
  controller: "PostController",
  template: template,
};
