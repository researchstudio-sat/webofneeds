import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";
import { groupChat } from "./uc-group-chat.js";

// import { resource } from "./uc-resource.js"; //TODO: ValueFlows useCase, currently excluded
// import { activity } from "./uc-activity.js"; //TODO: ValueFlows useCase, currently excluded

import ico36_plus from "../../images/won-icons/ico36_plus.svg";

export const otherGroup = {
  identifier: "otherGroup",
  label: "More...",
  icon: ico36_plus,
  subItems: {
    // resource: resource, //TODO: ValueFlows useCase, currently excluded
    // activity: activity, //TODO: ValueFlows useCase, currently excluded
    complain: complain,
    handleComplaint: handleComplaint,
    customUseCase: customUseCase,
    groupChat: groupChat,
  },
};
