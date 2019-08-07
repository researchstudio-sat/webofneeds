/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import { actionCreators } from "../actions/actions.js";
import { labels } from "../won-label-utils.js";
import { get } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import {
  getAtoms,
  getOwnedAtomByConnectionUri,
} from "../redux/selectors/general-selectors.js";

import "~/style/_group-image.scss";
import WonAtomIcon from "./atom-icon.jsx";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="gi__icons__icon"
        ng-repeat="groupMemberUri in self.groupMembersArray"
        ng-class="{'gi__icons__icon--spanCol': self.groupMembersSize == 1}"
        ng-if="self.groupMembersSize <= 4 || $index < 3">
        <won-preact class="atomImage" component="self.WonAtomIcon" props="{atomUri: groupMemberUri}"></won-preact>
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
      this.WonAtomIcon = WonAtomIcon;

      const selectFromState = state => {
        let groupMembers;

        if (this.connectionUri) {
          const ownedAtom = getOwnedAtomByConnectionUri(
            state,
            this.connectionUri
          );
          const connection =
            ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);
          const targetAtom =
            connection && get(getAtoms(state), connection.get("targetAtomUri"));
          groupMembers = targetAtom && targetAtom.get("groupMembers");
        } else if (this.atomUri) {
          const atom = get(getAtoms(state), this.atomUri);

          groupMembers = atom && atom.get("groupMembers");
        }

        return {
          groupMembersArray: groupMembers ? groupMembers.toArray() : [],
          groupMembersSize: groupMembers ? groupMembers.size : 0,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.atomUri"],
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
      atomUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupImage", [])
  .directive("wonGroupImage", genComponentConf).name;
