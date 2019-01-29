import angular from "angular";

import won from "../won-es6.js";
import { attach, get, getIn } from "../utils.js";
import * as needUtils from "../need-utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <img class="image"
      ng-if="::self.identiconSvg"
      alt="Auto-generated title image"
      ng-src="data:image/svg+xml;base64,{{::self.identiconSvg}}">
    <img class="personaImage"
      ng-if="::self.personaIdenticonSvg"
      alt="Auto-generated title image for persona that holds the need"
      ng-src="data:image/svg+xml;base64,{{::self.personaIdenticonSvg}}">
  `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const need = getIn(state, ["needs", this.uri]);
        const identiconSvg = needUtils.getIdenticonSvg(need);

        const personaUri = get(need, "heldBy");
        const persona = getIn(state, ["needs", personaUri]);
        const personaIdenticonSvg = needUtils.getIdenticonSvg(persona);

        return {
          isPersona: needUtils.isPersona(need),
          needInactive:
            need && get(need, "state") === won.WON.InactiveCompacted,
          needFailedToLoad:
            need &&
            getIn(state, ["process", "needs", need.get("uri"), "failedToLoad"]),
          identiconSvg,
          personaIdenticonSvg,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.uri"], this);

      classOnComponentRoot("inactive", () => this.needInactive, this);

      classOnComponentRoot(
        "won-failed-to-load",
        () => this.needFailedToLoad,
        this
      );

      classOnComponentRoot("won-is-persona", () => this.isPersona, this);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      uri: "=", // only read once
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.squareImage", [])
  .directive("wonSquareImage", genComponentConf).name;
