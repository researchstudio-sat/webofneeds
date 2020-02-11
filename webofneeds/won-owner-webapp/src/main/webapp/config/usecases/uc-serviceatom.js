import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import won from "../../app/service/won.js";
import { Generator } from "sparqljs";

window.SparqlGenerator4dbg = Generator;

export const serviceAtom = {
  identifier: "serviceAtom",
  label: "Bot",
  icon: "#ico36_robot",
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [won.BOT.ServiceAtomCompacted],
        sockets: {
          "#chatSocket": won.CHAT.ChatSocketCompacted,
          "#holderSocket": won.HOLD.HolderSocketCompacted,
          "#reviewSocket": won.REVIEW.ReviewSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  details: {
    personaName: { ...details.personaName, mandatory: true },
    description: { ...details.description },
    website: { ...details.website },
    termsOfService: { ...details.termsOfService },
  },
  seeksDetails: {},
};
