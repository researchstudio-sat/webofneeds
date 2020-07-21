import { getToKnow } from "./uc-get-to-know.js";
import { complain } from "./uc-complain";
import { handleComplaint } from "./uc-handle-complaint";

import ico36_uc_find_people from "../../images/won-icons/ico36_uc_find_people.svg";

export const socialGroup = {
  identifier: "socialGroup",
  label: "Social",
  icon: ico36_uc_find_people,
  subItems: {
    complain: complain,
    getToKnow: getToKnow,
    handleComplaint: handleComplaint,
  },
};
