/**
 * Created by ksinger on 11.08.2016.
 */

import L from "./leaflet-bundleable.js";
import {
  arrEq,
  checkHttpStatus,
  generateIdString,
  getIn,
  getRandomString,
  findAllFieldOccurancesRecursively,
  is,
  endOfXsdDateInterval,
} from "./utils.js";

import { ownerBaseUrl } from "config";
import urljoin from "url-join";
import { useCases } from "useCaseDefinitions";

import jsonld from "jsonld";
window.jsonld4dbg = jsonld;

import Immutable from "immutable";
import { get, parseXsdDateTime, isValidDate } from "./utils.js";

export function initLeaflet(mapMount) {
  if (!L) {
    throw new Error(
      "Tried to initialize a leaflet widget while leaflet wasn't loaded."
    );
  }
  Error;

  const baseMaps = initLeafletBaseMaps();

  const map = L.map(mapMount, {
    center: [37.44, -42.89], //centered on north-west africa
    zoom: 1, //world-map
    layers: [baseMaps["Detailed default map"]], //initially visible layers
  }); //.setView([51.505, -0.09], 13);

  //map.fitWorld() // shows every continent twice :|
  map.fitBounds([[-80, -190], [80, 190]]); // fitWorld without repetition

  L.control.layers(baseMaps).addTo(map);

  // Force it to adapt to actual size
  // for some reason this doesn't happen by default
  // when the map is within a tag.
  // this.map.invalidateSize();
  // ^ doesn't work (needs to be done manually atm);

  return map;
}

export function initLeafletBaseMaps() {
  if (!L) {
    throw new Error(
      "Tried to initialize leaflet map-sources while leaflet wasn't loaded."
    );
  }
  const secureOsmSource = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"; // secure osm.org
  const secureOsm = L.tileLayer(secureOsmSource, {
    attribution:
      '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
  });

  const transportSource =
    "http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png";
  const transport = L.tileLayer(transportSource, {
    attribution:
      'Maps &copy; <a href="http://www.thunderforest.com">Thunderforest</a>, Data &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
  });

  const baseMaps = {
    "Detailed default map": secureOsm,
    "Transport (Insecurely loaded!)": transport,
  };

  return baseMaps;
}

export function selectTimestamp(event) {
  /*
     * the "outer" event is from our own event
     * container. The receivedTimestamp there
     * should have been placed by our own node.
     *
     * The error are events that haven't
     * been confirmed yet. They don't have a
     * received timestamp, as these are optimistic
     * assumptions with only sent timestamps.
     */
  return event.get("hasReceivedTimestamp") || event.get("hasSentTimestamp");
}

/**
 * Makes sure the select-statement is reevaluated, should
 * one of the watched fields change.
 *
 * example usage:
 * ```
 * reduxSelectDependsOnProperties(['self.needUri', 'self.timestamp'], selectFromState, this)
 * ```
 *
 * @param properties a list of watch expressions
 * @param selectFromState same as $ngRedux.connect
 * @param ctrl the controller to bind the results to. needs to have `$ngRedux` and `$scope` attached.
 * @returns {*}
 * @returns a function to unregister the watch
 */
export function reduxSelectDependsOnProperties(
  properties,
  selectFromState,
  ctrl
) {
  const firstVals = properties.map(p => getIn(ctrl.$scope, p.split(".")));
  let firstTime = true;
  return ctrl.$scope.$watchGroup(properties, (newVals, oldVals) => {
    if ((firstTime && !arrEq(newVals, firstVals)) || !arrEq(newVals, oldVals)) {
      const state = ctrl.$ngRedux.getState();
      const stateSlice = selectFromState(state);
      Object.assign(ctrl, stateSlice);
    }
    if (firstTime) {
      firstTime = false;
    }
  });
}

/**
 * Connects a component to ng-redux, sets up watches for the
 * properties that `selectFromState` depends on and handles
 * cleanup when the component is destroyed.
 * @param selectFromState
 * @param actionCreators
 * @param properties
 * @param ctrl a controller/component with `$scope` and `$ngRedux` attached
 */
export function connect2Redux(
  selectFromState,
  actionCreators,
  properties,
  ctrl
) {
  const disconnectRdx = ctrl.$ngRedux.connect(
    selectFromState,
    actionCreators
  )(ctrl);
  const disconnectProps = reduxSelectDependsOnProperties(
    properties,
    selectFromState,
    ctrl
  );
  ctrl.$scope.$on("$destroy", () => {
    disconnectRdx();
    disconnectProps();
  });
}

