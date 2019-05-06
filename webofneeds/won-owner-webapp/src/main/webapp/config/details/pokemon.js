//import Immutable from "immutable";
import won from "../../app/won-es6.js";

export const pokemonGym = {
  identifier: "pokemonGym",
  label: "Gym",
  icon: "#ico36_dumbbell", //TODO: Create and use better icon
  messageEnabled: false,
  component: "won-title-picker",
  viewerComponent: "won-title-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "won:gym": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["won:gym"], "xsd:string");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const pokemonRaid = {
  identifier: "pokemonRaid",
  label: "Raid Boss",
  icon: "#ico36_pokeball", //TODO: Create and use better icon
  messageEnabled: false,
  component: "won-title-picker",
  viewerComponent: "won-title-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "won:raid": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["won:raid"], "xsd:string");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};
