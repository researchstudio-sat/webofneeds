/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { getIn } from "../../app/utils.js";
import {
  filterInVicinity,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

export const taxiOffer = {
  identifier: "taxiOffer",
  label: "Offer Taxi Service",
  icon: "#ico36_uc_taxi_offer",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: { title: "Taxi", type: "s:TaxiService" },
    }),
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
  },
  generateQuery: (draft, resultName) => {
    const location = getIn(draft, ["content", "location"]);
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
};
