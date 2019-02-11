import { goodsOffer } from "./uc-goods-offer.js";
import { goodsServiceSearch } from "./uc-goods-service-search.js";
import { serviceOffer } from "./uc-service-offer.js";
import { goodsTransportSearch } from "./uc-goods-transport-search.js";
import { goodsTransportOffer } from "./uc-goods-transport-offer.js";

/*
TODO: create use case icons
*/

export const classifiedsGroup = {
  identifier: "classifiedsgroup",
  label: "Buy & Sell",
  icon: undefined,
  subItems: {
    goodsOffer: goodsOffer,
    goodsServiceSearch: goodsServiceSearch,
    serviceOffer: serviceOffer,
    // TODO: serviceSearch: serviceSearch,
    goodsTransportSearch: goodsTransportSearch,
    goodsTransportOffer: goodsTransportOffer,
  },
};
