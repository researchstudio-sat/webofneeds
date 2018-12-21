import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { custom } from "./uc-custom.js";

export const otherGroup = {
  identifier: "othergroup",
  label: "Other Use Cases",
  icon: undefined,
  useCases: {
    complain: complain,
    handleComplaint: handleComplaint,
    custom: custom,
  },
};
