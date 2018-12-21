import { jobSearch } from "./uc-job-search";
import { jobOffer } from "./uc-job-offer";
import { phdOffer } from "./uc-phd-offer.js";
import { phdSearch } from "./uc-phd-search.js";
import { postdocOffer } from "./uc-postdoc-offer.js";
import { postdocSearch } from "./uc-postdoc-search.js";

export const workGroup = {
  identifier: "workgroup",
  label: "Find or Offer Work",
  icon: undefined,
  useCases: {
    jobSearch: jobSearch,
    jobOffer: jobOffer,
    phdOffer: phdOffer,
    phdSearch: phdSearch,
    postdocOffer: postdocOffer,
    postdocSearch: postdocSearch,
  },
};
