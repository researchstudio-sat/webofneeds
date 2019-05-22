import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { getIn } from "../../app/utils.js";
import { sparqlQuery } from "../../app/sparql-builder-utils.js";

export const goodsTransportOffer = {
  identifier: "goodsTransportOffer",
  label: "Offer goods transport",
  icon: "#ico36_uc_transport_offer",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "Transportation offer",
        type: ["http://dbpedia.org/resource/Transport"],
      },
    }),
  },
  enabledUseCases: undefined,
  reactionUseCases: ["goodsTransportSearch"],
  details: {
    title: { ...details.title },
    location: { ...details.location },
  },
  seeksDetails: {
    tags: { ...details.tags },
    description: { ...details.description },
  },
  generateQuery: (draft, resultName) => {
    const location = getIn(draft, ["content", "location"]);
    let filter;

    if (location && location.lat && location.lng) {
      filter = {
        // to select seeks-branch
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a <http://dbpedia.org/resource/Cargo>.`,
          `${resultName} match:seeks ?seeks.`,
          "?seeks con:travelAction/s:fromLocation ?fromLocation.",
          "?seeks con:travelAction/s:toLocation ?toLocation.",
          "?fromLocation s:geo ?fromLocation_geo.",
          "?fromLocation_geo s:latitude ?fromLocation_lat;",
          "s:longitude ?fromLocation_lon;",
          `bind (abs(xsd:decimal(?fromLocation_lat) - ${
            location.lat
          }) as ?fromLatDiffRaw)`,
          `bind (abs(xsd:decimal(?fromLocation_lon) - ${
            location.lng
          }) as ?fromLonDiff)`,
          "bind (if ( ?fromLatDiffRaw > 180, 360 - ?fromLatDiffRaw, ?fromLatDiffRaw ) as ?fromLatDiff)",
          "bind ( ?fromLatDiff * ?fromLatDiff + ?fromLonDiff * ?fromLonDiff as ?fromLocation_geoDistanceScore)",
          "?toLocation s:geo ?toLocation_geo.",
          "?toLocation_geo s:latitude ?toLocation_lat;",
          "s:longitude ?toLocation_lon;",
          `bind (abs(xsd:decimal(?toLocation_lat) - ${
            location.lat
          }) as ?toLatDiffRaw)`,
          `bind (abs(xsd:decimal(?toLocation_lon) - ${
            location.lng
          }) as ?toLonDiff)`,
          "bind (if ( ?toLatDiffRaw > 180, 360 - ?toLatDiffRaw, ?toLatDiffRaw ) as ?toLatDiff)",
          "bind ( ?toLatDiff * ?toLatDiff + ?toLonDiff * ?toLonDiff as ?toLocation_geoDistanceScore)",
          "bind (?toLocation_geoDistanceScore + ?fromLocation_geoDistanceScore as ?distScore)",
        ],
      };
    } else {
      filter = {
        // to select seeks-branch
        prefixes: {
          won: won.defaultContext["won"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a <http://dbpedia.org/resource/Cargo>.`,
        ],
      };
    }

    const generatedQuery = sparqlQuery({
      prefixes: filter.prefixes,
      distinct: true,
      variables: [resultName],
      where: filter.operations,
      orderBy: [
        {
          order: "ASC",
          variable: "?distScore",
        },
      ],
    });

    return generatedQuery;
  },
};
