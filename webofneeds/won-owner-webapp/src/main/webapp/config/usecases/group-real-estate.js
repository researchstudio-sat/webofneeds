import { rentRealEstateOffer } from "./uc-rent-real-estate-offer";
import { rentRealEstateSearch } from "./uc-rent-real-estate-search";
import { rehearsalRoomOffer } from "./uc-rehearsal-room-offer.js";
import { rehearsalRoomSearch } from "./uc-rehearsal-room-search.js";
import ico36_uc_realestate from "../../images/won-icons/ico36_uc_realestate.svg";

export const realEstateGroup = {
  identifier: "realEstateGroup",
  label: "Homes",
  icon: ico36_uc_realestate,
  subItems: {
    rentRealEstateSearch: rentRealEstateSearch,
    rentRealEstateOffer: rentRealEstateOffer,
    rehearsalRoomSearch: rehearsalRoomSearch,
    rehearsalRoomOffer: rehearsalRoomOffer,
  },
};
