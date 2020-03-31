import { goodsOffer } from "./uc-goods-offer.js";
import { goodsServiceSearch } from "./uc-goods-service-search.js";
import { serviceOffer } from "./uc-service-offer.js";
import { goodsTransportSearch } from "./uc-goods-transport-search.js";
import { goodsTransportOffer } from "./uc-goods-transport-offer.js";
import { booksOffer } from "./uc-books-offer.js";
import { booksSearch } from "./uc-books-search.js";

import ico36_detail_price from "../../images/won-icons/ico36_detail_price.svg";

/*
TODO: create use case icons
*/

export const classifiedsGroup = {
  identifier: "classifiedsgroup",
  label: "Buy & Sell",
  icon: ico36_detail_price,
  subItems: {
    goodsOffer: goodsOffer,
    goodsServiceSearch: goodsServiceSearch,
    serviceOffer: serviceOffer,
    // TODO: serviceSearch: serviceSearch,
    goodsTransportSearch: goodsTransportSearch,
    goodsTransportOffer: goodsTransportOffer,
    booksOffer: booksOffer,
    booksSearch: booksSearch,
  },
};
