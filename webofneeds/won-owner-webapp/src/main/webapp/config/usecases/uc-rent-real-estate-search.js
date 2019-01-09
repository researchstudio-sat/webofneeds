/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import {
  realEstateRentRangeDetail,
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
  realEstateNumberOfRoomsRangeDetail,
} from "../details/real-estate.js";

import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterInVicinity,
  filterFloorSizeRange,
  filterNumOfRoomsRange,
  filterPriceRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/won-es6.js";

export const rentRealEstateSearch = {
  identifier: "searchRent",
  label: "Find a place to rent",
  icon: "#ico36_uc_realestate",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    seeks: {
      type: "won:RealEstateRentOffer",
      tags: ["RentOutRealEstate"],
    },
    content: {
      type: "won:RealEstateRentDemand",
      tags: ["SearchRealEstateToRent"],
    },
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

    const filters = [
      {
        // to select is-branch
        prefixes: {
          won: won.defaultContext["won"],
        },
        operations: [
          `${resultName} a won:Need.`,
          location && `${resultName} won:hasLocation ?location.`,
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

      filterInVicinity("?location", location),
    ];

    const concatenatedFilter = concatenateFilters(filters);

    return sparqlQuery({
      prefixes: concatenatedFilter.prefixes,
      distinct: true,
      variables: [resultName],
      where: concatenatedFilter.operations,
      orderBy: [
        {
          order: "ASC",
          variable: "?location_geoDistance",
        },
      ],
    });
  },
};
