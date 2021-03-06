/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import vocab from "../../app/service/vocab.js";
import { getIn } from "../../app/utils.js";
import { sparqlQuery } from "../../app/sparql-builder-utils.js";
import ico36_uc_route_demand from "../../images/won-icons/ico36_uc_route_demand.svg";

export const personalTransportSearch = {
  identifier: "personalTransportSearch",
  label: "Need a Lift",
  icon: ico36_uc_route_demand,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:PersonalTransportSearch"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["taxiOffer", "rideShareOffer"],
        refuseOwned: true,
      },
    },
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
          won: vocab.defaultContext["won"],
          s: vocab.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
          con: vocab.defaultContext["con"],
        },
        operations: [
          `
            ${resultName} <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> won:Atom.
            {
              { 
                ${resultName} <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wikidata.org/entity/Q16804155>.
                OPTIONAL { 
                  ${resultName} (con:travelAction/s:fromLocation) ?fromLocation.
                  ?fromLocation s:geo ?fromLocation_geo.
                  ?fromLocation_geo 
                    s:latitude ?fromLocation_lat;
                    s:longitude ?fromLocation_lon.
                  BIND(ABS((xsd:decimal(?fromLocation_lat)) - ${
                    fromLocation.lat
                  }) AS ?fromLatDiffRaw)
                  BIND(ABS((xsd:decimal(?fromLocation_lon)) - ${
                    fromLocation.lng
                  }) AS ?fromLonDiff)
                  BIND(IF(?fromLatDiffRaw > 180 , 360  - ?fromLatDiffRaw, ?fromLatDiffRaw) AS ?fromLatDiff)
                  BIND((?fromLatDiff * ?fromLatDiff) + (?fromLonDiff * ?fromLonDiff) AS ?fromLocation_geoDistanceScore)
                }
                OPTIONAL {
                  ${resultName} (con:travelAction/s:toLocation) ?toLocation.
                  ?toLocation s:geo ?toLocation_geo.
                  ?toLocation_geo 
                    s:latitude ?toLocation_lat;
                    s:longitude ?toLocation_lon.
                  BIND(ABS((xsd:decimal(?toLocation_lat)) - ${
                    toLocation.lat
                  }) AS ?toLatDiffRaw)
                  BIND(ABS((xsd:decimal(?toLocation_lon)) - ${
                    toLocation.lng
                  }) AS ?toLonDiff)
                  BIND(IF(?toLatDiffRaw > 180 , 360  - ?toLatDiffRaw, ?toLatDiffRaw) AS ?toLatDiff)
                  BIND((?toLatDiff * ?toLatDiff) + (?toLonDiff * ?toLonDiff) AS ?toLocation_geoDistanceScore)
                }
                FILTER ( bound(?fromLocation_geoDistanceScore) && bound (?toLocation_geoDistanceScore))
                BIND(if ( bound(?fromLocation_geoDistanceScore), ?fromLocation_geoDistanceScore, ?toLocation_geoDistanceScore) as ?fromScore )
                BIND(if ( bound(?toLocation_geoDistanceScore), ?toLocation_geoDistanceScore, ?fromLocation_geoDistanceScore) as ?toScore )
                BIND((?toScore + ?fromScore ) / 2 AS ?score)
              }
              UNION
              { 
                ${resultName} <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> s:TaxiService. 
                ${resultName} (won:location|s:location) ?location.
                ?location s:geo ?location_geo.
                ?location_geo 
                  s:latitude ?location_lat;
                  s:longitude ?location_lon.
                BIND(ABS((xsd:decimal(?location_lat)) - ${
                  fromLocation.lat
                }) AS ?fromLatDiffRaw)
                BIND(ABS((xsd:decimal(?location_lon)) - ${
                  fromLocation.lng
                }) AS ?fromLonDiff)
                BIND(IF(?fromLatDiffRaw > 180 , 360  - ?fromLatDiffRaw, ?fromLatDiffRaw) AS ?fromLatDiff)
                BIND((?fromLatDiff * ?fromLatDiff) + (?fromLonDiff * ?fromLonDiff) AS ?fromLocation_geoDistanceScore)
                BIND(ABS((xsd:decimal(?location_lat)) - ${
                  toLocation.lat
                }) AS ?toLatDiffRaw)
                BIND(ABS((xsd:decimal(?location_lon)) - ${
                  toLocation.lng
                }) AS ?toLonDiff)
                BIND(IF(?toLatDiffRaw > 180 , 360  - ?toLatDiffRaw, ?toLatDiffRaw) AS ?toLatDiff)
                BIND((?toLatDiff * ?toLatDiff) + (?toLonDiff * ?toLonDiff) AS ?toLocation_geoDistanceScore)
                BIND((?fromLocation_geoDistanceScore + ?toLocation_geoDistanceScore ) / 2 AS ?score)
              }
            }`,
        ],
      };
    } else if (fromLocation && fromLocation.lat && fromLocation.lng) {
      filter = {
        prefixes: {
          won: vocab.defaultContext["won"],
          s: vocab.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
          con: vocab.defaultContext["con"],
        },
        operations: [
          `${resultName} a won:Atom.
          {
            {
              ${resultName} a <http://www.wikidata.org/entity/Q16804155> ;
                            (con:travelAction/s:fromLocation) ?location.
            } 
            union 
            {
              ${resultName} a s:TaxiService ; 
                            s:location ?location.
            }
          }`,
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
          "bind (?location_geoDistanceScore as ?score)",
        ],
      };
    } else if (toLocation && toLocation.lat && toLocation.lng) {
      filter = {
        prefixes: {
          won: vocab.defaultContext["won"],
          s: vocab.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
          con: vocab.defaultContext["con"],
        },
        operations: [
          `${resultName} a won:Atom.
          {
            {
              ${resultName} a <http://www.wikidata.org/entity/Q16804155> ;
                            (con:travelAction/s:toLocation) ?location.
            } 
            union 
            {
              ${resultName} a s:TaxiService ; 
                            s:location ?location.
            }
          }`,
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
          "bind (?location_geoDistanceScore as ?score)",
        ],
      };
    } else {
      filter = {
        prefixes: {
          won: vocab.defaultContext["won"],
          s: vocab.defaultContext["s"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `{{${resultName} a <http://www.wikidata.org/entity/Q16804155>} union {${resultName} a s:TaxiService}}`,
        ],
      };
    }

    return sparqlQuery({
      prefixes: filter.prefixes,
      distinct: true,
      variables: [resultName, "?score"],
      where: filter.operations,
      orderBy: [
        {
          order: "ASC",
          variable: "?distScore",
        },
      ],
    });
  },
};
