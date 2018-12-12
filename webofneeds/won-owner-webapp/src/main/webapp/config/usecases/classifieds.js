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
  filterRentRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

/*
TODO: send hints to counterparts of searches
TODO: queries for offers
*/

const classifiedsUseCases = {
  goodsOffer: {
    identifier: "goodsOffer",
    label: "Offer Something",
    icon: "#ico36_uc_plus",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      content: {
        type: "s:Offer",
      },
      seeks: {
        type: "s:Demand",
      },
    },
    details: {
      title: { ...details.title, mandatory: true },
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
    seeksDetails: {
      tags: {
        ...details.tags,
        label: "Keywords to search for",
        mandatory: true,
      },
      priceRange: { ...details.pricerange },
      description: { ...details.description },
      location: { ...details.location },
    },
    generateQuery: (draft, resultName) => {
      let subQueries = [];
      let subScores = [];

      const tags = getIn(draft, ["seeks", "tags"]);
      let keywords = [];

      // prep tags for searching
      if (tags && is("Array", tags) && tags.length > 0) {
        keywords = tags
          .map(s => s.split(/\s+/)) // remove all whitespace
          .reduce((arrArr, arr) => arrArr.concat(arr), []); // flatten array, Array.flat() does not work on edge

        // search for all keywords in title, description and tags
        // this is probably horribly inefficient
        for (const keyword of keywords) {
          let titleSQ = textSearchSubQuery({
            resultName: resultName,
            bindScoreAs: "?title_" + keyword + "_index",
            pathToText: "dc:title",
            prefixesInPath: {
              dc: won.defaultContext["dc"],
            },
            keyword: keyword,
          });

          if (titleSQ) {
            subQueries.push(titleSQ);
            subScores.push("?title_" + keyword + "_index"); // dirty hack.
          }

          let descriptionSQ = textSearchSubQuery({
            resultName: resultName,
            bindScoreAs: "?description_" + keyword + "_index",
            pathToText: "dc:description",
            prefixesInPath: {
              dc: won.defaultContext["dc"],
            },
            keyword: keyword,
          });

          if (descriptionSQ) {
            subQueries.push(descriptionSQ);
            subScores.push("?description_" + keyword + "_index");
          }

          let tagsSQ = textSearchSubQuery({
            resultName: resultName,
            bindScoreAs: "?tags_" + keyword + "_index",
            pathToText: "won:hasTags",
            prefixesInPath: {},
            keyword: keyword,
          });

          if (tagsSQ) {
            subQueries.push(tagsSQ);
            subScores.push("?tags_" + keyword + "_index");
          }
        }
      }

      // dirty hack, part 2
      // this is done to be able to use the indices generated above
      // in the sparql query below
      const iterator = subScores.values();
      let subScoreString = ``;

      for (const value of iterator) {
        subScoreString += `COALESCE(` + value + `, 0) + `;
      }

      // prioritise close results
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

      // filter for price range
      const priceRange = getIn(draft, ["seeks", "pricerange"]);

      const filters = [
        {
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} rdf:type won:Need.`,
            `${resultName} rdf:type s:Offer.`,
            `BIND( ( ` +
            subScoreString + // contains all COALESCE statements for text search results
              `COALESCE(?location_geoScore, 0) 
            ) as ?score)`, // TODO: map this to a value from 0 to 1
          ],
        },
        priceRange &&
          // no idea why this method isn't called filterPriceRange
          filterRentRange(
            `${resultName}`,
            priceRange.min,
            priceRange.max,
            priceRange.currency
          ),
      ];

      const concatenatedFilters = concatenateFilters(filters);

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
        where: concatenatedFilters.operations,
        orderBy: [{ order: "DESC", variable: "?score" }],
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
