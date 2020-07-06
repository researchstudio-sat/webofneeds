/**
 * Created by kweinberger on 06.12.2018.
 */

import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_book from "../../images/won-icons/ico36_book.svg";

export const booksSearch = {
  identifier: "booksSearch",
  label: "Find a Book",
  icon: ico36_book,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:BookSearch"],
      },
      seeks: {
        type: ["demo:BookOffer"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["booksOffer"],
        refuseOwned: true,
      },
    },
  },
  seeksDetails: {
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
