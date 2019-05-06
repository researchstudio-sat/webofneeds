import { lunchPlan } from "./uc-lunch.js";
import { cyclingPlan } from "./uc-cycling.js";

export const hiddenGroup = {
  identifier: "hiddenGroup",
  label: "Hidden",
  icon: undefined,
  subItems: {
    lunchPlan: lunchPlan,
    cyclingPlan: cyclingPlan,
  },
};
