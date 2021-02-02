import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
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
          "#holderSocket": vocab.HOLD.HolderSocketCompacted,
          "#buddySocket": vocab.BUDDY.BuddySocketCompacted,
          "#worksForSocket": vocab.WXSCHEMA.WorksForSocketCompacted,
          "#memberOfSocket": vocab.WXSCHEMA.MemberOfSocketCompacted,
          "#interestSocket": vocab.WXPERSONA.InterestSocketCompacted,
          "#expertiseSocket": vocab.WXPERSONA.ExpertiseSocketCompacted,
          // "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted, //TODO: exclude the ability to review a persona for now
          "#sEventSocket": vocab.WXSCHEMA.EventSocketCompacted,
          "#sAttendeeInverseSocket":
            vocab.WXSCHEMA.AttendeeInverseSocketCompacted,
          // "#PrimaryAccountableOfSocket":
          //   vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted,
          // "#CustodianOfSocket": vocab.WXVALUEFLOWS.CustodianOfSocketCompacted,
          // "#ActorActivitySocket":
          //   vocab.WXVALUEFLOWS.ActorActivitySocketCompacted, //TODO VALUEFLOWS SOCKETS CURRENTLY EXCLUDED
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    // [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: {
    //   [vocab.WXVALUEFLOWS.ActorSocketCompacted]: {
    //     useCaseIdentifiers: ["activity"],
    //   },
    // },
    // [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]: {
    //   [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: {
    //     useCaseIdentifiers: ["resource"],
    //   },
    // },
    // [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: {
    //   [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: {
    //     useCaseIdentifiers: ["resource"],
    //   },
    // },
    // [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: {
    //   [vocab.WXVALUEFLOWS.ActorSocket]: {
    //     useCaseIdentifiers: ["action"],
    //   },
    // },
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
    [vocab.HOLD.HolderSocketCompacted]: {
      [vocab.HOLD.HoldableSocketCompacted]: {
        useCaseIdentifiers: ["*"],
        refuseNonOwned: true,
      },
    },
    [vocab.WXPERSONA.InterestSocketCompacted]: {
      [vocab.WXPERSONA.InterestOfSocketCompacted]: {
        useCaseIdentifiers: [
          "afterpartyInterest",
          "breakfastInterest",
          "cyclingInterest",
          "genericInterest",
          "lunchInterest",
          "pokemonInterest",
          "sightseeingInterest",
        ],
        refuseNonOwned: true,
      },
    },
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: {
      [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: {
        useCaseIdentifiers: ["*"],
        refuseNonOwned: true,
      },
    },
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
        refuseOwned: true,
      },
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["*"],
      },
    },
    [vocab.WXSCHEMA.AttendeeInverseSocketCompacted]: {
      [vocab.WXSCHEMA.AttendeeSocketCompacted]: {
        useCaseIdentifiers: ["event"],
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
