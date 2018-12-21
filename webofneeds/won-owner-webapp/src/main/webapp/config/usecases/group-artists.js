import { musicianSearch } from "./uc-musician-search.js";
import { bandSearch } from "./uc-band-search.js";
import { serviceOffer } from "./uc-service-offer.js";

export const artistGroup = {
  identifier: "artistgroup",
  label: "Artists and Bands",
  icon: undefined,
  useCases: {
    bandSearch: bandSearch,
    musicianSearch: musicianSearch,
    serviceOffer: serviceOffer,
  },
};
