/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { getIn, isValidDate } from "../../app/utils.js";
import {
  filterInVicinity,
  filterAboutTime,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

const mobilityUseCases = {
  liftDemand: {
    identifier: "liftDemand",
    label: "Need a Lift",
    icon: "#ico36_uc_route_demand",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: { title: "Need a lift", tags: "search-lift" },
      searchString: "offer-lift",
    },
    // TODO: amount of people? other details?
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
    },
    seeksDetails: {
      fromDatetime: { ...details.fromDatetime },
      travelAction: { ...details.travelAction },
    },
    generateQuery: (draft, resultName) => {
      const fromLocation = getIn(draft, [
        "seeks",
        "travelAction",
        "fromLocation",
      ]);
      const toLocation = getIn(draft, ["seeks", "travelAction", "toLocation"]);

      const baseFilter = {
        prefixes: {
          won: won.defaultContext["won"],
        },
        operations: [
          `${resultName} a won:Need.`,
          `${resultName} won:isInState won:Active. { ?is a <http://dbpedia.org/resource/Ridesharing>.  } union { ?is a s:TaxiService} }`,
          `${resultName} won:is ?is.`,
        ],
      };

      const locationFilter = filterInVicinity(
        "?location",
        fromLocation,
        /*radius=*/ 100
      );
      const fromLocationFilter = filterInVicinity(
        "?fromLocation",
        fromLocation,
        /*radius=*/ 5
      );
      const toLocationFilter = filterInVicinity(
        "?toLocation",
        toLocation,
        /*radius=*/ 5
      );

      const union = operations => {
        if (!operations || operations.length === 0) {
          return "";
        } else {
          return "{" + operations.join("} UNION {") + "}";
        }
      };
      const filterAndJoin = (arrayOfStrings, seperator) =>
        arrayOfStrings.filter(str => str).join(seperator);

      const locationFilters = {
        prefixes: locationFilter.prefixes,
        operations: union([
          filterAndJoin(
            [
              fromLocation &&
                `?is a <http://dbpedia.org/resource/Ridesharing>. ?is won:travelAction/s:fromLocation ?fromLocation. `,
              fromLocation && fromLocationFilter.operations.join(" "),
              toLocation && "?is won:travelAction/s:toLocation ?toLocation.",
              toLocation && toLocationFilter.operations.join(" "),
            ],
            " "
          ),
          filterAndJoin(
            [
              location &&
                `?is a s:TaxiService . ?is won:hasLocation ?location .`,
              location && locationFilter.operations.join(" "),
            ],
            " "
          ),
        ]),
      };

      const concatenatedFilter = concatenateFilters([
        baseFilter,
        locationFilters,
      ]);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        distinct: true,
        variables: [resultName],
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?location_geoDistance",
          },
        ],
      });
    },
  },
  taxiOffer: {
    identifier: "taxiOffer",
    label: "Offer Taxi Service",
    icon: "#ico36_uc_taxi_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: { title: "Taxi", type: "s:TaxiService" },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
    },
    generateQuery: (draft, resultName) => {
      const location = getIn(draft, ["is", "location"]);
      const filters = [
        {
          // to select seeks-branch
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:seeks ?seeks.`,
            location && "?seeks won:travelAction/s:fromLocation ?location.",
          ],
        },

        filterInVicinity("?location", location, /*radius=*/ 100),
      ];

      const concatenatedFilter = concatenateFilters(filters);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        distinct: true,
        variables: [resultName],
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?location_geoDistance",
          },
        ],
      });
    },
  },
  rideShareOffer: {
    identifier: "rideShareOffer",
    label: "Offer to Share a Ride",
    icon: "#ico36_uc_taxi_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: {
      ...emptyDraft,
      is: {
        title: "Share a Ride",
        type: "http://dbpedia.org/resource/Ridesharing",
      },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
      travelAction: { ...details.travelAction },
    },
    generateQuery: (draft, resultName) => {
      const toLocation = getIn(draft, ["is", "travelAction", "toLocation"]);
      const fromLocation = getIn(draft, ["is", "travelAction", "fromLocation"]);

      const fromTime = getIn(draft, ["is", "fromDatetime"]);
      const filters = [
        {
          // to select seeks-branch
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:seeks ?seeks.`,
            fromLocation &&
              "?seeks won:travelAction/s:fromLocation ?fromLocation.",
            toLocation && "?seeks won:travelAction/s:toLocation ?toLocation.",
            isValidDate(fromTime) && "?seeks s:validFrom ?starttime",
          ],
        },

        filterInVicinity("?fromLocation", fromLocation, /*radius=*/ 5),

        filterInVicinity("?toLocation", toLocation, /*radius=*/ 5),

        filterAboutTime("?starttime", fromTime, 12 /* hours before and after*/),
      ];

      const concatenatedFilter = concatenateFilters(filters);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        distinct: true,
        variables: [resultName],
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?fromLocation_geoDistance",
          },
        ],
      });
    },
  },
};

export const mobilityGroup = {
  identifier: "mobilitygroup",
  label: "Personal Mobility",
  icon: undefined,
  useCases: { ...mobilityUseCases },
};
