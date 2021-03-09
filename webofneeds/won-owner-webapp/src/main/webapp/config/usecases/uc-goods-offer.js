/**
 * Created by kweinberger on 06.12.2018.
 */

import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico16_uc_goods_offer from "../../images/won-icons/ico16_uc_goods_offer.svg";

export const goodsOffer = {
  identifier: "goodsOffer",
  label: "Offer Something",
  icon: ico16_uc_goods_offer,
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
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["goodsServiceSearch", "persona"],
        refuseOwned: true,
      },
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
