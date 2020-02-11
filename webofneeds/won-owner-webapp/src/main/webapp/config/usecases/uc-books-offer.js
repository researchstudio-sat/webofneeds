/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, mergeInEmptyDraft } from "../detail-definitions.js";

export const booksOffer = {
  identifier: "booksOffer",
  label: "Offer a Book",
  icon: "#ico36_book",
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
  reactionUseCases: ["booksSearch"],
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
