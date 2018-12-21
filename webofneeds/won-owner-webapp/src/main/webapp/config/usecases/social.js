import { getBreakfast } from "./uc-get-breakfast";
import { getLunch } from "./uc-get-lunch";
import { afterparty } from "./uc-afterparty";
import { sightseeing } from "./uc-sightseeing";

export const socialGroup = {
  identifier: "socialgroup",
  label: "Social Activities",
  icon: undefined,
  useCases: {
    getBreakfast: getBreakfast,
    getLunch: getLunch,
    afterparty: afterparty,
    sightseeing: sightseeing,
  },
};
