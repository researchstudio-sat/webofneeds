/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the remoteNeed are shown
 *    need-uri: then the participants of the need behind the need uri are shown
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
        ng-class="{'gi__icons__icon--spanCol': self.groupMembersSize == 1}"
        ng-if="self.groupMembersSize <= 4 || $index < 3">
        <won-square-image
          uri="groupMemberUri">
        </won-square-image>
      </div>
      <div class="gi__icons__more" ng-if="self.groupMembersSize > 4">
         +
      </div>
      <div
        class="gi__icons__more"
        ng-if="self.groupMembersSize <= 3"
        ng-class="{
          'gi__icons__more--spanCol': self.groupMembersSize == 2 || self.groupMembersSize == 0,
          'gi__icons__more--spanRow': self.groupMembersSize == 0
        }">
        {{ self.groupMembersSize }}
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;
      this.WON = won.WON;
      const selectFromState = state => {
        let groupMembers;

        if (this.connectionUri) {
          const ownedNeed = getOwnedNeedByConnectionUri(
            state,
            this.connectionUri
          );
          const connection =
            ownedNeed && ownedNeed.getIn(["connections", this.connectionUri]);
          const remoteNeed =
            connection && get(getNeeds(state), connection.get("remoteNeedUri"));
          groupMembers = remoteNeed && remoteNeed.get("groupMembers");
        } else if (this.needUri) {
          const need = get(getNeeds(state), this.needUri);

          groupMembers = need && need.get("groupMembers");
        }

        return {
          groupMembersArray: groupMembers ? groupMembers.toArray() : [],
          groupMembersSize: groupMembers ? groupMembers.size : 0,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.needUri"],
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
      needUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupImage", [squareImageModule])
  .directive("wonGroupImage", genComponentConf).name;
