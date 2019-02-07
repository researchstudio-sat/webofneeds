/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import {
  realEstateRentRangeDetail,
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
  realEstateNumberOfRoomsRangeDetail,
} from "../details/real-estate.js";

import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterFloorSizeRange,
  filterNumOfRoomsRange,
  filterPriceRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/won-es6.js";

export const rentRealEstateSearch = {
  identifier: "rentRealEstateSearch",
  label: "Find a place to rent",
  icon: "#ico36_uc_realestate",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      seeks: {
        type: "won:RealEstateRentOffer",
        tags: ["RentOutRealEstate"],
      },
      content: {
        type: "won:RealEstateRentDemand",
        tags: ["SearchRealEstateToRent"],
      },
    }),
  },
  details: undefined,
  seeksDetails: {
    location: { ...details.location },
    floorSizeRange: { ...realEstateFloorSizeRangeDetail },
    numberOfRoomsRange: { ...realEstateNumberOfRoomsRangeDetail },
    features: { ...realEstateFeaturesDetail },
    priceRange: { ...realEstateRentRangeDetail },
  },
  generateQuery: (draft, resultName) => {
    const seeksBranch = draft && draft.seeks;
    const rentRange = seeksBranch && seeksBranch.priceRange;
    const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
    const numberOfRoomsRange = seeksBranch && seeksBranch.numberOfRoomsRange;
    const location = seeksBranch && seeksBranch.location;

    let filter;
    if (location && location.lat && location.lng) {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
            xsd: won.defaultContext["xsd"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} a won:RealEstateRentOffer.`,
            `${resultName} (won:hasLocation|s:location) ?location.`,
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

        numberOfRoomsRange &&
          filterNumOfRoomsRange(
            `${resultName}`,
            numberOfRoomsRange.min,
            numberOfRoomsRange.max
          ),
      ];

      filter = concatenateFilters(filters);
    } else {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
            s: won.defaultContext["s"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} a won:RealEstateRentOffer.`,
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

        numberOfRoomsRange &&
          filterNumOfRoomsRange(
            `${resultName}`,
            numberOfRoomsRange.min,
            numberOfRoomsRange.max
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
