/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import {
  realEstateRentRangeDetail,
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
  realEstateNumberOfRoomsRangeDetail,
} from "../details/real-estate.js";

import {
  filterFloorSizeRange,
  filterNumOfRoomsRange,
  filterPriceRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_realestate from "../../images/won-icons/ico36_uc_realestate.svg";

export const rentRealEstateSearch = {
  identifier: "rentRealEstateSearch",
  label: "Find a place to rent",
  icon: ico36_uc_realestate,
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      seeks: {
        type: ["demo:RealEstateRentOffer"],
        tags: ["RentOutRealEstate"],
      },
      content: {
        type: ["demo:RealEstateRentDemand"],
        tags: ["SearchRealEstateToRent"],
      },
    }),
  },
  reactionUseCases: [
    {
      identifier: "rentRealEstateOffer",
      senderSocketType: vocab.CHAT.ChatSocketCompacted,
      targetSocketType: vocab.CHAT.ChatSocketCompacted,
    },
  ],
  details: undefined,
  seeksDetails: {
    location: { ...details.location },
    floorSizeRange: { ...realEstateFloorSizeRangeDetail },
    numberOfRoomsRange: { ...realEstateNumberOfRoomsRangeDetail },
    features: { ...realEstateFeaturesDetail },
    priceRange: { ...realEstateRentRangeDetail },
  },
  generateQuery: (draft, resultName) => {
    const seeksBranch = draft && draft.seeks;
    const rentRange = seeksBranch && seeksBranch.priceRange;
    const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
    const numberOfRoomsRange = seeksBranch && seeksBranch.numberOfRoomsRange;
    const location = seeksBranch && seeksBranch.location;

    let filter;
    if (location && location.lat && location.lng) {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
            xsd: won.defaultContext["xsd"],
            demo: won.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RealEstateRentOffer.`,
            `${resultName} (won:location|s:location) ?location.`,
            "?location s:geo ?location_geo.",
            "?location_geo s:latitude ?location_lat;",
            "s:longitude ?location_lon;",
            `bind (abs(xsd:decimal(?location_lat) - ${
              location.lat
            }) as ?latDiffRaw)`,
            `bind (abs(xsd:decimal(?location_lon) - ${
              location.lng
            }) as ?lonDiff)`,
            "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
            "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
            "bind (?location_geoDistanceScore as ?distScore)",
          ],
        },
        rentRange &&
          filterPriceRange(
            `${resultName}`,
            rentRange.min,
            rentRange.max,
            rentRange.currency
          ),

        floorSizeRange &&
          filterFloorSizeRange(
            `${resultName}`,
            floorSizeRange.min,
            floorSizeRange.max
          ),

        numberOfRoomsRange &&
          filterNumOfRoomsRange(
            `${resultName}`,
            numberOfRoomsRange.min,
            numberOfRoomsRange.max
          ),
      ];

      filter = concatenateFilters(filters);
    } else {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
            s: won.defaultContext["s"],
            demo: won.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RealEstateRentOffer.`,
          ],
        },
        rentRange &&
          filterPriceRange(
            `${resultName}`,
            rentRange.min,
            rentRange.max,
            rentRange.currency
          ),

        floorSizeRange &&
          filterFloorSizeRange(
            `${resultName}`,
            floorSizeRange.min,
            floorSizeRange.max
          ),

        numberOfRoomsRange &&
          filterNumOfRoomsRange(
            `${resultName}`,
            numberOfRoomsRange.min,
            numberOfRoomsRange.max
          ),
      ];

      filter = concatenateFilters(filters);
    }

    const generatedQuery = sparqlQuery({
      prefixes: filter.prefixes,
      distinct: true,
      variables: [resultName],
      where: filter.operations,
      orderBy: [
        {
          order: "ASC",
          variable: "?distScore",
        },
      ],
    });

    return generatedQuery;
  },
};
