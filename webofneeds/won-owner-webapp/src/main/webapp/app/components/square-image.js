import angular from "angular";

import { attach, get, getIn } from "../utils.js";
import * as needUtils from "../need-utils.js";
import * as processUtils from "../process-utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <div class="image usecaseimage" style="background-color: {{::self.useCaseIconBackground}}"
      ng-if="::self.useCaseIcon">
      <svg class="si__usecaseicon">
        <use xlink:href="{{ ::self.useCaseIcon }}" href="{{ ::self.useCaseIcon }}"></use>
      </svg>
    </div>
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
        const personaUri = get(need, "heldBy");
        const persona = getIn(state, ["needs", personaUri]);
        const personaIdenticonSvg = needUtils.getIdenticonSvg(persona);

        const isPersona = needUtils.isPersona(need);

        const useCaseIcon = !isPersona
          ? needUtils.getMatchedUseCaseIcon(need)
          : undefined;
        const useCaseIconBackground = !isPersona
          ? needUtils.getBackground(need)
          : undefined;

        const identiconSvg = !useCaseIcon
          ? needUtils.getIdenticonSvg(need)
          : undefined;
        const process = get(state, "process");
        return {
          isPersona,
          needInactive: needUtils.isInactive(need),
          needFailedToLoad:
            need && processUtils.hasNeedFailedToLoad(process, this.uri),
          useCaseIcon,
          useCaseIconBackground,
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
