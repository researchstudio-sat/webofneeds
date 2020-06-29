import "@webcomponents/custom-elements";

// import icons from "../../images/won-icons/*.svg"; //FIXME: WEBPACK IMPORT
import icons from "../../images/won-icons/ico36_detail_description.svg";

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
    const useElement = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "use"
    );
    useElement.setAttributeNS(
      "http://www.w3.org/1999/xlink",
      "xlink:href",
      icons[this.getAttribute("icon")]
    );
    useElement.setAttribute("href", icons[this.getAttribute("icon")]);
    svgElement.appendChild(useElement);
    this.appendChild(svgElement);
  }

  attributeChangedCallback(name, oldValue, newValue) {
    switch (name) {
      case "icon": {
        const link = this.querySelector("svg > use");
        if (link) {
          link.setAttributeNS(
            "http://www.w3.org/1999/xlink",
            "xlink:href",
            icons[newValue]
          );
          link.setAttribute("href", icons[newValue]);
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

customElements.define("won-svg-icon", SvgIcon);

export default SvgIcon;
