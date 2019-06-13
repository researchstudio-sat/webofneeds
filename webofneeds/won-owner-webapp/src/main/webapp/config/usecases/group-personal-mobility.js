import { taxiOffer } from "./uc-taxi-offer.js";
import { rideShareOffer } from "./uc-rideshare-offer.js";
import { personalTransportSearch } from "./uc-personal-transport-search.js";

export const personalMobilityGroup = {
  identifier: "mobilitygroup",
  label: "Mobility",
  icon: "#ico36_uc_route_demand",
  subItems: {
    personalTransportSearch: personalTransportSearch,
    taxiOffer: taxiOffer,
    rideShareOffer: rideShareOffer,
  },
};
