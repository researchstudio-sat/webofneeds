import { rentRealEstateOffer } from "./uc-rent-real-estate-offer";
import { rentRealEstateSearch } from "./uc-rent-real-estate-search";
import { rehearsalRoomOffer } from "./uc-rehearsal-room-offer.js";
import { rehearsalRoomSearch } from "./uc-rehearsal-room-search.js";

export const realEstateGroup = {
  identifier: "realestategroup",
  label: "Homes",
  icon: undefined,
  subItems: {
    rentRealEstateSearch: rentRealEstateSearch,
    rentRealEstateOffer: rentRealEstateOffer,
    rehearsalRoomSearch: rehearsalRoomSearch,
    rehearsalRoomOffer: rehearsalRoomOffer,
  },
};
