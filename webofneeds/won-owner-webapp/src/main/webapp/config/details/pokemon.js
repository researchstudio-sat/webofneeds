import Immutable from "immutable";
import { isValidDate, toLocalISODateString } from "../../app/utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import PokemonGymViewer from "../../app/components/details/react-viewer/pokemon-gym-viewer.jsx";
import PokemonRaidbossViewer from "../../app/components/details/react-viewer/pokemon-raidboss-viewer.jsx";

export const pokemonGymInfo = {
  identifier: "pokemonGymInfo",
  label: "Additional Gym Info",
  icon: "#ico36_dumbbell", //TODO: Create and use better icon
  messageEnabled: false,
  component: "pokemon-gym-picker",
  viewerComponent: "pokemon-gym-viewer",
  reactViewerComponent: PokemonGymViewer,
  isValid: function(value) {
    return value && value.ex;
  },
  parseToRDF: function({ value }) {
    if (this.isValid(value)) {
      return { "won:gymex": { "@value": !!value.ex, "@type": "xsd:boolean" } };
    }
    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    if (jsonLDImm) {
      const ex = jsonLdUtils.parseFrom(jsonLDImm, ["won:gymex"], "xsd:boolean");

      if (ex) {
        return Immutable.fromJS({ ex });
      }
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value && value.ex) {
      return includeLabel ? this.label + ": " + "Ex Gym" : "Ex Gym";
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
  reactViewerComponent: PokemonRaidbossViewer,
  filterDetail: {
    placeholder: "Filter by (name or id)",
  },
  findPokemonById: function(id, form) {
    if (id) {
      for (const idx in this.fullPokemonList) {
        if (
          this.fullPokemonList[idx].id == id &&
          ((form && this.fullPokemonList[idx].form === form) ||
            (!form && !this.fullPokemonList[idx].form))
        ) {
          return this.fullPokemonList[idx];
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
      !value.expires ||
      (!value.id && value.hatched) ||
      (!value.level && value.hatched) ||
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
    const level = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["won:raid", "won:level"],
      "xsd:int"
    );
    const expires = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["won:raid", "s:validThrough"],
      "xsd:dateTime"
    );

    if (expires) {
      const id = jsonLdUtils.parseFrom(
        jsonLDImm,
        ["won:raid", "won:pokemonid"],
        "xsd:int"
      );
      const hatched = !!id;
      const hatches =
        !hatched &&
        jsonLdUtils.parseFrom(
          jsonLDImm,
          ["won:raid", "s:validFrom"],
          "xsd:dateTime"
        );

      const form =
        id &&
        jsonLdUtils.parseFrom(
          jsonLDImm,
          ["won:raid", "won:pokemonform"],
          "xsd:string"
        );

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
      } else if (value.level) {
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
  // FIXME: replace pokeball icon with working pokemon image links
  // current icon is a placeholder from the noun project due to broken links
  fullPokemonList: [
    {
      id: 1,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/001-00.png",
      name: "Bisasam",
      isShiny: false,
    },
    {
      id: 2,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/002-00.png",
      name: "Bisaknosp",
      isShiny: false,
    },
    {
      id: 3,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/003-00.png",
      name: "Bisaflor",
      isShiny: false,
    },
    {
      id: 4,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/004-00.png",
      name: "Glumanda",
      isShiny: false,
    },
    {
      id: 5,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/005-00.png",
      name: "Glutexo",
      isShiny: false,
    },
    {
      id: 6,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/006-00.png",
      name: "Glurak",
      isShiny: false,
    },
    {
      id: 7,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/007-00.png",
      name: "Schiggy",
      isShiny: false,
    },
    {
      id: 8,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/008-00.png",
      name: "Schillok",
      isShiny: false,
    },
    {
      id: 9,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/009-00.png",
      name: "Turtok",
      isShiny: false,
    },
    {
      id: 10,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/010-00.png",
      name: "Raupy",
      isShiny: false,
    },
    {
      id: 11,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/011-00.png",
      name: "Safcon",
      isShiny: false,
    },
    {
      id: 12,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/012-00.png",
      name: "Smettbo",
      isShiny: false,
    },
    {
      id: 13,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/013-00.png",
      name: "Hornliu",
      isShiny: false,
    },
    {
      id: 14,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/014-00.png",
      name: "Kokuna",
      isShiny: false,
    },
    {
      id: 15,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/015-00.png",
      name: "Bibor",
      isShiny: false,
    },
    {
      id: 16,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/016-00.png",
      name: "Taubsi",
      isShiny: false,
    },
    {
      id: 17,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/017-00.png",
      name: "Tauboga",
      isShiny: false,
    },
    {
      id: 18,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/018-00.png",
      name: "Tauboss",
      isShiny: false,
    },
    {
      id: 19,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/019-01.png",
      name: "Rattfratz",
      isShiny: false,
    },
    {
      id: 19,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/019-61.png",
      name: "Rattfratz",
      isShiny: false,
    },
    {
      id: 20,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/020-01.png",
      name: "Rattikarl",
      isShiny: false,
    },
    {
      id: 20,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/020-61.png",
      name: "Rattikarl",
      isShiny: false,
    },
    {
      id: 21,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/021-00.png",
      name: "Habitak",
      isShiny: false,
    },
    {
      id: 22,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/022-00.png",
      name: "Ibitak",
      isShiny: false,
    },
    {
      id: 23,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/023-00.png",
      name: "Rettan",
      isShiny: false,
    },
    {
      id: 24,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/024-00.png",
      name: "Arbok",
      isShiny: false,
    },
    {
      id: 25,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/025-00.png",
      name: "Pikachu",
      isShiny: false,
    },
    {
      id: 26,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/026-01.png",
      name: "Raichu",
      isShiny: false,
    },
    {
      id: 26,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/026-61.png",
      name: "Raichu",
      isShiny: false,
    },
    {
      id: 27,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/027-00.png",
      name: "Sandan",
      isShiny: false,
    },
    {
      id: 27,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/027-61.png",
      name: "Sandan",
      isShiny: false,
    },
    {
      id: 28,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/028-00.png",
      name: "Sandamer",
      isShiny: false,
    },
    {
      id: 28,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/028-61.png",
      name: "Sandamer",
      isShiny: false,
    },
    {
      id: 29,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/029-00.png",
      name: "Nidoran?",
      isShiny: false,
    },
    {
      id: 30,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/030-00.png",
      name: "Nidorina",
      isShiny: false,
    },
    {
      id: 31,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/031-00.png",
      name: "Nidoqueen",
      isShiny: false,
    },
    {
      id: 32,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/032-00.png",
      name: "Nidoran?",
      isShiny: false,
    },
    {
      id: 33,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/033-00.png",
      name: "Nidorino",
      isShiny: false,
    },
    {
      id: 34,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/034-00.png",
      name: "Nidoking",
      isShiny: false,
    },
    {
      id: 35,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/035-00.png",
      name: "Piepi",
      isShiny: false,
    },
    {
      id: 36,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/036-00.png",
      name: "Pixi",
      isShiny: false,
    },
    {
      id: 37,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/037-00.png",
      name: "Vulpix",
      isShiny: false,
    },
    {
      id: 37,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/037-61.png",
      name: "Vulpix",
      isShiny: false,
    },
    {
      id: 38,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/038-00.png",
      name: "Vulnona",
      isShiny: false,
    },
    {
      id: 38,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/038-61.png",
      name: "Vulnona",
      isShiny: false,
    },
    {
      id: 39,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/039-00.png",
      name: "Pummeluff",
      isShiny: false,
    },
    {
      id: 40,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/040-00.png",
      name: "Knuddeluff",
      isShiny: false,
    },
    {
      id: 41,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/041-00.png",
      name: "Zubat",
      isShiny: false,
    },
    {
      id: 42,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/042-00.png",
      name: "Golbat",
      isShiny: false,
    },
    {
      id: 43,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/043-00.png",
      name: "Myrapla",
      isShiny: false,
    },
    {
      id: 44,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/044-00.png",
      name: "Duflor",
      isShiny: false,
    },
    {
      id: 45,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/045-00.png",
      name: "Giflor",
      isShiny: false,
    },
    {
      id: 46,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/046-00.png",
      name: "Paras",
      isShiny: false,
    },
    {
      id: 47,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/047-00.png",
      name: "Parasek",
      isShiny: false,
    },
    {
      id: 48,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/048-00.png",
      name: "Bluzuk",
      isShiny: false,
    },
    {
      id: 49,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/049-00.png",
      name: "Omot",
      isShiny: false,
    },
    {
      id: 50,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/050-00.png",
      name: "Digda",
      isShiny: false,
    },
    {
      id: 50,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/050-61.png",
      name: "Digda",
      isShiny: false,
    },
    {
      id: 51,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/051-00.png",
      name: "Digdri",
      isShiny: false,
    },
    {
      id: 51,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/051-61.png",
      name: "Digdri",
      isShiny: false,
    },
    {
      id: 52,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/052-00.png",
      name: "Mauzi",
      isShiny: false,
    },
    {
      id: 52,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/052-61.png",
      name: "Mauzi",
      isShiny: false,
    },
    {
      id: 53,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/053-00.png",
      name: "Snobilikat",
      isShiny: false,
    },
    {
      id: 53,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/053-61.png",
      name: "Snobilikat",
      isShiny: false,
    },
    {
      id: 54,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/054-00.png",
      name: "Enton",
      isShiny: false,
    },
    {
      id: 55,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/055-00.png",
      name: "Entoron",
      isShiny: false,
    },
    {
      id: 56,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/056-00.png",
      name: "Menki",
      isShiny: false,
    },
    {
      id: 57,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/057-00.png",
      name: "Rasaff",
      isShiny: false,
    },
    {
      id: 58,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/058-00.png",
      name: "Fukano",
      isShiny: false,
    },
    {
      id: 59,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/059-00.png",
      name: "Arkani",
      isShiny: false,
    },
    {
      id: 60,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/060-00.png",
      name: "Quapsel",
      isShiny: false,
    },
    {
      id: 61,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/061-00.png",
      name: "Quaputzi",
      isShiny: false,
    },
    {
      id: 62,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/062-00.png",
      name: "Quappo",
      isShiny: false,
    },
    {
      id: 63,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/063-00.png",
      name: "Abra",
      isShiny: false,
    },
    {
      id: 64,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/064-00.png",
      name: "Kadabra",
      isShiny: false,
    },
    {
      id: 65,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/065-00.png",
      name: "Simsala",
      isShiny: false,
    },
    {
      id: 66,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/066-00.png",
      name: "Machollo",
      isShiny: false,
    },
    {
      id: 67,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/067-00.png",
      name: "Maschock",
      isShiny: false,
    },
    {
      id: 68,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/068-00.png",
      name: "Machomei",
      isShiny: false,
    },
    {
      id: 69,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/069-00.png",
      name: "Knofensa",
      isShiny: false,
    },
    {
      id: 70,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/070-00.png",
      name: "Ultrigaria",
      isShiny: false,
    },
    {
      id: 71,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/071-00.png",
      name: "Sarzenia",
      isShiny: false,
    },
    {
      id: 72,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/072-00.png",
      name: "Tentacha",
      isShiny: false,
    },
    {
      id: 73,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/073-00.png",
      name: "Tentoxa",
      isShiny: false,
    },
    {
      id: 74,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/074-00.png",
      name: "Kleinstein",
      isShiny: false,
    },
    {
      id: 74,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/074-61.png",
      name: "Kleinstein",
      isShiny: false,
    },
    {
      id: 75,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/075-00.png",
      name: "Georok",
      isShiny: false,
    },
    {
      id: 75,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/075-61.png",
      name: "Georok",
      isShiny: false,
    },
    {
      id: 76,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/076-00.png",
      name: "Geowaz",
      isShiny: false,
    },
    {
      id: 76,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/076-61.png",
      name: "Geowaz",
      isShiny: false,
    },
    {
      id: 77,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/077-00.png",
      name: "Ponita",
      isShiny: false,
    },
    {
      id: 78,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/078-00.png",
      name: "Gallopa",
      isShiny: false,
    },
    {
      id: 79,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/079-00.png",
      name: "Flegmon",
      isShiny: false,
    },
    {
      id: 80,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/080-00.png",
      name: "Lahmus",
      isShiny: false,
    },
    {
      id: 81,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/081-00.png",
      name: "Magnetilo",
      isShiny: false,
    },
    {
      id: 82,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/082-00.png",
      name: "Magneton",
      isShiny: false,
    },
    {
      id: 83,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/083-00.png",
      name: "Porenta",
      isShiny: false,
    },
    {
      id: 84,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/084-00.png",
      name: "Dodu",
      isShiny: false,
    },
    {
      id: 85,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/085-00.png",
      name: "Dodri",
      isShiny: false,
    },
    {
      id: 86,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/086-00.png",
      name: "Jurob",
      isShiny: false,
    },
    {
      id: 87,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/087-00.png",
      name: "Jugong",
      isShiny: false,
    },
    {
      id: 88,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/088-00.png",
      name: "Sleima",
      isShiny: false,
    },
    {
      id: 88,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/088-61.png",
      name: "Sleima",
      isShiny: false,
    },
    {
      id: 89,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/089-00.png",
      name: "Sleimok",
      isShiny: false,
    },
    {
      id: 89,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/089-61.png",
      name: "Sleimok",
      isShiny: false,
    },
    {
      id: 90,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/090-00.png",
      name: "Muschas",
      isShiny: false,
    },
    {
      id: 91,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/091-00.png",
      name: "Austos",
      isShiny: false,
    },
    {
      id: 92,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/092-00.png",
      name: "Nebulak",
      isShiny: false,
    },
    {
      id: 93,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/093-00.png",
      name: "Alpollo",
      isShiny: false,
    },
    {
      id: 94,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/094-00.png",
      name: "Gengar",
      isShiny: false,
    },
    {
      id: 95,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/095-00.png",
      name: "Onix",
      isShiny: false,
    },
    {
      id: 96,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/096-00.png",
      name: "Traumato",
      isShiny: false,
    },
    {
      id: 97,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/097-00.png",
      name: "Hypno",
      isShiny: false,
    },
    {
      id: 98,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/098-00.png",
      name: "Krabby",
      isShiny: false,
    },
    {
      id: 99,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/099-00.png",
      name: "Kingler",
      isShiny: false,
    },
    {
      id: 100,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/100-00.png",
      name: "Voltobal",
      isShiny: false,
    },
    {
      id: 101,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/101-00.png",
      name: "Lektrobal",
      isShiny: false,
    },
    {
      id: 102,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/102-00.png",
      name: "Owei",
      isShiny: false,
    },
    {
      id: 103,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/103-00.png",
      name: "Kokowei",
      isShiny: false,
    },
    {
      id: 103,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/103-61.png",
      name: "Kokowei",
      isShiny: false,
    },
    {
      id: 104,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/104-00.png",
      name: "Tragosso",
      isShiny: false,
    },
    {
      id: 105,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/105-00.png",
      name: "Knogga",
      isShiny: false,
    },
    {
      id: 105,
      form: "Alola-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/105-61.png",
      name: "Knogga",
      isShiny: false,
    },
    {
      id: 106,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/106-00.png",
      name: "Kicklee",
      isShiny: false,
    },
    {
      id: 107,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/107-00.png",
      name: "Nockchan",
      isShiny: false,
    },
    {
      id: 108,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/108-00.png",
      name: "Schlurp",
      isShiny: false,
    },
    {
      id: 109,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/109-00.png",
      name: "Smogon",
      isShiny: false,
    },
    {
      id: 110,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/110-00.png",
      name: "Smogmog",
      isShiny: false,
    },
    {
      id: 111,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/111-00.png",
      name: "Rihorn",
      isShiny: false,
    },
    {
      id: 112,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/112-00.png",
      name: "Rizeros",
      isShiny: false,
    },
    {
      id: 113,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/113-00.png",
      name: "Chaneira",
      isShiny: false,
    },
    {
      id: 114,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/114-00.png",
      name: "Tangela",
      isShiny: false,
    },
    {
      id: 115,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/115-00.png",
      name: "Kangama",
      isShiny: false,
    },
    {
      id: 116,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/116-00.png",
      name: "Seeper",
      isShiny: false,
    },
    {
      id: 117,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/117-00.png",
      name: "Seemon",
      isShiny: false,
    },
    {
      id: 118,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/118-00.png",
      name: "Goldini",
      isShiny: false,
    },
    {
      id: 119,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/119-00.png",
      name: "Golking",
      isShiny: false,
    },
    {
      id: 120,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/120-00.png",
      name: "Sterndu",
      isShiny: false,
    },
    {
      id: 121,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/121-00.png",
      name: "Starmie",
      isShiny: false,
    },
    {
      id: 122,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/122-00.png",
      name: "Pantimos",
      isShiny: false,
    },
    {
      id: 123,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/123-00.png",
      name: "Sichlor",
      isShiny: false,
    },
    {
      id: 124,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/124-00.png",
      name: "Rossana",
      isShiny: false,
    },
    {
      id: 125,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/125-00.png",
      name: "Elektek",
      isShiny: false,
    },
    {
      id: 126,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/126-00.png",
      name: "Magmar",
      isShiny: false,
    },
    {
      id: 127,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/127-00.png",
      name: "Pinsir",
      isShiny: false,
    },
    {
      id: 128,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/128-00.png",
      name: "Tauros",
      isShiny: false,
    },
    {
      id: 129,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/129-00.png",
      name: "Karpador",
      isShiny: false,
    },
    {
      id: 130,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/130-00.png",
      name: "Garados",
      isShiny: false,
    },
    {
      id: 131,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/131-00.png",
      name: "Lapras",
      isShiny: false,
    },
    {
      id: 132,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/132-00.png",
      name: "Ditto",
      isShiny: false,
    },
    {
      id: 133,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/133-00.png",
      name: "Evoli",
      isShiny: false,
    },
    {
      id: 134,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/134-00.png",
      name: "Aquana",
      isShiny: false,
    },
    {
      id: 135,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/135-00.png",
      name: "Blitza",
      isShiny: false,
    },
    {
      id: 136,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/136-00.png",
      name: "Flamara",
      isShiny: false,
    },
    {
      id: 137,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/137-00.png",
      name: "Porygon",
      isShiny: false,
    },
    {
      id: 138,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/138-00.png",
      name: "Amonitas",
      isShiny: false,
    },
    {
      id: 139,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/139-00.png",
      name: "Amoroso",
      isShiny: false,
    },
    {
      id: 140,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/140-00.png",
      name: "Kabuto",
      isShiny: false,
    },
    {
      id: 141,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/141-00.png",
      name: "Kabutops",
      isShiny: false,
    },
    {
      id: 142,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/142-00.png",
      name: "Aerodactyl",
      isShiny: false,
    },
    {
      id: 143,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/143-00.png",
      name: "Relaxo",
      isShiny: false,
    },
    {
      id: 144,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/144-00.png",
      name: "Arktos",
      isShiny: false,
    },
    {
      id: 145,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/145-00.png",
      name: "Zapdos",
      isShiny: false,
    },
    {
      id: 146,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/146-00.png",
      name: "Lavados",
      isShiny: false,
    },
    {
      id: 147,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/147-00.png",
      name: "Dratini",
      isShiny: false,
    },
    {
      id: 148,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/148-00.png",
      name: "Dragonir",
      isShiny: false,
    },
    {
      id: 149,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/149-00.png",
      name: "Dragoran",
      isShiny: false,
    },
    {
      id: 150,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/150-00.png",
      name: "Mewtu",
      isShiny: false,
    },
    {
      id: 151,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/151-00.png",
      name: "Mew",
      isShiny: false,
    },
    {
      id: 152,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/152-00.png",
      name: "Endivie",
      isShiny: false,
    },
    {
      id: 153,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/153-00.png",
      name: "Lorblatt",
      isShiny: false,
    },
    {
      id: 154,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/154-00.png",
      name: "Meganie",
      isShiny: false,
    },
    {
      id: 155,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/155-00.png",
      name: "Feurigel",
      isShiny: false,
    },
    {
      id: 156,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/156-00.png",
      name: "Igelavar",
      isShiny: false,
    },
    {
      id: 157,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/157-00.png",
      name: "Tornupto",
      isShiny: false,
    },
    {
      id: 158,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/158-00.png",
      name: "Karnimani",
      isShiny: false,
    },
    {
      id: 159,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/159-00.png",
      name: "Tyracroc",
      isShiny: false,
    },
    {
      id: 160,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/160-00.png",
      name: "Impergator",
      isShiny: false,
    },
    {
      id: 161,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/161-00.png",
      name: "Wiesor",
      isShiny: false,
    },
    {
      id: 162,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/162-00.png",
      name: "Wiesenior",
      isShiny: false,
    },
    {
      id: 163,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/163-00.png",
      name: "Hoothoot",
      isShiny: false,
    },
    {
      id: 164,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/164-00.png",
      name: "Noctuh",
      isShiny: false,
    },
    {
      id: 165,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/165-00.png",
      name: "Ledyba",
      isShiny: false,
    },
    {
      id: 166,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/166-00.png",
      name: "Ledian",
      isShiny: false,
    },
    {
      id: 167,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/167-00.png",
      name: "Webarak",
      isShiny: false,
    },
    {
      id: 168,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/168-00.png",
      name: "Ariados",
      isShiny: false,
    },
    {
      id: 169,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/169-00.png",
      name: "Iksbat",
      isShiny: false,
    },
    {
      id: 170,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/170-00.png",
      name: "Lampi",
      isShiny: false,
    },
    {
      id: 171,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/171-00.png",
      name: "Lanturn",
      isShiny: false,
    },
    {
      id: 172,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/172-00.png",
      name: "Pichu",
      isShiny: false,
    },
    {
      id: 173,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/173-00.png",
      name: "Pii",
      isShiny: false,
    },
    {
      id: 174,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/174-00.png",
      name: "Fluffeluff",
      isShiny: false,
    },
    {
      id: 175,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/175-00.png",
      name: "Togepi",
      isShiny: false,
    },
    {
      id: 176,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/176-00.png",
      name: "Togetic",
      isShiny: false,
    },
    {
      id: 177,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/177-00.png",
      name: "Natu",
      isShiny: false,
    },
    {
      id: 178,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/178-00.png",
      name: "Xatu",
      isShiny: false,
    },
    {
      id: 179,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/179-00.png",
      name: "Voltilamm",
      isShiny: false,
    },
    {
      id: 180,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/180-00.png",
      name: "Waaty",
      isShiny: false,
    },
    {
      id: 181,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/181-00.png",
      name: "Ampharos",
      isShiny: false,
    },
    {
      id: 182,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/182-00.png",
      name: "Blubella",
      isShiny: false,
    },
    {
      id: 183,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/183-00.png",
      name: "Marill",
      isShiny: false,
    },
    {
      id: 184,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/184-00.png",
      name: "Azumarill",
      isShiny: false,
    },
    {
      id: 185,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/185-00.png",
      name: "Mogelbaum",
      isShiny: false,
    },
    {
      id: 186,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/186-00.png",
      name: "Quaxo",
      isShiny: false,
    },
    {
      id: 187,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/187-00.png",
      name: "Hoppspross",
      isShiny: false,
    },
    {
      id: 188,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/188-00.png",
      name: "Hubelupf",
      isShiny: false,
    },
    {
      id: 189,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/189-00.png",
      name: "Papungha",
      isShiny: false,
    },
    {
      id: 190,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/190-00.png",
      name: "Griffel",
      isShiny: false,
    },
    {
      id: 191,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/191-00.png",
      name: "Sonnkern",
      isShiny: false,
    },
    {
      id: 192,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/192-00.png",
      name: "Sonnflora",
      isShiny: false,
    },
    {
      id: 193,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/193-00.png",
      name: "Yanma",
      isShiny: false,
    },
    {
      id: 194,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/194-00.png",
      name: "Felino",
      isShiny: false,
    },
    {
      id: 195,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/195-00.png",
      name: "Morlord",
      isShiny: false,
    },
    {
      id: 196,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/196-00.png",
      name: "Psiana",
      isShiny: false,
    },
    {
      id: 197,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/197-00.png",
      name: "Nachtara",
      isShiny: false,
    },
    {
      id: 198,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/198-00.png",
      name: "Kramurx",
      isShiny: false,
    },
    {
      id: 199,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/199-00.png",
      name: "Laschoking",
      isShiny: false,
    },
    {
      id: 200,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/200-00.png",
      name: "Traunfugil",
      isShiny: false,
    },
    {
      id: 201,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/201-00.png",
      name: "Icognito",
      isShiny: false,
    },
    {
      id: 202,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/202-00.png",
      name: "Woingenau",
      isShiny: false,
    },
    {
      id: 203,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/203-00.png",
      name: "Girafarig",
      isShiny: false,
    },
    {
      id: 204,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/204-00.png",
      name: "Tannza",
      isShiny: false,
    },
    {
      id: 205,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/205-00.png",
      name: "Forstellka",
      isShiny: false,
    },
    {
      id: 206,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/206-00.png",
      name: "Dummisel",
      isShiny: false,
    },
    {
      id: 207,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/207-00.png",
      name: "Skorgla",
      isShiny: false,
    },
    {
      id: 208,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/208-00.png",
      name: "Stahlos",
      isShiny: false,
    },
    {
      id: 209,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/209-00.png",
      name: "Snubbull",
      isShiny: false,
    },
    {
      id: 210,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/210-00.png",
      name: "Granbull",
      isShiny: false,
    },
    {
      id: 211,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/211-00.png",
      name: "Baldorfish",
      isShiny: false,
    },
    {
      id: 212,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/212-00.png",
      name: "Scherox",
      isShiny: false,
    },
    {
      id: 213,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/213-00.png",
      name: "Pottrott",
      isShiny: false,
    },
    {
      id: 214,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/214-00.png",
      name: "Skaraborn",
      isShiny: false,
    },
    {
      id: 215,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/215-00.png",
      name: "Sniebel",
      isShiny: false,
    },
    {
      id: 216,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/216-00.png",
      name: "Teddiursa",
      isShiny: false,
    },
    {
      id: 217,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/217-00.png",
      name: "Ursaring",
      isShiny: false,
    },
    {
      id: 218,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/218-00.png",
      name: "Schneckmag",
      isShiny: false,
    },
    {
      id: 219,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/219-00.png",
      name: "Magcargo",
      isShiny: false,
    },
    {
      id: 220,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/220-00.png",
      name: "Quiekel",
      isShiny: false,
    },
    {
      id: 221,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/221-00.png",
      name: "Keifel",
      isShiny: false,
    },
    {
      id: 222,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/222-00.png",
      name: "Corasonn",
      isShiny: false,
    },
    {
      id: 223,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/223-00.png",
      name: "Remoraid",
      isShiny: false,
    },
    {
      id: 224,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/224-00.png",
      name: "Octillery",
      isShiny: false,
    },
    {
      id: 225,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/225-00.png",
      name: "Botogel",
      isShiny: false,
    },
    {
      id: 226,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/226-00.png",
      name: "Mantax",
      isShiny: false,
    },
    {
      id: 227,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/227-00.png",
      name: "Panzaeron",
      isShiny: false,
    },
    {
      id: 228,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/228-00.png",
      name: "Hunduster",
      isShiny: false,
    },
    {
      id: 229,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/229-00.png",
      name: "Hundemon",
      isShiny: false,
    },
    {
      id: 230,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/230-00.png",
      name: "Seedraking",
      isShiny: false,
    },
    {
      id: 231,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/231-00.png",
      name: "Phanpy",
      isShiny: false,
    },
    {
      id: 232,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/232-00.png",
      name: "Donphan",
      isShiny: false,
    },
    {
      id: 233,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/233-00.png",
      name: "Porygon2",
      isShiny: false,
    },
    {
      id: 234,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/234-00.png",
      name: "Damhirplex",
      isShiny: false,
    },
    {
      id: 235,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/235-00.png",
      name: "Farbeagle",
      isShiny: false,
    },
    {
      id: 236,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/236-00.png",
      name: "Rabauz",
      isShiny: false,
    },
    {
      id: 237,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/237-00.png",
      name: "Kapoera",
      isShiny: false,
    },
    {
      id: 238,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/238-00.png",
      name: "Kussilla",
      isShiny: false,
    },
    {
      id: 239,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/239-00.png",
      name: "Elekid",
      isShiny: false,
    },
    {
      id: 240,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/240-00.png",
      name: "Magby",
      isShiny: false,
    },
    {
      id: 241,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/241-00.png",
      name: "Miltank",
      isShiny: false,
    },
    {
      id: 242,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/242-00.png",
      name: "Heiteira",
      isShiny: false,
    },
    {
      id: 243,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/243-00.png",
      name: "Raikou",
      isShiny: false,
    },
    {
      id: 244,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/244-00.png",
      name: "Entei",
      isShiny: false,
    },
    {
      id: 245,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/245-00.png",
      name: "Suicune",
      isShiny: false,
    },
    {
      id: 246,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/246-00.png",
      name: "Larvitar",
      isShiny: false,
    },
    {
      id: 247,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/247-00.png",
      name: "Pupitar",
      isShiny: false,
    },
    {
      id: 248,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/248-00.png",
      name: "Despotar",
      isShiny: false,
    },
    {
      id: 249,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/249-00.png",
      name: "Lugia",
      isShiny: false,
    },
    {
      id: 250,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/250-00.png",
      name: "Ho-Oh",
      isShiny: false,
    },
    {
      id: 251,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/251-00.png",
      name: "Celebi",
      isShiny: false,
    },
    {
      id: 252,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/252-00.png",
      name: "Geckarbor",
      isShiny: false,
    },
    {
      id: 253,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/253-00.png",
      name: "Reptain",
      isShiny: false,
    },
    {
      id: 254,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/254-00.png",
      name: "Gewaldro",
      isShiny: false,
    },
    {
      id: 255,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/255-00.png",
      name: "Flemmli",
      isShiny: false,
    },
    {
      id: 256,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/256-00.png",
      name: "Jungglut",
      isShiny: false,
    },
    {
      id: 257,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/257-00.png",
      name: "Lohgock",
      isShiny: false,
    },
    {
      id: 258,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/258-00.png",
      name: "Hydropi",
      isShiny: false,
    },
    {
      id: 259,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/259-00.png",
      name: "Moorabbel",
      isShiny: false,
    },
    {
      id: 260,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/260-00.png",
      name: "Sumpex",
      isShiny: false,
    },
    {
      id: 261,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/261-00.png",
      name: "Fiffyen",
      isShiny: false,
    },
    {
      id: 262,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/262-00.png",
      name: "Magnayen",
      isShiny: false,
    },
    {
      id: 263,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/263-00.png",
      name: "Zigzachs",
      isShiny: false,
    },
    {
      id: 264,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/264-00.png",
      name: "Geradaks",
      isShiny: false,
    },
    {
      id: 265,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/265-00.png",
      name: "Waumpel",
      isShiny: false,
    },
    {
      id: 266,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/266-00.png",
      name: "Schaloko",
      isShiny: false,
    },
    {
      id: 267,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/267-00.png",
      name: "Papinella",
      isShiny: false,
    },
    {
      id: 268,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/268-00.png",
      name: "Panekon",
      isShiny: false,
    },
    {
      id: 269,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/269-00.png",
      name: "Pudox",
      isShiny: false,
    },
    {
      id: 270,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/270-00.png",
      name: "Loturzel",
      isShiny: false,
    },
    {
      id: 271,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/271-00.png",
      name: "Lombrero",
      isShiny: false,
    },
    {
      id: 272,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/272-00.png",
      name: "Kappalores",
      isShiny: false,
    },
    {
      id: 273,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/273-00.png",
      name: "Samurzel",
      isShiny: false,
    },
    {
      id: 274,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/274-00.png",
      name: "Blanas",
      isShiny: false,
    },
    {
      id: 275,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/275-00.png",
      name: "Tengulist",
      isShiny: false,
    },
    {
      id: 276,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/276-00.png",
      name: "Schwalbini",
      isShiny: false,
    },
    {
      id: 277,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/277-00.png",
      name: "Schwalboss",
      isShiny: false,
    },
    {
      id: 278,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/278-00.png",
      name: "Wingull",
      isShiny: false,
    },
    {
      id: 279,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/279-00.png",
      name: "Pelipper",
      isShiny: false,
    },
    {
      id: 280,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/280-00.png",
      name: "Trasla",
      isShiny: false,
    },
    {
      id: 281,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/281-00.png",
      name: "Kirlia",
      isShiny: false,
    },
    {
      id: 282,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/282-00.png",
      name: "Guardevoir",
      isShiny: false,
    },
    {
      id: 283,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/283-00.png",
      name: "Gehweiher",
      isShiny: false,
    },
    {
      id: 284,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/284-00.png",
      name: "Maskeregen",
      isShiny: false,
    },
    {
      id: 285,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/285-00.png",
      name: "Knilz",
      isShiny: false,
    },
    {
      id: 286,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/286-00.png",
      name: "Kapilz",
      isShiny: false,
    },
    {
      id: 287,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/287-00.png",
      name: "Bummelz",
      isShiny: false,
    },
    {
      id: 288,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/288-00.png",
      name: "Muntier",
      isShiny: false,
    },
    {
      id: 289,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/289-00.png",
      name: "Letarking",
      isShiny: false,
    },
    {
      id: 290,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/290-00.png",
      name: "Nincada",
      isShiny: false,
    },
    {
      id: 291,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/291-00.png",
      name: "Ninjask",
      isShiny: false,
    },
    {
      id: 292,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/292-00.png",
      name: "Ninjatom",
      isShiny: false,
    },
    {
      id: 293,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/293-00.png",
      name: "Flurmel",
      isShiny: false,
    },
    {
      id: 294,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/294-00.png",
      name: "Krakeelo",
      isShiny: false,
    },
    {
      id: 295,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/295-00.png",
      name: "Krawumms",
      isShiny: false,
    },
    {
      id: 296,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/296-00.png",
      name: "Makuhita",
      isShiny: false,
    },
    {
      id: 297,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/297-00.png",
      name: "Hariyama",
      isShiny: false,
    },
    {
      id: 298,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/298-00.png",
      name: "Azurill",
      isShiny: false,
    },
    {
      id: 299,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/299-00.png",
      name: "Nasgnet",
      isShiny: false,
    },
    {
      id: 300,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/300-00.png",
      name: "Eneco",
      isShiny: false,
    },
    {
      id: 301,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/301-00.png",
      name: "Enekoro",
      isShiny: false,
    },
    {
      id: 302,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/302-00.png",
      name: "Zobiris",
      isShiny: false,
    },
    {
      id: 303,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/303-00.png",
      name: "Flunkifer",
      isShiny: false,
    },
    {
      id: 304,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/304-00.png",
      name: "Stollunior",
      isShiny: false,
    },
    {
      id: 305,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/305-00.png",
      name: "Stollrak",
      isShiny: false,
    },
    {
      id: 306,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/306-00.png",
      name: "Stolloss",
      isShiny: false,
    },
    {
      id: 307,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/307-00.png",
      name: "Meditie",
      isShiny: false,
    },
    {
      id: 308,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/308-00.png",
      name: "Meditalis",
      isShiny: false,
    },
    {
      id: 309,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/309-00.png",
      name: "Frizelbliz",
      isShiny: false,
    },
    {
      id: 310,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/310-00.png",
      name: "Voltenso",
      isShiny: false,
    },
    {
      id: 311,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/311-00.png",
      name: "Plusle",
      isShiny: false,
    },
    {
      id: 312,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/312-00.png",
      name: "Minun",
      isShiny: false,
    },
    {
      id: 313,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/313-00.png",
      name: "Volbeat",
      isShiny: false,
    },
    {
      id: 314,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/314-00.png",
      name: "Illumise",
      isShiny: false,
    },
    {
      id: 315,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/315-00.png",
      name: "Roselia",
      isShiny: false,
    },
    {
      id: 316,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/316-00.png",
      name: "Schluppuck",
      isShiny: false,
    },
    {
      id: 317,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/317-00.png",
      name: "Schlukwech",
      isShiny: false,
    },
    {
      id: 318,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/318-00.png",
      name: "Kanivanha",
      isShiny: false,
    },
    {
      id: 319,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/319-00.png",
      name: "Tohaido",
      isShiny: false,
    },
    {
      id: 320,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/320-00.png",
      name: "Wailmer",
      isShiny: false,
    },
    {
      id: 321,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/321-00.png",
      name: "Wailord",
      isShiny: false,
    },
    {
      id: 322,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/322-00.png",
      name: "Camaub",
      isShiny: false,
    },
    {
      id: 323,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/323-00.png",
      name: "Camerupt",
      isShiny: false,
    },
    {
      id: 324,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/324-00.png",
      name: "Qurtel",
      isShiny: false,
    },
    {
      id: 325,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/325-00.png",
      name: "Spoink",
      isShiny: false,
    },
    {
      id: 326,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/326-00.png",
      name: "Groink",
      isShiny: false,
    },
    {
      id: 327,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/327-00.png",
      name: "Pandir",
      isShiny: false,
    },
    {
      id: 328,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/328-00.png",
      name: "Knacklion",
      isShiny: false,
    },
    {
      id: 329,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/329-00.png",
      name: "Vibrava",
      isShiny: false,
    },
    {
      id: 330,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/330-00.png",
      name: "Libelldra",
      isShiny: false,
    },
    {
      id: 331,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/331-00.png",
      name: "Tuska",
      isShiny: false,
    },
    {
      id: 332,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/332-00.png",
      name: "Noktuska",
      isShiny: false,
    },
    {
      id: 333,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/333-00.png",
      name: "Wablu",
      isShiny: false,
    },
    {
      id: 334,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/334-00.png",
      name: "Altaria",
      isShiny: false,
    },
    {
      id: 335,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/335-00.png",
      name: "Sengo",
      isShiny: false,
    },
    {
      id: 336,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/336-00.png",
      name: "Vipitis",
      isShiny: false,
    },
    {
      id: 337,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/337-00.png",
      name: "Lunastein",
      isShiny: false,
    },
    {
      id: 338,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/338-00.png",
      name: "Sonnfel",
      isShiny: false,
    },
    {
      id: 339,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/339-00.png",
      name: "Schmerbe",
      isShiny: false,
    },
    {
      id: 340,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/340-00.png",
      name: "Welsar",
      isShiny: false,
    },
    {
      id: 341,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/341-00.png",
      name: "Krebscorps",
      isShiny: false,
    },
    {
      id: 342,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/342-00.png",
      name: "Krebutack",
      isShiny: false,
    },
    {
      id: 343,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/343-00.png",
      name: "Puppance",
      isShiny: false,
    },
    {
      id: 344,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/344-00.png",
      name: "Lepumentas",
      isShiny: false,
    },
    {
      id: 345,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/345-00.png",
      name: "Liliep",
      isShiny: false,
    },
    {
      id: 346,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/346-00.png",
      name: "Wielie",
      isShiny: false,
    },
    {
      id: 347,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/347-00.png",
      name: "Anorith",
      isShiny: false,
    },
    {
      id: 348,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/348-00.png",
      name: "Armaldo",
      isShiny: false,
    },
    {
      id: 349,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/349-00.png",
      name: "Barschwa",
      isShiny: false,
    },
    {
      id: 350,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/350-00.png",
      name: "Milotic",
      isShiny: false,
    },
    {
      id: 351,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/351-01.png",
      name: "Formeo",
      isShiny: false,
    },
    {
      id: 351,
      form: "Regenform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/351-03.png",
      name: "Formeo",
      isShiny: false,
    },
    {
      id: 351,
      form: "Schneeform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/351-04.png",
      name: "Formeo",
      isShiny: false,
    },
    {
      id: 351,
      form: "Sonnenform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/351-02.png",
      name: "Formeo",
      isShiny: false,
    },
    {
      id: 352,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/352-00.png",
      name: "Kecleon",
      isShiny: false,
    },
    {
      id: 353,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/353-00.png",
      name: "Shuppet",
      isShiny: false,
    },
    {
      id: 354,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/354-00.png",
      name: "Banette",
      isShiny: false,
    },
    {
      id: 355,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/355-00.png",
      name: "Zwirrlicht",
      isShiny: false,
    },
    {
      id: 356,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/356-00.png",
      name: "Zwirrklop",
      isShiny: false,
    },
    {
      id: 357,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/357-00.png",
      name: "Tropius",
      isShiny: false,
    },
    {
      id: 358,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/358-00.png",
      name: "Palimpalim",
      isShiny: false,
    },
    {
      id: 359,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/359-00.png",
      name: "Absol",
      isShiny: false,
    },
    {
      id: 360,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/360-00.png",
      name: "Isso",
      isShiny: false,
    },
    {
      id: 361,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/361-00.png",
      name: "Schneppke",
      isShiny: false,
    },
    {
      id: 362,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/362-00.png",
      name: "Firnontor",
      isShiny: false,
    },
    {
      id: 363,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/363-00.png",
      name: "Seemops",
      isShiny: false,
    },
    {
      id: 364,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/364-00.png",
      name: "Seejong",
      isShiny: false,
    },
    {
      id: 365,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/365-00.png",
      name: "Walraisa",
      isShiny: false,
    },
    {
      id: 366,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/366-00.png",
      name: "Perlu",
      isShiny: false,
    },
    {
      id: 367,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/367-00.png",
      name: "Aalabyss",
      isShiny: false,
    },
    {
      id: 368,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/368-00.png",
      name: "Saganabyss",
      isShiny: false,
    },
    {
      id: 369,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/369-00.png",
      name: "Relicanth",
      isShiny: false,
    },
    {
      id: 370,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/370-00.png",
      name: "Liebiskus",
      isShiny: false,
    },
    {
      id: 371,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/371-00.png",
      name: "Kindwurm",
      isShiny: false,
    },
    {
      id: 372,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/372-00.png",
      name: "Draschel",
      isShiny: false,
    },
    {
      id: 373,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/373-00.png",
      name: "Brutalanda",
      isShiny: false,
    },
    {
      id: 374,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/374-00.png",
      name: "Tanhel",
      isShiny: false,
    },
    {
      id: 375,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/375-00.png",
      name: "Metang",
      isShiny: false,
    },
    {
      id: 376,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/376-00.png",
      name: "Metagross",
      isShiny: false,
    },
    {
      id: 377,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/377-00.png",
      name: "Regirock",
      isShiny: false,
    },
    {
      id: 378,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/378-00.png",
      name: "Regice",
      isShiny: false,
    },
    {
      id: 379,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/379-00.png",
      name: "Registeel",
      isShiny: false,
    },
    {
      id: 380,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/380-00.png",
      name: "Latias",
      isShiny: false,
    },
    {
      id: 381,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/381-00.png",
      name: "Latios",
      isShiny: false,
    },
    {
      id: 382,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/382-00.png",
      name: "Kyogre",
      isShiny: false,
    },
    {
      id: 383,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/383-00.png",
      name: "Groudon",
      isShiny: false,
    },
    {
      id: 384,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/384-00.png",
      name: "Rayquaza",
      isShiny: false,
    },
    {
      id: 385,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/385-00.png",
      name: "Jirachi",
      isShiny: false,
    },
    {
      id: 386,
      form: "Normalform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/386-00.png",
      name: "Deoxys",
      isShiny: false,
    },
    {
      id: 386,
      form: "Angriffsform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/386-02.png",
      name: "Deoxys",
      isShiny: false,
    },
    {
      id: 386,
      form: "Verteidigungsform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/386-03.png",
      name: "Deoxys",
      isShiny: false,
    },
    {
      id: 386,
      form: "Initiativeform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/386-04.png",
      name: "Deoxys",
      isShiny: false,
    },
    {
      id: 387,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/387-00.png",
      name: "Chelast",
      isShiny: false,
    },
    {
      id: 388,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/388-00.png",
      name: "Chelcarain",
      isShiny: false,
    },
    {
      id: 389,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/389-00.png",
      name: "Chelterrar",
      isShiny: false,
    },
    {
      id: 390,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/390-00.png",
      name: "Panflam",
      isShiny: false,
    },
    {
      id: 391,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/391-00.png",
      name: "Panpyro",
      isShiny: false,
    },
    {
      id: 392,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/392-00.png",
      name: "Panferno",
      isShiny: false,
    },
    {
      id: 393,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/393-00.png",
      name: "Plinfa",
      isShiny: false,
    },
    {
      id: 394,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/394-00.png",
      name: "Pliprin",
      isShiny: false,
    },
    {
      id: 395,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/395-00.png",
      name: "Impoleon",
      isShiny: false,
    },
    {
      id: 396,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/396-00.png",
      name: "Staralili",
      isShiny: false,
    },
    {
      id: 397,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/397-00.png",
      name: "Staravia",
      isShiny: false,
    },
    {
      id: 398,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/398-00.png",
      name: "Staraptor",
      isShiny: false,
    },
    {
      id: 399,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/399-00.png",
      name: "Bidiza",
      isShiny: false,
    },
    {
      id: 400,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/400-00.png",
      name: "Bidifas",
      isShiny: false,
    },
    {
      id: 401,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/401-00.png",
      name: "Zirpurze",
      isShiny: false,
    },
    {
      id: 402,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/402-00.png",
      name: "Zirpeise",
      isShiny: false,
    },
    {
      id: 403,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/403-00.png",
      name: "Sheinux",
      isShiny: false,
    },
    {
      id: 404,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/404-00.png",
      name: "Luxio",
      isShiny: false,
    },
    {
      id: 405,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/405-00.png",
      name: "Luxtra",
      isShiny: false,
    },
    {
      id: 406,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/406-00.png",
      name: "Knospi",
      isShiny: false,
    },
    {
      id: 407,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/407-00.png",
      name: "Roserade",
      isShiny: false,
    },
    {
      id: 408,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/408-00.png",
      name: "Koknodon",
      isShiny: false,
    },
    {
      id: 409,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/409-00.png",
      name: "Rameidon",
      isShiny: false,
    },
    {
      id: 410,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/410-00.png",
      name: "Schilterus",
      isShiny: false,
    },
    {
      id: 411,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/411-00.png",
      name: "Bollterus",
      isShiny: false,
    },
    {
      id: 412,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/412-11.png",
      name: "Burmy",
      isShiny: false,
    },
    {
      id: 413,
      form: "Pflanzenumhang",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/413-11.png",
      name: "Burmadame",
      isShiny: false,
    },
    {
      id: 413,
      form: "Sandumhang",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/413-12.png",
      name: "Burmadame",
      isShiny: false,
    },
    {
      id: 413,
      form: "Lumpenumhang",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/413-13.png",
      name: "Burmadame",
      isShiny: false,
    },
    {
      id: 414,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/414-00.png",
      name: "Moterpel",
      isShiny: false,
    },
    {
      id: 415,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/415-00.png",
      name: "Wadribie",
      isShiny: false,
    },
    {
      id: 416,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/416-00.png",
      name: "Honweisel",
      isShiny: false,
    },
    {
      id: 417,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/417-00.png",
      name: "Pachirisu",
      isShiny: false,
    },
    {
      id: 418,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/418-00.png",
      name: "Bamelin",
      isShiny: false,
    },
    {
      id: 419,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/419-00.png",
      name: "Bojelin",
      isShiny: false,
    },
    {
      id: 420,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/420-00.png",
      name: "Kikugi",
      isShiny: false,
    },
    {
      id: 421,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/421-00.png",
      name: "Kinoso",
      isShiny: false,
    },
    {
      id: 422,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/422-00.png",
      name: "Schalellos",
      isShiny: false,
    },
    {
      id: 423,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/423-00.png",
      name: "Gastrodon",
      isShiny: false,
    },
    {
      id: 424,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/424-00.png",
      name: "Ambidiffel",
      isShiny: false,
    },
    {
      id: 425,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/425-00.png",
      name: "Driftlon",
      isShiny: false,
    },
    {
      id: 426,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/426-00.png",
      name: "Drifzepeli",
      isShiny: false,
    },
    {
      id: 427,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/427-00.png",
      name: "Haspiror",
      isShiny: false,
    },
    {
      id: 428,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/428-00.png",
      name: "Schlapor",
      isShiny: false,
    },
    {
      id: 429,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/429-00.png",
      name: "Traunmagil",
      isShiny: false,
    },
    {
      id: 430,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/430-00.png",
      name: "Kramshef",
      isShiny: false,
    },
    {
      id: 431,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/431-00.png",
      name: "Charmian",
      isShiny: false,
    },
    {
      id: 432,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/432-00.png",
      name: "Shnurgarst",
      isShiny: false,
    },
    {
      id: 433,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/433-00.png",
      name: "Klingplim",
      isShiny: false,
    },
    {
      id: 434,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/434-00.png",
      name: "Skunkapuh",
      isShiny: false,
    },
    {
      id: 435,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/435-00.png",
      name: "Skuntank",
      isShiny: false,
    },
    {
      id: 436,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/436-00.png",
      name: "Bronzel",
      isShiny: false,
    },
    {
      id: 437,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/437-00.png",
      name: "Bronzong",
      isShiny: false,
    },
    {
      id: 438,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/438-00.png",
      name: "Mobai",
      isShiny: false,
    },
    {
      id: 439,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/439-00.png",
      name: "Pantimimi",
      isShiny: false,
    },
    {
      id: 440,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/440-00.png",
      name: "Wonneira",
      isShiny: false,
    },
    {
      id: 441,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/441-00.png",
      name: "Plaudagei",
      isShiny: false,
    },
    {
      id: 442,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/442-00.png",
      name: "Kryppuk",
      isShiny: false,
    },
    {
      id: 443,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/443-00.png",
      name: "Kaumalat",
      isShiny: false,
    },
    {
      id: 444,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/444-00.png",
      name: "Knarksel",
      isShiny: false,
    },
    {
      id: 445,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/445-00.png",
      name: "Knakrack",
      isShiny: false,
    },
    {
      id: 446,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/446-00.png",
      name: "Mampfaxo",
      isShiny: false,
    },
    {
      id: 447,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/447-00.png",
      name: "Riolu",
      isShiny: false,
    },
    {
      id: 448,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/448-00.png",
      name: "Lucario",
      isShiny: false,
    },
    {
      id: 449,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/449-00.png",
      name: "Hippopotas",
      isShiny: false,
    },
    {
      id: 450,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/450-00.png",
      name: "Hippoterus",
      isShiny: false,
    },
    {
      id: 451,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/451-00.png",
      name: "Pionskora",
      isShiny: false,
    },
    {
      id: 452,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/452-00.png",
      name: "Piondragi",
      isShiny: false,
    },
    {
      id: 453,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/453-00.png",
      name: "Glibunkel",
      isShiny: false,
    },
    {
      id: 454,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/454-00.png",
      name: "Toxiquak",
      isShiny: false,
    },
    {
      id: 455,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/455-00.png",
      name: "Venuflibis",
      isShiny: false,
    },
    {
      id: 456,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/456-00.png",
      name: "Finneon",
      isShiny: false,
    },
    {
      id: 457,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/457-00.png",
      name: "Lumineon",
      isShiny: false,
    },
    {
      id: 458,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/458-00.png",
      name: "Mantirps",
      isShiny: false,
    },
    {
      id: 459,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/459-00.png",
      name: "Shnebedeck",
      isShiny: false,
    },
    {
      id: 460,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/460-00.png",
      name: "Rexblisar",
      isShiny: false,
    },
    {
      id: 461,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/461-00.png",
      name: "Snibunna",
      isShiny: false,
    },
    {
      id: 462,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/462-00.png",
      name: "Magnezone",
      isShiny: false,
    },
    {
      id: 463,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/463-00.png",
      name: "Schlurplek",
      isShiny: false,
    },
    {
      id: 464,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/464-00.png",
      name: "Rihornior",
      isShiny: false,
    },
    {
      id: 465,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/465-00.png",
      name: "Tangoloss",
      isShiny: false,
    },
    {
      id: 466,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/466-00.png",
      name: "Elevoltek",
      isShiny: false,
    },
    {
      id: 467,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/467-00.png",
      name: "Magbrant",
      isShiny: false,
    },
    {
      id: 468,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/468-00.png",
      name: "Togekiss",
      isShiny: false,
    },
    {
      id: 469,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/469-00.png",
      name: "Yanmega",
      isShiny: false,
    },
    {
      id: 470,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/470-00.png",
      name: "Folipurba",
      isShiny: false,
    },
    {
      id: 471,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/471-00.png",
      name: "Glaziola",
      isShiny: false,
    },
    {
      id: 472,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/472-00.png",
      name: "Skorgro",
      isShiny: false,
    },
    {
      id: 473,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/473-00.png",
      name: "Mamutel",
      isShiny: false,
    },
    {
      id: 474,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/474-00.png",
      name: "Porygon-Z",
      isShiny: false,
    },
    {
      id: 475,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/475-00.png",
      name: "Galagladi",
      isShiny: false,
    },
    {
      id: 476,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/476-00.png",
      name: "Voluminas",
      isShiny: false,
    },
    {
      id: 477,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/477-00.png",
      name: "Zwirrfinst",
      isShiny: false,
    },
    {
      id: 478,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/478-00.png",
      name: "Frosdedje",
      isShiny: false,
    },
    {
      id: 479,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/479-00.png",
      name: "Rotom",
      isShiny: false,
    },
    {
      id: 479,
      form: "Hitze-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/479-12.png",
      name: "Rotom",
      isShiny: false,
    },
    {
      id: 479,
      form: "Wasch-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/479-13.png",
      name: "Rotom",
      isShiny: false,
    },
    {
      id: 479,
      form: "Frost-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/479-14.png",
      name: "Rotom",
      isShiny: false,
    },
    {
      id: 479,
      form: "Wirbel-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/479-15.png",
      name: "Rotom",
      isShiny: false,
    },
    {
      id: 479,
      form: "Schneid-Form",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/479-16.png",
      name: "Rotom",
      isShiny: false,
    },
    {
      id: 480,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/480-00.png",
      name: "Selfe",
      isShiny: false,
    },
    {
      id: 481,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/481-00.png",
      name: "Vesprit",
      isShiny: false,
    },
    {
      id: 482,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/482-00.png",
      name: "Tobutz",
      isShiny: false,
    },
    {
      id: 483,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/483-00.png",
      name: "Dialga",
      isShiny: false,
    },
    {
      id: 484,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/484-00.png",
      name: "Palkia",
      isShiny: false,
    },
    {
      id: 485,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/485-00.png",
      name: "Heatran",
      isShiny: false,
    },
    {
      id: 486,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/486-00.png",
      name: "Regigigas",
      isShiny: false,
    },
    {
      id: 487,
      form: "Wandelform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/487-11.png",
      name: "Giratina",
      isShiny: false,
    },
    {
      id: 487,
      form: "Urform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/487-12.png",
      name: "Giratina",
      isShiny: false,
    },
    {
      id: 488,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/488-00.png",
      name: "Cresselia",
      isShiny: false,
    },
    {
      id: 489,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/489-00.png",
      name: "Phione",
      isShiny: false,
    },
    {
      id: 490,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/490-00.png",
      name: "Manaphy",
      isShiny: false,
    },
    {
      id: 491,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/491-00.png",
      name: "Darkrai",
      isShiny: false,
    },
    {
      id: 492,
      form: "Landform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/492-00.png",
      name: "Shaymin",
      isShiny: false,
    },
    {
      id: 492,
      form: "Zenitform",
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/492-12.png",
      name: "Shaymin",
      isShiny: false,
    },
    {
      id: 493,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/493-00.png",
      name: "Arceus",
      isShiny: false,
    },
    {
      id: 808,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/808-00.png",
      name: "Meltan",
      isShiny: false,
    },
    {
      id: 809,
      imageUrl:
        "https://files.pokefans.net/images/pokemon-go/modelle/809-00.png",
      name: "Melmetal",
      isShiny: false,
    },
  ],
};
