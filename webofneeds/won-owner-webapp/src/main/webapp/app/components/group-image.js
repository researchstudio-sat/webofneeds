/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Created by quasarchimaere on 15.01.2019.
 */
import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import squareImageModule from "./square-image.js";
import { actionCreators } from "../actions/actions.js";
import { labels } from "../won-label-utils.js";
import { attach, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { isChatToGroup } from "../connection-utils.js";
import {
  getOwnedNeedByConnectionUri,
  getNeeds,
} from "../selectors/general-selectors.js";

import "style/_group-image.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="gi__icons__icon"
        ng-repeat="groupMemberUri in self.groupMembersArray"
        ng-if="self.groupMembersSize <= 4 || $index < 3">
        <won-square-image
          uri="groupMemberUri">
        </won-square-image>
      </div>
      <div class="gi__icons__more" ng-if="self.groupMembersSize > 4">
         +
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;
      this.WON = won.WON;
      const selectFromState = state => {
        const ownedNeed = getOwnedNeedByConnectionUri(
          state,
          this.connectionUri
        );
        const connection =
          ownedNeed && ownedNeed.getIn(["connections", this.connectionUri]);
        const remoteNeed =
          connection && get(getNeeds(state), connection.get("remoteNeedUri"));
        const groupMembers = remoteNeed && remoteNeed.get("groupMembers");

        return {
          connection,
          groupMembersArray: groupMembers && groupMembers.toArray(),
          groupMembersSize: groupMembers ? groupMembers.size : 0,
          ownedNeed,
          remoteNeed,
          isConnectionToGroup: isChatToGroup(
            state.get("needs"),
            get(ownedNeed, "uri"),
            this.connectionUri
          ),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupImage", [squareImageModule])
  .directive("wonGroupImage", genComponentConf).name;
