import * as personDetails from "./details/person.js";
import * as locationDetails from "./details/location.js";
import * as timeDetails from "./details/datetime.js";
import * as fileDetails from "./details/files.js";
import * as priceDetails from "./details/price.js";
import * as basicDetails from "./details/basic.js";
import * as workflowDetails from "./details/workflow.js";
import * as reviewDetails from "./details/review.js";

import * as abstractDetails_ from "./details/abstract.js";
export const abstractDetails = abstractDetails_; // reexport
import Immutable from "immutable";

const emptyDraftImm = Immutable.fromJS({
  content: {
    facets: [
      { "@id": "#chatFacet", "@type": "won:ChatFacet" },
      { "@id": "#holdableFacet", "@type": "won:HoldableFacet" },
    ],
    defaultFacet: { "@id": "#chatFacet", "@type": "won:ChatFacet" },
  },
  seeks: {},
  matchingContext: undefined,
});

export function mergeInEmptyDraft(contentToMerge) {
  if (!contentToMerge) return mergedDraftImm.toJS();
  const contentToMergeImm = Immutable.fromJS(contentToMerge);

  const mergedDraftImm = emptyDraftImm.mergeDeep(contentToMergeImm);
  return mergedDraftImm.toJS();
}

export const details = {
  title: basicDetails.title,
  personaName: basicDetails.personaName,
  description: basicDetails.description,
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
  sPlanAction: basicDetails.sPlanAction,
  facets: basicDetails.facets,
  defaultFacet: basicDetails.defaultFacet,
};

export const messageDetails = {
  suggestPost: basicDetails.suggestPost,
  bpmnWorkflow: workflowDetails.bpmnWorkflow,
  petriNetWorkflow: workflowDetails.petriNetWorkflow,
  petriNetTransition: workflowDetails.petriNetTransition,
};
