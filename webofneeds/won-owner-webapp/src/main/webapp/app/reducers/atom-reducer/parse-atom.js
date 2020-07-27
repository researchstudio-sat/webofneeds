import Immutable from "immutable";
import vocab from "../../service/vocab.js";
import * as useCaseUtils from "../../usecase-utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import {
  generateHexColor,
  generateRgbColorArray,
  generateFakePersonaName,
  get,
  getIn,
  getUri,
} from "../../utils.js";
import shajs from "sha.js";
import Identicon from "identicon.js";

export function parseAtom(jsonldAtom) {
  const jsonldAtomImm = Immutable.fromJS(jsonldAtom);

  const atomUri = get(jsonldAtomImm, "@id");
  if (atomUri) {
    const detailsToParse = useCaseUtils.getAllDetails();

    let parsedAtom = {
      uri: atomUri,
      identiconSvg: generateIdenticon(atomUri),
      nodeUri: getIn(jsonldAtomImm, [vocab.WON.wonNodeCompacted, "@id"]),
      state: extractState(jsonldAtomImm),
      content: generateContent(jsonldAtomImm, detailsToParse),
      seeks: generateContent(get(jsonldAtomImm, "match:seeks"), detailsToParse),
      creationDate: extractCreationDate(jsonldAtomImm),
      lastUpdateDate: extractCreationDate(jsonldAtomImm), //Used for sorting/updates (e.g. if connection comes in etc...)
      modifiedDate: extractLastModifiedDate(jsonldAtomImm), //Used as a flag if the atom itself has changed (e.g. atom edit)
      humanReadable: undefined, //can only be determined after we generated The Content
      fakePersonaName: generateFakePersonaName(atomUri),
      matchedUseCase: {
        identifier: undefined,
        icon: undefined,
        reactions: undefined,
      },
      background: generateBackground(atomUri),
      unread: false,
      isBeingCreated: false,
      connections: Immutable.Map(),
    };

    if (!parsedAtom.creationDate || !parsedAtom.lastUpdateDate) {
      console.error(
        "Cant parse atom, creationDate or lastUpdateDate not set",
        jsonldAtomImm && jsonldAtomImm.toJS()
      );
      return undefined;
    }

    let parsedAtomImm = Immutable.fromJS(parsedAtom);
    const matchingUseCase = useCaseUtils.findUseCaseByAtom(parsedAtomImm);

    if (matchingUseCase) {
      parsedAtomImm = parsedAtomImm
        .setIn(["matchedUseCase", "identifier"], matchingUseCase.identifier)
        .setIn(["matchedUseCase", "icon"], matchingUseCase.icon)
        .setIn(
          ["matchedUseCase", "reactions"],
          matchingUseCase.reactions
            ? Immutable.fromJS(matchingUseCase.reactions)
            : Immutable.Map()
        );
    }

    parsedAtomImm = parsedAtomImm.set(
      "humanReadable",
      getHumanReadableStringFromAtom(parsedAtomImm, detailsToParse)
    );

    //Sort content details
    const atomContent = get(parsedAtomImm, "content");
    const atomSeeksContent = get(parsedAtomImm, "seeks");

    const sortContentDetailsImm = (_, contentIdentifier) => {
      if (
        contentIdentifier === "title" ||
        contentIdentifier === "personaName"
      ) {
        return "1";
      }
      if (contentIdentifier === "description") {
        return "2";
      }

      return get(detailsToParse[contentIdentifier], "label");
    };

    if (atomContent) {
      parsedAtomImm = parsedAtomImm.set(
        "content",
        atomContent.toOrderedMap().sortBy(sortContentDetailsImm)
      );
    }
    if (atomSeeksContent) {
      parsedAtomImm = parsedAtomImm.set(
        "seeks",
        atomSeeksContent.toOrderedMap().sortBy(sortContentDetailsImm)
      );
    }

    return parsedAtomImm;
  } else {
    console.error(
      "Cant parse atom, data is an invalid atom-object: ",
      jsonldAtomImm && jsonldAtomImm.toJS()
    );
    return undefined;
  }
}

