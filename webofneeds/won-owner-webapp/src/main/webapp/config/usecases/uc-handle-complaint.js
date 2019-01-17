/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const handleComplaint = {
  identifier: "handleComplaint",
  label: "Handle complaints",
  icon: "#ico36_uc_wtf_interest",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    content: {
      type: "won:HandleComplaint",
      title: "I'll discuss complaints",
      searchString: "wtf",
    },
    seeks: {
      type: "won:Complaint",
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
  },
  seeksDetails: undefined,
};
