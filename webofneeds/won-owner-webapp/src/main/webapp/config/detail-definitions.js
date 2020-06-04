import * as personDetails from "./details/person.js";
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

import * as abstractDetails_ from "./details/abstract.js";
import vocab from "../app/service/vocab.js";
export const abstractDetails = abstractDetails_; // reexport
import Immutable from "immutable";

const emptyDraftImm = Immutable.fromJS({
  content: {
    sockets: {
      "#chatSocket": vocab.CHAT.ChatSocketCompacted,
      "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
    },
  },
  seeks: {},
});

/**
 * This is used so we can inject preset values for certain UseCases, be aware that it does not merge the content completely.
 *
 * Sockets will be overwritten if set in the useCase itself FIXME: Figure out a better way to handle or communicate
 * this
 * @param contentToMerge
 * @returns {any|*}
 */
export function mergeInEmptyDraft(contentToMerge) {
  if (!contentToMerge) return emptyDraftImm.toJS();
  const contentToMergeImm = Immutable.fromJS(contentToMerge);
  const mergeSockets = contentToMergeImm.getIn(["content", "sockets"]);

  let mergedDraftImm = emptyDraftImm;

  if (mergeSockets && mergeSockets.size > 0) {
    mergedDraftImm = mergedDraftImm.removeIn(["content", "sockets"]);
  }

  mergedDraftImm = mergedDraftImm.mergeDeep(contentToMergeImm);
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

  person: personDetails.person,

  files: fileDetails.files,
  images: fileDetails.images,

  pricerange: priceDetails.pricerange,
  price: priceDetails.price,
  review: reviewDetails.review,
  responseToUri: basicDetails.responseToUri,
  website: basicDetails.website,
  flags: basicDetails.flags,
  eventObjectAboutUris: basicDetails.eventObjectAboutUris,
  sockets: basicDetails.sockets,
  type: basicDetails.type,
  pokemonGymInfo: pokemonDetails.pokemonGymInfo,
  pokemonRaid: pokemonDetails.pokemonRaid,
  isbn: extendedDetails.isbn,
  author: extendedDetails.author,
};

export const messageDetails = {
  suggestPost: basicDetails.suggestPost,
  bpmnWorkflow: workflowDetails.bpmnWorkflow,
  petriNetWorkflow: workflowDetails.petriNetWorkflow,
  petriNetTransition: workflowDetails.petriNetTransition,
  paypalPayment: paymentDetails.paypalPayment,
};
