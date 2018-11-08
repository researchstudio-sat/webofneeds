/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import {
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
} from "../details/real-estate.js";
import won from "../../app/won-es6.js";
import {
  genresDetail,
  instrumentsDetail,
  perHourRentRangeDetail,
  perHourRentDetail,
} from "../details/musician.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  filterInVicinity,
  filterFloorSizeRange,
  filterNumericProperty,
  filterRentRange,
  filterRent,
  concatenateFilters,
  vicinityScoreSubQuery,
  tagOverlapScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

import { getIn } from "../../app/utils.js";

export const musicianGroup = {
  identifier: "musiciangroup",
  label: "Artists and Bands",
  icon: undefined,
  useCases: {
    findBand: {
      identifier: "findBand",
      label: "Find Band",
      icon: "#ico36_uc_find_band",
      timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm looking for a band!",
          type: "won:Musician",
        },
        seeks: {
          type: "s:MusicGroup",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        instruments: {
          ...instrumentsDetail,
        },
      },
      seeksDetails: {
        description: { ...details.description },
        genres: { ...genresDetail },
        location: {
          ...details.location,
          mandatory: true,
        },
      },
      generateQuery: (draft, resultName) => {
        // genres
        const genresSQ = tagOverlapScoreSubQuery({
          resultName: resultName,
          bindScoreAs: "?genres_jaccardIndex",
          pathToTags: "won:is/won:genres",
          prefixesInPath: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
          },
          tagLikes: getIn(draft, ["seeks", "genres"]),
        });

        // instruments
        const instrumentsSQ = tagOverlapScoreSubQuery({
          resultName: resultName,
          bindScoreAs: "?instruments_jaccardIndex",
          pathToTags: "won:seeks/won:instruments",
          prefixesInPath: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
          },
          tagLikes: getIn(draft, ["content", "instruments"]),
        });

        const vicinityScoreSQ = vicinityScoreSubQuery({
          resultName: resultName,
          bindScoreAs: "?location_geoScore",
          pathToGeoCoords: "won:is/s:location/s:geo",
          prefixesInPath: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
          },

          geoCoordinates: getIn(draft, ["seeks", "location"]),
        });

        const subQueries = [genresSQ, instrumentsSQ, vicinityScoreSQ]
          .filter(sq => sq) // filter out non-existing details (the SQs should be `undefined` for them)
          .map(sq => ({
            query: sq,
            optional: true, // so counterparts without that detail don't get filtered out (just assigned a score of 0 via `coalesce`)
          }));

        const query = sparqlQuery({
          prefixes: {
            won: won.defaultContext["won"],
            rdf: won.defaultContext["rdf"],
            s: won.defaultContext["s"],
          },
          distinct: true,
          variables: [resultName],
          subQueries: subQueries,
          where: [
            `${resultName} rdf:type won:Need.`,
            `${resultName} won:is/rdf:type s:MusicGroup.`,

            // calculate average of scores; can be weighed if necessary
            `BIND( ( 
              COALESCE(?instruments_jaccardIndex, 0) + 
              COALESCE(?genres_jaccardIndex, 0) + 
              COALESCE(?location_geoScore, 0) 
            ) / 3  as ?aggregatedScore)`,
            //`FILTER(?aggregatedScore > 0)`,
          ],
          orderBy: [{ order: "DESC", variable: "?aggregatedScore" }],
        });

        return query;
      },
    },
    findMusician: {
      identifier: "findMusician",
      label: "Find Musician",
      icon: "#ico36_uc_find_musician",
      timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "Looking for a Musician!",
          type: "s:MusicGroup",
        },
        seeks: {
          type: "won:Musician",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        genres: { ...genresDetail },
        location: {
          ...details.location,
          mandatory: true,
        },
      },
      seeksDetails: {
        description: { ...details.description },
        instruments: {
          ...instrumentsDetail,
        },
      },
      generateQuery: (draft, resultName) => {
        // genres
        const genresSQ = tagOverlapScoreSubQuery({
          resultName: resultName,
          bindScoreAs: "?genres_jaccardIndex",
          pathToTags: "won:seeks/won:genres",
          prefixesInPath: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
          },
          tagLikes: getIn(draft, ["content", "genres"]),
        });

        // instruments
        const instrumentsSQ = tagOverlapScoreSubQuery({
          resultName: resultName,
          bindScoreAs: "?instruments_jaccardIndex",
          pathToTags: "won:is/won:instruments",
          prefixesInPath: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
          },
          tagLikes: getIn(draft, ["seeks", "instruments"]),
        });

        const vicinityScoreSQ = vicinityScoreSubQuery({
          resultName: resultName,
          bindScoreAs: "?location_geoScore",
          pathToGeoCoords: "won:seeks/s:location/s:geo",
          prefixesInPath: {
            s: won.defaultContext["s"],
            won: won.defaultContext["won"],
          },
          geoCoordinates: getIn(draft, ["content", "location"]),
        });

        const subQueries = [genresSQ, instrumentsSQ, vicinityScoreSQ]
          .filter(sq => sq) // filter out non-existing details (the SQs should be `undefined` for them)
          .map(sq => ({
            query: sq,
            optional: true, // so counterparts without that detail don't get filtered out (just assigned a score of 0 via `coalesce`)
          }));

        const query = sparqlQuery({
          prefixes: {
            won: won.defaultContext["won"],
            rdf: won.defaultContext["rdf"],
            s: won.defaultContext["s"],
          },
          distinct: true,
          variables: [resultName],
          subQueries: subQueries,
          where: [
            `${resultName} rdf:type won:Need.`,
            `${resultName} won:is/rdf:type won:Musician.`,

            // calculate average of scores; can be weighed if necessary
            `BIND( ( 
              COALESCE(?instruments_jaccardIndex, 0) + 
              COALESCE(?genres_jaccardIndex, 0) + 
              COALESCE(?location_geoScore, 0) 
            ) / 3  as ?aggregatedScore)`,
            //`FILTER(?aggregatedScore > 0)`,
          ],
          orderBy: [{ order: "DESC", variable: "?aggregatedScore" }],
        });

        return query;
      },
    },
    findRehearsalRoom: {
      identifier: "findRehearsalRoom",
      label: "Find Rehearsal Room",
      icon: "#ico36_uc_realestate",
      timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 7,
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          type: "won:RehearsalRoomRentDemand",
          title: "Looking for Rehearsal Room!",
          searchString: "Rehearsal Room",
        },
        seeks: {
          type: "won:RehearsalRoomRentOffer",
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
        rentRange: { ...perHourRentRangeDetail },
        fromDatetime: { ...details.fromDatetime },
        throughDatetime: { ...details.throughDatetime },
      },
      generateQuery: (draft, resultName) => {
        const seeksBranch = draft && draft.seeks;
        const rentRange = seeksBranch && seeksBranch.rentRange;
        const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
        const location = seeksBranch && seeksBranch.location;

        const filters = [
          {
            // to select is-branch
            prefixes: {
              won: won.defaultContext["won"],
              rdf: won.defaultContext["rdf"],
            },
            operations: [
              `${resultName} a won:Need.`,
              `${resultName} won:is ?is.`,
              `?is rdf:type won:RehearsalRoomRentOffer.`,
              location && "?is won:hasLocation ?location.",
            ],
          },
          rentRange &&
            filterRentRange(
              "?is",
              rentRange.min,
              rentRange.max,
              rentRange.currency
            ),

          floorSizeRange &&
            filterFloorSizeRange("?is", floorSizeRange.min, floorSizeRange.max),

          filterInVicinity("?location", location),
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
    offerRehearsalRoom: {
      identifier: "OfferRehearsalRoom",
      label: "Offer Rehearsal Room",
      icon: "#ico36_uc_realestate",
      timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          type: "won:RehearsalRoomRentOffer",
          title: "Offer Rehearsal Room!",
        },
        seeks: {
          type: "won:RehearsalRoomRentDemand",
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
        features: {
          ...realEstateFeaturesDetail,
          placeholder: "e.g. PA, Drumkit",
        },
        rent: {
          ...perHourRentDetail,
          mandatory: true,
        },
        fromDatetime: { ...details.fromDatetime },
        throughDatetime: { ...details.throughDatetime },
      },

      seeksDetails: undefined,
      generateQuery: (draft, resultName) => {
        const draftContent = draft && draft.content;
        const location = draftContent && draftContent.location;
        const rent = draftContent && draftContent.rent;
        const floorSize = draftContent && draftContent.floorSize;

        const filters = [
          {
            // to select is-branch
            prefixes: {
              won: won.defaultContext["won"],
              rdf: won.defaultContext["rdf"],
              sh: won.defaultContext["sh"], //needed for the filterNumericProperty calls
            },
            operations: [
              `${resultName} a won:Need.`,
              `${resultName} won:seeks ?seeks.`,
              `${resultName} won:is/rdf:type won:RehearsalRoomRentDemand.`,
              location && "?seeks won:hasLocation ?location.",
            ],
          },
          rent && filterRent("?seeks", rent.amount, rent.currency, "rent"),
          floorSize &&
            filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
          filterInVicinity("?location", location),
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
  },
};
