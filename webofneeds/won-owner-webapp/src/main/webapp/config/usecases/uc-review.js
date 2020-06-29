import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";

export const review = {
  identifier: "review",
  label: "Review",
  icon: undefined, //No Icon For Persona UseCase (uses identicon)
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Review"],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#sReviewInverseSocket": vocab.WXSCHEMA.ReviewInverseSocketCompacted,
          "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
          "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXSCHEMA.ReviewInverseSocketCompacted]: {
      [vocab.WXSCHEMA.ReviewSocketCompacted]: {
        useCaseIdentifiers: ["*"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    reviewRating: { ...details.reviewRating, mandatory: true },
    description: { ...details.description },
    images: { ...details.images },
  },
  seeksDetails: {},
};
