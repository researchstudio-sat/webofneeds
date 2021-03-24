import * as locationDetails from "./details/location.js";
import * as timeDetails from "./details/datetime.js";
import * as fileDetails from "./details/files.js";
import * as priceDetails from "./details/price.js";
import * as basicDetails from "./details/basic.js";
import * as workflowDetails from "./details/workflow.js";
import * as reviewDetails from "./details/review.js";
import * as paymentDetails from "./details/payment.js";
import * as pokemonDetails from "./details/pokemon.js";
import * as extendedDetails from "./details/extended.js";
import * as actions from "./details/action.js";

import * as abstractDetails_ from "./details/abstract.js";
import vocab from "../app/service/vocab.js";
import Immutable from "immutable";

export const abstractDetails = abstractDetails_; // reexport

// Grant access to connect to all sockets 009
export const connectToSocketsAuthorization = {
  [vocab.AUTH.granteeCompacted]: { "@id": vocab.AUTH.anyoneCompacted },
  [vocab.AUTH.grantCompacted]: {
    [vocab.AUTH.graphCompacted]: {
      [vocab.AUTH.operationCompacted]: { "@id": vocab.AUTH.opReadCompacted },
    },
    [vocab.AUTH.socketCompacted]: {
      [vocab.AUTH.operationCompacted]: [
        { "@id": vocab.AUTH.opConnectCloseCompacted },
      ],
      [vocab.AUTH.connectionsCompacted]: {
        [vocab.AUTH.inheritCompacted]: false,
      },
    },
  },
};

// See holderSocket connections of atoms buddies
export const seeHolderSocketConnectionsAuthorization = {
  [vocab.AUTH.granteeCompacted]: {
    [vocab.AUTH.socketCompacted]: {
      [vocab.AUTH.socketTypeCompacted]: {
        "@id": vocab.BUDDY.BuddySocketCompacted,
      },
      [vocab.AUTH.connectionCompacted]: {
        [vocab.AUTH.targetAtomCompacted]: {},
        [vocab.AUTH.connectionStateCompacted]: {
          "@id": vocab.WON.ConnectedCompacted,
        },
      },
    },
  },
  [vocab.AUTH.grantCompacted]: {
    [vocab.AUTH.graphCompacted]: {
      [vocab.AUTH.operationCompacted]: { "@id": vocab.AUTH.opReadCompacted },
    },
    [vocab.AUTH.socketCompacted]: {
      [vocab.AUTH.socketTypeCompacted]: {
        "@id": vocab.HOLD.HolderSocketCompacted,
      },
      [vocab.AUTH.connectionsCompacted]: {
        [vocab.AUTH.connectionStateCompacted]: {
          "@id": vocab.WON.ConnectedCompacted,
        },
        [vocab.AUTH.operationCompacted]: [
          { "@id": vocab.AUTH.opReadCompacted },
        ],
        [vocab.AUTH.connectionMessagesCompacted]: {
          [vocab.AUTH.inheritCompacted]: false,
        },
      },
    },
  },
};

// Atoms that are connected can see their connections to communicate
export const connectedConnectionsAuthorization = {
  [vocab.AUTH.granteeCompacted]: {
    [vocab.AUTH.socketCompacted]: {
      [vocab.AUTH.connectionCompacted]: {
        [vocab.AUTH.targetAtomCompacted]: {},
        [vocab.AUTH.connectionStateCompacted]: {
          "@id": vocab.WON.ConnectedCompacted,
        },
      },
    },
  },
  [vocab.AUTH.grantCompacted]: {
    [vocab.AUTH.connectionCompacted]: {
      [vocab.AUTH.targetAtomCompacted]: {
        [vocab.AUTH.atomCompacted]: {
          "@id": vocab.AUTH.operationRequestorCompacted,
        },
      },
      [vocab.AUTH.operationCompacted]: [
        { "@id": vocab.AUTH.opReadCompacted },
        { "@id": vocab.AUTH.opConnectCloseCompacted },
        { "@id": vocab.AUTH.opCommunicateCompacted },
      ],
      [vocab.AUTH.connectionMessagesCompacted]: {
        [vocab.AUTH.inheritCompacted]: false,
      },
    },
  },
};

