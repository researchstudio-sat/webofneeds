/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, emptyDraft } from "../detail-definitions.js";

export const goodsOffer = {
  identifier: "goodsOffer",
  label: "Offer Something",
  icon: "#ico36_plus",
  draft: {
    ...emptyDraft,
    content: {
      type: "s:Offer",
    },
    seeks: {
      type: "s:Demand",
    },
  },
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
