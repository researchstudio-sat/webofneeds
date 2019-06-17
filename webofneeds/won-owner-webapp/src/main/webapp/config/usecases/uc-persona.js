import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import won from "../../app/service/won.js";
import { Generator } from "sparqljs";

window.SparqlGenerator4dbg = Generator;

export const persona = {
  identifier: "persona",
  label: "Persona",
  icon: undefined, //No Icon For Persona UseCase (uses identicon)
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:Persona"],
        sockets: {
          "#reviewSocket": won.REVIEW.ReviewSocketCompacted,
          "#holderSocket": won.HOLD.HolderSocketCompacted,
          "#buddySocket": won.BUDDY.BuddySocketCompacted,
        },
        flags: ["match:NoHintForMe", "match:NoHintForCounterpart"],
      },
      seeks: {},
    }),
  },
  details: {
    personaName: { ...details.personaName, mandatory: true },
    description: { ...details.description },
    website: { ...details.website },
  },
  seeksDetails: {},
};
