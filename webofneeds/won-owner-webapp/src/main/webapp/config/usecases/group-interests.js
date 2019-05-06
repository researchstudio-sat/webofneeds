import { cyclingInterest } from "./uc-cycling.js";
import { lunchInterest } from "./uc-lunch.js";

export const interestsGroup = {
  identifier: "interestsgroup",
  label: "Interests",
  icon: undefined,
  subItems: {
    lunchInterest: lunchInterest,
    cyclingInterest: cyclingInterest,
  },
};
