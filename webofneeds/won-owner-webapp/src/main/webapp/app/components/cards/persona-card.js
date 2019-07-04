import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../actions/actions.js";
import { getIn, get } from "../../utils.js";
import { attach } from "../../cstm-ng-utils.js";
import { connect2Redux } from "../../configRedux.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_persona-card.scss";
import Immutable from "immutable";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- Icon Information -->
      <div class="card__icon clickable"
          ng-class="{
            'inactive': self.isInactive,
          }"
          ng-click="::self.atomClick(self.atomUri)">
          <img class="identicon"
              ng-if="self.showDefaultIcon && self.identiconSvg"
              alt="Auto-generated title image"
              ng-src="data:image/svg+xml;base64,{{::self.identiconSvg}}"/>
          <img class="image"
              ng-if="self.atomImage"
              alt="{{self.atomImage.get('name')}}"
              ng-src="data:{{self.atomImage.get('type')}};base64,{{self.atomImage.get('data')}}"/>
      </div>
      <!-- Main Information -->
      <div class="card__main clickable" ng-click="::self.atomClick(self.atomUri)">
          <div class="card__main__name">
              {{ self.personaName }}
          </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const identiconSvg = atomUtils.getIdenticonSvg(atom);
        const atomImage = atomUtils.getDefaultPersonaImage(atom);

        return {
          isInactive: atomUtils.isInactive(atom),
          atom,
          personaName: get(atom, "humanReadable"),
          atomImage,
          showDefaultIcon: !atomImage,
          identiconSvg,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.atomUri", "self.disableDefaultAtomInteraction"],
        this
      );
    }

    generateTitle() {
      return this.atom.get("humanReadable");
    }

    atomClick(atomUri) {
      if (!this.disableDefaultAtomInteraction) {
        this.showAtomTab(atomUri, "DETAIL");
      }
    }

    showAtomTab(atomUri, tab = "DETAIL") {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
      );
      this.router__stateGo("post", { postUri: atomUri });
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      atomUri: "=",
      disableDefaultAtomInteraction: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.personaCard", [])
  .directive("wonPersonaCard", genComponentConf).name;
