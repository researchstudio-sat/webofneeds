import personDetails from "./details/person.js";
import locationDetails from "./details/location.js";
import timeDetails from "./details/datetime.js";
import fileDetails from "./details/files.js";
import priceDetails from "./details/price.js";
import basicDetails from "./details/basic.js";

import abstractDetails_ from "./details/abstract.js";
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
  bpmnWorkflow: fileDetails.bpmnWorkflow,
  petrinetWorkflow: fileDetails.petrinetWorkflow,

  pricerange: priceDetails.pricerange,
  price: priceDetails.price,
};
