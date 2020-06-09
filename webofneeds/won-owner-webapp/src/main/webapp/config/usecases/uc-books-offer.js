/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_book from "../../images/won-icons/ico36_book.svg";

export const booksOffer = {
  identifier: "booksOffer",
  label: "Offer a Book",
  icon: ico36_book,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:BookOffer"],
      },
      seeks: {
        type: ["demo:BookSearch"],
      },
    }),
  },
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: ["booksSearch"],
    },
  },
  details: {
    title: { ...details.title, mandatory: true },
    author: { ...details.author },
    isbn: { ...details.isbn },
    description: { ...details.description },
    price: { ...details.price },
    tags: { ...details.tags },
    location: { ...details.location },
    images: { ...details.images },
    website: { ...details.website },
  },
};
