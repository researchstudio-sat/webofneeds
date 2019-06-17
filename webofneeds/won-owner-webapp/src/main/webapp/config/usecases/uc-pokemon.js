import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import won from "../../app/service/won.js";
import { Generator } from "sparqljs";
import * as wonUtils from "../../app/won-utils.js";

window.SparqlGenerator4dbg = Generator;

export const pokemonGoRaid = {
  identifier: "pokemonGoRaid",
  label: "Plan a Pokémon Raid",
  icon: "#ico36_pokemon-raid", //TODO: Better Icon
  doNotMatchAfter: wonUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:PlanAction"],
        eventObject: "http://dbpedia.org/resource/Pokemon_Go",
        sockets: {
          "#groupSocket": won.GROUP.GroupSocketCompacted,
          "#holdableSocket": won.HOLD.HoldableSocketCompacted,
        },
        defaultSocket: { "#groupSocket": won.GROUP.GroupSocketCompacted },
      },
      seeks: {},
    }),
  },
  reactionUseCases: ["pokemonInterest"],
  details: {
    pokemonRaid: { ...details.pokemonRaid, mandatory: true },
    location: { ...details.location, mandatory: true },
    pokemonGymInfo: { ...details.pokemonGymInfo },
    description: { ...details.description },
  },
  seeksDetails: {},
};

export const pokemonInterest = {
  identifier: "pokemonInterest",
  label: "Add Interest in Pokémon Go",
  icon: "#ico36_pokeball", //TODO: Better Icon
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:Interest"],
      },
      seeks: {
        type: ["s:PlanAction"],
        eventObject: "http://dbpedia.org/resource/Pokemon_Go",
      },
    }),
  },
  enabledUseCases: ["pokemonGoRaid"],
  reactionUseCases: ["pokemonGoRaid"],
  details: {
    title: { ...details.title },
  },
  seeksDetails: {},
};
