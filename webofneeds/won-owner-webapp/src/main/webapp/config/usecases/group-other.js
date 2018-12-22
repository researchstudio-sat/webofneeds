import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";

export const otherGroup = {
  identifier: "othergroup",
  label: "More...",
  icon: undefined,
  useCases: {
    complain: complain,
    handleComplaint: handleComplaint,
    customUseCase: customUseCase,
  },
};
