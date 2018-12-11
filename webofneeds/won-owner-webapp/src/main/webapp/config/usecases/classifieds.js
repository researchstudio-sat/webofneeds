/**
 * Created by kweinberger on 06.12.2018.
 */

import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { getIn, is } from "../../app/utils.js";
import {
  filterInVicinity,
  textSearchSubQuery,
  vicinityScoreSubQuery,
  //concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

const classifiedsUseCases = {
  goodsOffer: {
    identifier: "goodsOffer",
    label: "Offer Something",
    icon: "#ico36_uc_plus",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      content: {
        title: "I'm offering something!",
        type: "s:Offer",
      },
      seeks: {
        type: "s:Demand",
      },
    },
    details: {
      title: { ...details.title },
      description: { ...details.description },
      price: { ...details.price, mandatory: true },
      tags: { ...details.tags },
      location: { ...details.location },
      images: { ...details.images },
    },
    // TODO: what should this match on?
    generateQuery: (draft, resultName) => {
      const location = getIn(draft, ["content", "location"]);

      const filters = [
        {
          // to select seeks-branch
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:seeks ?seeks.`,
            location && "?seeks won:hasLocation/s:location ?location.",
          ],
        },

        filterInVicinity("?location", location, /*radius=*/ 5),
      ];

      // exists only because of compile restrictions
      if (draft && resultName && filters) {
        // TODO: delete this
      }
    },
  },
  goodsDemand: {
    identifier: "goodsDemand",
    label: "Look for something",
    icon: "#ico36_uc_plus",
    draft: {
      ...emptyDraft,
      content: {
        title: "I'm looking for something!",
        type: "s:Demand",
      },
      seeks: {
        type: "s:Offer",
      },
    },
    details: {
      location: { ...details.location }, // can this picker be displayed below other details?
    },
    seeksDetails: {
      tags: {
        ...details.tags,
        label: "Keywords to search for",
        mandatory: true,
      },
      priceRange: { ...details.pricerange },
      description: { ...details.description },
    },
    generateQuery: (draft, resultName) => {
      let subQueries = [];
      // TODO: bindScoreAs should be unique, can't contain spaces -> filter tags and add to name
      const tags = getIn(draft, ["seeks", "tags"]);
      if (tags && is("Array", tags) && tags.length > 0) {
        for (let tag of tags) {
          let keywordTitleSQ = textSearchSubQuery({
            resultName: resultName,
            bindScoreAs: "?keywordsTitle_index",
            pathToText: "dc:title",
            prefixesInPath: {
              dc: won.defaultContext["dc"],
            },
            keyword: tag,
          });

          subQueries.push(keywordTitleSQ);
        }
      }

      const vicinityScoreSQ = vicinityScoreSubQuery({
        resultName: resultName,
        bindScoreAs: "?location_geoScore",
        pathToGeoCoords: "s:location/s:geo",
        prefixesInPath: {
          s: won.defaultContext["s"],
        },
        geoCoordinates: getIn(draft, ["seeks", "location"]),
      });

      subQueries.push(vicinityScoreSQ);

      subQueries = subQueries
        .filter(sq => sq) // filters out "undefined" subqueries
        .map(sq => ({
          query: sq,
          optional: true,
        }));

      const query = sparqlQuery({
        prefixes: {
          won: won.defaultContext["won"],
          rdf: won.defaultContext["rdf"],
          dc: won.defaultContext["dc"],
          s: won.defaultContext["s"],
        },
        distinct: true,
        variables: [resultName],
        subQueries: subQueries,
        where: [
          `${resultName} rdf:type won:Need.`,
          `${resultName} rdf:type s:Offer.`,

          `BIND( ( 
              COALESCE(?keywordsTitle_index, 0) +
              COALESCE(?location_geoScore, 0) 
            ) as ?aggregatedScore)`,
        ],
        orderBy: [{ order: "DESC", variable: "?aggregatedScore" }],
      });

      return query;
    },
  },
  // TODO: go over details for fitting demand counterpart
  // TODO: query
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
    // generateQuery: (draft, resultName) => {
    //   if (draft && resultName) {
    //     // do nothing
    //   }
    // },
  },
  // TODO: servicesDemand: {},
};

export const classifiedsGroup = {
  identifier: "classifiedsgroup",
  label: "Classified Ads",
  icon: undefined,
  useCases: { ...classifiedsUseCases },
};
