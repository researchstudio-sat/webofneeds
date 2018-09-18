/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "detailDefinitions";
import {
  realEstateRentRangeDetail,
  realEstateRentDetail,
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
  realEstateNumberOfRoomsRangeDetail,
} from "realEstateDetails";

import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterInVicinity,
  filterFloorSizeRange,
  filterNumOfRoomsRange,
  filterRentRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/won-es6.js";

const realEstateUseCases = {
  searchRent: {
    identifier: "searchRent",
    label: "Find a place to rent",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      seeks: {
        type: "won:RealEstateRentOffer",
        tags: ["RentOutRealEstate"],
      },
      is: {
        type: "won:RealEstateRentDemand",
        tags: ["SearchRealEstateToRent"],
      },
    },
    isDetails: undefined,
    seeksDetails: {
      location: { ...details.location },
      floorSizeRange: { ...realEstateFloorSizeRangeDetail },
      numberOfRoomsRange: { ...realEstateNumberOfRoomsRangeDetail },
      features: { ...realEstateFeaturesDetail },
      rentRange: { ...realEstateRentRangeDetail },
    },
    generateQuery: (draft, resultName) => {
      const seeksBranch = draft && draft.seeks;
      const rentRange = seeksBranch && seeksBranch.rentRange;
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
            `${resultName} won:is ?is.`,
            location && "?is won:hasLocation ?location.",
          ],
        },
        rentRange &&
          filterRentRange(
            "?is",
            rentRange.min,
            rentRange.max,
            rentRange.currency
          ),

        floorSizeRange &&
          filterFloorSizeRange("?is", floorSizeRange.min, floorSizeRange.max),

        numberOfRoomsRange &&
          filterNumOfRoomsRange(
            "?is",
            numberOfRoomsRange.min,
            numberOfRoomsRange.max
          ),

        filterInVicinity("?location", location),
      ];

      const concatenatedFilter = concatenateFilters(filters);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        selectDistinct: resultName,
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?location_geoDistance",
          },
        ],
      });
    },
  },
  offerRent: {
    identifier: "offerRent",
    label: "Rent a place out",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        type: "won:RealEstateRentOffer",
        title: "For Rent",
        tags: ["RentOutRealEstate"],
      },
      seeks: {
        type: "won:RealEstateRentDemand",
        tags: ["SearchRealEstateToRent"],
      },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: {
        ...details.location,
        mandatory: true,
      },
      floorSize: {
        ...realEstateFloorSizeDetail,
        mandatory: true,
      },
      numberOfRooms: {
        ...realEstateNumberOfRoomsDetail,
        mandatory: true,
      },
      features: { ...realEstateFeaturesDetail },
      rent: {
        ...realEstateRentDetail,
        mandatory: true,
      },
    },
    seeksDetails: undefined,
  },
  // searchBuy: {},
  // offerBuy: {},
};

export const realEstateGroup = {
  identifier: "realestategroup",
  label: "Real Estate",
  icon: undefined,
  useCases: { ...realEstateUseCases },
};
