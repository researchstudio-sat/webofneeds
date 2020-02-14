/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import won from "../../app/service/won.js";

export const booksSearch = {
  identifier: "booksSearch",
  label: "Find a Book",
  icon: "#ico36_book",
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
  reactionUseCases: [
    {
      identifier: "booksOffer",
      senderSocketType: won.CHAT.ChatSocketCompacted,
      targetSocketType: won.CHAT.ChatSocketCompacted,
    },
  ],
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
