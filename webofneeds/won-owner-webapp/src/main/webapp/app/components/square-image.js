import angular from "angular";

import won from "../won-es6.js";
import { attach, generateRgbColorArray, get, getIn } from "../utils.js";
import { isPersona } from "../need-utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import Identicon from "identicon.js";
window.Identicon4dbg = Identicon;

import shajs from "sha.js";
window.shajs4dbg = shajs;

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

      this.personaIdenticonCacheSvg;
      this.needIdenticonCacheSvg;

      const selectFromState = state => {
        const identiconSvg = this.parseIdenticon(
          this.uri,
          this.needIdenticonCacheSvg
        );

        const need = getIn(state, ["needs", this.uri]);
        const personaUri = get(need, "heldBy");

        const personaIdenticonSvg = this.parseIdenticon(
          personaUri,
          this.personaIdenticonCacheSvg
        );

        return {
          isPersona: isPersona(need),
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

    parseIdenticon(input, cache) {
      if (cache) {
        return cache;
      }

      if (!input) {
        return;
      }
      // quick extra hash here as identicon.js only uses first 15
      // chars (which aren't very unique for our uris due to the base-url):
      const hash = new shajs.sha512().update(input).digest("hex");
      const rgbColorArray = generateRgbColorArray(hash);
      const idc = new Identicon(hash, {
        size: 100,
        foreground: [255, 255, 255, 255], // rgba white
        background: [...rgbColorArray, 255], // rgba
        margin: 0.2,
        format: "svg",
      });
      cache = idc.toString();
      return cache;
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
