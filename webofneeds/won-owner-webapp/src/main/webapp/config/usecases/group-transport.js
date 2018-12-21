import { goodsTransportSearch } from "./uc-goods-transport-search.js";
import { goodsTransportOffer } from "./uc-goods-transport-offer.js";

export const transportGroup = {
  identifier: "transportgroup",
  label: "Transport and Delivery",
  icon: undefined,
  useCases: {
    goodsTransportSearch: goodsTransportSearch,
    goodsTransportOffer: goodsTransportOffer,
  },
};
