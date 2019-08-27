/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";

import WonCreateAtom from "../components/create-atom.jsx";
import WonModalDialog from "../components/modal-dialog.jsx";
import WonToasts from "../components/toasts.jsx";
import WonMenu from "../components/menu.jsx";
import WonFooter from "../components/footer.jsx";
import createSearchModule from "../components/create-search.js";
import usecasePickerModule from "../components/usecase-picker.js";
import usecaseGroupModule from "../components/usecase-group.js";
import { get, getIn } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import { h } from "preact";

import "~/style/_create.scss";
import "~/style/_responsiveness-utils.scss";
import * as accountUtils from "../redux/utils/account-utils";

const template = (
  <container>
    <won-preact
      className="modalDialog"
      component="self.WonModalDialog"
      props="{}"
      ng-if="self.showModalDialog"
    />
    <won-topnav page-title="::'Create'" />
    <won-preact
      className="menu"
      component="self.WonMenu"
      props="{}"
      ng-if="self.isLoggedIn"
    />
    <won-preact className="toasts" component="self.WonToasts" props="{}" />
    <won-slide-in ng-if="self.showSlideIns" />
    {/* RIGHT SIDE */}
    <main className="ownercreate">
      <won-usecase-picker ng-if="self.showUseCasePicker" />
      <won-usecase-group ng-if="self.showUseCaseGroups" />
      <won-preact
        component="self.WonCreateAtom"
        props="{}"
        ng-if="self.showCreatePost"
      />
      <won-create-search ng-if="self.showCreateSearch" />
    </main>
    <won-preact className="footer" component="self.WonFooter" props="{}" />
  </container>
);

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class CreateController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.WonCreateAtom = WonCreateAtom;
    this.WonModalDialog = WonModalDialog;
    this.WonToasts = WonToasts;
    this.WonMenu = WonMenu;
    this.WonFooter = WonFooter;

    const selectFromState = state => {
      const useCase = generalSelectors.getUseCaseFromRoute(state);
      const useCaseGroup = generalSelectors.getUseCaseGroupFromRoute(state);

      const fromAtomUri = generalSelectors.getFromAtomUriFromRoute(state);
      const mode = generalSelectors.getModeFromRoute(state);

      const showCreateFromPost = !!(fromAtomUri && mode);

      const showUseCaseGroups = !useCase && !!useCaseGroup;
      const showCreatePost =
        showCreateFromPost || (!!useCase && useCase !== "search");
      const showCreateSearch = !!useCase && useCase === "search";

      const accountState = get(state, "account");

      return {
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showUseCasePicker: !(
          showUseCaseGroups ||
          showCreatePost ||
          showCreateSearch
        ),
        showUseCaseGroups,
        showCreatePost,
        showCreateSearch,
        showSlideIns:
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);
    this.$scope.$on("$destroy", disconnect);
  }
}

CreateController.$inject = [];

export default {
  module: angular
    .module("won.owner.components.create", [
      ngAnimate,
      usecasePickerModule,
      usecaseGroupModule,
      createSearchModule,
    ])
    .controller("CreateController", [...serviceDependencies, CreateController])
    .name,
  controller: "CreateController",
  template: template,
};
