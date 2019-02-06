/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import {
  realEstateRentDetail,
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
} from "../details/real-estate.js";

import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterInVicinity,
  filterNumericProperty,
  filterPrice,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/won-es6.js";

export const rentRealEstateOffer = {
  identifier: "rentRealEstateOffer",
  label: "Rent a place out",
  icon: "#ico36_uc_realestate",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:RealEstateRentOffer"],
        title: "For Rent",
        tags: ["RentOutRealEstate"],
      },
      seeks: {
        type: ["won:RealEstateRentDemand"],
        tags: ["SearchRealEstateToRent"],
      },
    }),
  },
  details: {
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
  generateQuery: (draft, resultName) => {
    const draftContent = draft && draft.content;
    const location = draftContent && draftContent.location;
    const rent = draftContent && draftContent.rent;
    const numberOfRooms = draftContent && draftContent.numberOfRooms;
    const floorSize = draftContent && draftContent.floorSize;

    const filters = [
      {
        // to select is-branch
        prefixes: {
          won: won.defaultContext["won"],
          sh: won.defaultContext["sh"], //needed for the filterNumericProperty calls
        },
        operations: [
          `${resultName} a won:Need.`,
          `${resultName} won:seeks ?seeks.`,
          location && "?seeks (s:location|won:hasLocation) ?location.",
        ],
      },
      rent && filterPrice("?seeks", rent.amount, rent.currency, "rent"),
      floorSize &&
        filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
      numberOfRooms &&
        filterNumericProperty(
          "?seeks",
          numberOfRooms,
          "s:numberOfRooms",
          "rooms"
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
