import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../../details/jobs.js";
import { jobLocation } from "../../details/location.js";
import {
  vicinityScoreSubQuery,
  tagOverlapScoreSubQuery,
} from "../../../app/sparql-builder-utils.js";

import { getIn } from "../../../app/utils.js";

export const jobSearch = {
  identifier: "jobSearch",
  label: "Search a Job",
  // icon: "#ico36_uc_find_people", TODO
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    is: {
      "@type": "s:Person",
      tags: ["search-job"],
    },
    seeks: {
      "@type": "s:JobPosting",
    },
    searchString: ["offer-job"],
  },
  isDetails: {
    title: { ...details.title },
    description: { ...details.description },
    // location: { ...details.location }, // why would your current location of residency matter?
    // person: { ...details.person }, // has current company fields, that are weird for a search
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
  seeksDetails: {
    description: { ...details.description },
    jobLocation: { ...jobLocation },
    industry: { ...industryDetail },
    employmentTypes: { ...employmentTypesDetail },
    organizationNames: { ...organizationNamesDetail },
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
   *             won:is ?is.
   *             ?is s:industry ?industry .
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
    // const pathInDraft = ["is", "skills"];
    // const sparqlPredicatePath = "won:seeks/s:knowsAbout";
    // const sparqlVarName = "?skills";

    // hiringOrganizationName
    // const pathInDraft = ["seeks", "organizationNames"];
    // const sparqlPredicatePath = "won:is/s:hiringOrganization/s:name";
    // const sparqlVarName = "?organizationName";

    // employmentType
    // const pathInDraft = ["seeks", "employmentTypes"];
    // const sparqlPredicatePath = "won:is/s:employmentType";
    // const sparqlVarName = "?employmentType";

    // industry:
    const industries = getIn(draft, ["seeks", "industry"]);
    const industryScoreSQ = tagOverlapScoreSubQuery(
      resultName,
      "?industry_jaccardIndex",
      "won:is/s:industry",
      industries
    );

    const jobLocation = getIn(draft, ["seeks", "jobLocation"]); // TODO move to better place
    const vicinityScoreSQ = vicinityScoreSubQuery(
      resultName,
      "?jobLocation_geoScore",
      "won:is/s:jobLocation",
      jobLocation
    );

    console.log(
      "job location filter deleteme 2: ",
      vicinityScoreSQ,
      industryScoreSQ
    );

    // console.log(draft, resultName, subQuery, tagLikes, query, "deleteme");

    //TODO: STOPPED HERE
    // use `subQueries` field of args to `sparqlQuery` to combine the queries

    // return query;
    throw new Error("NOT IMPLEMENTED YET");
  },
};
