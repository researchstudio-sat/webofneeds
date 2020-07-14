import { cyclingInterest, cyclingPlan } from "./uc-cycling.js";
import { lunchInterest, lunchPlan } from "./uc-lunch.js";
import { genericInterest, genericPlan } from "./uc-interest.js";
import { pokemonInterest, pokemonGoRaid } from "./uc-pokemon.js";

import ico36_detail_interests from "../../images/won-icons/ico36_detail_interests.svg";
import ico36_detail_datetime from "~/images/won-icons/ico36_detail_datetime.svg";

export const interestsGroup = {
  identifier: "interestsgroup",
  label: "Interests",
  icon: ico36_detail_interests,
  subItems: {
    lunchInterest: lunchInterest,
    cyclingInterest: cyclingInterest,
    pokemonInterest: pokemonInterest,
    genericInterest: genericInterest,
  },
};

export const plansGroup = {
  identifier: "plansGroup",
  label: "Plans",
  icon: ico36_detail_datetime,
  subItems: {
    lunchPlan: lunchPlan,
    cyclingPlan: cyclingPlan,
    pokemonGoRaid: pokemonGoRaid,
    genericPlan: genericPlan,
  },
};
