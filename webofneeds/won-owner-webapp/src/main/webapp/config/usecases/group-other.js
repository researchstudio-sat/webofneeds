import vocab from "../../app/service/vocab.js";

import { complain } from "./uc-complain.js";
import { organization } from "./uc-organization.js";
import { newsarticle } from "./uc-newsarticle.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";
import { resource } from "./uc-resource.js";
import { activity } from "./uc-activity.js";
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";

import ico36_plus from "../../images/won-icons/ico36_plus.svg";
import ico36_uc_custom from "../../images/won-icons/ico36_uc_custom.svg";

export const otherGroup = {
  identifier: "othergroup",
  label: "More...",
  icon: ico36_plus,
  subItems: {
    resource: resource,
    activity: activity,
    complain: complain,
    handleComplaint: handleComplaint,
    customUseCase: customUseCase,
    organization: organization,
    newsarticle: newsarticle,
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
