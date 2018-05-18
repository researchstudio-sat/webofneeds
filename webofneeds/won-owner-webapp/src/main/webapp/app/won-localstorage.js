/**
 * Created by fsuda on 25.04.2018.
 */
const READ_URIS = "wonReadUris";

export function markUriAsRead(uri) {
  //TODO: BETTER IMPL
  if (!isUriRead(uri)) {
    let readUrisString = localStorage.getItem(READ_URIS);
    if (!readUrisString) {
      readUrisString = JSON.stringify([uri]);
    } else {
      try {
        let readUriList = JSON.parse(readUrisString);
        readUriList.push(uri);
        readUrisString = JSON.stringify(readUriList);
      } catch (e) {
        clearReadUris();
        readUrisString = JSON.stringify([uri]);
      }
    }

    localStorage.setItem(READ_URIS, readUrisString);
  }
}

export function isUriRead(uri) {
  //TODO: BETTER IMPL
  let readUrisString = localStorage.getItem(READ_URIS);

  if (readUrisString) {
    let readUriList = JSON.parse(readUrisString);

    for (const readUri of readUriList) {
      if (readUri === uri) {
        return true;
      }
    }
  }
  return false;
}

export function clearReadUris() {
  localStorage.removeItem(READ_URIS);
}

export function clearPrivateId() {
  localStorage.removeItem("privateId");
}

export function savePrivateId(privateId) {
  localStorage.setItem("privateId", privateId);
}
