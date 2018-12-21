import { rentRealEstateOffer } from "./uc-rent-real-estate-offer";
import { rentRealEstateSearch } from "./uc-rent-real-estate-search";

export const realEstateGroup = {
  identifier: "realestategroup",
  label: "Real Estate",
  icon: undefined,
  useCases: {
    searchRent: rentRealEstateSearch,
    offerRent: rentRealEstateOffer,
  },
};
