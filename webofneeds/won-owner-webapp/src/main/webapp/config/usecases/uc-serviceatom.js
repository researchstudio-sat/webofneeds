import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_robot from "../../images/won-icons/ico36_robot.svg";

export const serviceAtom = {
  identifier: "serviceAtom",
  label: "Bot",
  icon: ico36_robot,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.BOT.ServiceAtomCompacted],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#holderSocket": vocab.HOLD.HolderSocketCompacted,
          "#reviewSocket": vocab.REVIEW.ReviewSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.HOLD.HolderSocketCompacted]: {
      [vocab.HOLD.HoldableSocketCompacted]: {
        useCaseIdentifiers: ["*"],
        refuseNonOwned: true,
      },
    },
  },
  details: {
    personaName: { ...details.personaName, mandatory: true },
    description: { ...details.description },
    website: { ...details.website },
    termsOfService: { ...details.termsOfService },
  },
  seeksDetails: {},
};
