import { lunchInterest } from "./uc-lunch-interest.js";
import { lunchPlan } from "./uc-lunch-plan.js";

export const lunchGroup = {
  identifier: "lunchGroup",
  label: "Lunch",
  icon: "#ico36_uc_meal-half",
  subItems: {
    lunchInterest: lunchInterest,
    lunchPlan: lunchPlan,
  },
};
