import { cyclingInterest } from "./uc-cycling.js";
import { lunchInterest } from "./uc-lunch.js";
import { pokemonInterest } from "./uc-pokemon.js";

import ico36_detail_interests from "../../images/won-icons/ico36_detail_interests.svg";

export const interestsGroup = {
  identifier: "interestsgroup",
  label: "Interests",
  icon: ico36_detail_interests,
  subItems: {
    lunchInterest: lunchInterest,
    cyclingInterest: cyclingInterest,
    pokemonInterest: pokemonInterest,
  },
};
