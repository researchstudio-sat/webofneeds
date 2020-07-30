import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";

export const activity = {
  identifier: "activity",
  label: "Activity",
  icon: ico36_plus,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.WXVALUEFLOWS.ActivityCompacted],
        sockets: {
          "#ResourceSocket": vocab.WXVALUEFLOWS.ResourceSocketCompacted,
          "#ActorSocket": vocab.WXVALUEFLOWS.ActorSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: {
      [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: {
        useCaseIdentifiers: ["resource"],
      },
    },
    [vocab.WXVALUEFLOWS.ActorSocketCompacted]: {
      [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
  },
  seeksDetails: {},
};
