import { musicianSearch } from "./uc-musician-search.js";
import { bandSearch } from "./uc-band-search.js";
import { rehearsalRoomSearch } from "./uc-rehearsal-room-search.js";
import { rehearsalRoomOffer } from "./uc-rehearsal-room-offer.js";

export const musicianGroup = {
  identifier: "musiciangroup",
  label: "Artists and Bands",
  icon: undefined,
  useCases: {
    bandSearch: bandSearch,
    musicianSearch: musicianSearch,
    rehearsalRoomSearch: rehearsalRoomSearch,
    rehearsalRoomOffer: rehearsalRoomOffer,
  },
};
