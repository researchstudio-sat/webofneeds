/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import {
  realEstateRentRangeDetail,
  realEstateRentDetail,
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
  realEstateFloorSizeRangeDetail,
} from "../details/real-estate.js";
import { genresDetail, instrumentsDetail } from "../details/musician.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import {
  vicinityScoreSubQuery,
  tagOverlapScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

import won from "../../app/won-es6.js";

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
        is: {
          title: "I'm looking for a band!",
          //tags: ["musician", "band"],
        },
        searchString: "band",
      },
      isDetails: {
        title: { ...details.title },
        description: { ...details.description },
        instruments: {
          ...instrumentsDetail,
          //mandatory: true,
        },
      },
      seeksDetails: {
        description: { ...details.description },
        genres: { ...genresDetail },
        location: { ...details.location },
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
          tagLikes: getIn(draft, ["is", "instruments"]),
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
            `${resultName} won:is/rdf:type s:Person.`,

            // calculate average of scores; can be weighed if necessary
            `BIND( ( 
              COALESCE(?instruments_jaccardIndex, 0) + 
              COALESCE(?genres_jaccardIndex, 0) + 
              COALESCE(?location_geoScore, 0) 
            ) / 3  as ?aggregatedScore)`,
            `FILTER(?aggregatedScore > 0)`,
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
        is: {
          title: "Looking for a Musician!",
          //tags: ["band", "musician"],
        },
        searchString: "musician",
      },
      isDetails: {
        title: { ...details.title },
        description: { ...details.description },
        genres: { ...genresDetail },
        location: { ...details.location },
      },
      seeksDetails: {
        description: { ...details.description },
        instruments: {
          ...instrumentsDetail,
          //mandatory: true,
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
          tagLikes: getIn(draft, ["is", "genres"]),
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
          geoCoordinates: getIn(draft, ["is", "location"]),
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
            `${resultName} won:is/rdf:type s:Person.`,

            // calculate average of scores; can be weighed if necessary
            `BIND( ( 
              COALESCE(?instruments_jaccardIndex, 0) + 
              COALESCE(?genres_jaccardIndex, 0) + 
              COALESCE(?location_geoScore, 0) 
            ) / 3  as ?aggregatedScore)`,
            `FILTER(?aggregatedScore > 0)`,
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
        is: {
          title: "Looking for Rehearsal Room!",
          tags: ["SearchRehearsal"],
        },
        seeks: {
          tags: ["OfferRehearsal"],
        },
        searchString: "Rehearsal Room",
      },
      isDetails: undefined,
      seeksDetails: {
        location: { ...details.location },
        floorSizeRange: { ...realEstateFloorSizeRangeDetail },
        features: {
          ...realEstateFeaturesDetail,
          placeholder: "e.g. PA, Drumkit",
        },
        rentRange: { ...realEstateRentRangeDetail },
        fromDatetime: { ...details.fromDatetime },
        throughDatetime: { ...details.throughDatetime },
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
        is: {
          title: "Offer Rehearsal Room!",
          tags: ["OfferRehearsal"],
        },
        seeks: {
          tags: ["SearchRehearsal"],
        },
      },
      isDetails: {
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
          ...realEstateRentDetail,
          mandatory: true,
        },
        fromDatetime: { ...details.fromDatetime },
        throughDatetime: { ...details.throughDatetime },
      },

      seeksDetails: undefined,
    },
  },
};
