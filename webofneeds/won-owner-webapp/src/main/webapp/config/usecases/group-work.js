import { jobSearch } from "./uc-job-search";
import { jobOffer } from "./uc-job-offer";
import { phdOffer } from "./uc-phd-offer.js";
import { phdSearch } from "./uc-phd-search.js";
import { postdocOffer } from "./uc-postdoc-offer.js";
import { postdocSearch } from "./uc-postdoc-search.js";
import ico36_uc_consortium_search from "../../images/won-icons/ico36_uc_consortium-search.svg";
export const workGroup = {
  identifier: "workGroup",
  label: "Jobs",
  icon: ico36_uc_consortium_search, //TODO proper icon
  subItems: {
    jobSearch: jobSearch,
    jobOffer: jobOffer,
    phdOffer: phdOffer,
    phdSearch: phdSearch,
    postdocOffer: postdocOffer,
    postdocSearch: postdocSearch,
  },
};
