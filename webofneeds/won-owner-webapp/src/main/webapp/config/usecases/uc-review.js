import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico16_uc_review from "../../images/won-icons/ico16_uc_review.svg";

export const review = {
  identifier: "review",
  label: "Review",
  icon: ico16_uc_review,
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
