import { getBreakfast } from "./uc-get-breakfast";
import { getLunch } from "./uc-get-lunch";
import { afterparty } from "./uc-afterparty";
import { sightseeing } from "./uc-sightseeing";
import { getToKnow } from "./uc-get-to-know.js";
import { complain } from "./uc-complain";
import { handleComplaint } from "./uc-handle-complaint";

export const socialGroup = {
  identifier: "socialgroup",
  label: "Social",
  icon: undefined,
  subItems: {
    getBreakfast: getBreakfast,
    getLunch: getLunch,
    afterparty: afterparty,
    sightseeing: sightseeing,
    getToKnow: getToKnow,
    complain: complain,
    handleComplaint: handleComplaint,
  },
};
