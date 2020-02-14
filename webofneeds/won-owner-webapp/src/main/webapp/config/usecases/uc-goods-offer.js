/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import won from "../../app/service/won.js";

export const goodsOffer = {
  identifier: "goodsOffer",
  label: "Offer Something",
  icon: "#ico36_plus",
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Offer"],
      },
      seeks: {
        type: ["s:Demand"],
      },
    }),
  },
  reactionUseCases: [
    {
      identifier: "goodsServiceSearch",
      senderSocketType: won.CHAT.ChatSocketCompacted,
      targetSocketType: won.CHAT.ChatSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title, mandatory: true },
    description: { ...details.description },
    price: { ...details.price, mandatory: true },
    tags: { ...details.tags },
    location: { ...details.location },
    images: { ...details.images },
  },
  // TODO: what should this match on?
  // generateQuery: (draft, resultName) => {
  //   // return query
  // },
};
