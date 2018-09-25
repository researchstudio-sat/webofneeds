import angular from "angular";
import { Elm } from "../../../elm/Settings.elm";
import Identicon from "identicon.js";
import "@webcomponents/custom-elements";
import { generateRgbColorArray } from "../../utils.js";
import shajs from "sha.js";

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
}

customElements.define("svg-icon", SvgIcon);

class IdenticonElement extends HTMLElement {
  static get observedAttributes() {
    return ["hash"];
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
}

customElements.define("won-identicon", IdenticonElement);

function genComponentConf() {
  return {
    restrict: "E",
    link: (scope, element) => {
      Elm.Settings.init({ node: element[0] });
    },
  };
}
export default angular
  .module("won.owner.components.settingsWrapper", [])
  .directive("settingsWrapper", genComponentConf).name;
