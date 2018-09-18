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
} from "realEstateDetails";
import { genresDetail, instrumentsDetail } from "musicianDetails";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const musicianGroup = {
  identifier: "musiciangroup",
  label: "Artists and Bands",
  icon: undefined,
  useCases: { ...musicianUseCases },
};

const musicianUseCases = {
  findBand: {
    identifier: "findBand",
    label: "Find Band",
    icon: "#ico36_uc_find_band",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'm looking for a band!",
        //tags: ["musician", "band"],
      },
      searchString: "band",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      instruments: {
        ...instrumentsDetail,
        //mandatory: true,
      },
    },
    seeksDetails: {
      description: { ...details.description },
      genres: { ...genresDetail },
      location: { ...details.location },
    },
  },
  findMusician: {
    identifier: "findMusician",
    label: "Find Musician",
    icon: "#ico36_uc_find_musician",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "Looking for a Musician!",
        //tags: ["band", "musician"],
      },
      searchString: "musician",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      genres: { ...genresDetail },
      location: { ...details.location },
    },
    seeksDetails: {
      description: { ...details.description },
      instruments: {
        ...instrumentsDetail,
        //mandatory: true,
      },
    },
  },
  findRehearsalRoom: {
    identifier: "findRehearsalRoom",
    label: "Find Rehearsal Room",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "Looking for Rehearsal Room!",
        tags: ["SearchRehearsal"],
      },
      seeks: {
        tags: ["OfferRehearsal"],
      },
      searchString: "Rehearsal Room",
    },
    isDetails: undefined,
    seeksDetails: {
      location: { ...details.location },
      floorSizeRange: { ...realEstateFloorSizeRangeDetail },
      features: {
        ...realEstateFeaturesDetail,
        placeholder: "e.g. PA, Drumkit",
      },
      rentRange: { ...realEstateRentRangeDetail },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
    },
  },
  offerRehearsalRoom: {
    identifier: "OfferRehearsalRoom",
    label: "Offer Rehearsal Room",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "Offer Rehearsal Room!",
        tags: ["OfferRehearsal"],
      },
      seeks: {
        tags: ["SearchRehearsal"],
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
      features: {
        ...realEstateFeaturesDetail,
        placeholder: "e.g. PA, Drumkit",
      },
      rent: {
        ...realEstateRentDetail,
        mandatory: true,
      },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
    },

    seeksDetails: undefined,
  },
};