/**
 * Checks whether the user has a logged-in session.
 * Returns a promise with the user-object if successful
 * or a failing promise if an error has occured.
 *
 * @returns {*}
 */
export function checkLoginStatus() {
  return fetch("rest/users/isSignedIn", { credentials: "include" })
    .then(checkHttpStatus) // will reject if not logged in
    .then(resp => resp.json());
}

/**
 * Registers the account with the server.
 * The returned promise fails if something went
 * wrong during creation.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export function registerAccount(credentials) {
  const { email, password } = parseCredentials(credentials);
  const url = urljoin(ownerBaseUrl, "/rest/users/");
  const httpOptions = {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ username: email, password: password }),
  };
  return fetch(url, httpOptions).then(checkHttpStatus);
}

/**
 * Transfer an existing privateId User,
 * to a non existing User
 * @param credentials {email, password, privateId}
 * @returns {*}
 */
export function transferPrivateAccount(credentials) {
  const { email, password, privateId } = credentials;
  const privateUsername = privateId2Credentials(privateId).email;
  const privatePassword = privateId2Credentials(privateId).password;

  return fetch("/owner/rest/users/transfer", {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      username: email,
      password: password,
      privateUsername: privateUsername,
      privatePassword: privatePassword,
    }),
  }).then(checkHttpStatus);
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export function login(credentials) {
  const { email, password, rememberMe } = parseCredentials(credentials);
  const loginUrl = urljoin(ownerBaseUrl, "/rest/users/signin");
  const params =
    "username=" +
    encodeURIComponent(email) +
    "&password=" +
    encodeURIComponent(password) +
    (rememberMe ? "&remember-me=true" : "");

  return fetch(loginUrl, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
    },
    body: params,
    credentials: "include",
  }).then(checkHttpStatus);
}

export function logout() {
  const url = urljoin(ownerBaseUrl, "/rest/users/signout");
  const httpOptions = {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({}),
  };
  return fetch(url, httpOptions).then(checkHttpStatus);
}

/**
 * Generates a privateId of `[usernameFragment]-[password]`
 * @returns {string}
 */
export function generatePrivateId() {
  return generateIdString(8) + "-" + generateIdString(8); //<usernameFragment>-<password>
}

/**
 * Parses a given privateId into a fake email address and a password.
 * @param privateId
 * @returns {{email: string, password: *}}
 */
