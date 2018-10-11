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

customElements.define("won-svg-icon", SvgIcon);

export default SvgIcon;
