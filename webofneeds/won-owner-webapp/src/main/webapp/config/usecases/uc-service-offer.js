/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, mergeInEmptyDraft } from "../detail-definitions.js";

export const serviceOffer = {
  identifier: "serviceOffer",
  label: "Offer a Service",
  icon: "#ico36_plus",
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Offer"],
      },
    }),
  },
  reactionUseCases: ["goodsServiceSearch"],
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
