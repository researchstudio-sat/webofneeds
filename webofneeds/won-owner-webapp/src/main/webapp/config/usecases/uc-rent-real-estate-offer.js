/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import {
  realEstateRentDetail,
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
} from "../details/real-estate.js";

import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import {
  filterNumericProperty,
  filterPrice,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_realestate from "../../images/won-icons/ico36_uc_realestate.svg";

export const rentRealEstateOffer = {
  identifier: "rentRealEstateOffer",
  label: "Rent a place out",
  icon: ico36_uc_realestate,
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:RealEstateRentOffer"],
        tags: ["RentOutRealEstate"],
      },
      seeks: {
        type: ["demo:RealEstateRentDemand"],
        tags: ["SearchRealEstateToRent"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["rentRealEstateSearch"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: {
      ...details.location,
      mandatory: true,
    },
    floorSize: {
      ...realEstateFloorSizeDetail,
      mandatory: true,
    },
    numberOfRooms: {
      ...realEstateNumberOfRoomsDetail,
      mandatory: true,
    },
    features: { ...realEstateFeaturesDetail },
    rent: {
      ...realEstateRentDetail,
      mandatory: true,
    },
    images: { ...details.images },
    files: { ...details.files },
  },
  seeksDetails: undefined,
  generateQuery: (draft, resultName) => {
    const draftContent = draft && draft.content;
    const location = draftContent && draftContent.location;
    const rent = draftContent && draftContent.rent;
    const numberOfRooms = draftContent && draftContent.numberOfRooms;
    const floorSize = draftContent && draftContent.floorSize;

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
            xsd: "http://www.w3.org/2001/XMLSchema#",
            demo: vocab.defaultContext["demo"],
            match: vocab.defaultContext["match"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RealEstateRentDemand.`,
            `${resultName} match:seeks ?seeks.`,
            "?seeks (won:location|s:location) ?location.",
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
        rent && filterPrice("?seeks", rent.amount, rent.currency, "rent"),
        floorSize &&
          filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
        numberOfRooms &&
          filterNumericProperty(
            "?seeks",
            numberOfRooms,
            "s:numberOfRooms",
            "rooms"
          ),
      ];

      filter = concatenateFilters(filters);
    } else {
      //Location is set to mandatory, hence this clause will never get called
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: vocab.defaultContext["won"],
            sh: vocab.defaultContext["sh"], //needed for the filterNumericProperty calls
            demo: vocab.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} a demo:RealEstateRentDemand.``${resultName} match:seeks ?seeks.`,
          ],
        },
        rent && filterPrice("?seeks", rent.amount, rent.currency, "rent"),
        floorSize &&
          filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
        numberOfRooms &&
          filterNumericProperty(
            "?seeks",
            numberOfRooms,
            "s:numberOfRooms",
            "rooms"
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
