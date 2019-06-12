import Immutable from "immutable";
import won from "../../app/won-es6.js";
import {
  isValidDate,
  //parseDatetimeStrictly,
  toLocalISODateString,
} from "../../app/utils.js";

export const pokemonGymInfo = {
  identifier: "pokemonGymInfo",
  label: "Additional Gym Info",
  icon: "#ico36_dumbbell", //TODO: Create and use better icon
  messageEnabled: false,
  component: "pokemon-gym-picker",
  viewerComponent: "pokemon-gym-viewer",
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
      const ex = won.parseFrom(jsonLDImm, ["won:gymex"], "xsd:boolean");

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
  // FIXME: replace pokeball icon with working pokemon image links
  // current icon is a placeholder from the noun project due to broken links
  fullPokemonList: [
    {
      id: 1,
      name: "Bisasam",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 2,
      name: "Bisaknosp",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 3,
      name: "Bisaflor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 4,
      name: "Glumanda",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 5,
      name: "Glutexo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 6,
      name: "Glurak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 7,
      name: "Schiggy",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 8,
      name: "Schillok",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 9,
      name: "Turtok",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 10,
      name: "Raupy",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 11,
      name: "Safcon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 12,
      name: "Smettbo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 13,
      name: "Hornliu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 14,
      name: "Kokuna",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 15,
      name: "Bibor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 16,
      name: "Taubsi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 17,
      name: "Tauboga",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 18,
      name: "Tauboss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 19,
      name: "Rattfratz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 20,
      name: "Rattikarl",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 21,
      name: "Habitak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 22,
      name: "Ibitak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 23,
      name: "Rettan",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 24,
      name: "Arbok",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 25,
      name: "Pikachu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 26,
      name: "Raichu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 27,
      name: "Sandan",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 28,
      name: "Sandamer",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 29,
      name: "Nidoran?",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 30,
      name: "Nidorina",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 31,
      name: "Nidoqueen",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 32,
      name: "Nidoran?",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 33,
      name: "Nidorino",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 34,
      name: "Nidoking",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 35,
      name: "Piepi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 36,
      name: "Pixi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 37,
      name: "Vulpix",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 38,
      name: "Vulnona",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 39,
      name: "Pummeluff",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 40,
      name: "Knuddeluff",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 41,
      name: "Zubat",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 42,
      name: "Golbat",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 43,
      name: "Myrapla",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 44,
      name: "Duflor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 45,
      name: "Giflor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 46,
      name: "Paras",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 47,
      name: "Parasek",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 48,
      name: "Bluzuk",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 49,
      name: "Omot",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 50,
      name: "Digda",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 51,
      name: "Digdri",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 52,
      name: "Mauzi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 53,
      name: "Snobilikat",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 54,
      name: "Enton",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 55,
      name: "Entoron",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 56,
      name: "Menki",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 57,
      name: "Rasaff",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 58,
      name: "Fukano",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 59,
      name: "Arkani",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 60,
      name: "Quapsel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 61,
      name: "Quaputzi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 62,
      name: "Quappo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 63,
      name: "Abra",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 64,
      name: "Kadabra",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 65,
      name: "Simsala",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 66,
      name: "Machollo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 67,
      name: "Maschock",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 68,
      name: "Machomei",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 69,
      name: "Knofensa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 70,
      name: "Ultrigaria",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 71,
      name: "Sarzenia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 72,
      name: "Tentacha",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 73,
      name: "Tentoxa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 74,
      name: "Kleinstein",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 75,
      name: "Georok",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 76,
      name: "Geowaz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 77,
      name: "Ponita",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 78,
      name: "Gallopa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 79,
      name: "Flegmon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 80,
      name: "Lahmus",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 81,
      name: "Magnetilo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 82,
      name: "Magneton",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 83,
      name: "Porenta",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 84,
      name: "Dodu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 85,
      name: "Dodri",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 86,
      name: "Jurob",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 87,
      name: "Jugong",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 88,
      name: "Sleima",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 89,
      name: "Sleimok",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 90,
      name: "Muschas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 91,
      name: "Austos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 92,
      name: "Nebulak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 93,
      name: "Alpollo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 94,
      name: "Gengar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 95,
      name: "Onix",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 96,
      name: "Traumato",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 97,
      name: "Hypno",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 98,
      name: "Krabby",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 99,
      name: "Kingler",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 100,
      name: "Voltobal",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 101,
      name: "Lektrobal",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 102,
      name: "Owei",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 103,
      name: "Kokowei",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 104,
      name: "Tragosso",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 105,
      name: "Knogga",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 106,
      name: "Kicklee",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 107,
      name: "Nockchan",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 108,
      name: "Schlurp",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 109,
      name: "Smogon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 110,
      name: "Smogmog",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 111,
      name: "Rihorn",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 112,
      name: "Rizeros",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 113,
      name: "Chaneira",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 114,
      name: "Tangela",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 115,
      name: "Kangama",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 116,
      name: "Seeper",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 117,
      name: "Seemon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 118,
      name: "Goldini",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 119,
      name: "Golking",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 120,
      name: "Sterndu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 121,
      name: "Starmie",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 122,
      name: "Pantimos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 123,
      name: "Sichlor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 124,
      name: "Rossana",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 125,
      name: "Elektek",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 126,
      name: "Magmar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 127,
      name: "Pinsir",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 128,
      name: "Tauros",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 129,
      name: "Karpador",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 130,
      name: "Garados",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 131,
      name: "Lapras",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 132,
      name: "Ditto",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 133,
      name: "Evoli",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 134,
      name: "Aquana",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 135,
      name: "Blitza",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 136,
      name: "Flamara",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 137,
      name: "Porygon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 138,
      name: "Amonitas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 139,
      name: "Amoroso",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 140,
      name: "Kabuto",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 141,
      name: "Kabutops",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 142,
      name: "Aerodactyl",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 143,
      name: "Relaxo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 144,
      name: "Arktos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 145,
      name: "Zapdos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 146,
      name: "Lavados",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 147,
      name: "Dratini",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 148,
      name: "Dragonir",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 149,
      name: "Dragoran",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 150,
      name: "Mewtu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 151,
      name: "Mew",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 152,
      name: "Endivie",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 153,
      name: "Lorblatt",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 154,
      name: "Meganie",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 155,
      name: "Feurigel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 156,
      name: "Igelavar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 157,
      name: "Tornupto",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 158,
      name: "Karnimani",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 159,
      name: "Tyracroc",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 160,
      name: "Impergator",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 161,
      name: "Wiesor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 162,
      name: "Wiesenior",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 163,
      name: "Hoothoot",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 164,
      name: "Noctuh",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 165,
      name: "Ledyba",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 166,
      name: "Ledian",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 167,
      name: "Webarak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 168,
      name: "Ariados",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 169,
      name: "Iksbat",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 170,
      name: "Lampi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 171,
      name: "Lanturn",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 172,
      name: "Pichu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 173,
      name: "Pii",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 174,
      name: "Fluffeluff",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 175,
      name: "Togepi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 176,
      name: "Togetic",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 177,
      name: "Natu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 178,
      name: "Xatu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 179,
      name: "Voltilamm",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 180,
      name: "Waaty",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 181,
      name: "Ampharos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 182,
      name: "Blubella",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 183,
      name: "Marill",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 184,
      name: "Azumarill",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 185,
      name: "Mogelbaum",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 186,
      name: "Quaxo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 187,
      name: "Hoppspross",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 188,
      name: "Hubelupf",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 189,
      name: "Papungha",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 190,
      name: "Griffel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 191,
      name: "Sonnkern",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 192,
      name: "Sonnflora",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 193,
      name: "Yanma",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 194,
      name: "Felino",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 195,
      name: "Morlord",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 196,
      name: "Psiana",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 197,
      name: "Nachtara",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 198,
      name: "Kramurx",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 199,
      name: "Laschoking",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 200,
      name: "Traunfugil",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 201,
      name: "Icognito",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 202,
      name: "Woingenau",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 203,
      name: "Girafarig",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 204,
      name: "Tannza",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 205,
      name: "Forstellka",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 206,
      name: "Dummisel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 207,
      name: "Skorgla",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 208,
      name: "Stahlos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 209,
      name: "Snubbull",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 210,
      name: "Granbull",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 211,
      name: "Baldorfish",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 212,
      name: "Scherox",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 213,
      name: "Pottrott",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 214,
      name: "Skaraborn",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 215,
      name: "Sniebel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 216,
      name: "Teddiursa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 217,
      name: "Ursaring",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 218,
      name: "Schneckmag",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 219,
      name: "Magcargo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 220,
      name: "Quiekel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 221,
      name: "Keifel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 222,
      name: "Corasonn",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 223,
      name: "Remoraid",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 224,
      name: "Octillery",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 225,
      name: "Botogel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 226,
      name: "Mantax",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 227,
      name: "Panzaeron",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 228,
      name: "Hunduster",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 229,
      name: "Hundemon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 230,
      name: "Seedraking",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 231,
      name: "Phanpy",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 232,
      name: "Donphan",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 233,
      name: "Porygon2",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 234,
      name: "Damhirplex",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 235,
      name: "Farbeagle",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 236,
      name: "Rabauz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 237,
      name: "Kapoera",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 238,
      name: "Kussilla",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 239,
      name: "Elekid",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 240,
      name: "Magby",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 241,
      name: "Miltank",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 242,
      name: "Heiteira",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 243,
      name: "Raikou",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 244,
      name: "Entei",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 245,
      name: "Suicune",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 246,
      name: "Larvitar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 247,
      name: "Pupitar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 248,
      name: "Despotar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 249,
      name: "Lugia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 250,
      name: "Ho-Oh",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 251,
      name: "Celebi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 252,
      name: "Geckarbor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 253,
      name: "Reptain",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 254,
      name: "Gewaldro",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 255,
      name: "Flemmli",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 256,
      name: "Jungglut",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 257,
      name: "Lohgock",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 258,
      name: "Hydropi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 259,
      name: "Moorabbel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 260,
      name: "Sumpex",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 261,
      name: "Fiffyen",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 262,
      name: "Magnayen",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 263,
      name: "Zigzachs",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 264,
      name: "Geradaks",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 265,
      name: "Waumpel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 266,
      name: "Schaloko",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 267,
      name: "Papinella",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 268,
      name: "Panekon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 269,
      name: "Pudox",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 270,
      name: "Loturzel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 271,
      name: "Lombrero",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 272,
      name: "Kappalores",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 273,
      name: "Samurzel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 274,
      name: "Blanas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 275,
      name: "Tengulist",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 276,
      name: "Schwalbini",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 277,
      name: "Schwalboss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 278,
      name: "Wingull",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 279,
      name: "Pelipper",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 280,
      name: "Trasla",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 281,
      name: "Kirlia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 282,
      name: "Guardevoir",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 283,
      name: "Gehweiher",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 284,
      name: "Maskeregen",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 285,
      name: "Knilz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 286,
      name: "Kapilz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 287,
      name: "Bummelz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 288,
      name: "Muntier",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 289,
      name: "Letarking",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 290,
      name: "Nincada",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 291,
      name: "Ninjask",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 292,
      name: "Ninjatom",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 293,
      name: "Flurmel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 294,
      name: "Krakeelo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 295,
      name: "Krawumms",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 296,
      name: "Makuhita",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 297,
      name: "Hariyama",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 298,
      name: "Azurill",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 299,
      name: "Nasgnet",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 300,
      name: "Eneco",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 301,
      name: "Enekoro",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 302,
      name: "Zobiris",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 303,
      name: "Flunkifer",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 304,
      name: "Stollunior",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 305,
      name: "Stollrak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 306,
      name: "Stolloss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 307,
      name: "Meditie",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 308,
      name: "Meditalis",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 309,
      name: "Frizelbliz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 310,
      name: "Voltenso",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 311,
      name: "Plusle",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 312,
      name: "Minun",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 313,
      name: "Volbeat",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 314,
      name: "Illumise",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 315,
      name: "Roselia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 316,
      name: "Schluppuck",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 317,
      name: "Schlukwech",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 318,
      name: "Kanivanha",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 319,
      name: "Tohaido",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 320,
      name: "Wailmer",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 321,
      name: "Wailord",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 322,
      name: "Camaub",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 323,
      name: "Camerupt",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 324,
      name: "Qurtel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 325,
      name: "Spoink",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 326,
      name: "Groink",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 327,
      name: "Pandir",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 328,
      name: "Knacklion",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 329,
      name: "Vibrava",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 330,
      name: "Libelldra",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 331,
      name: "Tuska",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 332,
      name: "Noktuska",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 333,
      name: "Wablu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 334,
      name: "Altaria",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 335,
      name: "Sengo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 336,
      name: "Vipitis",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 337,
      name: "Lunastein",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 338,
      name: "Sonnfel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 339,
      name: "Schmerbe",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 340,
      name: "Welsar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 341,
      name: "Krebscorps",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 342,
      name: "Krebutack",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 343,
      name: "Puppance",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 344,
      name: "Lepumentas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 345,
      name: "Liliep",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 346,
      name: "Wielie",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 347,
      name: "Anorith",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 348,
      name: "Armaldo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 349,
      name: "Barschwa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 350,
      name: "Milotic",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 351,
      name: "Formeo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 352,
      name: "Kecleon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 353,
      name: "Shuppet",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 354,
      name: "Banette",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 355,
      name: "Zwirrlicht",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 356,
      name: "Zwirrklop",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 357,
      name: "Tropius",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 358,
      name: "Palimpalim",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 359,
      name: "Absol",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 360,
      name: "Isso",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 361,
      name: "Schneppke",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 362,
      name: "Firnontor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 363,
      name: "Seemops",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 364,
      name: "Seejong",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 365,
      name: "Walraisa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 366,
      name: "Perlu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 367,
      name: "Aalabyss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 368,
      name: "Saganabyss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 369,
      name: "Relicanth",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 370,
      name: "Liebiskus",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 371,
      name: "Kindwurm",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 372,
      name: "Draschel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 373,
      name: "Brutalanda",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 374,
      name: "Tanhel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 375,
      name: "Metang",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 376,
      name: "Metagross",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 377,
      name: "Regirock",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 378,
      name: "Regice",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 379,
      name: "Registeel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 380,
      name: "Latias",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 381,
      name: "Latios",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 382,
      name: "Kyogre",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 383,
      name: "Groudon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 384,
      name: "Rayquaza",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 385,
      name: "Jirachi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 386,
      name: "Deoxys",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Normalform",
    },
    {
      id: 387,
      name: "Chelast",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 388,
      name: "Chelcarain",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 389,
      name: "Chelterrar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 390,
      name: "Panflam",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 391,
      name: "Panpyro",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 392,
      name: "Panferno",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 393,
      name: "Plinfa",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 394,
      name: "Pliprin",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 395,
      name: "Impoleon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 396,
      name: "Staralili",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 397,
      name: "Staravia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 398,
      name: "Staraptor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 399,
      name: "Bidiza",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 400,
      name: "Bidifas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 401,
      name: "Zirpurze",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 402,
      name: "Zirpeise",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 403,
      name: "Sheinux",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 404,
      name: "Luxio",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 405,
      name: "Luxtra",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 406,
      name: "Knospi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 407,
      name: "Roserade",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 408,
      name: "Koknodon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 409,
      name: "Rameidon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 410,
      name: "Schilterus",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 411,
      name: "Bollterus",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 412,
      name: "Burmy",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 413,
      name: "Burmadame",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Pflanzenumhang",
    },
    {
      id: 414,
      name: "Moterpel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 415,
      name: "Wadribie",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 416,
      name: "Honweisel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 417,
      name: "Pachirisu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 418,
      name: "Bamelin",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 419,
      name: "Bojelin",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 420,
      name: "Kikugi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 421,
      name: "Kinoso",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 422,
      name: "Schalellos",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 423,
      name: "Gastrodon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 424,
      name: "Ambidiffel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 425,
      name: "Driftlon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 426,
      name: "Drifzepeli",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 427,
      name: "Haspiror",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 428,
      name: "Schlapor",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 429,
      name: "Traunmagil",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 430,
      name: "Kramshef",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 431,
      name: "Charmian",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 432,
      name: "Shnurgarst",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 433,
      name: "Klingplim",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 434,
      name: "Skunkapuh",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 435,
      name: "Skuntank",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 436,
      name: "Bronzel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 437,
      name: "Bronzong",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 438,
      name: "Mobai",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 439,
      name: "Pantimimi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 440,
      name: "Wonneira",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 441,
      name: "Plaudagei",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 442,
      name: "Kryppuk",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 443,
      name: "Kaumalat",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 444,
      name: "Knarksel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 445,
      name: "Knakrack",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 446,
      name: "Mampfaxo",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 447,
      name: "Riolu",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 448,
      name: "Lucario",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 449,
      name: "Hippopotas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 450,
      name: "Hippoterus",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 451,
      name: "Pionskora",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 452,
      name: "Piondragi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 453,
      name: "Glibunkel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 454,
      name: "Toxiquak",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 455,
      name: "Venuflibis",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 456,
      name: "Finneon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 457,
      name: "Lumineon",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 458,
      name: "Mantirps",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 459,
      name: "Shnebedeck",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 460,
      name: "Rexblisar",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 461,
      name: "Snibunna",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 462,
      name: "Magnezone",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 463,
      name: "Schlurplek",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 464,
      name: "Rihornior",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 465,
      name: "Tangoloss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 466,
      name: "Elevoltek",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 467,
      name: "Magbrant",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 468,
      name: "Togekiss",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 469,
      name: "Yanmega",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 470,
      name: "Folipurba",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 471,
      name: "Glaziola",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 472,
      name: "Skorgro",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 473,
      name: "Mamutel",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 474,
      name: "Porygon-Z",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 475,
      name: "Galagladi",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 476,
      name: "Voluminas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 477,
      name: "Zwirrfinst",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 478,
      name: "Frosdedje",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 479,
      name: "Rotom",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Standardform",
    },
    {
      id: 480,
      name: "Selfe",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 481,
      name: "Vesprit",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 482,
      name: "Tobutz",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 483,
      name: "Dialga",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 484,
      name: "Palkia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 485,
      name: "Heatran",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 486,
      name: "Regigigas",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 487,
      name: "Giratina",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Wandelform",
    },
    {
      id: 488,
      name: "Cresselia",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 489,
      name: "Phione",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 490,
      name: "Manaphy",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 491,
      name: "Darkrai",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 492,
      name: "Shaymin",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
      form: "Landform",
    },
    {
      id: 493,
      name: "Arceus",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 808,
      name: "Meltan",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
    {
      id: 809,
      name: "Melmetal",
      imageUrl: "https://static.thenounproject.com/png/18316-200.png",
    },
  ],
};
