import { cyclingInterest } from "./uc-cycling-interest.js";
import { lunchInterest } from "./uc-lunch-interest.js";

export const interestsGroup = {
  identifier: "interestsgroup",
  label: "Interests",
  icon: undefined,
  subItems: {
    lunchInterest: lunchInterest,
    cyclingInterest: cyclingInterest,
  },
};
