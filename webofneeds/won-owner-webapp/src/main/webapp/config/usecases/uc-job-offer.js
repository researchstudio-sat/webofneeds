import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
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
import won from "../../app/won-es6.js";

import { getIn } from "../../app/utils.js";

export const jobOffer = {
  identifier: "jobOffer",
  label: "Find people for a Job",
  icon: "#ico36_uc_consortium-offer", //TODO proper icon
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
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
   * prefix won:   <http://purl.org/webofneeds/model#>
   * select distinct * where {
   *   {
   *     select
   *       ${resultName}
   *       (sum(?var1) + sum(?var2) as ?targetOverlap)
   *       (count(${resultName}) as ?targetTotal)
   *     where {
   *       ${resultName} a won:Need;
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
      pathToTags: "won:seeks/s:hiringOrganization/s:name",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["content", "organizationNames"]),
    });

    // employmentType
    const employmentTypesSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?employmentTypes_jaccardIndex",
      pathToTags: "won:seeks/s:employmentType",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["content", "employmentTypes"]),
    });

    // industry:
    const industryScoreSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?industry_jaccardIndex",
      pathToTags: "won:seeks/s:industry",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["content", "industry"]),
    });

    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?jobLocation_geoScore",
      pathToGeoCoords: "won:seeks/s:jobLocation/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
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
        `${resultName} rdf:type won:Need.`,
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