/**
 * Tries to extract all the metaData of a given metaAtom into a state that is similar to the ones we use for the parseAtom
 * in general, metaAtoms are retrieved when accessing whatsNew and whatsAround (for now) and are json structures based on the
 * AtomPojo of the owner app.
 * @param metaAtom
 * @returns {*}
 */
export function parseMetaAtom(metaAtom) {
  const metaAtomImm = Immutable.fromJS(metaAtom);

  const extractEventObjectAboutUris = eventObjectUris =>
    eventObjectUris &&
    Immutable.Set(
      eventObjectUris.map(eventObjectUri =>
        eventObjectUri
          .replace(vocab.WON.baseUri, vocab.WON.prefix + ":")
          .replace(vocab.WONCON.baseUri, vocab.WONCON.prefix + ":")
          .replace(vocab.DEMO.baseUri, vocab.DEMO.prefix + ":")
          .replace(vocab.WONMATCH.baseUri, vocab.WONMATCH.prefix + ":")
          .replace("http://schema.org/", "s:")
      )
    );

  const extractTypes = types =>
    types &&
    Immutable.Set(
      types.map(type =>
        type
          .replace(vocab.WON.baseUri, vocab.WON.prefix + ":")
          .replace(vocab.WONCON.baseUri, vocab.WONCON.prefix + ":")
          .replace(vocab.DEMO.baseUri, vocab.DEMO.prefix + ":")
          .replace(vocab.BOT.baseUri, vocab.BOT.prefix + ":")
          .replace(vocab.WONMATCH.baseUri, vocab.WONMATCH.prefix + ":")
          .replace(vocab.VALUEFLOWS.baseUri, vocab.VALUEFLOWS.prefix + ":")
          .replace(vocab.WXVALUEFLOWS.baseUri, vocab.WXVALUEFLOWS.prefix + ":")
          .replace("http://schema.org/", "s:")
      )
    );
  const extractFlags = flags =>
    flags &&
    flags.map(flag =>
      flag
        .replace(vocab.WON.baseUri, vocab.WON.prefix + ":")
        .replace(vocab.WONCON.baseUri, vocab.WONCON.prefix + ":")
        .replace(vocab.DEMO.baseUri, vocab.DEMO.prefix + ":")
        .replace(vocab.WONMATCH.baseUri, vocab.WONMATCH.prefix + ":")
        .replace("http://schema.org/", "s:")
    );
  const extractLocation = location =>
    location && {
      address: "",
      lat: get(location, "latitude"),
      lng: get(location, "longitude"),
    };

  const extractSockets = socketTypeUriMap =>
    socketTypeUriMap &&
    socketTypeUriMap.map(socketType =>
      socketType
        .replace(vocab.CHAT.baseUri, vocab.CHAT.prefix + ":")
        .replace(vocab.GROUP.baseUri, vocab.GROUP.prefix + ":")
        .replace(vocab.HOLD.baseUri, vocab.HOLD.prefix + ":")
        .replace(vocab.BUDDY.baseUri, vocab.BUDDY.prefix + ":")
        .replace(vocab.BOT.baseUri, vocab.BOT.prefix + ":")
        .replace(vocab.WXSCHEMA.baseUri, vocab.WXSCHEMA.prefix + ":")
        .replace(vocab.WXVALUEFLOWS.baseUri, vocab.WXVALUEFLOWS.prefix + ":")
    );

  const extractStateFromMeta = state => {
    switch (state) {
      case "ACTIVE":
        return vocab.WON.ActiveCompacted;
      case "INACTIVE":
        return vocab.WON.InactiveCompacted;
      case "DELETED":
        return vocab.WON.DeletedCompacted;
      default:
        return undefined;
    }
  };

  if (metaAtomImm) {
    let parsedMetaAtom = {
      uri: getUri(metaAtomImm),
      identiconSvg: undefined,
      nodeUri: undefined,
      state: extractStateFromMeta(get(metaAtomImm, "state")),
      rating: undefined,
      content: {
        type: extractTypes(get(metaAtomImm, "types")),
        sockets: extractSockets(get(metaAtomImm, "socketTypeUriMap")),
        flags: extractFlags(get(metaAtomImm, "flags")),
        location: extractLocation(get(metaAtomImm, "location")),
        jobLocation: extractLocation(get(metaAtomImm, "jobLocation")),
        eventObjectAboutUris: extractEventObjectAboutUris(
          get(metaAtomImm, "eventObjectAboutUris")
        ),
      },
      seeks: {
        type: extractTypes(get(metaAtomImm, "seeksTypes")),
        eventObjectAboutUris: extractEventObjectAboutUris(
          get(metaAtomImm, "seeksEventObjectAboutUris")
        ),
      },
      modifiedDate:
        get(metaAtomImm, "modifiedDate") &&
        new Date(get(metaAtomImm, "modifiedDate")),
      creationDate:
        get(metaAtomImm, "creationDate") &&
        new Date(get(metaAtomImm, "creationDate")),
      lastUpdateDate:
        get(metaAtomImm, "creationDate") &&
        new Date(get(metaAtomImm, "creationDate")),
      humanReadable: undefined,
      fakePersonaName: generateFakePersonaName(getUri(metaAtomImm)),
      matchedUseCase: {
        identifier: undefined,
        icon: undefined,
        reactions: undefined,
      },
      background: generateBackground(getUri(metaAtomImm)),
      unread: false,
      isBeingCreated: false,
      connections: Immutable.Map(),
    };

    if (
      parsedMetaAtom.state &&
      parsedMetaAtom.modifiedDate &&
      parsedMetaAtom.creationDate
    ) {
      const parsedAtomImm = Immutable.fromJS(parsedMetaAtom);
      const matchingUseCase = useCaseUtils.findUseCaseByAtom(parsedAtomImm);

      if (matchingUseCase) {
        return parsedAtomImm
          .setIn(["matchedUseCase", "identifier"], matchingUseCase.identifier)
          .setIn(["matchedUseCase", "icon"], matchingUseCase.icon)
          .setIn(
            ["matchedUseCase", "reactions"],
            matchingUseCase.reactions
              ? Immutable.fromJS(matchingUseCase.reactions)
              : Immutable.Map()
          );
      }

      return parsedAtomImm;
    } else {
      console.error(
        "Cant parse metaAtom, data is an invalid atom-object: ",
        metaAtomImm && metaAtomImm.toJS()
      );
      return undefined;
    }
  }
  return undefined;
}

