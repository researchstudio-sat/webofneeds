/**
 * Created by kweinberger on 06.12.2018.
 */

import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import { getIn, is } from "../../app/utils.js";
import {
  textSearchSubQuery,
  vicinityScoreSubQuery,
  filterPriceRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import ico16_uc_goods_service_search from "../../images/won-icons/ico16_uc_goods_service_search.svg";

export const goodsServiceSearch = {
  identifier: "goodsServiceSearch",
  label: "Look for something",
  icon: ico16_uc_goods_service_search,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Demand"],
      },
      seeks: {
        type: ["s:Offer"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["serviceOffer", "goodsOffer", "persona"],
        refuseOwned: true,
      },
    },
  },
  seeksDetails: {
    title: { ...details.title },
    tags: {
      ...details.tags,
      label: "Keywords to search for",
      mandatory: true,
    },
    priceRange: { ...details.pricerange },
    description: { ...details.description },
    location: { ...details.location },
    images: { ...details.images },
  },
  generateQuery: (draft, resultName) => {
    let subQueries = [];
    let subScores = [];

    const tags = getIn(draft, ["seeks", "tag"]);
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
            dc: vocab.defaultContext["dc"],
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
            dc: vocab.defaultContext["dc"],
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
          pathToText: "con:tag",
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
        s: vocab.defaultContext["s"],
        con: vocab.defaultContext["con"],
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
          won: vocab.defaultContext["won"],
          s: vocab.defaultContext["s"],
          rdf: vocab.defaultContext["rdf"],
        },
        operations: [
          `${resultName} rdf:type won:Atom.`,
          `${resultName} rdf:type s:Offer.`,
          `BIND( ( ` +
          subScoreString + // contains all COALESCE statements for text search results
            `COALESCE(?location_geoScore, 0) 
            ) as ?score)`,
        ],
      },
      priceRange &&
        filterPriceRange(
          `${resultName}`,
          priceRange.min,
          priceRange.max,
          priceRange.currency
        ),
    ];

    const concatenatedFilters = concatenateFilters(filters);

    return sparqlQuery({
      prefixes: {
        won: vocab.defaultContext["won"],
        rdf: vocab.defaultContext["rdf"],
        dc: vocab.defaultContext["dc"],
        s: vocab.defaultContext["s"],
      },
      distinct: true,
      variables: [resultName],
      subQueries: subQueries,
      where: concatenatedFilters.operations,
      orderBy: [{ order: "DESC", variable: "?score" }],
    });
  },
};