// Only members see members of organizsation 008
export const onlyMembersSeeMembersAuthorization = {
  [vocab.AUTH.granteeCompacted]: {
    [vocab.AUTH.socketCompacted]: {
      [vocab.AUTH.socketTypeCompacted]: {
        "@id": vocab.WXSCHEMA.MemberSocketCompacted,
      },
      [vocab.AUTH.connectionCompacted]: {
        [vocab.AUTH.targetAtomCompacted]: {},
        [vocab.AUTH.connectionStateCompacted]: {
          "@id": vocab.WON.ConnectedCompacted,
        },
      },
    },
  },
  [vocab.AUTH.grantCompacted]: {
    [vocab.AUTH.graphCompacted]: {
      [vocab.AUTH.operationCompacted]: { "@id": vocab.AUTH.opReadCompacted },
    },
    [vocab.AUTH.socketCompacted]: {
      [vocab.AUTH.socketTypeCompacted]: {
        "@id": vocab.WXSCHEMA.MemberSocketCompacted,
      },
      [vocab.AUTH.connectionsCompacted]: {
        [vocab.AUTH.connectionStateCompacted]: {
          "@id": vocab.WON.ConnectedCompacted,
        },
        [vocab.AUTH.operationCompacted]: [
          { "@id": vocab.AUTH.opReadCompacted },
        ],
        [vocab.AUTH.connectionMessagesCompacted]: {
          [vocab.AUTH.inheritCompacted]: false,
        },
      },
    },
  },
};

// Atom can be seen by any other atom
export const defaultPublicAtomAuthorization = {
  [vocab.AUTH.granteeCompacted]: { "@id": vocab.AUTH.anyoneCompacted },
  [vocab.AUTH.grantCompacted]: {
    [vocab.AUTH.graphCompacted]: {
      [vocab.AUTH.operationCompacted]: [{ "@id": vocab.AUTH.opReadCompacted }],
    },
  },
};

export const emptyDraftImm = Immutable.fromJS({
  content: {
    sockets: {
      "#chatSocket": vocab.CHAT.ChatSocketCompacted,
      "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
      "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
    },
  },
  seeks: {},
  acl: [
    defaultPublicAtomAuthorization,
    connectedConnectionsAuthorization,
    connectToSocketsAuthorization,
  ],
});

