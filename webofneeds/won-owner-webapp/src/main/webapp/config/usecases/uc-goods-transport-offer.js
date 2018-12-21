import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { getIn } from "../../app/utils.js";
import {
  filterInVicinity,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

export const goodsTransportOffer = {
  identifier: "transportOffer",
  label: "Offer goods transport",
  icon: "#ico36_uc_transport_offer",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    content: {
      title: "Transportation offer",
      type: "http://dbpedia.org/resource/Transport",
    },
  },
  details: {
    title: { ...details.title },
    location: { ...details.location },
  },
  seeksDetails: {
    tags: { ...details.tags },
    description: { ...details.description },
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
          `${resultName} a <http://dbpedia.org/resource/Cargo>.`,
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
