/**
 * Created by quasarchimaere on 08.01.2019.
 */

import angular from "angular";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import { getGroupChatPostUriFromRoute } from "../selectors/general-selectors.js";

import connectionHeaderModule from "./connection-header.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_group-chat-selection-item-line.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- todo impl and include groupchat header -->
      <div class="gcsi__label clickable"
        ng-click="self.setOpen()">
        Group Chat (click to view)
      </div>
      <!--won-connection-header
        connection-uri="self.connectionUri"
        timestamp="self.lastUpdateTimestamp"
        ng-click="self.setOpen()"
        class="clickable">
      </won-connection-header-->
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const openGroupChatPostUri = getGroupChatPostUriFromRoute(state);

        return {
          openGroupChatPostUri,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);

      classOnComponentRoot("selected", () => this.isOpen(), this);
    }
    isOpen() {
      return this.openGroupChatPostUri === this.needUri;
    }

    setOpen() {
      this.onSelected({ needUri: this.needUri }); //trigger callback with scope-object
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      needUri: "=",
      onSelected: "&",
    },
    template: template,
  };
}
export default angular
  .module("won.owner.components.groupChatSelectionItem", [
    connectionHeaderModule,
  ])
  .directive("wonGroupChatSelectionItem", genComponentConf).name;
