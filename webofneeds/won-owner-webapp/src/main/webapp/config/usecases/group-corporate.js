/**
 * Created by ms on 10.03.2021
 */

import ico16_uc_organization from "../../images/won-icons/ico16_uc_organization.svg";
import { organization } from "./uc-organization";
import { project } from "./uc-project";
import { newsarticle } from "./uc-newsarticle";
import { event } from "./uc-event";
import { jobSearch } from "./uc-job-search";
import { jobOffer } from "./uc-job-offer";
import { groupChat } from "./uc-group-chat.js";

export const corporateGroup = {
  identifier: "corporateGroup",
  label: "Corporate",
  icon: ico16_uc_organization,
  subItems: {
    organization: organization,
    project: project,
    newsarticle: newsarticle,
    event: event,
    jobSearch: jobSearch,
    jobOffer: jobOffer,
    groupChat: groupChat,
  },
};
