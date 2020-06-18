import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_pokemon_raid from "../../images/won-icons/ico36_pokemon-raid.svg";
import ico36_pokeball from "../../images/won-icons/ico36_pokeball.svg";

export const pokemonGoRaid = {
  identifier: "pokemonGoRaid",
  label: "Plan a Pokémon Raid",
  icon: ico36_pokemon_raid, //TODO: Better Icon
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
      },
      seeks: {},
    }),
  },
  reactions: {
    [vocab.GROUP.GroupSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["pokemonInterest", "persona"],
      },
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["pokemonGoRaid"],
      },
    },
  },
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
  icon: ico36_pokeball, //TODO: Better Icon
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
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["pokemonGoRaid"],
      },
    },
  },
  details: {
    title: { ...details.title },
  },
  seeksDetails: {},
};
