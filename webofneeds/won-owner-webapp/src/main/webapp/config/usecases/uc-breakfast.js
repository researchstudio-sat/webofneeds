/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_breakfast from "../../images/won-icons/ico36_uc_breakfast.svg";
import ico36_detail_datetime from "~/images/won-icons/ico36_detail_datetime.svg";
import vocab from "~/app/service/vocab";
import { sparqlQuery, vicinityScoreSubQuery } from "~/app/sparql-builder-utils";
import won from "~/app/service/won";
import { getIn } from "~/app/utils";

export const breakfastPlan = {
  identifier: "breakfastPlan",
  label: "Plan Breakfast!",
  icon: ico36_detail_datetime,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:PlanAction"],
        eventObjectAboutUris: "http://www.wikidata.org/entity/Q80973",
        sockets: {
          "#groupSocket": vocab.GROUP.GroupSocketCompacted,
          "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
          "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.GROUP.GroupSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["breakfastInterest"],
      },
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["breakfastPlan"],
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    fromDatetime: { ...details.fromDatetime },
  },
  seeksDetails: {},

  generateQuery: (draft, resultName) => {
    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?location_geoScore",
      pathToGeoCoords: "s:location/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        con: won.defaultContext["con"],
      },
      geoCoordinates: getIn(draft, ["content", "location"]),
    });

    const subQueries = [vicinityScoreSQ]
      .filter(sq => sq) // filter out non-existing details (the SQs should be `undefined` for them)
      .map(sq => ({
        query: sq,
        optional: true, // so counterparts without that detail don't get filtered out (just assigned a score of 0 via `coalesce`)
      }));

    const query = sparqlQuery({
      prefixes: {
        won: won.defaultContext["won"],
        rdf: won.defaultContext["rdf"],
        buddy: won.defaultContext["buddy"],
        hold: won.defaultContext["hold"],
        s: won.defaultContext["s"],
        match: won.defaultContext["match"],
        demo: won.defaultContext["demo"],
      },
      distinct: true,
      variables: [resultName, "?score"],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type demo:Interest.`,
        `${resultName} s:object ?planObject.`,
        `?planObject s:about <http://www.wikidata.org/entity/Q80973>.`,
        `?thisAtom hold:heldBy/buddy:buddy/hold:holds ${resultName}.`,
        `BIND( ( 
          COALESCE(?location_geoScore, 0) 
        ) / 5  as ?score)`,
        // `FILTER(?score > 0)`, // not necessary atm to filter; there are parts of -postings we can't match yet (e.g. NLP on description). also content's sparse anyway.
      ],
      orderBy: [{ order: "DESC", variable: "?score" }],
    });

    return query;
  },
};

export const breakfastInterest = {
  identifier: "breakfastInterest",
  label: "Interest in Lunch",
  icon: ico36_uc_breakfast,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:Interest"],
        eventObjectAboutUris: "http://www.wikidata.org/entity/Q80973",
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["breakfastPlan"],
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
  },
  seeksDetails: {},

  generateQuery: (draft, resultName) => {
    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?location_geoScore",
      pathToGeoCoords: "s:location/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        con: won.defaultContext["con"],
      },
      geoCoordinates: getIn(draft, ["content", "location"]),
    });

    const subQueries = [vicinityScoreSQ]
      .filter(sq => sq) // filter out non-existing details (the SQs should be `undefined` for them)
      .map(sq => ({
        query: sq,
        optional: true, // so counterparts without that detail don't get filtered out (just assigned a score of 0 via `coalesce`)
      }));

    const query = sparqlQuery({
      prefixes: {
        won: won.defaultContext["won"],
        rdf: won.defaultContext["rdf"],
        buddy: won.defaultContext["buddy"],
        hold: won.defaultContext["hold"],
        s: won.defaultContext["s"],
      },
      distinct: true,
      variables: [resultName, "?score"],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type won:Atom.`,
        `${resultName} rdf:type s:PlanAction.`,
        `${resultName} s:object ?planObject.`,
        `?planObject s:about <http://www.wikidata.org/entity/Q80973>.`,
        `?thisAtom hold:heldBy/buddy:buddy/hold:holds ${resultName}.`,
        // calculate average of scores; can be weighed if necessary
        `BIND( (
          COALESCE(?location_geoScore, 0)
        ) / 5  as ?score)`,
        // `FILTER(?score > 0)`, // not necessary atm to filter; there are parts of -postings we can't match yet (e.g. NLP on description). also content's sparse anyway.
      ],
      orderBy: [{ order: "DESC", variable: "?score" }],
    });

    return query;
  },
};
