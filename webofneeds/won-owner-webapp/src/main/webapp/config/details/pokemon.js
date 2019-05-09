import Immutable from "immutable";
import won from "../../app/won-es6.js";
import {
  isValidDate,
  //parseDatetimeStrictly,
  toLocalISODateString,
  getIn,
} from "../../app/utils.js";
import {
  genSPlace,
  genDetailBaseUri,
  parseSPlace,
} from "../../app/won-utils.js";

export const pokemonGym = {
  identifier: "pokemonGym",
  label: "Gym",
  icon: "#ico36_dumbbell", //TODO: Create and use better icon
  messageEnabled: false,
  component: "pokemon-gym-picker",
  viewerComponent: "won-description-viewer", //TODO: IMPL CORRECT VIEWER
  locationDetail: {
    placeholder: "Search for location",
  },
  infoDetail: {
    placeholder: "Info about this gym (not mandatory, markdown enabled)",
  },
  nameDetail: {
    placeholder: "Name (mandatory)",
  },
  isValid: function(value) {
    return value && value.location && value.name;
  },
  parseToRDF: function({ value, identifier, contentUri }) {
    //TODO: IMPL CORRECT FUNCTION

    if (this.isValid(value)) {
      return {
        "won:gym": {
          "s:name": { "@value": value.name, "@type": "xsd:string" },
          "s:description": value.info
            ? { "@value": value.info, "@type": "xsd:string" }
            : undefined,
          "won:gymex": { "@value": !!value.ex, "@type": "xsd:boolean" },
          "s:location": genSPlace({
            geoData: value.location,
            baseUri: genDetailBaseUri(contentUri, identifier),
          }),
        },
      };
    }
    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    if (jsonLDImm) {
      const name = won.parseFrom(
        jsonLDImm,
        ["won:gym", "s:name"],
        "xsd:string"
      );

      const jsonLdLocation = getIn(jsonLDImm, ["won:gym", "s:location"]);
      const location = jsonLdLocation && parseSPlace(jsonLdLocation);

      if (name && location) {
        const info = won.parseFrom(
          jsonLDImm,
          ["won:gym", "s:description"],
          "xsd:string"
        );

        const ex = won.parseFrom(
          jsonLDImm,
          ["won:gym", "won:gymex"],
          "xsd:boolean"
        );

        return Immutable.fromJS({
          name,
          info,
          location,
          ex,
        });
      }
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value && value.name) {
      let labelPart = value.name + (value.ex ? " (Ex Gym)" : "");

      return includeLabel ? this.label + ": " + labelPart : labelPart;
    }
    return undefined;
  },
};

