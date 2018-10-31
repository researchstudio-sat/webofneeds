import Immutable from "immutable";
import won from "../../won-es6.js";
import { getAllDetails } from "../../won-utils.js";

export function parseNeed(jsonldNeed, ownNeed) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  let parsedNeed = {
    uri: undefined,
    nodeUri: undefined,
    types: undefined,
    state: undefined,
    connections: Immutable.Map(),
    creationDate: undefined,
    lastUpdateDate: undefined,
    unread: false,
    ownNeed: !!ownNeed,
    isBeingCreated: false,
    isWhatsAround: false,
    isWhatsNew: false,
    hasFlags: undefined,
    matchingContexts: undefined,
    searchString: undefined,
    jsonld: jsonldNeed,
    hasFacets: Immutable.Map(),
    heldBy: undefined,
    holds: undefined,
  };

  if (jsonldNeedImm) {
    const uri = jsonldNeedImm.get("@id");
    const nodeUri = jsonldNeedImm.getIn(["won:hasWonNode", "@id"]);
    const is = jsonldNeedImm.get("won:is");
    const seeks = jsonldNeedImm.get("won:seeks");
    const isPresent = is && is.size > 0;
    const seeksPresent = seeks && seeks.size > 0;

    const searchString = jsonldNeedImm.get("won:hasSearchString");

    if (uri) {
      parsedNeed.uri = uri;
    } else {
      return undefined;
    }

    /*
     * The following code-snippet is solely to determine if the parsed need
     * is a special "whats around"-need, in order to do this we have to make
     * sure that the won:hasFlag is checked in two forms, both as a string
     * and an immutable object
     */
    const wonHasFlags = jsonldNeedImm.get("won:hasFlag");

    const hasFlags = extractFlags(jsonldNeedImm.get("won:hasFlag"));

    const isWhatsAround =
      wonHasFlags &&
      wonHasFlags.filter(function(flag) {
        if (flag instanceof Immutable.Map) {
          return flag.get("@id") === "won:WhatsAround";
        } else {
          return flag === "won:WhatsAround";
        }
      }).size > 0;

    const isWhatsNew =
      wonHasFlags &&
      wonHasFlags.filter(function(flag) {
        if (flag instanceof Immutable.Map) {
          return flag.get("@id") === "won:WhatsNew";
        } else {
          return flag === "won:WhatsNew";
        }
      }).size > 0;

    const wonHasMatchingContexts = jsonldNeedImm.get("won:hasMatchingContext");

    const creationDate =
      jsonldNeedImm.get("dct:created") ||
      jsonldNeedImm.get("http://purl.org/dc/terms/created");
    if (creationDate) {
      parsedNeed.creationDate = new Date(creationDate);
      parsedNeed.lastUpdateDate = parsedNeed.creationDate;
    }

    const state = jsonldNeedImm.getIn([won.WON.isInStateCompacted, "@id"]);
    if (state === won.WON.ActiveCompacted) {
      // we use to check for active
      // state and everything else
      // will be inactive
      parsedNeed.state = state;
    } else {
      parsedNeed.state = won.WON.InactiveCompacted;
    }

    parsedNeed.heldBy = jsonldNeedImm.getIn(["won:heldBy", "@id"]);

    let isPart = undefined;
    let seeksPart = undefined;
    let type = undefined;
    const detailsToParse = getAllDetails();

    parsedNeed.holds =
      won.parseListFrom(jsonldNeedImm, ["won:holds"], "xsd:ID") ||
      Immutable.List();

    parsedNeed.heldBy = won.parseFrom(jsonldNeedImm, ["won:heldBy"], "xsd:ID");

    parsedNeed.types = (rawTypes => {
      if (Immutable.List.isList(rawTypes)) {
        return Immutable.Set(rawTypes);
      } else {
        return Immutable.Set([rawTypes]);
      }
    })(jsonldNeedImm.get("@type"));

    if (isPresent) {
      isPart = generateContent(is, type, detailsToParse);
    }
    if (seeksPresent) {
      seeksPart = generateContent(seeks, type, detailsToParse);
    }
    if (searchString) {
      parsedNeed.searchString = searchString;
    }

    parsedNeed.is = isPart;
    parsedNeed.seeks = seeksPart;

    parsedNeed.isWhatsAround = !!isWhatsAround;
    parsedNeed.isWhatsNew = !!isWhatsNew;
    parsedNeed.matchingContexts = wonHasMatchingContexts
      ? Immutable.List.isList(wonHasMatchingContexts)
        ? wonHasMatchingContexts
        : Immutable.List.of(wonHasMatchingContexts)
      : undefined;
    parsedNeed.hasFlags = hasFlags;
    parsedNeed.hasFacets = extractFacets(jsonldNeedImm.get("won:hasFacet"));
    parsedNeed.nodeUri = nodeUri;
    parsedNeed.humanReadable = getHumanReadableStringFromNeed(
      parsedNeed,
      detailsToParse
    );
  } else {
    console.error(
      "Cant parse need, data is an invalid need-object: ",
      jsonldNeedImm && jsonldNeedImm.toJS()
    );
    return undefined;
  }

  return Immutable.fromJS(parsedNeed);
}

