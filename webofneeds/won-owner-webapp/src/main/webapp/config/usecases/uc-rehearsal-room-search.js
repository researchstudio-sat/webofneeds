/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import {
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
} from "../details/real-estate.js";
import won from "../../app/won-es6.js";
import { perHourRentRangeDetail } from "../details/musician.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterFloorSizeRange,
  filterPriceRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

export const rehearsalRoomSearch = {
  identifier: "rehearsalRoomSearch",
  label: "Find Rehearsal Room",
  icon: "#ico36_uc_realestate",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 7,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:RehearsalRoomRentDemand"],
        title: "Looking for Rehearsal Room!",
        searchString: "Rehearsal Room",
      },
      seeks: {
        type: ["demo:RehearsalRoomRentOffer"],
      },
    }),
  },
  reactionUseCases: ["rehearsalRoomOffer"],
  details: undefined,
  seeksDetails: {
    location: { ...details.location },
    floorSizeRange: { ...realEstateFloorSizeRangeDetail },
    features: {
      ...realEstateFeaturesDetail,
      placeholder: "e.g. PA, Drumkit",
    },
    priceRange: { ...perHourRentRangeDetail },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
  },
  generateQuery: (draft, resultName) => {
    const seeksBranch = draft && draft.seeks;
    const rentRange = seeksBranch && seeksBranch.priceRange;
    const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
    const location = seeksBranch && seeksBranch.location;

    let filter;
    if (location && location.lat && location.lng) {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
            rdf: won.defaultContext["rdf"],
            sh: won.defaultContext["sh"], //needed for the filterNumericProperty calls
            s: won.defaultContext["s"],
            geo: "http://www.bigdata.com/rdf/geospatial#",
            xsd: won.defaultContext["xsd"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RehearsalRoomRentOffer.`,
            `${resultName} (won:location|s:location) ?location.`,
            "?location s:geo ?location_geo.",
            "?location_geo s:latitude ?location_lat;",
            "s:longitude ?location_lon;",
            `bind (abs(xsd:decimal(?location_lat) - ${
              location.lat
            }) as ?latDiffRaw)`,
            `bind (abs(xsd:decimal(?location_lon) - ${
              location.lng
            }) as ?lonDiff)`,
            "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
            "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
            "bind (?location_geoDistanceScore as ?distScore)",
          ],
        },
        rentRange &&
          filterPriceRange(
            `${resultName}`,
            rentRange.min,
            rentRange.max,
            rentRange.currency
          ),

        floorSizeRange &&
          filterFloorSizeRange(
            `${resultName}`,
            floorSizeRange.min,
            floorSizeRange.max
          ),
      ];

      filter = concatenateFilters(filters);
    } else {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
            rdf: won.defaultContext["rdf"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RehearsalRoomRentOffer.`,
          ],
        },
        rentRange &&
          filterPriceRange(
            `${resultName}`,
            rentRange.min,
            rentRange.max,
            rentRange.currency
          ),

        floorSizeRange &&
          filterFloorSizeRange(
            `${resultName}`,
            floorSizeRange.min,
            floorSizeRange.max
          ),
      ];

      filter = concatenateFilters(filters);
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
