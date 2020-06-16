/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_wtf from "../../images/won-icons/ico36_uc_wtf.svg";

export const complain = {
  identifier: "complain",
  label: "Complain about something",
  icon: ico36_uc_wtf,
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
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["handleComplaint"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
  },
  seeksDetails: undefined,
};
