import { taxiOffer } from "./uc-taxi-offer.js";
import { rideShareOffer } from "./uc-rideshare-offer.js";
import { personalTransportSearch } from "./uc-personal-transport-search.js";
import { goodsTransportSearch } from "./uc-goods-transport-search.js";
import { goodsTransportOffer } from "./uc-goods-transport-offer.js";
import ico36_uc_transport_offer from "../../images/won-icons/ico36_uc_transport_offer.svg";

export const transportGroup = {
  identifier: "transportGroup",
  label: "Transport",
  icon: ico36_uc_transport_offer,
  subItems: {
    personalTransportSearch: personalTransportSearch,
    taxiOffer: taxiOffer,
    rideShareOffer: rideShareOffer,
    goodsTransportSearch: goodsTransportSearch,
    goodsTransportOffer: goodsTransportOffer,
  },
};
