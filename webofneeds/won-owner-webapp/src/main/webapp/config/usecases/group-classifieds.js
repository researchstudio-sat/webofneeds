/**
 * Created by kweinberger on 06.12.2018.
 */

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
  label: "Classified Ads",
  icon: undefined,
  useCases: {
    goodsOffer: goodsOffer,
    goodsSearch: goodsServiceSearch,
    serviceOffer: serviceOffer,
    // TODO: serviceSearch: serviceSearch,
    goodsTransportSearch: goodsTransportSearch,
    goodsTransportOffer: goodsTransportOffer,
  },
};
