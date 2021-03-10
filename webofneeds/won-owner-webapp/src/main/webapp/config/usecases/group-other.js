import vocab from "../../app/service/vocab.js";

import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";
// import { resource } from "./uc-resource.js"; //TODO: ValueFlows useCase, currently excluded
// import { activity } from "./uc-activity.js"; //TODO: ValueFlows useCase, currently excluded
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";

import ico36_plus from "../../images/won-icons/ico36_plus.svg";
import ico36_uc_custom from "../../images/won-icons/ico36_uc_custom.svg";

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
    groupChat: {
      identifier: "groupChat",
      label: "Custom GroupChat",
      icon: ico36_uc_custom,
      draft: {
        ...mergeInEmptyDraft({
          content: {
            type: [vocab.WON.GenericGroupChatCompacted],
            sockets: {
              "#groupSocket": vocab.GROUP.GroupSocketCompacted,
              "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
              "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
            },
          },
        }),
      },
      reactions: {
        ...defaultReactions,
        [vocab.GROUP.GroupSocketCompacted]: {
          [vocab.CHAT.ChatSocketCompacted]: {
            useCaseIdentifiers: ["*"],
          },
          [vocab.GROUP.GroupSocketCompacted]: {
            useCaseIdentifiers: ["*"],
          },
        },
      },
      details: details,
      seeksDetails: details,
    },
  },
};