export const pokemonRaid = {
  identifier: "pokemonRaid",
  label: "Raid Boss",
  icon: "#ico36_pokeball", //TODO: Create and use better icon
  messageEnabled: false,
  component: "pokemon-raidboss-picker",
  viewerComponent: "pokemon-raidboss-viewer",
  filterDetail: {
    placeholder: "Filter by (name or id)",
  },
  pokemonList: [
    //TODO: UPDATE LIST OR EXTRACT INTO SEPARATE FILE OR FIND WS that supplies these
    {
      id: 1,
      name: "Bulbasaur",
      imageUrl: "https://files.pokefans.net/images/pokemon-go/modelle/001.png",
    },
    {
      id: 2,
      name: "Ivysaur",
      imageUrl: "https://files.pokefans.net/images/pokemon-go/modelle/002.png",
    },
    {
      id: 3,
      name: "Venusaur",
      imageUrl: "https://files.pokefans.net/images/pokemon-go/modelle/003.png",
    },
    {
      id: 4,
      name: "Charmander",
      imageUrl: "https://files.pokefans.net/images/pokemon-go/modelle/004.png",
    },
    {
      id: 386,
      name: "Deoxys",
      imageUrl: "https://files.pokefans.net/images/pokemon-go/modelle/386.png",
    },
    {
      id: 386,
      form: "init",
      name: "Deoxys",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/386-04.png",
    },
  ],
  findPokemonById: function(id, form) {
    if (id) {
      for (const idx in this.pokemonList) {
        if (
          this.pokemonList[idx].id == id &&
          ((form && this.pokemonList[idx].form === form) ||
            (!form && !this.pokemonList[idx].form))
        ) {
          return this.pokemonList[idx];
        }
      }
    }
    return undefined;
  },
  getPokemonNameById: function(id, form) {
    const pokemon = this.findPokemonById(id, form);
    return pokemon && pokemon.name;
  },
  isValid: function(value) {
    if (
      !value ||
      !value.level ||
      !value.expires ||
      (!value.id && value.hatched) ||
      (!value.hatched && !value.hatches)
    )
      return false;

    const isAfter = (beforeDate, afterDate) => {
      if (beforeDate && isValidDate(beforeDate)) {
        return isValidDate(afterDate) && beforeDate < afterDate;
      }
      return !!afterDate;
    };
    return value.hatched || isAfter(value.hatches, value.expires);
  },
  getLevelLabel: function(level) {
    switch (level) {
      case 1:
        return "Level 1 ★";
      case 2:
        return "Level 2 ★★";
      case 3:
        return "Level 3 ★★★ (Rare)";
      case 4:
        return "Level 4 ★★★★ (Rare)";
      case 5:
        return "Level 5 ★★★★★ (Legendary)";
    }
    return level;
  },
  parseToRDF: function({ value }) {
    if (this.isValid(value)) {
      if (value.hatched) {
        return {
          "won:raid": {
            "won:level": { "@value": value.level, "@type": "xsd:int" },
            "won:pokemonid": { "@value": value.id, "@type": "xsd:int" },
            "won:pokemonform": value.form
              ? { "@value": value.form, "@type": "xsd:string" }
              : undefined,
            "s:validThrough": {
              "@value": toLocalISODateString(value.expires),
              "@type": "xsd:dateTime",
            },
          },
        };
      } else {
        return {
          "won:raid": {
            "won:level": { "@value": value.level, "@type": "xsd:int" },
            "s:validFrom": {
              "@value": toLocalISODateString(value.hatches),
              "@type": "xsd:dateTime",
            },
            "s:validThrough": {
              "@value": toLocalISODateString(value.expires),
              "@type": "xsd:dateTime",
            },
          },
        };
      }
    }
    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const level = won.parseFrom(
      jsonLDImm,
      ["won:raid", "won:level"],
      "xsd:int"
    );
    const expires = won.parseFrom(
      jsonLDImm,
      ["won:raid", "s:validThrough"],
      "xsd:dateTime"
    );

    if (level && expires) {
      const id = won.parseFrom(
        jsonLDImm,
        ["won:raid", "won:pokemonid"],
        "xsd:int"
      );
      const hatched = !!id;
      const hatches =
        !hatched &&
        won.parseFrom(jsonLDImm, ["won:raid", "s:validFrom"], "xsd:dateTime");

      const form =
        id &&
        won.parseFrom(jsonLDImm, ["won:raid", "won:pokemonform"], "xsd:string");

      if (hatched) {
        return Immutable.fromJS({
          id,
          form,
          hatched,
          level,
          expires,
        });
      } else if (hatches) {
        return Immutable.fromJS({
          hatched,
          level,
          expires,
          hatches,
        });
      }
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      let labelPart = "";
      if (value.hatched) {
        labelPart =
          this.getPokemonNameById(value.id, value.form) +
          (value.form
            ? " (" + value.form + ")"
            : "") /* +
          ", expires at: " +
          parseDatetimeStrictly(value.expires)*/;
      } else {
        labelPart = this.getLevelLabel(
          value.level
        ) /* +
          " , hatches at: " +
          parseDatetimeStrictly(value.hatches) +
          ", expires at: " +
          parseDatetimeStrictly(value.expires)*/;
      }

      return includeLabel ? this.label + ": " + labelPart : labelPart;
    }
    return undefined;
  },
};
