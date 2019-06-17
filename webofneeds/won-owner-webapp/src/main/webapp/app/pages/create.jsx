/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import won from "../won-es6.js";
import sendRequestModule from "../components/send-request.js";

import createPostModule from "../components/create-post.js";
import createSearchModule from "../components/create-search.js";
import usecasePickerModule from "../components/usecase-picker.js";
import usecaseGroupModule from "../components/usecase-group.js";
import { attach, getIn, get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import { h } from "preact";

import "~/style/_create.scss";
import "~/style/_responsiveness-utils.scss";
import * as accountUtils from "../redux/utils/account-utils";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <won-topnav page-title="::'Create'" />
    <won-menu ng-if="self.isLoggedIn" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    {/* RIGHT SIDE */}
    <main className="ownercreate">
      <won-usecase-picker ng-if="self.showUseCasePicker" />
      <won-usecase-group ng-if="self.showUseCaseGroups" />
      <won-create-post ng-if="self.showCreatePost" />
      <won-create-search ng-if="self.showCreateSearch" />
    </main>
    <won-footer />
  </container>
);

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class CreateController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.WON = won.WON;

    const selectFromState = state => {
      const useCase = generalSelectors.getUseCaseFromRoute(state);
      const useCaseGroup = generalSelectors.getUseCaseGroupFromRoute(state);

      const fromAtomUri = generalSelectors.getFromAtomUriFromRoute(state);
      const mode = generalSelectors.getModeFromRoute(state);

      const showCreateFromPost = !!(fromAtomUri && mode);

      const showUseCaseGroups = !useCase && !!useCaseGroup;
      const showCreatePost = showCreateFromPost || (!!useCase && useCase !== "search");
      const showCreateSearch = !!useCase && useCase === "search";

      const accountState = get(state, "account");

      return {
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showUseCasePicker: !(showUseCaseGroups || showCreatePost || showCreateSearch),
        showUseCaseGroups,
        showCreatePost,
        showCreateSearch,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);
  }
}

CreateController.$inject = [];

export default {
  module: angular
    .module("won.owner.components.create", [
      ngAnimate,
      sendRequestModule,
      usecasePickerModule,
      usecaseGroupModule,
      createPostModule,
      createSearchModule,
    ])
    .controller("CreateController", [...serviceDependencies, CreateController]).name,
  controller: "CreateController",
  template: template,
};
