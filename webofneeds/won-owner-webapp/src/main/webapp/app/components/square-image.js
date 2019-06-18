import angular from "angular";

import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { connect2Redux } from "../configRedux.js";
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
      alt="Auto-generated title image for persona that holds the atom"
      ng-src="data:image/svg+xml;base64,{{::self.personaIdenticonSvg}}">
  `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.uri]);
        const personaUri = atomUtils.getHeldByUri(atom);
        const persona = getIn(state, ["atoms", personaUri]);
        const personaIdenticonSvg = atomUtils.getIdenticonSvg(persona);

        const isPersona = atomUtils.isPersona(atom);

        const useCaseIcon = !isPersona
          ? atomUtils.getMatchedUseCaseIcon(atom)
          : undefined;
        const useCaseIconBackground = !isPersona
          ? atomUtils.getBackground(atom)
          : undefined;

        const identiconSvg = !useCaseIcon
          ? atomUtils.getIdenticonSvg(atom)
          : undefined;
        const process = get(state, "process");
        return {
          isPersona,
          atomInactive: atomUtils.isInactive(atom),
          atomFailedToLoad:
            atom && processUtils.hasAtomFailedToLoad(process, this.uri),
          useCaseIcon,
          useCaseIconBackground,
          identiconSvg,
          personaIdenticonSvg,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.uri"], this);

      classOnComponentRoot("inactive", () => this.atomInactive, this);

      classOnComponentRoot(
        "won-failed-to-load",
        () => this.atomFailedToLoad,
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
