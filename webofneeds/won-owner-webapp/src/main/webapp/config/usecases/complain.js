/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "detailDefinitions";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const complainUseCases = {
  complain: {
    identifier: "complain",
    label: "Complain about something",
    icon: "#ico36_uc_wtf",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "WTF?",
        tags: ["wtf"],
      },
      seeks: {},
      searchString: "wtf",
    },
    isDetails: {
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
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'll discuss complaints",
      },
      seeks: {},
      searchString: "wtf",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
};