window.parseMetaAtom4dbg = parseMetaAtom;

/**
 * Tries to extract all the detailsToParse from the given contentJsonLd
 * uses the parseFromRdf function defined in the detail to extract the content
 * uses the detail identifier as the key of the contentDetail that is to be added
 * @param contentJsonLd
 * @param detailsToParse
 * @returns {title: *, tags: *}
 */
function generateContent(contentJsonLd, detailsToParse) {
  let content = {};
  if (contentJsonLd && contentJsonLd.size > 0 && detailsToParse) {
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

function extractState(atomJsonLd) {
  // we use to check for active
  // state and everything else
  // will be inactive
  return getIn(atomJsonLd, [vocab.WON.atomStateCompacted, "@id"]) ===
    vocab.WON.ActiveCompacted
    ? vocab.WON.ActiveCompacted
    : vocab.WON.InactiveCompacted;
}

function generateIdenticon(atomUri) {
  if (!atomUri) {
    return;
  }
  // quick extra hash here as identicon.js only uses first 15
  // chars (which aren't very unique for our uris due to the base-url):
  const hash = new shajs.sha512().update(atomUri).digest("hex");
  const rgbColorArray = generateRgbColorArray(hash);
  const idc = new Identicon(hash, {
    size: 100,
    foreground: [255, 255, 255, 255], // rgba white
    background: [...rgbColorArray, 255], // rgba
    margin: 0.2,
    format: "svg",
  });
  return idc.toString();
}

function generateBackground(atomUri) {
  if (!atomUri) {
    return;
  }

  const hash = new shajs.sha512().update(atomUri).digest("hex");
  return generateHexColor(hash);
}

function extractCreationDate(atomJsonLd) {
  const creationDate =
    get(atomJsonLd, "dct:created") ||
    get(atomJsonLd, "http://purl.org/dc/terms/created");
  if (creationDate) {
    return new Date(get(creationDate, "@value"));
  }
  return undefined;
}

function extractLastModifiedDate(atomJsonLd) {
  const lastModifiedDate =
    get(atomJsonLd, "dct:modified") ||
    get(atomJsonLd, "http://purl.org/dc/terms/modified");
  if (lastModifiedDate) {
    return new Date(get(lastModifiedDate, "@value"));
  }
  return undefined;
}

function getHumanReadableStringFromAtom(atomImm, detailsToParse) {
  if (atomImm && detailsToParse) {
    const atomContent = atomUtils.getContent(atomImm);
    const seeksBranch = atomUtils.getSeeks(atomImm);

    const title = get(atomContent, "title");
    const seeksTitle = get(seeksBranch, "title");

    if (atomUtils.isServiceAtom(atomImm) || atomUtils.isPersona(atomImm)) {
      return get(atomContent, "personaName");
    }

    if (atomUtils.getMatchedUseCaseIdentifier(atomImm) === "pokemonGoRaid") {
      let raidReadable;
      let locationReadable;
      let gymReadable;

      const raidDetail = "pokemonRaid";
      const raidValueImm = get(atomContent, raidDetail);
      if (raidValueImm && detailsToParse[raidDetail]) {
        raidReadable = detailsToParse[raidDetail].generateHumanReadable({
          value: raidValueImm.toJS(),
          includeLabel: false,
        });
      }

      const locationDetail = "location";
      const locationValueImm = get(atomContent, locationDetail);
      if (locationValueImm && detailsToParse[locationDetail]) {
        locationReadable = detailsToParse[locationDetail].generateHumanReadable(
          {
            value: locationValueImm.toJS(),
            includeLabel: false,
          }
        );
      }

      const gymDetail = "pokemonGymInfo";
      const gymValueImm = get(atomContent, gymDetail);
      if (gymValueImm && detailsToParse[gymDetail]) {
        gymReadable = detailsToParse[gymDetail].generateHumanReadable({
          value: gymValueImm.toJS(),
          includeLabel: false,
        });
      }
      if (raidReadable && locationReadable) {
        return (
          raidReadable +
          " @ " +
          locationReadable +
          (gymReadable ? " (" + gymReadable + ")" : "")
        );
      }
    }

    if (title && seeksTitle) {
      return title + " - " + seeksTitle;
    } else if (seeksTitle) {
      return seeksTitle;
    } else if (title) {
      return title;
    }

    let humanReadableDetails = generateHumanReadableArray(
      atomContent.toJS(),
      detailsToParse
    );
    let humanReadableSeeksDetails = generateHumanReadableArray(
      seeksBranch.toJS(),
      detailsToParse
    );

    if (
      humanReadableDetails.length > 0 &&
      humanReadableSeeksDetails.length > 0
    ) {
      return (
        humanReadableDetails.join(" ") +
        " Seeks: " +
        humanReadableSeeksDetails.join(", ")
      );
    } else if (humanReadableDetails.length > 0) {
      return humanReadableDetails.join(", ");
    } else if (humanReadableSeeksDetails.length > 0) {
      return humanReadableSeeksDetails.join(", ");
    }
  }
  return undefined;
}

function generateHumanReadableArray(presentDetails, detailsToParse) {
  let humanReadableArray = [];
  if (presentDetails && detailsToParse) {
    for (const key in presentDetails) {
      if (!(key === "sockets" || key === "type")) {
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
  }
  return humanReadableArray;
}
