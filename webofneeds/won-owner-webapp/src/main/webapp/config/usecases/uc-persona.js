import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";

export const persona = {
  identifier: "persona",
  label: "Persona",
  icon: undefined, //No Icon For Persona UseCase (uses identicon)
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.WON.PersonaCompacted],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#reviewSocket": vocab.REVIEW.ReviewSocketCompacted,
          "#holderSocket": vocab.HOLD.HolderSocketCompacted,
          "#buddySocket": vocab.BUDDY.BuddySocketCompacted,
          "#worksForSocket": vocab.WXSCHEMA.WorksForSocketCompacted,
          "#memberOfSocket": vocab.WXSCHEMA.MemberOfSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    [vocab.WXSCHEMA.WorksForSocketCompacted]: {
      [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
      },
    },
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: {
      [vocab.WXSCHEMA.MemberSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
      },
    },
    [vocab.BUDDY.BuddySocketCompacted]: {
      [vocab.BUDDY.BuddySocketCompacted]: {
        useCaseIdentifiers: ["persona", "persona"],
        refuseOwned: true,
      },
    },
    [vocab.HOLD.HolderSocketCompacted]: {
      [vocab.HOLD.HoldableSocketCompacted]: {
        useCaseIdentifiers: ["*"],
        refuseNonOwned: true,
      },
    },
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["persona", "persona"],
        refuseOwned: true,
      },
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["*"],
      },
    },
  },
  details: {
    personaName: { ...details.personaName, mandatory: true },
    description: { ...details.description },
    website: { ...details.website },
    images: { ...details.images },
  },
  seeksDetails: {},
};
