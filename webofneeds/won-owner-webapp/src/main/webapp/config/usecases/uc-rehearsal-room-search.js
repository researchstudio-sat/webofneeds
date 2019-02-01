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
  filterInVicinity,
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
        type: "won:RehearsalRoomRentDemand",
        title: "Looking for Rehearsal Room!",
        searchString: "Rehearsal Room",
      },
      seeks: {
        type: "won:RehearsalRoomRentOffer",
      },
    }),
  },
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

    const filters = [
      {
        // to select is-branch
        prefixes: {
          won: won.defaultContext["won"],
          rdf: won.defaultContext["rdf"],
        },
        operations: [
          `${resultName} a won:Need.`,
          `${resultName} rdf:type won:RehearsalRoomRentOffer.`,
          location && `${resultName} (won:hasLocation|s:location) ?location.`,
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
