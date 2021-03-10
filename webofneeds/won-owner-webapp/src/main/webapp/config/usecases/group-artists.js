import { musicianSearch } from "./uc-musician-search.js";
import { bandSearch } from "./uc-band-search.js";
import { rehearsalRoomOffer } from "./uc-rehearsal-room-offer.js";
import { rehearsalRoomSearch } from "./uc-rehearsal-room-search.js";

import ico36_detail_instrument from "../../images/won-icons/ico36_detail_instrument.svg";

export const artistGroup = {
  identifier: "artistGroup",
  label: "Music",
  icon: ico36_detail_instrument,
  subItems: {
    rehearsalRoomOffer: rehearsalRoomOffer,
    bandSearch: bandSearch,
    rehearsalRoomSearch: rehearsalRoomSearch,
    musicianSearch: musicianSearch,
  },
};
