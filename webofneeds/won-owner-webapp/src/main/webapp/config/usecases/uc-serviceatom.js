import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import { Generator } from "sparqljs";

window.SparqlGenerator4dbg = Generator;

export const serviceAtom = {
  identifier: "serviceAtom",
  label: "Bot",
  icon: "#ico36_robot",
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.BOT.ServiceAtomCompacted],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#holderSocket": vocab.HOLD.HolderSocketCompacted,
          "#reviewSocket": vocab.REVIEW.ReviewSocketCompacted,
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
