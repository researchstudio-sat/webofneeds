/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import won from "../../app/service/won.js";

export const complain = {
  identifier: "complain",
  label: "Complain about something",
  icon: "#ico36_uc_wtf",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
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
  reactionUseCases: [
    {
      identifier: "handleComplaint",
      senderSocketType: won.CHAT.ChatSocketCompacted,
      targetSocketType: won.CHAT.ChatSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
  },
  seeksDetails: undefined,
};