export function privateId2Credentials(privateId) {
  const [usernameFragment, password] = privateId.split("-");
  const email = usernameFragment + "@matchat.org";
  return {
    email,
    password,
  };
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {email, password}
 */
export function parseCredentials(credentials) {
  return credentials.privateId
    ? privateId2Credentials(credentials.privateId)
    : credentials;
}

export function getRandomWonId() {
  // needs to start with a letter, so N3 doesn't run into
  // problems when serializing, see
  // https://github.com/RubenVerborgh/N3.js/issues/121
  return (
    getRandomString(1, "abcdefghijklmnopqrstuvwxyz") +
    getRandomString(11, "abcdefghijklmnopqrstuvwxyz0123456789")
  );
}
/**
 * Returns all the details that are defined in any useCase Defined in the useCaseDefinitions
 */
export function getAllDetails() {
  let details = {};

  if (hasSubElements(useCases)) {
    for (const useCaseKey in useCases) {
      const useCase = useCases[useCaseKey];
      if (useCase) {
        const isDetails = useCase.isDetails ? useCase.isDetails : {};
        const seeksDetails = useCase.seeksDetails ? useCase.seeksDetails : {};
        details = { ...details, ...isDetails, ...seeksDetails };
      }
    }
  }
  return details;
}

function hasSubElements(obj) {
  return obj && obj !== {} && Object.keys(obj).length > 0;
}

export function findLatestIntervallEndInJsonLd(draft, jsonld) {
  // get all occurrances of `dc:time`, `dc:date` or `dc:datetime` in the jsonld
  const allTimes = Array.concat(
    findAllFieldOccurancesRecursively("dc:date", jsonld),
    findAllFieldOccurancesRecursively("dc:datetime", jsonld)
  );

  // filter for string-literals (just in case)
  const allTimesStrs = allTimes.filter(s => {
    if (is("String", s)) {
      return true;
    } else {
      console.error(
        "Found a non-string date. Ignoring it for calculating `doNotMatchAfter`. ",
        s
      );
    }
  });

  if (allTimesStrs.length === 0) return undefined; // no time strings found

  // determine if any of them are intervals (e.g. days or months) and if so calculate the end of these
  const endDatetimes = allTimesStrs.map(str => endOfXsdDateInterval(str));

  // find the latest mentioned point in time
  const sorted = endDatetimes.sort((a, b) => b - a); // sort descending
  const latest = sorted[0];

  // convert to an `xsd:datetime` string and return
  return latest.toISOString();
}

/**
 * Behaves like `parseJsonldLeaf`, but can also handle
 * Arrays and Lists and turns those into ImmutableJS
 * Lists if they get passed. Otherwise takes and returns
 * flat values like compactValues.
 *
 * e.g.:
 * ```
 * parseJsonldLeafsImm("123.1") => 123.1
 * parseJsonldLeafsImm([
 *   {"@value": "123.1", "@type": "xsd:float"},
 *   {"@value": "7", "@type": "xsd:float"}
 * ]) // => Immutable.List([123.1, 7]);
 * ```
 * @param {*} val
 * @param {*} type if specified, guides parsing
 *  (see `compactValues`). Note that only one type can
 *   be specified atm, even if passing an list/array atm.
 *
 */

export function parseJsonldLeafsImm(val, type) {
  if (is("Array", val)) {
    const parsed = val.map(item => parseJsonldLeaf(item, type));
    return Immutable.List(parsed);
  } else if (Immutable.List.isList(val)) {
    return val.map(item => parseJsonldLeaf(item, type));
  } else {
    const parsed = parseJsonldLeaf(val, type);
    if (parsed) {
      // got a non-list; make it a 1-item list...
      return Immutable.List([parsed]);
    } else {
      // ... unless that one item would be `undefined` because lookup has failed.
      return undefined;
    }
  }
}

/**
 * @param {*} val
 *  * already parsed
 *  * `{"@value": "<someval>", "@type": "<sometype>"}`, where `<sometype>` is one of:
 *    * `xsd:float`
 *    * `xsd:dateTime`
 *    * `xsd:date`
 *    * `xsd:time`
 *    * `http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon`?, e.g. `"48.225073#16.358398"`
 *  * anything, that _strictly_ parses to a number or date or is a string
 * @param {*} type passing `val` and `type` is equivalent to passing an object with `@value` and `@type`
 *
 */
export function parseJsonldLeaf(val, type) {
  const unwrappedVal = get(val, "@value") || val;
  if (unwrappedVal === undefined || unwrappedVal === null) {
    return undefined;
  }

  const atType = get(val, "@type");
  if (type && atType && type !== atType) {
    throwParsingError(val, type, "Conflicting types.");
  }
  const type_ = get(val, "@type") || type;
  if (is("Number", unwrappedVal) || is("Date", unwrappedVal)) {
    // already parsed
    return unwrappedVal;
  }
  const throwErr = msg => throwParsingError(val, type, msg);
  switch (type_) {
    case "xsd:string":
      return unwrappedVal + ""; // everything can be parsed to a string in js

    case "xsd:float":
      {
        const parsedVal = Number(unwrappedVal);
        if (isNaN(parsedVal)) {
          throwErr("Annotated `xsd:float` isn't parsable to a `Number`.");
        } else {
          return parsedVal;
        }
      }
      break;

    case "xsd:dateTime":
      {
        const parsedDateTime = parseXsdDateTime(unwrappedVal);
        if (isValidDate(parsedDateTime)) {
          return parsedDateTime;
        } else {
          throwErr("Annotated `xsd:dateTime` isn't parsable to a `Date`.");
        }
      }
      break;

    // TODO
    // case "xsd:date":
    // case "xsd:time":
    // case "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon":
    //   break;

    default: {
      if (type_) {
        throwErr("Encountered unexpected type annotation/specification.");
      }

      // try strictly parsing without type information
      const asNum = Number(unwrappedVal);
      if (!isNaN(asNum)) {
        return asNum;
      }
      const asDateTime = parseXsdDateTime(unwrappedVal);
      if (isValidDate(asDateTime)) {
        return asDateTime;
      }

      // and as fallback check if it's at least a string (which it pretty almost will be)
      if (is("String", unwrappedVal)) {
        return unwrappedVal;
      }

      throwErr("Found no `@type`-annotation and also couldn't guess a type.");
    }
  }
}

function throwParsingError(val, type, prependedMsg = "") {
  const fullMsg =
    prependedMsg +
    ` Failed to parse jsonld value of type \`${type}\`:\n` +
    JSON.stringify(val);
  throw new Error(fullMsg.trim());
}
