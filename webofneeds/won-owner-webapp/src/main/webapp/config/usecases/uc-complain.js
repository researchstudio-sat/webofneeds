/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import * as wonUtils from "../../app/won-utils.js";

export const complain = {
  identifier: "complain",
  label: "Complain about something",
  icon: "#ico36_uc_wtf",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
  doNotMatchAfter: wonUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:Complaint"],
        title: "WTF?",
        tags: ["wtf"],
      },
      seeks: {
        type: ["demo:HandleComplaint"],
      },
    }),
  },
  reactionUseCases: ["handleComplaint"],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
  },
  seeksDetails: undefined,
};
