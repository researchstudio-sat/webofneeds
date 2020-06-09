import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_wtf from "../../images/won-icons/ico36_uc_wtf.svg";

export const newsarticle = {
  identifier: "newsarticle",
  label: "News Article",
  icon: ico36_uc_wtf, //TODO: Find better Icon
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:NewsArticle"],
        sockets: {
          "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
          "#associatedArticleInverseSocket":
            vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: {
      [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: ["organization"],
    },
  },
  details: {
    title: { ...details.title, mandatory: true },
    description: { ...details.description, mandatory: true },
  },
  seeksDetails: {},
};
