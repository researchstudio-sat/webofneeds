/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";
import { genresDetail, instrumentsDetail } from "../details/musician.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import {
  vicinityScoreSubQuery,
  tagOverlapScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

import { getIn } from "../../app/utils.js";
import ico36_uc_find_musician from "../../images/won-icons/ico36_uc_find_musician.svg";

export const musicianSearch = {
  identifier: "musicianSearch",
  label: "Find Musician",
  icon: ico36_uc_find_musician,
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "Looking for a Musician!",
        type: ["s:MusicGroup"],
      },
      seeks: {
        type: ["demo:Musician"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["bandSearch", "persona"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    genres: { ...genresDetail },
    images: { ...details.images },
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
      bindScoreAs: "?genre_jaccardIndex",
      pathToTags: "match:seeks/demo:genre",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["content", "genre"]),
    });

    // instruments
    const instrumentsSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?instruments_jaccardIndex",
      pathToTags: "demo:instrument",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["seeks", "instrument"]),
    });

    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?location_geoScore",
      pathToGeoCoords: "match:seeks/s:location/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        match: won.defaultContext["match"],
        con: won.defaultContext["con"],
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
        demo: won.defaultContext["demo"],
      },
      distinct: true,
      variables: [resultName],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type won:Atom.`,
        `${resultName} rdf:type demo:Musician.`,

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
};
