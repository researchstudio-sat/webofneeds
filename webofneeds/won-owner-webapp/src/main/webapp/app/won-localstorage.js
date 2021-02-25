/**
 * Created by fsuda on 25.04.2018.
 */
const READ_URIS = "wonReadUris";
const DISCLAIMER_ACCEPTED = "disclaimerAccepted";
const DELETED_URIS = "wonDeletedUris";

let deletedUrisCache;
let readUrisCache;
let disclaimerAcceptedCache;

export function markUriAsDeleted(uri) {
  if (!isUriDeleted(uri)) {
    deletedUrisCache = getDeletedUris();
    deletedUrisCache.push(uri);
    localStorage.setItem(DELETED_URIS, JSON.stringify(deletedUrisCache));
  }
}

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

export function getDeletedUris() {
  if (!deletedUrisCache) {
    let deletedUrisString = localStorage.getItem(DELETED_URIS);

    if (deletedUrisString) {
      try {
        deletedUrisCache = JSON.parse(deletedUrisString);
      } catch (e) {
        localStorage.removeItem(DELETED_URIS);
        deletedUrisCache = [];
      }
    } else {
      deletedUrisCache = [];
    }
  }

  return deletedUrisCache;
}

export function isUriRead(uri) {
  return getReadUris().includes(uri);
}

export function isUriDeleted(uri) {
  return getDeletedUris().includes(uri);
}

export function clearReadUris() {
  readUrisCache = undefined;
  localStorage.removeItem(READ_URIS);
}

export function clearDeletedUris() {
  deletedUrisCache = undefined;
  localStorage.removeItem(DELETED_URIS);
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
