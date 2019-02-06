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

export const personalTransportSearch = {
  identifier: "personalTransportSearch",
  label: "Need a Lift",
  icon: "#ico36_uc_route_demand",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "Need a lift",
        type: ["won:PersonalTransportSearch"],
      },
    }),
  },
  // TODO: amount of people? other details?
  details: {
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
        s: won.defaultContext["s"],
      },
      operations: [
        `${resultName} a won:Need.`,
        `${resultName} won:isInState won:Active. { { ${resultName} a <http://dbpedia.org/resource/Ridesharing>.  } union { ${resultName} a s:TaxiService} }`,
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
              `${resultName} a <http://dbpedia.org/resource/Ridesharing>. ${resultName} won:travelAction/s:fromLocation ?fromLocation. `,
            fromLocation && fromLocationFilter.operations.join(" "),
            toLocation &&
              `${resultName} won:travelAction/s:toLocation ?toLocation.`,
            toLocation && toLocationFilter.operations.join(" "),
          ],
          " "
        ),
        filterAndJoin(
          [
            location &&
              `${resultName} a s:TaxiService . ${resultName} (s:location|won:hasLocation) ?location .`,
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
    });
  },
};
