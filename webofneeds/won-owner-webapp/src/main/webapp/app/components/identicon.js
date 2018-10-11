import "@webcomponents/custom-elements";
import { generateRgbColorArray } from "../utils.js";
import shajs from "sha.js";
import Identicon from "identicon.js";

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

export default IdenticonElement;
