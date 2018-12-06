/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
//import won from "../../app/won-es6.js";
//import { getIn, isValidDate } from "../../app/utils.js";

const classifiedsUseCases = {
  goodsOffer: {
    identifier: "goodsOffer",
    label: "Offer Something",
    icon: "#ico36_uc_plus",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      content: {
        title: "Offer Something",
        type: "s:Offer",
      },
    },
    details: {
      title: { ...details.title },
      description: { ...details.description },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
      location: { ...details.location },
      price: { ...details.price },
      images: { ...details.images },
    },
    generateQuery: (draft, resultName) => {
      if (draft && resultName) {
        // do nothing
      }
    },
  },
  servicesOffer: {
    identifier: "servicesOffer",
    label: "Offer a Service",
    icon: "#ico36_uc_plus",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      content: {
        title: "Offer a Service",
        type: "s:Offer",
      },
    },
    details: {
      title: { ...details.title },
      description: { ...details.description },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
      location: { ...details.location },
      price: { ...details.price },
    },
    generateQuery: (draft, resultName) => {
      if (draft && resultName) {
        // do nothing
      }
    },
  },
};

export const classifiedsGroup = {
  identifier: "classifiedsgroup",
  label: "Classified Ads",
  icon: undefined,
  useCases: { ...classifiedsUseCases },
};
