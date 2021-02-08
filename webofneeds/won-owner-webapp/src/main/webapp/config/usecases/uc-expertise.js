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
// import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";
// import ico36_detail_datetime from "~/images/won-icons/ico36_detail_datetime.svg";
import ico36_detail_interests from "~/images/won-icons/ico36_detail_interests.svg";
// import * as jsonLdUtils from "~/app/service/jsonld-utils";

export const genericExpertise = {
  identifier: "genericExpertise",
  label: "Expertise",
  icon: ico36_detail_interests,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["wx-persona:Expertise"],
        sockets: {
          "#chatSocket": vocab.CHAT.ChatSocketCompacted,
          "#expertiseOfSocket": vocab.WXPERSONA.ExpertiseOfSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    // [vocab.CHAT.ChatSocketCompacted]: {
    //   [vocab.GROUP.GroupSocketCompacted]: {
    //     useCaseIdentifiers: ["genericPlan"],
    //   },
    // },
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
