/**
 * Created by fsuda on 18.09.2018.
 */

import { phdOffer } from "./uc-phd-offer.js";
import { phdSearch } from "./uc-phd-search.js";
import { postdocOffer } from "./uc-postdoc-offer.js";
import { postdocSearch } from "./uc-postdoc-search.js";
import { consortiumSearch } from "./uc-consortium-search.js";
import { consortiumOffer } from "./uc-consortium-offer.js";

import ico36_uc_phd from "../../images/won-icons/ico36_uc_phd.svg";

export const academicGroup = {
  identifier: "academicGroup",
  label: "Academia",
  icon: ico36_uc_phd,
  subItems: {
    phdOffer: phdOffer,
    phdSearch: phdSearch,
    postdocOffer: postdocOffer,
    postdocSearch: postdocSearch,
    consortiumOffer: consortiumOffer,
    consortiumSearch: consortiumSearch,
  },
};
