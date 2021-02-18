import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_detail_interests from "~/images/won-icons/ico36_detail_interests.svg";

export const genericExpertise = {
  identifier: "genericExpertise",
  label: "Expertise",
  icon: ico36_detail_interests,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.WXPERSONA.ExpertiseCompacted],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#expertiseOfSocket": vocab.WXPERSONA.ExpertiseOfSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["genericExpertise", "persona"],
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    eventObjectAboutUris: {
      ...details.eventObjectAboutUris,
      mandatory: true,
    },
  },
  seeksDetails: {},
};
