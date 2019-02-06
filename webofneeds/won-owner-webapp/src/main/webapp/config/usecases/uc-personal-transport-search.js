/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { getIn } from "../../app/utils.js";
import { sparqlQuery } from "../../app/sparql-builder-utils.js";

export const personalTransportSearch = {
  identifier: "personalTransportSearch",
  label: "Need a Lift",
  icon: "#ico36_uc_route_demand",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "Need a lift",
        type: "won:PersonalTransportSearch",
      },
    }),
  },
  // TODO: amount of people? other details?
  details: {
    title: { ...details.title },
    description: { ...details.description },
  },
  seeksDetails: {
    fromDatetime: { ...details.fromDatetime },
    travelAction: { ...details.travelAction },
  },
  generateQuery: (draft, resultName) => {
    const fromLocation = getIn(draft, [
      "seeks",
      "travelAction",
      "fromLocation",
    ]);
    const toLocation = getIn(draft, ["seeks", "travelAction", "toLocation"]);

    let filter;
    if (
      fromLocation &&
      fromLocation.lat &&
      fromLocation.lng &&
      toLocation &&
      toLocation.lat &&
      toLocation.lng
    ) {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Need.`,
          `{{${resultName} a <http://dbpedia.org/resource/Ridesharing>} union {${resultName} a s:TaxiService}}`,
          `${resultName} (won:hasLocation|s:location) ?location.`,
          "?location s:geo ?location_geo.",
          "?location_geo s:latitude ?location_lat;",
          "s:longitude ?location_lon;",
          `bind (abs(xsd:decimal(?location_lat) - ${
            fromLocation.lat
          }) as ?fromLatDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            fromLocation.lng
          }) as ?fromLonDiff)`,
          "bind (if ( ?fromLatDiffRaw > 180, 360 - ?fromLatDiffRaw, ?fromLatDiffRaw ) as ?fromLatDiff)",
          "bind ( ?fromLatDiff * ?fromLatDiff + ?fromLonDiff * ?fromLonDiff as ?fromLocation_geoDistanceScore)",
          `bind (abs(xsd:decimal(?location_lat) - ${
            toLocation.lat
          }) as ?latDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            toLocation.lng
          }) as ?toLonDiff)`,
          "bind (if ( ?toLatDiffRaw > 180, 360 - ?toLatDiffRaw, ?toLatDiffRaw ) as ?toLatDiff)",
          "bind ( ?toLatDiff * ?toLatDiff + ?toLonDiff * ?toLonDiff as ?toLocation_geoDistanceScore)",
          "bind (?fromLocation_geoDistanceScore + ?toLocation_geoDistanceScore as ?distScore)",
        ],
      };
    } else if (fromLocation && fromLocation.lat && fromLocation.lng) {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Need.`,
          `{{${resultName} a <http://dbpedia.org/resource/Ridesharing>} union {${resultName} a s:TaxiService}}`,
          `${resultName} (won:hasLocation|s:location) ?location.`,
          "?location s:geo ?location_geo.",
          "?location_geo s:latitude ?location_lat;",
          "s:longitude ?location_lon;",
          `bind (abs(xsd:decimal(?location_lat) - ${
            fromLocation.lat
          }) as ?latDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            fromLocation.lng
          }) as ?lonDiff)`,
          "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
          "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
          "bind (?location_geoDistanceScore as ?distScore)",
        ],
      };
    } else if (toLocation && toLocation.lat && toLocation.lng) {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Need.`,
          `${resultName} a <http://dbpedia.org/resource/Transport>.`,
          `${resultName} (won:hasLocation|s:location) ?location.`,
          "?location s:geo ?location_geo.",
          "?location_geo s:latitude ?location_lat;",
          "s:longitude ?location_lon;",
          `bind (abs(xsd:decimal(?location_lat) - ${
            toLocation.lat
          }) as ?latDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            toLocation.lng
          }) as ?lonDiff)`,
          "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
          "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
          "bind (?location_geoDistanceScore as ?distScore)",
        ],
      };
    } else {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
        },
        operations: [
          `${resultName} a won:Need.`,
          `{{${resultName} a <http://dbpedia.org/resource/Ridesharing>} union {${resultName} a s:TaxiService}}`,
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
