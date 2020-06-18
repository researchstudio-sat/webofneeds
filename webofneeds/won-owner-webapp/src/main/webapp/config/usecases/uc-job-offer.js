import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../details/jobs.js";
import { jobLocation } from "../details/location.js";
import {
  vicinityScoreSubQuery,
  tagOverlapScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";

import { getIn } from "../../app/utils.js";
import ico36_uc_consortium_offer from "../../images/won-icons/ico36_uc_consortium-offer.svg";

export const jobOffer = {
  identifier: "jobOffer",
  label: "Find people for a Job",
  icon: ico36_uc_consortium_offer, //TODO proper icon
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:JobPosting"],
      },
      seeks: {
        type: ["s:Person"],
      },
    }),
  },
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["jobSearch", "persona"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    jobLocation: { ...jobLocation },
    industry: { ...industryDetail },
    employmentTypes: { ...employmentTypesDetail },
    organizationNames: { ...organizationNamesDetail },
  },
  seeksDetails: {
    description: { ...details.description },
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
  /**
   *
   * e.g.: with just industries:
   * ```
   * # index for industries using binds
   * prefix s: <http://schema.org/>
   * prefix won:   <https://w3id.org/won/core#>
   * select distinct * where {
   *   {
   *     select
   *       ${resultName}
   *       (sum(?var1) + sum(?var2) as ?targetOverlap)
   *       (count(${resultName}) as ?targetTotal)
   *     where {
   *       ${resultName} a won:Atom;
   *             s:industry ?industry .
   *       bind(if(str(?industry) = "design",1,0) as ?var1)
   *       bind(if(str(?industry) = "computer science",1,0) as ?var2)
   *     } group by (${resultName})
   *   }
   *   bind (?targetOverlap / ( ?targetTotal + 2 - ?targetOverlap ) as ?jaccardIndex )
   * } order by desc(?jaccardIndex)
   * limit 100
   * ```
   */
  generateQuery: (draft, resultName) => {
    // skills
    const skillsSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?skills_jaccardIndex",
      pathToTags: "s:knowsAbout",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["seeks", "skills"]),
    });

    // hiringOrganizationName
    const organizationNameSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?organizationName_jaccardIndex",
      pathToTags: "match:seeks/s:hiringOrganization/s:name",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        match: won.defaultContext["match"],
      },
      tagLikes: getIn(draft, ["content", "organizationNames"]),
    });

    // employmentType
    const employmentTypesSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?employmentTypes_jaccardIndex",
      pathToTags: "match:seeks/s:employmentType",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        match: won.defaultContext["match"],
      },
      tagLikes: getIn(draft, ["content", "employmentTypes"]),
    });

    // industry:
    const industryScoreSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?industry_jaccardIndex",
      pathToTags: "match:seeks/s:industry",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        match: won.defaultContext["match"],
      },
      tagLikes: getIn(draft, ["content", "industry"]),
    });

    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?jobLocation_geoScore",
      pathToGeoCoords: "match:seeks/s:jobLocation/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        match: won.defaultContext["match"],
        con: won.defaultContext["con"],
      },
      geoCoordinates: getIn(draft, ["content", "jobLocation"]),
    });

    const subQueries = [
      industryScoreSQ,
      vicinityScoreSQ,
      employmentTypesSQ,
      organizationNameSQ,
      skillsSQ,
    ]
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
      variables: [resultName, "?score"],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type won:Atom.`,
        `${resultName} rdf:type s:Person.`,

        // calculate average of scores; can be weighed if necessary
        `BIND( ( 
          COALESCE(?industry_jaccardIndex, 0) + 
          COALESCE(?skills_jaccardIndex, 0) + 
          COALESCE(?organizationName_jaccardIndex, 0) + 
          COALESCE(?employmentTypes_jaccardIndex, 0) + 
          COALESCE(?jobLocation_geoScore, 0) 
        ) / 5  as ?score)`,
        // `FILTER(?score > 0)`, // not necessary atm to filter; there are parts of job-postings we can't match yet (e.g. NLP on description). also content's sparse anyway.
      ],
      orderBy: [{ order: "DESC", variable: "?score" }],
    });

    return query;
  },
};
