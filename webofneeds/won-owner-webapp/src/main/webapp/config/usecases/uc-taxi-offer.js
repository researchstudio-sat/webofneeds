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
import { getIn } from "../../app/utils.js";
import { sparqlQuery } from "../../app/sparql-builder-utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_taxi_offer from "../../images/won-icons/ico36_uc_taxi_offer.svg";

export const taxiOffer = {
  identifier: "taxiOffer",
  label: "Offer Taxi Service",
  icon: ico36_uc_taxi_offer,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: { type: ["s:TaxiService"] },
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
    location: { ...details.location },
    images: { ...details.images },
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
          xsd: won.defaultContext["xsd"],
          demo: won.defaultContext["demo"],
          match: won.defaultContext["match"],
          con: won.defaultContext["con"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a demo:PersonalTransportSearch.`,
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
          demo: won.defaultContext["demo"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a demo:PersonalTransportSearch.`,
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
