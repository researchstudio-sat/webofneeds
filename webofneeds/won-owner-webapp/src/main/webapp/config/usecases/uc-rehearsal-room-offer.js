/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import {
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
} from "../details/real-estate.js";
import won from "../../app/won-es6.js";
import { perHourRentDetail } from "../details/musician.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterInVicinity,
  filterNumericProperty,
  filterPrice,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

export const rehearsalRoomOffer = {
  identifier: "rehearsalRoomOffer",
  label: "Offer Rehearsal Room",
  icon: "#ico36_uc_realestate",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    content: {
      type: "won:RehearsalRoomRentOffer",
      title: "Offer Rehearsal Room!",
    },
    seeks: {
      type: "won:RehearsalRoomRentDemand",
    },
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
    features: {
      ...realEstateFeaturesDetail,
      placeholder: "e.g. PA, Drumkit",
    },
    rent: {
      ...perHourRentDetail,
      mandatory: true,
    },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
  },

  seeksDetails: undefined,
  generateQuery: (draft, resultName) => {
    const draftContent = draft && draft.content;
    const location = draftContent && draftContent.location;
    const rent = draftContent && draftContent.rent;
    const floorSize = draftContent && draftContent.floorSize;

    const filters = [
      {
        // to select is-branch
        prefixes: {
          won: won.defaultContext["won"],
          rdf: won.defaultContext["rdf"],
          sh: won.defaultContext["sh"], //needed for the filterNumericProperty calls
        },
        operations: [
          `${resultName} a won:Need.`,
          `${resultName} won:seeks ?seeks.`,
          `${resultName} rdf:type won:RehearsalRoomRentDemand.`,
          location && "?seeks won:hasLocation ?location.",
        ],
      },
      rent && filterPrice("?seeks", rent.amount, rent.currency, "rent"),
      floorSize &&
        filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
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
