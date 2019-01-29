import { cyclingInterest } from "./uc-cycling-interest.js";
import { cyclingPlan } from "./uc-cycling-plan.js";
//import { cyclingPlan } from "./uc-cycling-plan.js";

export const cyclingGroup = {
  identifier: "cyclingGroup",
  label: "Cycling",
  icon: undefined,
  useCases: {
    cyclingInterest: cyclingInterest,
    cyclingPlan: cyclingPlan,
  },
};