export const defaultReactions = {
  [vocab.WXSCHEMA.ReviewSocketCompacted]: {
    [vocab.WXSCHEMA.ReviewInverseSocketCompacted]: {
      useCaseIdentifiers: ["review"],
      refuseNonOwned: true,
      labels: {
        owned: {
          default: "Review",
          addNew: "New Review",
          picker: "Pick a Review",
        },
        nonOwned: {
          default: "Review",
          addNew: "New Review",
          picker: "Pick a Review",
        },
      },
    },
  },
  [vocab.BUDDY.BuddySocketCompacted]: {
    [vocab.BUDDY.BuddySocketCompacted]: {
      useCaseIdentifiers: ["persona"],
      refuseOwned: true,
      labels: {
        owned: {
          default: "Buddy",
          addNew: "Add New Persona as Buddy",
          picker: "Pick a Persona to add as Buddy",
        },
        nonOwned: {
          default: "Buddy",
          addNew: "New Persona to send Request",
          picker: "Pick a Persona to add as Buddy",
        },
      },
    },
  },
  [vocab.CHAT.ChatSocketCompacted]: {
    [vocab.CHAT.ChatSocketCompacted]: {
      useCaseIdentifiers: ["*"],
      refuseOwned: true,
    },
  },
  [vocab.HOLD.HoldableSocketCompacted]: {
    [vocab.HOLD.HolderSocketCompacted]: {
      useCaseIdentifiers: ["persona"],
      refuseNonOwned: true,
    },
  },
  [vocab.WXPERSONA.InterestOfSocketCompacted]: {
    [vocab.WXPERSONA.InterestSocketCompacted]: {
      useCaseIdentifiers: ["persona"],
      refuseNonOwned: true,
    },
  },
  [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: {
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: {
      useCaseIdentifiers: ["persona"],
      refuseNonOwned: true,
    },
  },
  [vocab.WXSCHEMA.EventSocketCompacted]: {
    [vocab.WXSCHEMA.EventInverseSocketCompacted]: {
      useCaseIdentifiers: ["event"],
      refuseNonOwned: true,
      labels: {
        owned: {
          default: "Event",
          addNew: "Organize New Event",
          picker: "Pick an Event",
        },
        nonOwned: {
          default: "Event",
          addNew: "Organize New Event",
          picker: "Pick an Event",
        },
      },
    },
  },
  [vocab.WXSCHEMA.AttendeeSocketCompacted]: {
    [vocab.WXSCHEMA.AttendeeInverseSocketCompacted]: {
      useCaseIdentifiers: ["persona"],
    },
  },
};

/**
 * This is used so we can inject preset values for certain UseCases, be aware that it does not merge the content completely.
 *
 * Sockets will be overwritten if set in the useCase itself FIXME: Figure out a better way to handle or communicate
 * this
 * @param contentToMerge
 * @returns {any|*}
 */
export function mergeInEmptyDraft(draftToMerge) {
  if (!draftToMerge) return emptyDraftImm.toJS();
  const draftToMergeImm = Immutable.fromJS(draftToMerge);
  const mergeSockets = draftToMergeImm.getIn(["content", "sockets"]);
  let mergedDraftImm = emptyDraftImm;

  if (mergeSockets && mergeSockets.size > 0) {
    mergedDraftImm = mergedDraftImm.removeIn(["content", "sockets"]);
  }
  const mergeAcl = draftToMergeImm.getIn(["acl"]);
  if (mergeAcl && mergeAcl.size > 0) {
    mergedDraftImm = mergedDraftImm.removeIn(["acl"]);
  }

  mergedDraftImm = mergedDraftImm.mergeDeep(draftToMergeImm);

  return mergedDraftImm.toJS();
}

export const details = {
  title: basicDetails.title,
  personaName: basicDetails.personaName,
  description: basicDetails.description,
  termsOfService: basicDetails.termsOfService,
  tags: basicDetails.tags,
  searchString: basicDetails.searchString,

  fromDatetime: timeDetails.fromDatetime,
  throughDatetime: timeDetails.throughDatetime,
  datetimeRange: timeDetails.datetimeRange,

  location: locationDetails.location,
  travelAction: locationDetails.travelAction,

  files: fileDetails.files,
  images: fileDetails.images,

  pricerange: priceDetails.pricerange,
  price: priceDetails.price,
  reviewRating: reviewDetails.reviewRating,
  responseToUri: basicDetails.responseToUri,
  website: basicDetails.website,
  flags: basicDetails.flags,
  eventObjectAboutUris: basicDetails.eventObjectAboutUris,
  sockets: basicDetails.sockets,
  type: basicDetails.type,
  pokemonGymInfo: pokemonDetails.pokemonGymInfo,
  pokemonRaid: pokemonDetails.pokemonRaid,
  isbn: extendedDetails.isbn,
  classifiedAs: extendedDetails.classifiedAs,
  imageUrl: basicDetails.imageUrl,
  author: extendedDetails.author,
};

export const messageDetails = {
  suggestPost: basicDetails.suggestPost,
  bpmnWorkflow: workflowDetails.bpmnWorkflow,
  petriNetWorkflow: workflowDetails.petriNetWorkflow,
  petriNetTransition: workflowDetails.petriNetTransition,
  paypalPayment: paymentDetails.paypalPayment,

  customAction: actions.customAction,
  raiseAction: actions.raiseAction,
};
