import { lunchPlan } from "./uc-lunch-plan.js";
import { cyclingPlan } from "./uc-cycling-plan.js";

export const hiddenGroup = {
  identifier: "hiddenGroup",
  label: "Hidden",
  icon: undefined,
  subItems: {
    lunchPlan: lunchPlan,
    cyclingPlan: cyclingPlan,
  },
};
