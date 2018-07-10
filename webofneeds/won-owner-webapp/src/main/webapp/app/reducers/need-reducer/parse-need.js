import Immutable from "immutable";
import won from "../../won-es6.js";
import { getAllDetails } from "../../won-utils.js";

export function parseNeed(jsonldNeed, ownNeed) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  let parsedNeed = {
    uri: undefined,
    nodeUri: undefined,
    title: undefined,
    type: undefined,
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
  };

  if (jsonldNeedImm) {
    const uri = jsonldNeedImm.get("@id");
    const nodeUri = jsonldNeedImm.getIn(["won:hasWonNode", "@id"]);
    const is = jsonldNeedImm.get("won:is");
    const seeks = jsonldNeedImm.get("won:seeks");
    const isPresent = is && is.size > 0;
    const seeksPresent = seeks && seeks.size > 0;

    const searchString = jsonldNeedImm.get("won:hasSearchString");

    //TODO We need to decide which is the main title? Or combine?
    const title = isPresent
      ? is.get("dc:title")
      : seeksPresent
        ? seeks.get("dc:title")
        : undefined;
    parsedNeed.title = title;

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

    let isPart = undefined;
    let seeksPart = undefined;
    let type = undefined;
    const detailsToParse = getAllDetails();

    if (isPresent) {
      type = seeksPresent
        ? won.WON.BasicNeedTypeCombinedCompacted
        : won.WON.BasicNeedTypeSupplyCompacted;
      isPart = generateContent(is, type, detailsToParse);
    }
    if (seeksPresent) {
      type = isPresent ? type : won.WON.BasicNeedTypeDemandCompacted;
      seeksPart = generateContent(seeks, type, detailsToParse);
    }
    if (searchString) {
      parsedNeed.searchString = searchString;
    }

    parsedNeed.is = isPart;
    parsedNeed.seeks = seeksPart;

    if (isWhatsAround) {
      parsedNeed.type = won.WON.BasicNeedTypeWhatsAroundCompacted;
    } else if (isWhatsNew) {
      parsedNeed.type = won.WON.BasicNeedTypeWhatsNewCompacted;
    } else {
      parsedNeed.type = type;
    }

    parsedNeed.isWhatsAround = !!isWhatsAround;
    parsedNeed.isWhatsNew = !!isWhatsNew;
    parsedNeed.matchingContexts = wonHasMatchingContexts
      ? Immutable.List.isList(wonHasMatchingContexts)
        ? wonHasMatchingContexts
        : Immutable.List.of(wonHasMatchingContexts)
      : undefined;
    parsedNeed.hasFlags = hasFlags;
    parsedNeed.nodeUri = nodeUri;
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
    title: contentJsonLd.get("dc:title"),
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
