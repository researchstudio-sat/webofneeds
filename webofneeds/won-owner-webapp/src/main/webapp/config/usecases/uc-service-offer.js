/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_plus from "../../images/won-icons/ico36_plus.svg";

export const serviceOffer = {
  identifier: "serviceOffer",
  label: "Offer a Service",
  icon: ico36_plus,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Offer"],
      },
    }),
  },
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: ["goodsServiceSearch"],
    },
  },
  reactionUseCases: [
    {
      identifier: "goodsServiceSearch",
      senderSocketType: vocab.CHAT.ChatSocketCompacted,
      targetSocketType: vocab.CHAT.ChatSocketCompacted,
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
