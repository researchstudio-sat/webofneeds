import "@webcomponents/custom-elements";

import ico16_arrow_down from "../../images/won-icons/ico16_arrow_down.svg";
import ico16_arrow_up from "../../images/won-icons/ico16_arrow_up.svg";
import ico36_plus from "../../images/won-icons/ico36_plus.svg";

class SvgIcon extends HTMLElement {
  static get observedAttributes() {
    return ["icon", "color"];
  }

  getIcon(iconName) {
    if (iconName === "ico16_arrow_down") {
      return ico16_arrow_down;
    } else if (iconName === "ico16_arrow_up") {
      return ico16_arrow_up;
    } else {
      return ico36_plus;
    }
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
      this.getIcon(this.getAttribute("icon"))
    );
    useElement.setAttribute("href", this.getIcon(this.getAttribute("icon")));
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
            this.getIcon(newValue)
          );
          link.setAttribute("href", this.getIcon(newValue));
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
