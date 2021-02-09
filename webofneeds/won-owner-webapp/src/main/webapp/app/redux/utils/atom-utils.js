/**
 * Created by fsuda on 08.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get, getIn, getUri } from "../../utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import * as connectionUtils from "./connection-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import Immutable from "immutable";

/**
 * Determines if a given atom is a Active
 * @param atom
 * @returns {*|boolean}
 */
export function isActive(atom) {
  return getState(atom) === vocab.WON.ActiveCompacted;
}

export function getState(atom) {
  return get(atom, "state");
}

export function getFakePersonaName(atom) {
  return get(atom, "fakePersonaName");
}

export function getLastUpdateDate(atom) {
  return get(atom, "lastUpdateDate");
}

export function getCreationDate(atom) {
  return get(atom, "creationDate");
}

export function getModifiedDate(atom) {
  return get(atom, "modifiedDate");
}

export function isBeingCreated(atom) {
  return get(atom, "isBeingCreated");
}

export function getContent(atom) {
  return get(atom, "content");
}

export function getSeeks(atom) {
  return get(atom, "seeks");
}
/**
 * Determines if a given atom is a Inactive
 * @param atom
 * @returns {*|boolean}
 */
export function isInactive(atom) {
  return getState(atom) === vocab.WON.InactiveCompacted;
}

export function getIdenticonSvg(atom) {
  return get(atom, "identiconSvg");
}

export function getMatchedUseCaseIcon(atom) {
  return getIn(atom, ["matchedUseCase", "icon"]);
}

export function matchesDefinitions(atom, useCaseDefinitions) {
  return (
    !!useCaseDefinitions &&
    !!useCaseDefinitions.find(useCaseDefinition =>
      matchesDefinition(atom, useCaseDefinition)
    )
  );
}

export function matchesDefinition(atom, useCaseDefinition) {
  return (
    !!useCaseDefinition &&
    get(useCaseDefinition, "identifier") ===
      getMatchedUseCaseIdentifier(atom) &&
    hasSocket(atom, get(useCaseDefinition, "senderSocketType"))
  );
}

/**
 * Generates the Title Based on the Human Readable but use externalDataState if available
 * @param atom
 * @param externalDataState
 * @returns {any}
 */
export function getTitle(atom, externalDataState, separator = ", ") {
  const atomContent = getContent(atom);
  const title = get(atomContent, "title");
  if (title) {
    return title;
  }

  const personaName = get(atomContent, "personaName");

  if (personaName) {
    return personaName;
  }

  const eventObjectAboutUris = get(atomContent, "eventObjectAboutUris");
  const classifiedAs = get(atomContent, "classifiedAs");

  const wikiDataHumanReadable = [];

  const generateHumanReadable = uris => {
    const externalDataMap =
      uris &&
      uris.map(uri => get(externalDataState, uri)).filter(data => !!data);

    externalDataMap &&
      externalDataMap.map(data => {
        const wikiDataName = get(data, "personaName");
        const wikiDataTitle = get(data, "title");
        if (wikiDataName || wikiDataTitle) {
          wikiDataHumanReadable.push(wikiDataName || wikiDataTitle);
        }
      });
  };

  generateHumanReadable(eventObjectAboutUris);
  generateHumanReadable(classifiedAs);

  return wikiDataHumanReadable.length > 0
    ? wikiDataHumanReadable.join(separator)
    : get(atom, "humanReadable");
}

export function getDuration(atom) {
  const fromDatetime = getIn(atom, ["content", "fromDatetime"]);
  const throughDatetime = getIn(atom, ["content", "throughDatetime"]);
  if (fromDatetime && throughDatetime) {
    return { fromDatetime, throughDatetime };
  } else {
    return undefined;
  }
}

export function generateIcalDownloadLink(atom) {
  const duration = getDuration(atom);
  const title = getTitle(atom);

  const _zp = function(s) {
    return ("0" + s).slice(-2);
  };
  //iso date for ical formats
  const _isofix = function(d) {
    const offset = ("0" + new Date().getTimezoneOffset() / 60).slice(-2);

    if (typeof d == "string") {
      return d.replace(/-/g, "") + "T" + offset + "0000Z";
    } else {
      return (
        d.getFullYear() +
        _zp(d.getMonth() + 1) +
        _zp(d.getDate()) +
        "T" +
        _zp(d.getHours()) +
        "0000Z"
      );
    }
  };

  const now = new Date();
  const ics_lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//RSAFG.//iCalAdUnit//EN",
    "METHOD:REQUEST",
    "BEGIN:VEVENT",
    "UID:event-" + now.getTime() + "@matchat.org",
    "DTSTAMP:" + _isofix(now),
    "DTSTART:" + _isofix(duration.fromDatetime),
    "DTEND:" + _isofix(duration.throughDatetime),
    "DESCRIPTION:" + getUri(atom),
    "SUMMARY:" + title,
    "LAST-MODIFIED:" + _isofix(now),
    "SEQUENCE:0",
    "END:VEVENT",
    "END:VCALENDAR",
  ];

  return "data:text/calendar;base64," + btoa(ics_lines.join("\r\n"));
}

