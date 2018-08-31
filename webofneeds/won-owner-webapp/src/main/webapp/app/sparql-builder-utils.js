/**
 * Module for utility-functions for string-building sparql-queries.
 * NOTE: This is a super-hacky/-fragile approach and should be replaced by a proper lib / ast-utils
 */

import won from "./won-es6.js";

/**
 *
 * @param {*} location: an object containing `lat` and `lng`
 * @param {Number} radius: distance in km that matches can be away from the location
 * @returns an object a `filterString` and `prefixes` used in the former.
 */
export function filterInVicinity(location, radius = 10) {
  if (!location || !location.lat || !location.lng) {
    return { prefixes: {}, filter: "" };
  } else {
    return {
      prefixes: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        geo: "http://www.bigdata.com/rdf/geospatial#",
        geoliteral: "http://www.bigdata.com/rdf/geospatial/literals/v1#",
      },
      filterString: `?result won:is/won:hasLocation/s:geo ?geo
SERVICE geo:search {
  ?geo geo:search "inCircle" .
  ?geo geo:searchDatatype geoliteral:lat-lon .
  ?geo geo:predicate won:geoSpatial .
  ?geo geo:spatialCircleCenter "${location.lat}#${location.lng}" .
  ?geo geo:spatialCircleRadius "${radius}" .
  ?geo geo:distanceValue ?geoDistance .
}`,
    };
  }
}

/**
 * @param {*} prefixes an object which' keys are the prefixes
 *  and values the long-form URIs.
 * @returns {String} in the form of e.g.
 * ```
 * prefix s: <http://schema.org/>
 * prefix won: <http://purl.org/webofneeds/model#>
 * ```
 */
export function prefixesString(prefixes) {
  if (!prefixes) {
    return "";
  } else {
    const prefixesStrings = Object.entries(prefixes).map(
      ([prefix, uri]) => `prefix ${prefix}: <${uri}>\n`
    );
    return prefixesStrings.join("");
  }
}
//TODO should return a context-def as well
