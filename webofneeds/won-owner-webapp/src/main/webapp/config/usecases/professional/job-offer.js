import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../../details/jobs.js";
import { jobLocation } from "../../details/location.js";

export const jobOffer = {
  identifier: "jobOffer",
  label: "Find people for a Job",
  icon: "#ico36_uc_consortium-offer", //TODO proper icon
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    is: {
      type: "s:JobPosting",
      tags: ["offer-job"],
    },
    seeks: {
      type: "s:Person",
    },
    searchString: ["search-job"],
  },
  isDetails: {
    title: { ...details.title },
    description: { ...details.description },
    jobLocation: { ...jobLocation },
    industry: { ...industryDetail },
    employmentTypes: { ...employmentTypesDetail },
    organizationNames: { ...organizationNamesDetail },
  },
  seeksDetails: {
    description: { ...details.description },
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
};
