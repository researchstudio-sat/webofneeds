import { cyclingGroup } from "./group-cycling.js";
import { lunchGroup } from "./group-lunch.js";

export const interestGroup = {
  identifier: "interestGroup",
  label: "Interest",
  icon: undefined,
  useCases: {
    lunchGroup: lunchGroup,
    cyclingGroup: cyclingGroup,
  },
};