/**
 * Tries to extract all the detailsToParse from the given contentJsonLd
 * uses the parseFromRdf function defined in the detail to extract the content
 * uses the detail identifier as the key of the contentDetail that is to be added
 * @param contentJsonLd
 * @param type
 * @param detailsToParse
 * @returns {{title: *, type: *}}
 */
function generateContent(contentJsonLd, type, detailsToParse) {
  let content = {
    type: type,
  };

  if (detailsToParse) {
    for (const detailKey in detailsToParse) {
      const detailToParse = detailsToParse[detailKey];
      const detailIdentifier = detailToParse && detailToParse.identifier;
      const detailValue =
        detailToParse && detailToParse.parseFromRDF(contentJsonLd);

      if (detailIdentifier && detailValue) {
        content[detailIdentifier] = detailValue;
      }
    }
  }

  return content;
}

function extractFlags(wonHasFlags) {
  let hasFlags = Immutable.List();

  wonHasFlags &&
    wonHasFlags.map(function(flag) {
      if (flag instanceof Immutable.Map) {
        hasFlags = hasFlags.push(flag.get("@id"));
      } else {
        hasFlags = hasFlags.push(flag);
      }
    });

  return hasFlags;
}

function extractFacets(wonHasFacets) {
  let hasFacets = Immutable.Map();

  if (wonHasFacets) {
    if (Immutable.List.isList(wonHasFacets)) {
      wonHasFacets.map(facet => {
        hasFacets = hasFacets.set(facet.get("@id"), facet.get("@type"));
      });
      return hasFacets;
    } else {
      return hasFacets.set(wonHasFacets.get("@id"), wonHasFacets.get("@type"));
    }
  }
  return hasFacets;
}

function getHumanReadableStringFromNeed(need, detailsToParse) {
  if (need && detailsToParse) {
    const isBranch = need.is;
    const seeksBranch = need.seeks;

    const isTitle = isBranch && isBranch.title;
    const seeksTitle = seeksBranch && seeksBranch.title;

    if (need.isWhatsNew) {
      return "What's New";
    }

    if (need.isWhatsAround) {
      let location = isBranch["location"] || seeksBranch["location"];

      const locationJS =
        location && Immutable.Iterable.isIterable(location)
          ? location.toJS()
          : location;

      return (
        "What's Around " +
        detailsToParse["location"].generateHumanReadable({
          value: locationJS,
          includeLabel: false,
        })
      );
    }

    if (isTitle && seeksTitle) {
      return isTitle + " - " + seeksTitle;
    } else if (seeksTitle) {
      return seeksTitle;
    } else if (isTitle) {
      return isTitle;
    } else {
      const searchString = need && need.searchString;

      if (searchString) {
        return "Search: " + searchString;
      }
    }

    let humanReadableIsDetails = generateHumanReadableArray(
      isBranch,
      detailsToParse
    );
    let humanReadableSeeksDetails = generateHumanReadableArray(
      seeksBranch,
      detailsToParse
    );

    if (
      humanReadableIsDetails.length > 0 &&
      humanReadableSeeksDetails.length > 0
    ) {
      return (
        "Is: " +
        humanReadableIsDetails.join(" ") +
        " Seeks: " +
        humanReadableSeeksDetails.join(", ")
      );
    } else if (humanReadableIsDetails.length > 0) {
      return humanReadableIsDetails.join(", ");
    } else if (humanReadableSeeksDetails.length > 0) {
      return humanReadableSeeksDetails.join(", ");
    }
  }
  return undefined;
}

function generateHumanReadableArray(presentDetails, detailsToParse) {
  let humanReadableArray = [];
  if (presentDetails) {
    for (const key in presentDetails) {
      const detailToParse = detailsToParse[key];
      if (detailToParse) {
        const detailValue = presentDetails[key];
        const detailValueJS =
          detailValue && Immutable.Iterable.isIterable(detailValue)
            ? detailValue.toJS()
            : detailValue;

        if (detailValueJS) {
          humanReadableArray.push(
            detailToParse.generateHumanReadable({
              value: detailValueJS,
              includeLabel: true,
            })
          );
        }
      }
    }
  }
  return humanReadableArray;
}