window.generateIcalDownloadLink4dbg = generateIcalDownloadLink;

export function getBackground(atom) {
  return get(atom, "background");
}

export function getMatchedUseCaseIdentifier(atom) {
  return getIn(atom, ["matchedUseCase", "identifier"]);
}

/**
 * returns the stored reactions for the specific atom (if socketType is undefined return all reactions)
 * only return reactions that actually are corresponding to a socket of the atom itself
 * @param atom
 * @param socketType
 * @returns {*}
 */
export function getReactions(atom, socketType) {
  const reactions = getIn(atom, ["matchedUseCase", "reactions"]);

  const possibleReactions =
    reactions &&
    reactions.filter((_, targetSocketType) =>
      hasSocket(atom, targetSocketType)
    );

  return socketType ? get(possibleReactions, socketType) : possibleReactions;
}

export function hasMatchedUseCase(atom) {
  return !!getMatchedUseCaseIdentifier(atom);
}

export function hasImages(atom) {
  return !!getImages(atom) || !!getSeeksImages(atom);
}

export function hasLocation(atom) {
  return !!getLocation(atom);
}

/**
 * Returns the first location found in the content of the atom
 * jobLocation has priority over "default" location
 * If no location is present undefined will be returned
 * @param atom
 * @returns {*}
 */
export function getLocation(atom) {
  const atomContent = getContent(atom);
  const atomSeeks = getSeeks(atom);

  return (
    get(atomContent, "jobLocation") ||
    get(atomContent, "location") ||
    get(atomSeeks, "jobLocation") ||
    get(atomSeeks, "location")
  );
}

/**
 * Get distance between the first found atomLocation and the location (parameter)
 * Distance in meters, undefined if atom has no location or location parameter is undefined
 * Priority of atomLocation is defined within getLocation
 * @param atom
 * @param location
 * @returns {*}
 */
