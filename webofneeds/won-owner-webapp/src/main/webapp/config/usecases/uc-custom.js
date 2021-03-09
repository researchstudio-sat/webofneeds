import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico16_uc_custom_post from "../../images/won-icons/ico16_uc_custom_post.svg";
import vocab from "../../app/service/vocab";

export const customUseCase = {
  identifier: "customUseCase",
  label: "Custom Post",
  icon: ico16_uc_custom_post,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: { ...mergeInEmptyDraft() },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
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
