/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const complainGroup = {
  identifier: "complaingroup",
  label: "Complaints",
  icon: undefined,
  useCases: {
    complain: {
      identifier: "complain",
      label: "Complain about something",
      icon: "#ico36_uc_wtf",
      timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "WTF?",
          tags: ["wtf"],
          searchString: "wtf",
        },
        seeks: {},
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        location: { ...details.location },
        tags: { ...details.tags },
      },
      seeksDetails: undefined,
    },
    handleComplaints: {
      identifier: "handleComplaints",
      label: "Handle complaints",
      icon: "#ico36_uc_wtf_interest",
      timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'll discuss complaints",
          searchString: "wtf",
        },
        seeks: {},
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        location: { ...details.location },
        tags: { ...details.tags },
      },
      seeksDetails: undefined,
    },
  },
};
