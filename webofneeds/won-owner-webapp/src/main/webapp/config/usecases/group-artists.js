import { musicianSearch } from "./uc-musician-search.js";
import { bandSearch } from "./uc-band-search.js";
import { serviceOffer } from "./uc-service-offer.js";
import { rehearsalRoomOffer } from "./uc-rehearsal-room-offer.js";
import { rehearsalRoomSearch } from "./uc-rehearsal-room-search.js";

export const artistGroup = {
  identifier: "artistgroup",
  label: "Music",
  icon: "#ico36_detail_instrument",
  subItems: {
    rehearsalRoomOffer: rehearsalRoomOffer,
    bandSearch: bandSearch,
    serviceOffer: serviceOffer,
    rehearsalRoomSearch: rehearsalRoomSearch,
    musicianSearch: musicianSearch,
  },
};
