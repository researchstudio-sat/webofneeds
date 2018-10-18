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

export const emptyDraft = {
  is: {},
  seeks: {},
  matchingContext: undefined,
};

export const details = {
  title: basicDetails.title,
  description: basicDetails.description,
  tags: basicDetails.tags,

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
};

export const messageDetails = {
  suggestPost: basicDetails.suggestPost,
  bpmnWorkflow: workflowDetails.bpmnWorkflow,
  petriNetWorkflow: workflowDetails.petriNetWorkflow,
  petriNetTransition: workflowDetails.petriNetTransition,
};
