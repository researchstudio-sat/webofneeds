/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_wtf from "../../images/won-icons/ico36_uc_wtf.svg";

export const organization = {
  identifier: "organization",
  label: "Organization",
  icon: ico36_uc_wtf, //TODO: Find better Icon
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Organization"],
        sockets: {
          "#worksForInverseSocket":
            vocab.WXSCHEMA.WorksForInverseSocketCompacted,
          "#memberSocket": vocab.WXSCHEMA.MemberSocketCompacted,
          "#associatedArticleSocket":
            vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
        },
      },
    }),
  },
  reactions: {
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: {
      [vocab.WXSCHEMA.WorksForSocketCompacted]: ["persona"],
    },
    [vocab.WXSCHEMA.MemberSocketCompacted]: {
      [vocab.WXSCHEMA.MemberOfSocketCompacted]: ["persona"],
    },
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: {
      [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: ["newsarticle"],
    },
  },
  reactionUseCases: [
    {
      identifier: "persona",
      senderSocketType: vocab.WXSCHEMA.WorksForSocketCompacted,
      targetSocketType: vocab.WXSCHEMA.WorksForInverseSocketCompacted,
    },
    {
      identifier: "persona",
      senderSocketType: vocab.WXSCHEMA.MemberOfSocketCompacted,
      targetSocketType: vocab.WXSCHEMA.MemberSocketCompacted,
    },
    {
      identifier: "newsarticle",
      senderSocketType: vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted,
      targetSocketType: vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
    },
  ],
  enabledUseCases: [
    {
      identifier: "newsarticle",
      senderSocketType: vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted,
      targetSocketType: vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
  },
  seeksDetails: undefined,
};
