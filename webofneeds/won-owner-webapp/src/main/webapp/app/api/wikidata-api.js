import rdfFetch from "@rdfjs/fetch";

export function searchWikiData(searchString, language = "en", limit = 20) {
  const url = `https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encodeURIComponent(
    searchString
  )}&format=json&language=${language}&limit=${limit}&origin=*`;

  return fetch(url).then(resp => resp.json());
}

export function fetchWikiData(uri, format = "ttl") {
  const entityId = uri.substr(uri.lastIndexOf("/") + 1);
  const specialDataUrl = `https://www.wikidata.org/wiki/Special:EntityData/${entityId}.${format}?flavor=dump`;

  return rdfFetch(specialDataUrl).then(response => response.dataset());
}
