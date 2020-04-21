import { getBreakfast } from "./uc-get-breakfast";
import { afterparty } from "./uc-afterparty";
import { sightseeing } from "./uc-sightseeing";
import { getToKnow } from "./uc-get-to-know.js";
import { complain } from "./uc-complain";
import { handleComplaint } from "./uc-handle-complaint";

import ico36_uc_find_people from "../../images/won-icons/ico36_uc_find_people.svg";

export const socialGroup = {
  identifier: "socialgroup",
  label: "Social",
  icon: ico36_uc_find_people,
  subItems: {
    getBreakfast: getBreakfast,
    afterparty: afterparty,
    complain: complain,
    getToKnow: getToKnow,
    sightseeing: sightseeing,
    handleComplaint: handleComplaint,
  },
};
