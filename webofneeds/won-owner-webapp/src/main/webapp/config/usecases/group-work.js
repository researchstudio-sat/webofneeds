import { jobSearch } from "./uc-job-search";
import { jobOffer } from "./uc-job-offer";
import ico36_uc_consortium_search from "../../images/won-icons/ico36_uc_consortium-search.svg";
export const workGroup = {
  identifier: "workGroup",
  label: "Jobs",
  icon: ico36_uc_consortium_search, //TODO proper icon
  subItems: {
    jobSearch: jobSearch,
    jobOffer: jobOffer,
  },
};
