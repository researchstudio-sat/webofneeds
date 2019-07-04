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
      ng-if="self.useCaseIcon">
      <svg class="si__usecaseicon">
        <use xlink:href="{{ ::self.useCaseIcon }}" href="{{ ::self.useCaseIcon }}"></use>
      </svg>
    </div>
    <img class="image"
      ng-if="self.showIdenticon"
      alt="Auto-generated title icon"
      ng-src="data:image/svg+xml;base64,{{::self.identiconSvg}}"/>
    <img class="image"
      ng-if="self.showImage"
      alt="{{self.image.get('name')}}"
      ng-src="data:{{self.image.get('type')}};base64,{{self.image.get('data')}}"/>
    <img class="holderIcon"
      ng-if="::self.showHolderIdenticon"
      alt="Auto-generated title image for persona that holds the atom"
      ng-src="data:image/svg+xml;base64,{{::self.holderIdenticonSvg}}"/>
    <img class="holderIcon"
      ng-if="::self.showHolderImage"
      alt="{{self.holderImage.get('name')}}"
      ng-src="data:{{self.holderImage.get('type')}};base64,{{self.holderImage.get('data')}}"/>
  `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.uri]);
        const isPersona = atomUtils.isPersona(atom);
        const image = isPersona && atomUtils.getDefaultPersonaImage(atom);

        const useCaseIcon = !isPersona
          ? atomUtils.getMatchedUseCaseIcon(atom)
          : undefined;
        const useCaseIconBackground = !isPersona
          ? atomUtils.getBackground(atom)
          : undefined;

        const identiconSvg = !useCaseIcon
          ? atomUtils.getIdenticonSvg(atom)
          : undefined;

        // Icons/Images of the AtomHolder
        const personaUri = atomUtils.getHeldByUri(atom);
        const persona = getIn(state, ["atoms", personaUri]);
        const holderImage = atomUtils.getDefaultPersonaImage(persona);
        const holderIdenticonSvg = atomUtils.getIdenticonSvg(persona);
        const showHolderIdenticon = !holderImage && holderIdenticonSvg;
        const showHolderImage = holderImage;

        const process = get(state, "process");
        return {
          isPersona,
          atomImage: isPersona && atomUtils.getDefaultPersonaImage(atom),
          atomInactive: atomUtils.isInactive(atom),
          atomFailedToLoad:
            atom && processUtils.hasAtomFailedToLoad(process, this.uri),
          useCaseIcon,
          useCaseIconBackground,
          showIdenticon: !image && identiconSvg,
          showImage: image,
          identiconSvg,
          image,
          showHolderIdenticon,
          showHolderImage,
          holderImage,
          holderIdenticonSvg,
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
