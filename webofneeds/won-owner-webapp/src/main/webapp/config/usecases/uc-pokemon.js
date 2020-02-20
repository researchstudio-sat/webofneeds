import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import { Generator } from "sparqljs";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

window.SparqlGenerator4dbg = Generator;

export const pokemonGoRaid = {
  identifier: "pokemonGoRaid",
  label: "Plan a Pokémon Raid",
  icon: "#ico36_pokemon-raid", //TODO: Better Icon
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:PlanAction"],
        eventObjectAboutUris: "http://dbpedia.org/resource/Pokemon_Go",
        sockets: {
          "#groupSocket": vocab.GROUP.GroupSocketCompacted,
          "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
        },
        defaultSocket: { "#groupSocket": vocab.GROUP.GroupSocketCompacted },
      },
      seeks: {},
    }),
  },
  reactionUseCases: [
    {
      identifier: "pokemonInterest",
      senderSocketType: vocab.CHAT.ChatSocketCompacted,
      targetSocketType: vocab.GROUP.GroupSocketCompacted,
    },
  ],
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
        eventObjectAboutUris: "http://dbpedia.org/resource/Pokemon_Go",
      },
    }),
  },
  enabledUseCases: [
    {
      identifier: "pokemonGoRaid",
      senderSocketType: vocab.GROUP.GroupSocketCompacted,
      targetSocketType: vocab.CHAT.ChatSocketCompacted,
    },
  ],
  reactionUseCases: [
    {
      identifier: "pokemonGoRaid",
      senderSocketType: vocab.GROUP.GroupSocketCompacted,
      targetSocketType: vocab.CHAT.ChatSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title },
  },
  seeksDetails: {},
};
