import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_custom from "../../images/won-icons/ico36_uc_custom.svg";
import vocab from "../../app/service/vocab";

export const customUseCase = {
  identifier: "customUseCase",
  label: "Custom Post",
  icon: ico36_uc_custom,
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
