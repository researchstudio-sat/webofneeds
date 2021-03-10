/**
 * Created by ms on 10.03.2021
 */
import vocab from "../../app/service/vocab.js";
import ico36_uc_custom from "../../images/won-icons/ico36_uc_custom.svg";
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";

export const groupChat = {
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
};
