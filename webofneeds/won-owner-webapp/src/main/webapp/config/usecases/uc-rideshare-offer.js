/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";
import { getIn, isValidDate } from "../../app/utils.js";
import {
  filterAboutTime,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_taxi_offer from "../../images/won-icons/ico36_uc_taxi_offer.svg";

export const rideShareOffer = {
  identifier: "rideShareOffer",
  label: "Offer to Share a Ride",
  icon: ico36_uc_taxi_offer,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["http://www.wikidata.org/entity/Q16804155"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["personalTransportSearch"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
    travelAction: { ...details.travelAction },
    images: { ...details.images },
  },
  generateQuery: (draft, resultName) => {
    const fromLocation = getIn(draft, [
      "content",
      "travelAction",
      "fromLocation",
    ]);
    const toLocation = getIn(draft, ["content", "travelAction", "toLocation"]);
    const fromTime = getIn(draft, ["content", "fromDatetime"]);

    let filter;
    if (
      fromLocation &&
      fromLocation.lat &&
      fromLocation.lng &&
      toLocation &&
      toLocation.lat &&
      toLocation.lng
    ) {
      const baseFilter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
          match: won.defaultContext["match"],
          con: won.defaultContext["con"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} match:seeks ?seeks.`,
          isValidDate(fromTime) && `?seeks s:validFrom ?starttime.`,
          `?seeks con:travelAction/s:fromLocation ?fromLocation.`,
          `?seeks con:travelAction/s:toLocation ?toLocation.`,
          "?fromLocation s:geo ?fromLocation_geo.",
          "?fromLocation_geo s:latitude ?fromLocation_lat;",
          "s:longitude ?fromLocation_lon;",
          `bind (abs(xsd:decimal(?fromLocation_lat) - ${
            fromLocation.lat
          }) as ?fromLatDiffRaw)`,
          `bind (abs(xsd:decimal(?fromLocation_lon) - ${
            fromLocation.lng
          }) as ?fromLonDiff)`,
          "bind (if ( ?fromLatDiffRaw > 180, 360 - ?fromLatDiffRaw, ?fromLatDiffRaw ) as ?fromLatDiff)",
          "bind ( ?fromLatDiff * ?fromLatDiff + ?fromLonDiff * ?fromLonDiff as ?fromLocation_geoDistanceScore)",
          "?toLocation s:geo ?toLocation_geo.",
          "?toLocation_geo s:latitude ?toLocation_lat;",
          "s:longitude ?toLocation_lon;",
          `bind (abs(xsd:decimal(?toLocation_lat) - ${
            toLocation.lat
          }) as ?toLatDiffRaw)`,
          `bind (abs(xsd:decimal(?toLocation_lon) - ${
            toLocation.lng
          }) as ?toLonDiff)`,
          "bind (if ( ?toLatDiffRaw > 180, 360 - ?toLatDiffRaw, ?toLatDiffRaw ) as ?toLatDiff)",
          "bind ( ?toLatDiff * ?toLatDiff + ?toLonDiff * ?toLonDiff as ?toLocation_geoDistanceScore)",
          "bind (?toLocation_geoDistanceScore + ?fromLocation_geoDistanceScore as ?distScore)",
        ],
      };

      const timeFilter = filterAboutTime(
        "?starttime",
        fromTime,
        12 /* hours before and after*/
      );

      filter = concatenateFilters([baseFilter, timeFilter]);
    } else if (fromLocation && fromLocation.lat && fromLocation.lng) {
      const baseFilter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
          match: won.defaultContext["match"],
          con: won.defaultContext["con"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} match:seeks ?seeks.`,
          isValidDate(fromTime) && `?seeks s:validFrom ?starttime.`,
          `?seeks con:travelAction/s:fromLocation ?fromLocation.`,
          "?fromLocation s:geo ?fromLocation_geo.",
          "?fromLocation_geo s:latitude ?fromLocation_lat;",
          "s:longitude ?fromLocation_lon;",
          `bind (abs(xsd:decimal(?fromLocation_lat) - ${
            fromLocation.lat
          }) as ?fromLatDiffRaw)`,
          `bind (abs(xsd:decimal(?fromLocation_lon) - ${
            fromLocation.lng
          }) as ?fromLonDiff)`,
          "bind (if ( ?fromLatDiffRaw > 180, 360 - ?fromLatDiffRaw, ?fromLatDiffRaw ) as ?fromLatDiff)",
          "bind ( ?fromLatDiff * ?fromLatDiff + ?fromLonDiff * ?fromLonDiff as ?fromLocation_geoDistanceScore)",
          "bind (?fromLocation_geoDistanceScore as ?distScore)",
        ],
      };

      const timeFilter = filterAboutTime(
        "?starttime",
        fromTime,
        12 /* hours before and after*/
      );

      filter = concatenateFilters([baseFilter, timeFilter]);
    } else if (toLocation && toLocation.lat && toLocation.lng) {
      const baseFilter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
          match: won.defaultContext["match"],
          con: won.defaultContext["con"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} match:seeks ?seeks.`,
          isValidDate(fromTime) && `?seeks s:validFrom ?starttime.`,
          `?seeks con:travelAction/s:toLocation ?toLocation.`,
          "?toLocation s:geo ?toLocation_geo.",
          "?toLocation_geo s:latitude ?toLocation_lat;",
          "s:longitude ?toLocation_lon;",
          `bind (abs(xsd:decimal(?toLocation_lat) - ${
            toLocation.lat
          }) as ?toLatDiffRaw)`,
          `bind (abs(xsd:decimal(?toLocation_lon) - ${
            toLocation.lng
          }) as ?toLonDiff)`,
          "bind (if ( ?toLatDiffRaw > 180, 360 - ?toLatDiffRaw, ?toLatDiffRaw ) as ?toLatDiff)",
          "bind ( ?toLatDiff * ?toLatDiff + ?toLonDiff * ?toLonDiff as ?toLocation_geoDistanceScore)",
          "bind (?toLocation_geoDistanceScore as ?distScore)",
        ],
      };

      const timeFilter = filterAboutTime(
        "?starttime",
        fromTime,
        12 /* hours before and after*/
      );

      filter = concatenateFilters([baseFilter, timeFilter]);
    } else {
      const baseFilter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          match: won.defaultContext["match"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} match:seeks ?seeks.`,
          isValidDate(fromTime) && `?seeks s:validFrom ?starttime.`,
        ],
      };

      const timeFilter = filterAboutTime(
        "?starttime",
        fromTime,
        12 /* hours before and after*/
      );

      filter = concatenateFilters([baseFilter, timeFilter]);
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
