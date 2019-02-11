import { cyclingInterest } from "./uc-cycling-interest.js";
import { cyclingPlan } from "./uc-cycling-plan.js";

export const cyclingGroup = {
  identifier: "cyclingGroup",
  label: "Cycling",
  icon: undefined,
  subItems: {
    cyclingInterest: cyclingInterest,
    cyclingPlan: cyclingPlan,
  },
};
