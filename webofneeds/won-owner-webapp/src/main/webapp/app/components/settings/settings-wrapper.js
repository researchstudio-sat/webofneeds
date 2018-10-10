import angular from "angular";
import { Elm } from "../../../elm/Settings/Personas.elm";
import Identicon from "identicon.js";
import "@webcomponents/custom-elements";
import { generateRgbColorArray } from "../../utils.js";
import shajs from "sha.js";
import { actionCreators } from "../../actions/actions";

class SvgIcon extends HTMLElement {
  static get observedAttributes() {
    return ["icon", "color"];
  }

  connectedCallback() {
    const svgElement = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "svg"
    );
    svgElement.style.setProperty("--local-primary", this.getAttribute("color"));
    svgElement.style.width = "100%";
    svgElement.style.height = "100%";
    const useElement = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "use"
    );
    useElement.setAttribute("xlink:href", "#" + this.getAttribute("icon"));
    useElement.setAttribute("href", "#" + this.getAttribute("icon"));
    svgElement.appendChild(useElement);
    this.appendChild(svgElement);
  }

  attributeChangedCallback(name, oldValue, newValue) {
    switch (name) {
      case "icon": {
        const link = this.querySelector("svg > use");
        if (link) {
          link.setAttribute("xlink:href", "#" + newValue);
          link.setAttribute("href", "#" + newValue);
        }
        break;
      }
      case "color": {
        const svg = this.querySelector("svg");
        if (svg) svg.style.setProperty("--local-primary", newValue);
        break;
      }
    }
  }
}

customElements.define("svg-icon", SvgIcon);

class IdenticonElement extends HTMLElement {
  static get observedAttributes() {
    return ["data"];
  }

  connectedCallback() {
    const hash = new shajs.sha512()
      .update(this.getAttribute("data"))
      .digest("hex");
    const rgbColorArray = generateRgbColorArray(hash);
    const identicon = new Identicon(hash, {
      size: 100,
      foreground: [255, 255, 255, 255], // rgba white
      background: [...rgbColorArray, 255], // rgba
      margin: 0.2,
      format: "svg",
    });

    const imgElement = document.createElement("img");
    imgElement.setAttribute(
      "src",
      `data:image/svg+xml;base64,${identicon.toString()}`
    );
    imgElement.style.width = "100%";
    imgElement.style.height = "100%";
    this.appendChild(imgElement);
  }

  attributeChangedCallback(name, oldValue, newValue) {
    switch (name) {
      case "data": {
        const hash = new shajs.sha512().update(newValue).digest("hex");
        const rgbColorArray = generateRgbColorArray(hash);
        const identicon = new Identicon(hash, {
          size: 100,
          foreground: [255, 255, 255, 255], // rgba white
          background: [...rgbColorArray, 255], // rgba
          margin: 0.2,
          format: "svg",
        });
        const imgElement = this.querySelector("img");
        if (!imgElement) {
          return;
        }
        imgElement.setAttribute(
          "src",
          `data:image/svg+xml;base64,${identicon.toString()}`
        );
        return;
      }
    }
  }
}

customElements.define("won-identicon", IdenticonElement);

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    link: (scope, element) => {
      const elmApp = Elm.Settings.Personas.init({ node: element[0] });
      elmApp.ports.personaOut.subscribe(persona => {
        $ngRedux.dispatch(actionCreators.personas__create(persona));
      });
      const convertPersonas = personas => {
        const conversion = personas
          .entrySeq()
          .map(([url, persona]) => {
            return {
              url: url,
              ...persona,
            };
          })
          .toJS();
        return conversion;
      };
      const personas = $ngRedux.getState().get("personas");
      if (personas) {
        elmApp.ports.personaIn.send(convertPersonas(personas));
      }

      const disconnect = $ngRedux.connect(state => {
        return { personas: state.get("personas") };
      })(state => {
        if (!state.personas) {
          return;
        }
        elmApp.ports.personaIn.send(convertPersonas(state.personas));
      });

      scope.$on("$destroy", () => {
        elmApp.ports.personaOut.unsubscribe();
        disconnect();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.settingsWrapper", [])
  .directive("settingsWrapper", genComponentConf).name;
