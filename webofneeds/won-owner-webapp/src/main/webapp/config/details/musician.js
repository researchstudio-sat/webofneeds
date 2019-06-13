/**
 * Created by fsuda on 18.09.2018.
 */
import Immutable from "immutable";
import { details } from "../detail-definitions.js";

import { is } from "../../app/utils.js";

export const instrumentsDetail = {
  ...details.tags,
  identifier: "instruments",
  label: "Instruments",
  icon: "#ico36_detail_instrument",
  placeholder: "e.g. Guitar, Vocals",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "demo:instrument": undefined };
    }
    return { "demo:instrument": value };
  },
  parseFromRDF: function(jsonLDImm) {
    const instruments = jsonLDImm && jsonLDImm.get("demo:instrument");
    if (!instruments) {
      return undefined;
    } else if (is("String", instruments)) {
      return Immutable.fromJS([instruments]);
    } else if (is("Array", instruments)) {
      return Immutable.fromJS(instruments);
    } else if (Immutable.List.isList(instruments)) {
      return instruments; // id; it is already in the format we want
    } else {
      console.error(
        "Found unexpected format of instruments (should be Array, " +
          "Immutable.List, or a single tag as string): " +
          JSON.stringify(instruments)
      );
      return undefined;
    }
  },
};

export const genresDetail = {
  ...details.tags,
  identifier: "genres",
  label: "Genres",
  icon: "#ico36_detail_genre",
  placeholder: "e.g. Rock, Pop",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "demo:genre": undefined };
    }
    return { "demo:genre": value };
  },
  parseFromRDF: function(jsonLDImm) {
    const genres = jsonLDImm && jsonLDImm.get("demo:genre");
    if (!genres) {
      return undefined;
    } else if (is("String", genres)) {
      return Immutable.fromJS([genres]);
    } else if (is("Array", genres)) {
      return Immutable.fromJS(genres);
    } else if (Immutable.List.isList(genres)) {
      return genres; // id; it is already in the format we want
    } else {
      console.error(
        "Found unexpected format of genres (should be Array, " +
          "Immutable.List, or a single tag as string): " +
          JSON.stringify(genres)
      );
      return undefined;
    }
  },
};

export const perHourRentDetail = {
  ...details.price,
  identifier: "rent",
  label: "Rent",
  icon: "#ico36_detail_rent",
  currency: [{ value: "EUR", label: "€", default: true }],
  unitCode: [{ value: "HUR", label: "per hour", default: true }],
  messageEnabled: false,
  parseFromRDF: function() {
    //That way we can make sure that parsing fromRDF is made only by the price detail itself
    return undefined;
  },
};

export const perHourRentRangeDetail = {
  ...details.pricerange,
  identifier: "rentRange",
  label: "Rent in EUR/hour",
  minLabel: "From",
  maxLabel: "To",
  currency: [{ value: "EUR", label: "€", default: true }],
  unitCode: [{ value: "HUR", label: "per hour", default: true }],
  icon: "#ico36_detail_rent",
  messageEnabled: false,
  parseFromRDF: function() {
    return undefined;
  },
};
