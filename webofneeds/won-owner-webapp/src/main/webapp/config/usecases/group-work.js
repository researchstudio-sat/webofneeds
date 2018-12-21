import { complain } from "./uc-complain";
import { handleComplaint } from "./uc-handle-complaint";

export const complainGroup = {
  identifier: "complaingroup",
  label: "Complaints",
  icon: undefined,
  useCases: {
    complain: complain,
    handleComplaint: handleComplaint,
  },
};
