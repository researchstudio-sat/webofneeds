import Immutable from "immutable";
import shajs from "sha.js";
import Identicon from "identicon.js";

export const parse = async (jsonldAtomAndAuth, fakeNames, vocab) => {
  const jsonldAtomAndAuthImm = Immutable.fromJS(jsonldAtomAndAuth);
  const jsonldAtomImm = get(jsonldAtomAndAuthImm, "atom");
  const jsonldAuthImm = get(jsonldAtomAndAuthImm, "auth");

  const atomUri = get(jsonldAtomImm, "@id");
  if (atomUri) {
    let parsedAtom = {
      uri: atomUri,
      identiconSvg: generateIdenticon(atomUri),
      nodeUri: getIn(jsonldAtomImm, [vocab.WON.wonNodeCompacted, "@id"]),
      connectionContainerUri: getIn(jsonldAtomImm, [
        vocab.WON.connectionsCompacted,
        "@id",
      ]),
      state: extractState(jsonldAtomImm, vocab),
      content: {}, // content: generateContent(jsonldAtomImm, detailsToParse), //TODO FIX THIS SOMEHOW OR MOVE TO DIFFERENT PLACE (currently still parse-atom.js:parseAtomContent())
      seeks: {}, // seeks: generateContent(get(jsonldAtomImm, "match:seeks"), detailsToParse), //TODO: FIX THIS SOMEHOW OR MOVE TO DIFFERENT PLACE (currently still parse-atom.js:parseAtomContent())
      creationDate: extractCreationDate(jsonldAtomImm),
      lastUpdateDate: extractCreationDate(jsonldAtomImm), //Used for sorting/updates (e.g. if connection comes in etc...)
      modifiedDate: extractLastModifiedDate(jsonldAtomImm), //Used as a flag if the atom itself has changed (e.g. atom edit)
      humanReadable: undefined, //can only be determined after we generated The Content
      fakePersonaName: generateFakePersonaName(atomUri, fakeNames),
      matchedUseCase: {
        identifier: undefined,
        icon: undefined,
        reactions: undefined,
      },
      auth: extractAuthList(jsonldAuthImm),
      tokenAuth: extractTokenAuth(jsonldAuthImm, vocab),
      tokenScopeUris: extractTokenScopeUris(jsonldAuthImm, vocab),
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
    /*
    // FIXME: THIS PART IS SOMEHOW NOT REALLY WORKING IN A SERVICE WORKER FOR NOW (currently still parse-atom.js:parseAtomContent())
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
    }*/
    parsedAtomImm = parsedAtomImm.set("jsonLdAtom", jsonldAtomImm); //jsonLdAtom is just for partialParsing purposes (remove jsonLdAtom once parseAtomContent is moved into this (currently still parse-atom.js:parseAtomContent()))
    return parsedAtomImm.toJS(); //we can't return a Immutable object -> it won't be parsed Correctly
  } else {
    console.error(
      "Cant parse atom, data is an invalid atom-object: ",
      jsonldAtomImm && jsonldAtomImm.toJS()
    );
    return undefined;
  }
};

// Moved these methods from parse-atom.js
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

function extractAuthList(authList) {
  const auths = get(authList, "@graph");
  return auths ? auths.map(auth => auth.delete("@type")) : Immutable.List();
}

function extractTokenAuth(authList, vocab) {
  const auths = extractAuthList(authList);

  return auths
    ? auths.filter(auth => {
        const grants = get(auth, vocab.AUTH.grant);
        return !!grants.find(grant => {
          const operations = get(grant, vocab.AUTH.operation);
          return (
            !!operations &&
            !is("String", operations) &&
            operations.find(
              op => !is("String", op) && !!get(op, vocab.AUTH.requestToken)
            )
          );
        });
      })
    : Immutable.List();
}

function extractTokenScopeUris(authList, vocab) {
  const tokenAuths = extractTokenAuth(authList, vocab);

  const tokenScopeUris = [];

  for (const tokenAuth of tokenAuths) {
    const authTokenOperations = tokenAuth
      .get(vocab.AUTH.grant)
      .flatMap(grant => get(grant, vocab.AUTH.operation))
      .map(op => get(op, vocab.AUTH.requestToken))
      .filter(op => !!op);

    for (const authTokenOperation of authTokenOperations) {
      const tokenScopeUri = getIn(authTokenOperation, [
        vocab.AUTH.tokenScope,
        "@id",
      ]);
      tokenScopeUri && tokenScopeUris.push(tokenScopeUri);
    }
  }

  return tokenScopeUris;
}

function extractState(atomJsonLd, vocab) {
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

// Moved these methods from utils.js
function generateFakePersonaName(atomUri, fakeNames) {
  let hash = 0;

  const hashFunc = text => {
    if (text) {
      for (const char of text.split("")) {
        hash = char.charCodeAt(0) + ((hash << 5) - hash);
      }

      return hash < 0 ? hash * -1 : hash;
    }
  };

  const animalHash = hashFunc("animal" + atomUri);
  const adjectiveHash = hashFunc("adj" + atomUri);

  return (
    fakeNames.adjectives[adjectiveHash % fakeNames.adjectives.length] +
    " " +
    fakeNames.animals[animalHash % fakeNames.animals.length]
  );
}

function generateRgbColorArray(text) {
  if (!text) return [0, 0, 0];
  const hexStr = generateHexColor(text);
  return [
    // ignore first char, as that's the `#`
    hexStr.slice(1, 3),
    hexStr.slice(3, 5),
    hexStr.slice(5, 7),
  ].map(hexColor => parseInt(hexColor, 16));
}

// DUPLICATE CODE (from utils.js)
function generateHexColor(text) {
  let hash = 0;

  if (text) {
    for (const char of text.split("")) {
      hash = char.charCodeAt(0) + ((hash << 5) - hash);
    }
  }

  const c = (hash & 0x00ffffff).toString(16);

  return "#" + ("00000".substring(0, 6 - c.length) + c);
}

/**
 * Returns a property of a given object, no matter whether
 * it's a normal or an immutable-js object.
 * @param obj
 * @param property
 */
function get(obj, property) {
  if (!obj) {
    return undefined;
  } else if (obj.get) {
    /* obj is an immutabljs-object
           * NOTE: the canonical check atm would be `Immutable.Iterable.isIterable(obj)`
           * but that would require including immutable as dependency her and it'd be better
           * to keep this library independent of anything.
           */
    return obj.get(property);
  } else {
    /* obj is a vanilla object */
    return obj[property];
  }
}

/**
 * Tries to look up a property-path on a nested object-structure.
 * Where `obj.x.y` would throw an error if `x` wasn't defined
 * `get(obj, ['x','y'])` would return undefined.
 * @param obj
 * @param path
 * @return {*}
 */
function getIn(obj, path) {
  if (!path || !obj || path.length === 0) {
    return undefined;
  } else {
    const child = get(obj, path[0]);
    // let child;
    // if (obj.toJS && obj.get) {
    //   /* obj is an immutabljs-object
    //          * NOTE: the canonical check atm would be `Immutable.Iterable.isIterable(obj)`
    //          * but that would require including immutable as dependency her and it'd be better
    //          * to keep this library independent of anything.
    //          */
    //   child = obj.get(path[0]);
    // } else {
    //   /* obj is a vanilla object */
    //   child = obj[path[0]];
    // }
    if (path.length === 1) {
      /* end of the path */
      return child;
    } else {
      /* recurse */
      return getIn(child, path.slice(1));
    }
  }
}

function is(type, obj) {
  const clas = Object.prototype.toString.call(obj).slice(8, -1);
  return obj !== undefined && obj !== null && clas === type;
}
