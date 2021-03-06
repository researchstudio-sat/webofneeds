/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import {
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
} from "../details/real-estate.js";
import vocab from "../../app/service/vocab.js";
import { perHourRentRangeDetail } from "../details/musician.js";
import {
  filterFloorSizeRange,
  filterPriceRange,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_realestate from "../../images/won-icons/ico36_uc_realestate.svg";

export const rehearsalRoomSearch = {
  identifier: "rehearsalRoomSearch",
  label: "Find Rehearsal Room",
  icon: ico36_uc_realestate,
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 7,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:RehearsalRoomRentDemand"],
        searchString: "Rehearsal Room",
      },
      seeks: {
        type: ["demo:RehearsalRoomRentOffer"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["rehearsalRoomOffer"],
        refuseOwned: true,
      },
    },
  },
  details: undefined,
  seeksDetails: {
    location: { ...details.location },
    floorSizeRange: { ...realEstateFloorSizeRangeDetail },
    features: {
      ...realEstateFeaturesDetail,
      placeholder: "e.g. PA, Drumkit",
    },
    priceRange: { ...perHourRentRangeDetail },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
  },
  generateQuery: (draft, resultName) => {
    const seeksBranch = draft && draft.seeks;
    const rentRange = seeksBranch && seeksBranch.priceRange;
    const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
    const location = seeksBranch && seeksBranch.location;

    let filter;
    if (location && location.lat && location.lng) {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: vocab.defaultContext["won"],
            rdf: vocab.defaultContext["rdf"],
            sh: vocab.defaultContext["sh"], //needed for the filterNumericProperty calls
            s: vocab.defaultContext["s"],
            geo: "http://www.bigdata.com/rdf/geospatial#",
            xsd: vocab.defaultContext["xsd"],
            demo: vocab.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RehearsalRoomRentOffer.`,
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
      ];

      filter = concatenateFilters(filters);
    } else {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: vocab.defaultContext["won"],
            rdf: vocab.defaultContext["rdf"],
            demo: vocab.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RehearsalRoomRentOffer.`,
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
      ];

      filter = concatenateFilters(filters);
    }

    return sparqlQuery({
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
  },
};
