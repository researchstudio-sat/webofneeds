/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const complain = {
  identifier: "complain",
  label: "Complain about something",
  icon: "#ico36_uc_wtf",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:Complaint"],
        title: "WTF?",
        tags: ["wtf"],
      },
      seeks: {
        type: ["won:HandleComplaint"],
      },
    }),
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
  },
  seeksDetails: undefined,
};
