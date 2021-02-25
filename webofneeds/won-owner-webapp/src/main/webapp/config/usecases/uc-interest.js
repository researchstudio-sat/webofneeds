import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
// import {
//   vicinityScoreSubQuery,
//   sparqlQuery,
// } from "../../app/sparql-builder-utils.js";
// import { getIn } from "../../app/utils.js";
// import vocab from "../../app/service/vocab.js";
import vocab from "../../app/service/vocab.js";
import ico36_detail_datetime from "~/images/won-icons/ico36_detail_datetime.svg";
import ico36_detail_interests from "~/images/won-icons/ico36_detail_interests.svg";
import * as jsonLdUtils from "~/app/service/jsonld-utils";

export const genericPlan = {
  identifier: "genericPlan",
  label: "Plan",
  icon: ico36_detail_datetime,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:PlanAction"],
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
        useCaseIdentifiers: ["genericInterest", "persona"],
      },
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["genericPlan"],
      },
    },
  },
  details: {
    title: { ...details.title },
    eventObjectAboutUris: { ...details.eventObjectAboutUris, mandatory: true },
    description: { ...details.description },
    location: { ...details.location, mandatory: true },
    fromDatetime: { ...details.fromDatetime },
  },
  seeksDetails: {},

  /*generateQuery: (draft, resultName) => {
    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?location_geoScore",
      pathToGeoCoords: "s:location/s:geo",
      prefixesInPath: {
        s: vocab.defaultContext["s"],
        won: vocab.defaultContext["won"],
        con: vocab.defaultContext["con"],
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
        won: vocab.defaultContext["won"],
        rdf: vocab.defaultContext["rdf"],
        buddy: vocab.defaultContext["buddy"],
        hold: vocab.defaultContext["hold"],
        s: vocab.defaultContext["s"],
        match: vocab.defaultContext["match"],
        demo: vocab.defaultContext["demo"],
        "wx-persona": vocab.defaultContext["wx-persona"],
      },
      distinct: true,
      variables: [resultName, "?score"],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type wx-persona:Interest.`,
        `${resultName} s:object ?planObject.`,
        `?planObject s:about <http://www.wikidata.org/entity/Q12896105>.`,
        `?thisAtom hold:heldBy/buddy:buddy/hold:holds ${resultName}.`,
        `BIND( ( 
          COALESCE(?location_geoScore, 0) 
        ) / 5  as ?score)`,
        // `FILTER(?score > 0)`, // not necessary atm to filter; there are parts of -postings we can't match yet (e.g. NLP on description). also content's sparse anyway.
      ],
      orderBy: [{ order: "DESC", variable: "?score" }],
    });

    return query;
  },*/
};

export const genericInterest = {
  identifier: "genericInterest",
  label: "Interest",
  icon: ico36_detail_interests,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.WXPERSONA.InterestCompacted],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#interestOfSocket": vocab.WXPERSONA.InterestOfSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.GROUP.GroupSocketCompacted]: {
        useCaseIdentifiers: ["genericPlan"],
      },
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["genericInterest", "persona"],
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    eventObjectAboutUris: {
      ...details.eventObjectAboutUris,
      mandatory: true,
    },
    location: {
      ...details.location,
    },
  },
  seeksDetails: {},
  //TODO: Fix Query,
  /*generateQuery: (draft, resultName) => {
    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?location_geoScore",
      pathToGeoCoords: "s:location/s:geo",
      prefixesInPath: {
        s: vocab.defaultContext["s"],
        won: vocab.defaultContext["won"],
        con: vocab.defaultContext["con"],
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
        won: vocab.defaultContext["won"],
        rdf: vocab.defaultContext["rdf"],
        buddy: vocab.defaultContext["buddy"],
        hold: vocab.defaultContext["hold"],
        s: vocab.defaultContext["s"],
      },
      distinct: true,
      variables: [resultName, "?score"],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type won:Atom.`,
        `${resultName} rdf:type s:PlanAction.`,
        `${resultName} s:object ?planObject.`,
        `?planObject s:about <http://www.wikidata.org/entity/Q12896105>.`,
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
  },*/
};
