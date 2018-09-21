import angular from "angular";
import { Elm } from "./Settings.elm";
import "@webcomponents/custom-elements";

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

function genComponentConf() {
  return {
    restrict: "E",
    link: (scope, element) => {
      Elm.Main.init({ node: element[0] });
    },
  };
}
export default angular
  .module("won.owner.components.settingsWrapper", [])
  .directive("settingsWrapper", genComponentConf).name;
