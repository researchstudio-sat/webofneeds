import { breakfastInterest, breakfastEvent } from "./uc-breakfast.js";
import { afterpartyInterest, afterpartyEvent } from "./uc-afterparty.js";
import { cyclingInterest, cyclingEvent } from "./uc-cycling.js";
import { lunchInterest, lunchEvent } from "./uc-lunch.js";
import { genericInterest } from "./uc-interest.js";
import { pokemonInterest, pokemonGoRaid } from "./uc-pokemon.js";
import { sightseeingInterest, sightseeingEvent } from "./uc-sightseeing.js";
import { event } from "./uc-event.js";

import ico36_detail_interests from "../../images/won-icons/ico36_detail_interests.svg";
import ico36_detail_datetime from "~/images/won-icons/ico36_detail_datetime.svg";

export const interestsGroup = {
  identifier: "interestsGroup",
  label: "Interests",
  icon: ico36_detail_interests,
  subItems: {
    sightseeingInterest: sightseeingInterest,
    afterpartyInterest: afterpartyInterest,
    breakfastInterest: breakfastInterest,
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
    sightseeingEvent: sightseeingEvent,
    afterpartyEvent: afterpartyEvent,
    breakfastEvent: breakfastEvent,
    lunchEvent: lunchEvent,
    cyclingEvent: cyclingEvent,
    pokemonGoRaid: pokemonGoRaid,
    event: event,
  },
};
