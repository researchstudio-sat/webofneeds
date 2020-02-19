/**
 * Created by fsuda on 25.04.2018.
 */
const READ_URIS = "wonReadUris";
const DISCLAIMER_ACCEPTED = "disclaimerAccepted";

let readUrisCache;
let disclaimerAcceptedCache;

export function markUriAsRead(uri) {
  if (!isUriRead(uri)) {
    readUrisCache = getReadUris();
    readUrisCache.push(uri);
    localStorage.setItem(READ_URIS, JSON.stringify(readUrisCache));
  }
}

export function getReadUris() {
  if (!readUrisCache) {
    let readUrisString = localStorage.getItem(READ_URIS);

    if (readUrisString) {
      try {
        readUrisCache = JSON.parse(readUrisString);
      } catch (e) {
        localStorage.removeItem(READ_URIS);
        readUrisCache = [];
      }
    } else {
      readUrisCache = [];
    }
  }

  return readUrisCache;
}

export function isUriRead(uri) {
  return getReadUris().includes(uri);
}

export function clearReadUris() {
  readUrisCache = undefined;
  localStorage.removeItem(READ_URIS);
}

export function isDisclaimerAccepted() {
  if (!disclaimerAcceptedCache) {
    disclaimerAcceptedCache = !!localStorage.getItem(DISCLAIMER_ACCEPTED);
  }
  return disclaimerAcceptedCache;
}

export function setDisclaimerAccepted() {
  disclaimerAcceptedCache = true;
  localStorage.setItem(DISCLAIMER_ACCEPTED, disclaimerAcceptedCache);
}

export function clearDisclaimerAccepted() {
  disclaimerAcceptedCache = undefined;
  localStorage.removeItem(DISCLAIMER_ACCEPTED);
}
