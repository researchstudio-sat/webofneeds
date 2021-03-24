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
  [vocab.AUTH.grantee]: { "@id": vocab.AUTH.anyone },
  [vocab.AUTH.grant]: {
    [vocab.AUTH.graph]: {
      [vocab.AUTH.operation]: { "@id": vocab.AUTH.opRead },
    },
    [vocab.AUTH.socket]: {
      [vocab.AUTH.operation]: [{ "@id": vocab.AUTH.opConnectClose }],
      [vocab.AUTH.connections]: {
        [vocab.AUTH.inherit]: false,
      },
    },
  },
};

// Atoms that are connected can see their connections to communicate
export const connectedConnectionsAuthorization = {
  [vocab.AUTH.grantee]: {
    [vocab.AUTH.socket]: {
      [vocab.AUTH.connection]: {
        [vocab.AUTH.targetAtom]: {},
        [vocab.AUTH.connectionState]: {
          "@id": vocab.WON.Connected,
        },
      },
    },
  },
  [vocab.AUTH.grant]: {
    [vocab.AUTH.connection]: {
      [vocab.AUTH.targetAtom]: {
        [vocab.AUTH.atom]: {
          "@id": vocab.AUTH.operationRequestor,
        },
      },
      [vocab.AUTH.operation]: [
        { "@id": vocab.AUTH.opRead },
        { "@id": vocab.AUTH.opConnectClose },
        { "@id": vocab.AUTH.opCommunicate },
      ],
      [vocab.AUTH.connectionMessages]: {
        [vocab.AUTH.inherit]: false,
      },
    },
  },
};

// Atom can be seen by any other atom
export const defaultPublicAtomAuthorization = {
  [vocab.AUTH.grantee]: { "@id": vocab.AUTH.anyone },
  [vocab.AUTH.grant]: {
    [vocab.AUTH.graph]: {
      [vocab.AUTH.operation]: [{ "@id": vocab.AUTH.opRead }],
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