export function getDistanceFrom(atom, location) {
  const atomLocation = getLocation(atom);

  /**
   * Calculates distance between two locations in meters
   * If any of the locations or lat, lng of the location are undefined/null, return undefined
   * @param locationA json {lat, lng]
   * @param locationB json {lat, lng]
   * @returns {number} distance between these two coordinates in meters
   */
  const calculateDistance = (locationA, locationB) => {
    const locationAImm = locationA && Immutable.fromJS(locationA);
    const locationBImm = locationB && Immutable.fromJS(locationB);

    if (
      !locationAImm ||
      !locationAImm.get("lat") ||
      !locationAImm.get("lng") ||
      !locationBImm ||
      !locationBImm.get("lat") ||
      !locationBImm.get("lng")
    ) {
      return;
    }

    const earthRadius = 6371000; // earth radius in meters
    const dLat =
      ((locationBImm.get("lat") - locationAImm.get("lat")) * Math.PI) / 180;
    const dLon =
      ((locationBImm.get("lng") - locationAImm.get("lng")) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((locationAImm.get("lat") * Math.PI) / 180) *
        Math.cos((locationBImm.get("lat") * Math.PI) / 180) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const d = earthRadius * c;

    return Math.round(d);
  };

  return calculateDistance(atomLocation, location);
}

export function getImages(atom) {
  const atomContent = getContent(atom);
  return get(atomContent, "images");
}

export function getImageUrl(atom) {
  const atomContent = getContent(atom);
  return get(atomContent, "imageUrl");
}

export function getSeeksImages(atom) {
  const atomSeeks = getSeeks(atom);
  return get(atomSeeks, "images");
}

/**
 * Returns the "Default" Image (currently the content branch is checked before seeks) of an atom
 * if the atom does not have any images we return undefined
 * @param atom
 */
export function getDefaultImage(atom) {
  if (hasImages(atom)) {
    const contentImages = getImages(atom);

    if (contentImages) {
      const defaultImage = contentImages.find(image => get(image, "default"));

      if (defaultImage) {
        return defaultImage;
      }
    }

    const seeksImages = getSeeksImages(atom);

    if (seeksImages) {
      const defaultImage = seeksImages.find(image => get(image, "default"));

      if (defaultImage) {
        return defaultImage;
      } else {
        return seeksImages.first();
      }
    } else {
      return contentImages.first();
    }
  }
  return undefined;
}

/**
 * Returns the "Default" Image (currently the content branch is checked) of an atom which is a persona
 * if the atom does not have any images we return undefined
 * @param atom
 */
export function getDefaultPersonaImage(atom) {
  if (isPersona(atom) && hasImages(atom)) {
    const contentImages = getImages(atom);

    if (contentImages) {
      const defaultImage = contentImages.find(image => get(image, "default"));

      if (defaultImage) {
        return defaultImage;
      }
    }
  }
  return undefined;
}

/**
 * Determines if a given atom is Invisible (contains the no hint for counterpart flag)
 * @param atom
 * @returns {*|boolean}
 */
export function isInvisibleAtom(atom) {
  const atomContent = getContent(atom);
  const flags = get(atomContent, "flags");
  return flags && flags.contains(vocab.WONMATCH.NoHintForCounterpartCompacted);
}

export function isOrganization(atom) {
  const atomContent = getContent(atom);
  const types = get(atomContent, "type");

  return types && types.has("s:Organization");
}

export function isPersona(atom) {
  const atomContent = getContent(atom);
  const types = get(atomContent, "type");

  return types && types.has(vocab.WON.PersonaCompacted);
}

export function isRole(atom) {
  const atomContent = getContent(atom);
  const types = get(atomContent, "type");

  return types && types.has("s:Role");
}

export function isServiceAtom(atom) {
  const atomContent = getContent(atom);
  const types = get(atomContent, "type");

  return types && types.has(vocab.BOT.ServiceAtomCompacted);
}

export function isAtom(atom) {
  const atomContent = getContent(atom);
  const types = get(atomContent, "type");

  return types && types.has(vocab.WON.AtomCompacted);
}

export function hasChatSocket(atom) {
  return hasSocket(atom, vocab.CHAT.ChatSocketCompacted);
}

export function hasGroupSocket(atom) {
  return hasSocket(atom, vocab.GROUP.GroupSocketCompacted);
}

export function hasHoldableSocket(atom) {
  for (const holdableSocketType of vocab.holdableSocketTypes) {
    if (hasSocket(atom, holdableSocketType)) {
      return true;
    }
  }

  return false;
}

export function hasExpertiseOfSocket(atom) {
  return hasSocket(atom, vocab.WXPERSONA.ExpertiseOfSocketCompacted);
}

export function hasInterestOfSocket(atom) {
  return hasSocket(atom, vocab.WXPERSONA.InterestOfSocketCompacted);
}

export function hasHolderSocket(atom) {
  return hasSocket(atom, vocab.HOLD.HolderSocketCompacted);
}

export function hasBuddySocket(atom) {
  return hasSocket(atom, vocab.BUDDY.BuddySocketCompacted);
}

export function hasPartnerActivitySocket(atom) {
  return hasSocket(atom, vocab.VALUEFLOWS.PartnerActivitySocketCompacted);
}

export function hasSocket(atom, socket) {
  const sockets = getSockets(atom);
  return sockets && sockets.contains(socket);
}

export function getChatSocket(atom) {
  return getSocketUri(atom, vocab.CHAT.ChatSocketCompacted);
}

export function getPartnerActivitySocket(atom) {
  return getSocketUri(atom, vocab.VALUEFLOWS.PartnerActivitySocketCompacted);
}

export function getGroupSocket(atom) {
  return getSocketUri(atom, vocab.GROUP.GroupSocketCompacted);
}

export function hasSuggestedConnections(atom) {
  return !!getConnections(atom).find(conn => connectionUtils.isSuggested(conn));
}

export function getSuggestedConnections(atom, socketType) {
  return getConnections(atom, socketType).filter(conn =>
    connectionUtils.isSuggested(conn)
  );
}

export function getRequestReceivedConnections(atom, socketType) {
  return getConnections(atom, socketType).filter(conn =>
    connectionUtils.isRequestReceived(conn)
  );
}

export function hasUnreadSuggestedConnections(atom) {
  return !!getConnections(atom).find(
    conn => connectionUtils.isSuggested(conn) && connectionUtils.isUnread(conn)
  );
}

export function getConnectionBySocketUris(atom, socketUri, targetSocketUri) {
  return getConnections(atom).find(
    conn =>
      connectionUtils.hasSocketUri(conn, socketUri) &&
      connectionUtils.hasTargetSocketUri(conn, targetSocketUri)
  );
}

// to be used on personas
export function hasUnreadBuddyRequests(atom) {
  return !!getConnections(atom).find(
    conn =>
      connectionUtils.isRequestReceived(conn) ||
      connectionUtils.isRequestSent(conn)
  );
}

/**
 * Generates an array that contains all atom flags, using a human readable label if available.
 */
export function generateFullFlagLabels(atomImm) {
  const flags = atomImm && atomImm.getIn(["content", "flags"]);
  const flagsArray =
    flags &&
    flags
      // use nicer socket labels if available
      // TODO: remove this to match RDF state?
      .map(wonLabelUtils.getFlagLabel)
      .toArray();
  return flagsArray;
}

/**
 * Generates an array that contains all atom sockets, using a human readable label if available.
 */
export function generateFullSocketLabels(atomImm) {
  const sockets = getSockets(atomImm);
  const socketsArray =
    sockets &&
    sockets
      // use nicer socket labels if available
      // TODO: remove this to match RDF state?
      .map(wonLabelUtils.getSocketLabel)
      .toArray();
  return socketsArray;
}

/**
 * Retrieves the Label of the used useCase as an atomType, if no usecase is specified we check if atom is a searchAtom
 * @param {*} atomImm the atom as saved in the state
 */
export function generateTypeLabel(atomImm, defaultType) {
  const useCaseLabel = useCaseUtils.getUseCaseLabel(
    getMatchedUseCaseIdentifier(atomImm)
  );

  if (useCaseLabel) {
    return useCaseLabel;
  }
  return defaultType ? defaultType : "";
}

/**
 * Generates an array that contains some atom sockets, using a human readable label if possible.
 */
export function generateShortSocketLabels(atomImm) {
  const sockets = getSockets(atomImm);
  const socketsArray =
    sockets &&
    sockets
      // rename sockets
      // TODO: check if this can be used anywhere or whether it should be Group Chat Enabled
      .filter(socket => socket === vocab.GROUP.GroupSocketCompacted)
      .map(wonLabelUtils.getSocketLabel)
      .toArray();
  return socketsArray;
}

/**
 * Generates an array that contains some atom flags, using a human readable label if possible.
 */
export function generateShortFlagLabels(atomImm) {
  const flags = atomImm && atomImm.getIn(["content", "flags"]);
  const flagsArray =
    flags &&
    flags

      // rename flags
      // TODO: flags should have explanatory hovertext
      .filter(
        flag =>
          flag === vocab.WONMATCH.NoHintForCounterpartCompacted ||
          flag === vocab.WONMATCH.NoHintForMeCompacted
      )
      .map(wonLabelUtils.getFlagLabel)
      .toArray();
  return flagsArray;
}

export function getSockets(atomImm) {
  return getIn(atomImm, ["content", "sockets"]) || Immutable.Map();
}

export function getSocketsWithKeysReset(atomImm) {
  const sockets = getSockets(atomImm);

  return sockets ? getSocketKeysReset(sockets) : undefined;
}

export function getSocketUri(atomImm, socketType) {
  const sockets = socketType && getSockets(atomImm);

  return (
    sockets &&
    sockets
      .filter(type => type === socketType)
      .keySeq()
      .first()
  );
}

export function getSocketType(atomImm, socketUri) {
  return getIn(atomImm, ["content", "sockets", socketUri]);
}

export function getHeldByUri(atomImm) {
  for (const holdableSocketType of vocab.holdableSocketTypes) {
    const heldAtomHoldableConnections =
      hasSocket(atomImm, holdableSocketType) &&
      getConnectedConnections(atomImm, holdableSocketType);
    if (heldAtomHoldableConnections && heldAtomHoldableConnections.size === 1) {
      return connectionUtils.getTargetAtomUri(
        heldAtomHoldableConnections.first()
      );
    }
  }
  return undefined;
}

export function getOrganizationUriForRole(atomImm) {
  if (!isRole(atomImm)) {
    return;
  }
  const organizationRoleOfConnections =
    hasSocket(atomImm, vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted) &&
    getConnectedConnections(
      atomImm,
      vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted
    );
  if (
    organizationRoleOfConnections &&
    organizationRoleOfConnections.size === 1
  ) {
    return connectionUtils.getTargetAtomUri(
      organizationRoleOfConnections.first()
    );
  } else {
    return undefined;
  }
}

export function isHeld(atomImm) {
  return !!getHeldByUri(atomImm);
}

export function getSeeksSocketsWithKeysReset(atomImm) {
  const sockets = getIn(atomImm, ["seeks", "sockets"]);

  if (sockets) {
    return getSocketKeysReset(sockets);
  }
  return undefined;
}

function getSocketKeysReset(socketsImm) {
  //TODO: Needs to be generic somehow, otherwise every socket that is added would not be able to be reset correctly
  return socketsImm.mapKeys(key => key.substr(key.lastIndexOf("#")));
}

/**
 * Return all non-closed connections of the given atom
 * @param atomImm immutable atom that stores connections
 * @param socketType compactedSocketType Uri (senderSocket)
 */
export function getNonClosedConnections(atomImm, socketType) {
  const connections = getConnections(atomImm, socketType);

  return (
    connections && connections.filter(conn => !connectionUtils.isClosed(conn))
  );
}

export function getConnection(atomImm, connectionUri) {
  return getIn(atomImm, ["connections", connectionUri]);
}

/**
 * Return all connections of the given atom
 * @param atomImm immutable atom that stores connections
 * @param socketType compactedSocketType Uri (senderSocket)
 */
export function getConnections(atomImm, socketType) {
  const socketUri = getSocketUri(atomImm, socketType);

  const connections = get(atomImm, "connections");

  if (socketUri) {
    return connections
      ? connections.filter(conn =>
          connectionUtils.hasSocketUri(conn, socketUri)
        )
      : Immutable.Map();
  }

  return socketType || !connections ? Immutable.Map() : connections;
}

/**
 * Return all Active (non-closed) connections of the given atom
 * @param atomImm immutable atom that stores connections
 * @param socketType compactedSocketType Uri (senderSocket)
 */
export function getConnectedConnections(atomImm, socketType) {
  return getConnections(atomImm, socketType).filter(conn =>
    connectionUtils.isConnected(conn)
  );
}

export function getAllNonClosedNonSuggestedChatConnections(atomImm) {
  const chatSocketUri = getChatSocket(atomImm);

  return getConnections(atomImm).filter(
    conn =>
      connectionUtils.hasSocketUri(conn, chatSocketUri) &&
      !(connectionUtils.isClosed(conn) || connectionUtils.isSuggested(conn))
  );
}

export function getAllNonClosedNonSuggestedPartnerActivityConnections(atomImm) {
  const partnerActivitySocketUri = getPartnerActivitySocket(atomImm);

  return getConnections(atomImm).filter(
    conn =>
      connectionUtils.hasSocketUri(conn, partnerActivitySocketUri) &&
      !(connectionUtils.isClosed(conn) || connectionUtils.isSuggested(conn))
  );
}

export function getAllConnectionsWithTargetSocketUri(atomImm, targetSocketUri) {
  return getConnections(atomImm).filter(conn =>
    connectionUtils.hasTargetSocketUri(conn, targetSocketUri)
  );
}

export function getAllConnectedChatAndGroupConnections(atomImm) {
  const groupSocketUri = getGroupSocket(atomImm);
  const chatSocketUri = getChatSocket(atomImm);

  return getConnections(atomImm)
    .filter(conn => connectionUtils.isConnected(conn))
    .filter(
      conn =>
        connectionUtils.hasSocketUri(conn, chatSocketUri) ||
        connectionUtils.hasSocketUri(conn, groupSocketUri)
    );
}

/**
 * This function checks if the holder of the heldAtom is actually "verified" -> meaning a connected holdable <-> holder connection exists from the heldAtom to the holderAtom
 * and a connected holder <-> holdable connection exists from the holderAtom to the heldAtom
 * @param heldAtom
 * @param holderAtom
 * @returns {*|boolean}
 */
export function isHolderVerified(heldAtom, holderAtom) {
  const heldByUri = getHeldByUri(heldAtom);

  if (holderAtom && heldByUri === getUri(holderAtom)) {
    for (const holdableSocketType of vocab.holdableSocketTypes) {
      const holderSocketType = vocab.holderSockets[holdableSocketType];

      const holderAtomHolderConnections = getConnectedConnections(
        holderAtom,
        holderSocketType
      );

      const connections = holderAtomHolderConnections.filter(
        conn => connectionUtils.getTargetAtomUri(conn) === getUri(heldAtom)
      );

      if (connections && connections.size === 1) {
        return true;
      }
    }
  }

  return false;
}
