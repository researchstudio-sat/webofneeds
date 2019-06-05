import { jobSearch } from "./uc-job-search";
import { jobOffer } from "./uc-job-offer";
import { phdOffer } from "./uc-phd-offer.js";
import { phdSearch } from "./uc-phd-search.js";
import { postdocOffer } from "./uc-postdoc-offer.js";
import { postdocSearch } from "./uc-postdoc-search.js";

export const workGroup = {
  identifier: "workgroup",
  label: "Jobs",
  icon: "#ico36_uc_consortium-search", //TODO proper icon
  subItems: {
    jobSearch: jobSearch,
    jobOffer: jobOffer,
    phdOffer: phdOffer,
    phdSearch: phdSearch,
    postdocOffer: postdocOffer,
    postdocSearch: postdocSearch,
  },
};
