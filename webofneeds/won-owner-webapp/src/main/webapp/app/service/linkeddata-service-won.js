/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * Created by fkleedorfer on 05.09.2014.
 */
import { is, clone, getIn, get } from "../utils.js";

import { ownerBaseUrl } from "~/config/default.js";
import urljoin from "url-join";

import rdfstore from "../../scripts/rdfstore-js/rdf_store.js";
import jsonld from "jsonld/dist/jsonld.js";
import won from "./won.js";
import vocab from "./vocab.js";

(function() {
  const NEWLINE_REPLACEMENT_STRING = "#%§%#§";
  const NEWLINE_REPLACEMENT_PATTERN = /#%§%#§/gm;
  const DOUBLEQUOTE_REPLACEMENT_STRING = "§#%§%#";
  const DOUBLEQUOTE_REPLACEMENT_PATTERN = /§#%§%#/gm;
  /**
   * paging parameters as found
   * [here](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-node-webapp/doc/linked-data-paging.md)
   * @type {string[]}
   */
  const legitQueryParameters = [
    "p",
    "resumebefore",
    "resumeafter",
    "type",
    "state",
    "timeof",
    "deep",
  ];

  /**
   * taken from: https://esdiscuss.org/topic/es6-iteration-over-object-values
   *
   * example usage:
   *
   * ```javascript
   * for (let [key, value] of entries(o)) {
   *   console.log(key, ' --> ', value)
   * }
   * ```
   * @param obj the object to generate a (key,value)-pair iterator for
   */
  function* entries(obj) {
    for (let key of Object.keys(obj)) {
      yield [key, obj[key]];
    }
  }

  /**
   * This function is used to generate the query-strings.
   * Should anything about the way the API is accessed changed,
   * adapt this function.
   * @param dataUri
   * @param queryParams a config object whose fields get appended as get parameters.
   *     important parameters include:
   *         * requesterWebId: the WebID used to access the ressource (used
   *                 by the owner-server to pick the right key-pair)
   *         * deep: 'true' to automatically resolve containers (e.g.
   *                 the message-container)
   *         * paging parameters as found
   *           [here](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-node-webapp/doc/linked-data-paging.md)
   * @returns {string}
   */
  function queryString(dataUri, queryParams = {}) {
    let queryOnOwner = urljoin(ownerBaseUrl, "/rest/linked-data/") + "?";

    if (queryParams.requesterWebId) {
      queryOnOwner +=
        "requester=" + encodeURIComponent(queryParams.requesterWebId) + "&";
    }

    // The owner hands this part -- the one in the `uri=` paramater -- directly to the node.
    let firstParam = true;
    let queryOnNode = dataUri;

    const contains = (arr, el) => {
      return arr.indexOf(el) > 0;
    };

    for (let [paramName, paramValue] of entries(queryParams)) {
      if (contains(legitQueryParameters, paramName)) {
        queryOnNode = queryOnNode + (firstParam ? "?" : "&");
        firstParam = false;
        queryOnNode = queryOnNode + paramName + "=" + paramValue;
      }
    }

    let query = queryOnOwner + "uri=" + encodeURIComponent(queryOnNode);

    // server can't resolve uri-encoded colons. revert the encoding done in `queryString`.
    query = query.replace(new RegExp("%3A", "g"), ":");

    return query;
  }

  const privateData = {};

  won.clearStore = function() {
    //create an rdfstore-js based store as a cache for rdf data.
    privateData.store = rdfstore.create();
    privateData.store.setPrefix(vocab.WONMSG.prefix, vocab.WONMSG.baseUri);
    privateData.store.setPrefix(
      "rdf",
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    );
    privateData.store.setPrefix(
      "rdfs",
      "http://www.w3.org/2000/01/rdf-schema#"
    );
    privateData.store.setPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    privateData.store.setPrefix(vocab.WON.prefix, vocab.WON.baseUri);

    window.store4dbg = privateData.store;

    privateData.readUpdateLocksPerUri = {}; //uri -> ReadUpdateLock
    privateData.cacheStatus = {}; //uri -> {timestamp, cacheItemState}
    privateData.documentToGraph = {}; // document-uri -> Set<uris of contained graphs>
  };

  won.clearStoreWithPromise = async function() {
    //TODO: CHECK IF THERE ARE CURRENTLY LOCKS IN PLACE
    won.clearStore();
  };

  /**
   * OK: fully fetched
   * DIRTY: has changed
   * UNRESOLVABLE: sthg went wrong during fetching
   * FETCHING: a request has been sent but not returned yet
   * PARTIALLY_FETCHED: the request succeeded but only a part of the ressource was requested (i.e. ld-pagination was used)
   * @type {{OK: number, DIRTY: number, UNRESOLVABLE: number, FETCHING: number, PARTIALLY_FETCHED: number}}
   */
  const CACHE_ITEM_STATE = {
    OK: 1,
    DIRTY: 2,
    UNRESOLVABLE: 3,
    FETCHING: 4,
    PARTIALLY_FETCHED: 5,
  };

  won.clearStore();

  /**
   * An emulation of a lock that can be acquired by any number of readers
   * as long as there is no updater trying to acquire it. An updater that tries
   * to acquire the lock is blocked until all readers have released their lock.
   * All updaters acquiring the lock are blocked until the update function is execeuted,
   * then all writers are unblocked.
   *
   * Clients have to make sure to call releaseReadLock() or releaseWriteLock() after their
   * critical section is finished, including all promises that were created in them.
   *
   * @constructor
   */
  const ReadUpdateLock = function(uri) {
    //arrays holding deferred objects until they may proceeed
    this.uri = uri;
    this.blockedUpdaters = [];
    this.blockedReaders = [];
    //number of readers currently in possession of the lock
    this.activeReaderCount = 0;
    this.activeUpdaterCount = 0;
    this.updateInProgress = false;
  };
  ReadUpdateLock.prototype = {
    constructor: won.ReadUpdateLock,
    acquireReadLock: function() {
      let deferred = {};
      let promise = new Promise((resolve, reject) => {
        deferred = { resolve, reject };
      });
      if (this.updateInProgress || this.blockedUpdaters.length > 0) {
        //updates are already in progress or are waiting. block.
        this.blockedReaders.push(deferred);
        this.grantLockToUpdaters();
      } else {
        //nobody wishes to update the resource, the caller may read it
        //add the deferred execution to the blocked list, just in case
        //there are others blocket there, and then grant access to all
        this.blockedReaders.push(deferred);
        this.grantLockToReaders();
      }
      return promise;
    },
    acquireUpdateLock: function() {
      let deferred = {};
      let promise = new Promise((resolve, reject) => {
        deferred = { resolve, reject };
      });

      if (this.activeReaderCount > 0) {
        //readers are present, we have to wait till they are done
        this.blockedUpdaters.push(deferred);
      } else {
        //add the deferred update to the list of blocked updates just
        //in case there are more, then grant the lock to all of them
        this.blockedUpdaters.push(deferred);
        this.grantLockToUpdaters();
      }

      return promise;
    },
    releaseReadLock: function() {
      this.activeReaderCount--;
      if (this.activeReaderCount < 0) {
        throw { message: "Released a read lock that was never acquired" };
      } else if (this.activeReaderCount == 0) {
        //no readers currently have a lock: we can update - if we should
        this.grantLockToUpdaters();
      }
    },
    releaseUpdateLock: function() {
      this.activeUpdaterCount--;
      if (this.activeUpdaterCount < 0) {
        throw { message: "Released an update lock that was never acquired" };
      } else if (this.activeUpdaterCount == 0) {
        //no readers currently have a lock: we can update - if we should
        this.updateInProgress = false;
        this.grantLockToReaders();
      }
    },
    grantLockToUpdaters: function() {
      if (this.blockedUpdaters.length > 0 && !this.updateInProgress) {
        //there are blocked updaters. let them proceed.
        this.updateInProgress = true;

        this.activeUpdaterCount += this.blockedUpdaters.length;
        for (const promise of this.blockedUpdaters) {
          promise.resolve();
        }
        this.blockedUpdaters = [];
      }
    },
    grantLockToReaders: function() {
      if (this.blockedReaders.length > 0) {
        //there are blocked readers. let them proceed.
        this.activeReaderCount += this.blockedReaders.length;
        for (const promise of this.blockedReaders) {
          promise.resolve();
        }
        this.blockedReaders = [];
      }
    },
  };

  /**
   * We got the rdf but didn't know the uri of the resource
   * first (e.g. due to server-side bundling like http-2)
   * Preferably use `cacheItemMarkAccessed` as it's
   * undefined-check might catch some bugs early.
   *
   * @param uri
   * @param partial
   */
  const cacheItemInsertOrOverwrite = function(uri, partial) {
    privateData.cacheStatus[uri] = {
      timestamp: new Date().getTime(),
      state: partial ? CACHE_ITEM_STATE.PARTIALLY_FETCHED : CACHE_ITEM_STATE.OK,
    };
  };

  const cacheItemIsOkOrUnresolvableOrFetching = function(uri) {
    const entry = privateData.cacheStatus[uri];
    return (
      entry &&
      (entry.state === CACHE_ITEM_STATE.OK ||
        entry.state === CACHE_ITEM_STATE.UNRESOLVABLE ||
        entry.state === CACHE_ITEM_STATE.FETCHING)
    );
    /*
        const ret = false;
        if (typeof entry === 'undefined') {
            ret = false
        } else {
            ret =
                entry.state === CACHE_ITEM_STATE.OK ||
                entry.state === CACHE_ITEM_STATE.UNRESOLVABLE ||
                entry.state === CACHE_ITEM_STATE.FETCHING;
        }
        const retStr = (ret + "     ").substr(0,5);
        return ret;
        */
  };

  const cacheItemMarkAccessed = function(uri) {
    const entry = privateData.cacheStatus[uri];
    if (typeof entry === "undefined") {
      const message = "Trying to mark unloaded uri " + uri + " as accessed";
      throw { message };
    } else if (entry.state === CACHE_ITEM_STATE.DIRTY) {
      const message =
        "Trying to mark uri " + uri + " as accessed, but it is already dirty";
      throw { message };
    }
    privateData.cacheStatus[uri].timestamp = new Date().getTime();
  };

  const cacheItemMarkFetching = function(uri) {
    privateData.cacheStatus[uri] = {
      timestamp: new Date().getTime(),
      state: CACHE_ITEM_STATE.FETCHING,
    };
  };

  const cacheItemRemove = function(uri) {
    delete privateData.cacheStatus[uri];
  };

  const getReadUpdateLockPerUri = function(uri) {
    let lock = privateData.readUpdateLocksPerUri[uri];
    if (typeof lock === "undefined" || lock == null) {
      lock = new ReadUpdateLock(uri);
      privateData.readUpdateLocksPerUri[uri] = lock;
    }
    return lock;
  };

  const getReadUpdateLocksPerUris = function(uris) {
    const locks = [];
    uris.map(function(uri) {
      locks.push(getReadUpdateLockPerUri(uri));
    });
    return locks;
  };

  /**
   * Acquires all locks, returns an array of promises.
   * @param locks
   * @returns {Array|*}
   */
  const acquireReadLocks = function(locks) {
    const acquiredLocks = [];
    locks.map(function(lock) {
      const promise = lock.acquireReadLock();
      acquiredLocks.push(promise);
    });
    return acquiredLocks;
  };

  /**
   * @deprecated doesn't really return a promise. use buildRejectionMessage instead.
   * Checks the query results (success, data) as returned by store.execute or store.node
   * and assuming that we are inside a deferred execution, calls q.reject if the
   * query failed.
   * returns: true if a reject happened, false otherwise
   * options: object with the following keys:
   * * allowNone: boolean - if false, empty data is an error
   * * allowMultiple: boolean - if false, more than one result in data is an error
   * * message: string - if set, the message is prepended to the generic error message (with 1 whitespace in between)
   */
  const rejectIfFailed = function(success, data, options) {
    const rejectionMessage = buildRejectionMessage(success, data, options);
    // observation: the error happens for #targetConnection property of suggested connection, but this
    // property is really not there (and should not be), so in that case it's not an error...
    return !!rejectionMessage;
  };

  function buildRejectionMessage(success, data, options) {
    let errorMessage = null;
    if (typeof options === "undefined" || options == null) {
      options = {};
    }
    if (!options.message) {
      options.message = "Query failed.";
    }
    if (!success) {
      errorMessage = "Query failed: " + data;
    } else if (
      typeof options.allowNone !== undefined &&
      options.allowNone == false &&
      data.length == 0
    ) {
      errorMessage = "No results found.";
    } else if (
      typeof options.allowMultiple !== undefined &&
      options.allowMultiple == false &&
      data.length > 1
    ) {
      errorMessage = "More than one result found.";
    }
    if (errorMessage === null) {
      return "";
    } else {
      // observation: the error happens for #targetConnection property of suggested connection, but this
      // property is really not there (and should not be), so in that case it's not an error...
      return options.message + " " + errorMessage;
    }
  }

  /**
   * Evaluates the specified property path via sparql on the default graph starting with the specified base uri.
   * Returns all solutions of the path.
   * @param baseUri
   * @param propertyPath
   * @param optionalSparqlPrefixes
   * @param optionalSparqlFragment
   * @returns {*}
   */
  won.resolvePropertyPathFromBaseUri = function resolvePropertyPath(
    baseUri,
    propertyPath,
    optionalSparqlPrefixes,
    optionalSparqlFragment
  ) {
    let query = "";
    if (won.isNull(baseUri)) {
      throw new Error("cannot evaluate property path: baseUri is null");
    }
    if (won.isNull(propertyPath)) {
      throw new Error("cannot evaluate property path: propertyPath is null");
    }
    if (!won.isNull(optionalSparqlPrefixes)) {
      query = query + optionalSparqlPrefixes;
    }
    query =
      query +
      "SELECT ?target where { \n" +
      "<::baseUri::> " +
      propertyPath +
      " ?target. \n";
    if (!won.isNull(optionalSparqlFragment)) {
      query = query + optionalSparqlFragment;
    }
    query = query + "} ";
    query = query.replace(/::baseUri::/g, baseUri);
    const resultObject = {};
    privateData.store.execute(query, [], [], function(success, results) {
      resultObject.result = [];
      results.forEach(function(elem) {
        resultObject.result.push(elem.target.value);
      });
    });
    return resultObject.result;
  };

  /**
   * Fetches the linked data for the specified URI and saves it in the local triple-store.
   * @param uri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {*}
   */
  won.ensureLoaded = async function(uri, fetchParams = {}) {
    if (!uri) {
      throw { message: "ensureLoaded: uri must not be null" };
    }

    //we allow suppressing the fetch - this is used when the data to be accessed is
    //known to be present in the local store
    if (fetchParams.doNotFetch) {
      cacheItemInsertOrOverwrite(uri, false);
      return Promise.resolve(uri);
    }
    /*
         * we also allow unresolvable resources, so as to avoid re-fetching them.
         * we also allow resources that are currently being fetched.
         * as containers (lists in rdf) are inherently prone to change and will
         * usually be accessed using some sort of paging, we skip them here
         * and thus always reload them.
         */
    const partialFetch = fetchesPartialRessource(fetchParams);

    // we might not even need to aquire a lock, if another call to ensureLoaded
    // has already finished.
    if (cacheItemIsOkOrUnresolvableOrFetching(uri)) {
      cacheItemMarkAccessed(uri);
      return uri;
    }

    const lock = getReadUpdateLockPerUri(uri);
    await lock.acquireUpdateLock();

    try {
      // lock has been aquired, but do we still need to load the resource?
      if (cacheItemIsOkOrUnresolvableOrFetching(uri)) {
        // another call to ensureLoaded has finished while we have been aquiring the lock.
        cacheItemMarkAccessed(uri);
        return uri;
      } else {
        // ok, we actually need to load the resource
        cacheItemMarkFetching(uri);
        const dataset = await loadFromOwnServerIntoCache(
          uri,
          fetchParams,
          true
        );

        if (!get(fetchParams, "deep")) {
          cacheItemInsertOrOverwrite(uri, partialFetch);
          return uri;
        } else {
          const allLoadedResources = await selectLoadedDocumentUrisFromDataset(
            dataset
          );
          allLoadedResources.forEach(resourceUri => {
            /*
                            * only mark root resource as partial.
                            * the other ressources should
                            * have been fetched fully during a
                            * request with the `deep`-flag
                            */
            cacheItemInsertOrOverwrite(
              resourceUri,
              partialFetch && resourceUri === uri
            );
          });
          return allLoadedResources;
        }
      }
    } catch (e) {
      rethrow(e, "Failed to fetch " + uri);
    } finally {
      lock.releaseUpdateLock();
    }
  };

  function fetchesPartialRessource(requestParams) {
    if (requestParams.pagingSize) {
      return true;
    } else {
      const qp = requestParams.queryParams || requestParams;
      return !!(
        qp["p"] ||
        qp["resumebefore"] ||
        qp["resumeafter"] ||
        qp["type"] ||
        qp["state"] ||
        qp["timeof"]
      );
    }
  }

  /**
   * When you have loaded multiple documents via `deep=true`,
   * use this function to identify which documents you have
   * actually loaded this way.
   *
   * Note: this function and uses thereof can become deprecated
   * if we'd use HTTP/2
   *
   * @param {*} dataset
   */
  function selectLoadedDocumentUrisFromDataset(dataset) {
    /*
         * create a temporary store to load the dataset into
         * so we only query over the new triples
         */
    const tmpstore = rdfstore.create();
    const storeWithDatasetP = loadIntoRdfStore(
      tmpstore,
      "application/ld+json",
      dataset
    ).then(() => tmpstore);

    return storeWithDatasetP.then(tmpstore => {
      const queryPromise = executeQueryOnRdfStore(
        tmpstore,
        `
                prefix won: <https://w3id.org/won/core#>
                prefix msg: <https://w3id.org/won/message#>
                prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>
                prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                select distinct ?s where {
                    { ?s rdf:type won:Atom } union
                    { ?s won:connections ?o } union

                    { ?s rdf:type won:Connection } union
                    { ?s won:messageContainer ?o } union

                    { ?s rdfs:member ?o } union
                    { ?s rdf:type won:MessageContainer } union

                    { ?s rdf:type msg:FromOwner } union
                    { ?s rdf:type msg:FromSystem } union
                    { ?s rdf:type msg:FromExternal } union
                    { ?s msg:messageType ?o } union
                    { ?s won:correspondingRemoteMessage ?o } union
                    { ?s won:recipient ?o }.
                }`
      );

      //final cleanup and return
      return queryPromise.then(queryResults =>
        queryResults.map(r => r.s.value)
      );
    });
  }

  /**
   * Note: this is a hack, as it heavily depends on the structure of the
   * dataset, the structure of uris and the internal structure of the rdfstore.
   * The much cleaner, more standardized and planned alternative is to use HTTP/2
   * on the server and replace the entire cache-system implemented here with that.
   *
   * @param {*} data
   * @returns a map from document-uri to a set of contained graph-uris
   */
  async function selectContainedDocumentAndGraphUrisHACK(data) {
    const loadedDocumentUris = await selectLoadedDocumentUrisFromDataset(data);

    const tmpstore = rdfstore.create(); // TODO reuse tmpstore from `selectLoadedDocumentUrisFromDataset`
    await loadIntoRdfStore(tmpstore, "application/ld+json", data);

    const baseUriForEvents = getIn(data, ["@context", "event"]);
    if (!baseUriForEvents) {
      throw new Error(
        "Couldn't resolve the 'event'-prefix needed " +
          "for the document-uris to graph-uris hack."
      );
    }

    const mappingsPromises = loadedDocumentUris.map(async docUri => {
      try {
        if (docUri.startsWith("event") || docUri.startsWith(baseUriForEvents)) {
          const messageUri = docUri;
          const queryResult = await executeQueryOnRdfStore(
            tmpstore,
            `
                          prefix event: <${baseUriForEvents}>
                          prefix msg: <https://w3id.org/won/message#>

                          select distinct ?graphOfMessage where {
                              { <${messageUri}> msg:correspondingRemoteMessage ?graphOfMessage } union
                              { ?graphOfMessage msg:correspondingRemoteMessage <${messageUri}> } union
                              { <${messageUri}> msg:forwardedMessage ?graphOfMessage } union
                              { <${messageUri}> msg:forwardedMessage/msg:correspondingRemoteMessage ?graphOfMessage }
                          }
                          `
          );

          const graphUrisOfMessage = queryResult.map(result =>
            getIn(result, ["graphOfMessage", "value"])
          );
          const urisInStoreThatStartWith = uri =>
            Array.from(
              new Set(
                Object.values(tmpstore.engine.lexicon.OIDToUri).filter(u =>
                  u.startsWith(uri)
                )
              )
            );

          const graphUrisInEventDoc = urisInStoreThatStartWith(
            messageUri + "#"
          ).concat(
            graphUrisOfMessage
              .map(uri => urisInStoreThatStartWith(uri + "#"))
              .reduce((arr1, arr2) => arr1.concat(arr2), []) //parse empty array of initial value to avoid exception
          );
          return {
            uri: messageUri,
            containedGraphUris: Array.from(new Set(graphUrisInEventDoc)), //deduplicate
          };
        }
      } catch (ex) {
        console.error(
          "An Exception occured while retrieving the query results",
          ex
        );
        rethrow(ex, `failed to loadDocumentUris due to reason: `);
      }
    });
    const mappingsArray = (await Promise.all(mappingsPromises)).filter(m => m);

    const mappings = {};
    mappingsArray.forEach(
      m => (mappings[m.uri] = new Set(m.containedGraphUris))
    );

    return mappings;
  }

  /**
   *
   * @param uri the uri of the ressource
   * @param params: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @param removeCacheItem
   * @return {Promise}
   */
  function loadFromOwnServerIntoCache(uri, params, removeCacheItem = false) {
    let requestUri = queryString(uri, params);

    return fetch(requestUri, {
      method: "get",
      credentials: "same-origin",
      headers: {
        Accept: "application/ld+json",
        Prefer: params.pagingSize
          ? `return=representation; max-member-count="${params.pagingSize}"`
          : undefined,
      },
    })
      .then(response => {
        if (response.status === 200) return response;
        else {
          let error = new Error(
            `${response.status} - ${
              response.statusText
            } for request ${uri}, ${JSON.stringify(params)}`
          );

          error.response = response;
          throw error;
        }
      })
      .then(dataset => dataset.json())
      .then(
        dataset =>
          //make sure we've got a non-empty dataset
          Object.keys(dataset).length === 0
            ? Promise.reject(
                "failed to load " + uri + ": Object.keys(dataset).length == 0"
              )
            : dataset
      )
      .then(dataset =>
        Promise.resolve()
          .then(() => {
            if (!fetchesPartialRessource(params)) {
              /* as paging is only used for containers
                     * and they don't lose entries, we can
                     * simply merge on top of the already
                     * loaded triples below. So we skip removing
                     * the previously loaded data. For everything
                     * remove any remaining stale data: */
              won.deleteDocumentFromStore(uri, removeCacheItem);
            }
          })
          .then(() => won.addJsonLdData(dataset, uri, get(params, "deep")))
          .then(() => dataset)
      )
      .catch(e => rethrow(e, `failed to load ${uri} due to reason: `));
  }

  /**
   * Stores the json-ld passed as `data` in the rdf-store.
   * It saves everything into the default graph, for running sparql-queries
   * (our rdf-store.js can't do cross-graph queries), saves it into it's seperate
   * graphs for later retrieval on a graph-level (e.g. when retrieving the
   * content-graph of a message), and saves it once into a graph with the documentUri
   * to be able to retrieve all triples belonging to a document (e.g. when it needs to
   * update the cache for a document).
   *
   * @param {*} data
   * @param {*} documentUri
   * @param {boolean} deep whether or not the dataset was loaded with `deep=true` and
   *   thus contains data from multiple documents.
   */
  won.addJsonLdData = async function(data, documentUri, deep) {
    // const graphsAddedSeperatelyP = Promise.resolve();
    const groupedP = groupByGraphs(data);

    const graphsAddedSeperatelyP = groupedP.then(grouped => {
      try {
        //  Save mapping from documentUri to graphUris, e.g. for future deletion operations
        const graphUris = grouped.map(g => g["@id"]);
        saveDocToGraphMapping(documentUri, graphUris);

        // save all the graphs into the rdf store
        return Promise.all(
          grouped.map(g =>
            loadIntoRdfStore(
              privateData.store,
              "application/ld+json",
              g,
              g["@id"]
            )
          )
        );
      } catch (error) {
        rethrow(
          error,
          "Failed to add subgraphs for " +
            documentUri +
            ". jsonld: " +
            JSON.stringify(grouped)
        );
      }
    });

    let triplesAddedToDeepDocumentGraphsP = Promise.resolve();
    if (deep) {
      if (!documentUri.endsWith("/msg")) {
        //TODO hack; looking at uri
        console.error(
          "Adding a dataset loaded with `deep=true` " +
            "that isn't a message-container. The cache will " +
            "be faulty and deletion won't work properly. Uri: ",
          documentUri
        );
      } else {
        const documentToGraphUri = await selectContainedDocumentAndGraphUrisHACK(
          data
        );

        //  Save mapping from documentUri to graphUris, e.g. for future deletion operations
        Object.entries(documentToGraphUri).map(([documentUri, graphUris]) => {
          saveDocToGraphMapping(documentUri, Array.from(graphUris));
        });

        triplesAddedToDeepDocumentGraphsP = groupedP.then(grouped => {
          return Promise.all(
            // iterate over documents contained in the dataset, get the
            // graphs related to those documents and add them to the store.
            Object.entries(documentToGraphUri).map(
              ([documentUri, graphUris]) => {
                const graphs = grouped.filter(graph =>
                  graphUris.has(graph["@id"])
                );
                return loadIntoRdfStore(
                  privateData.store,
                  "application/ld+json",
                  graphs,
                  documentUri
                );
              }
            )
          );
        });

        //triplesAddedToDocumentGraphsP
      }
    }

    const triplesAddedToMainDocumentGraphP = loadIntoRdfStore(
      privateData.store,
      "application/ld+json",
      data,
      documentUri
    );
    const triplesAddedToDefaultGraphP = loadIntoRdfStore(
      privateData.store,
      "application/ld+json",
      data
    );

    // const triplesAddedToDefaultGraphP = Promise.resolve();

    return Promise.all([
      graphsAddedSeperatelyP,
      triplesAddedToDefaultGraphP,
      triplesAddedToMainDocumentGraphP,
      triplesAddedToDeepDocumentGraphsP,
    ]).then(() => undefined); // no return value beyond success or failure of promise
  };

  /**
   *  Save mapping from documentUri to graphUris, e.g. for future deletion operations
   */
  function saveDocToGraphMapping(documentUri, graphUris) {
    const prevGraphUris = privateData.documentToGraph[documentUri];
    if (!prevGraphUris) {
      privateData.documentToGraph[documentUri] = new Set(graphUris);
    } else {
      graphUris.forEach(u => prevGraphUris.add(u));
    }
  }

  /**
   * Loads the atom-data without following up
   * with a request for the connection-container
   * to get the connection-uris. Thus it's faster.
   */
  won.getAtom = atomUri =>
    won
      .ensureLoaded(atomUri)
      .then(
        () =>
          new Promise(resolve =>
            privateData.store.graph(atomUri, (a, b) => resolve(b))
          )
      )
      .then(atomGraph =>
        triples2framedJson(atomUri, atomGraph.triples, {
          /* frame */
          "@id": atomUri, // start the framing from this uri. Otherwise will generate all possible nesting-variants.
          "@context": won.defaultContext,
        })
      )
      .then(atomJsonLd => {
        // usually the atom-data will be in a single object in the '@graph' array.
        // We can flatten this and still have valid json-ld
        const flattenedAtomJsonLd = getIn(atomJsonLd, ["@graph", 0])
          ? getIn(atomJsonLd, ["@graph", 0])
          : atomJsonLd;
        flattenedAtomJsonLd["@context"] = atomJsonLd["@context"]; // keep context

        if (
          !flattenedAtomJsonLd ||
          getIn(flattenedAtomJsonLd, ["@graph", "length"]) === 0
        ) {
          console.error(
            "Received empty graph ",
            atomJsonLd,
            " for atom ",
            atomUri
          );
          return { "@context": flattenedAtomJsonLd["@context"] };
        }

        return flattenedAtomJsonLd;
      });

  function triples2framedJson(atomUri, triples, frame) {
    const jsonldjsQuads = {
      // everything in our rdfstore is in the default-graph atm
      "@default": triples.map(triple => ({
        subject: rdfstorejsToJsonldjs(triple.subject),
        predicate: rdfstorejsToJsonldjs(triple.predicate),
        object: rdfstorejsToJsonldjs(triple.object),
        //object.datatype: ? //TODO
      })),
    };

    const context = frame["@context"] ? clone(frame["@context"]) : {}; //TODO
    context.useNativeTypes = true; //do some of the parsing from strings to numbers

    return jsonld.promises
      .fromRDF(jsonldjsQuads, context)
      .then(complexJsonLd => {
        //the framing algorithm expects an js-object with an `@graph`-property
        const complexJsonLd_ = complexJsonLd["@graph"]
          ? complexJsonLd
          : { "@graph": complexJsonLd };

        return jsonld.promises.frame(complexJsonLd_, frame);
      })
      .then(framed => {
        return framed;
      })
      .catch(err => {
        console.error("Failed to frame atom-data.", atomUri, err);
        throw err;
      });
  }

  /**
   * Receives a subject, predicate or object in rdfstorejs-style
   * and returns it's pendant in jsonldjs-style.
   * @param element
   * @return {{value: *, type: undefined}}
   */
  function rdfstorejsToJsonldjs(element) {
    const result = {
      value: element.nominalValue,
      type: undefined,
    };

    switch (element.interfaceName) {
      case "Literal":
        result.type = "literal";
        break;
      case "NamedNode":
        result.type = "IRI";
        break;
      case "BlankNode":
        result.type = "blank node";
        break;
      default:
        throw new Error(
          "Encountered triple with object of unknown type: " +
            element.object.interfaceName +
            "\n" +
            element.subject.nominalValue +
            " " +
            element.predicate.nominalValue +
            " " +
            element.object.nominalValue +
            " "
        );
    }

    return result;
  }

  function rdfstoreTriplesToString(triples, graphUri) {
    const toToken = x => {
      switch (x.interfaceName) {
        case "NamedNode":
          return `<${x.nominalValue}>`;
        case "Literal":
          return `"${x.nominalValue
            .replace(/(\r\n|\n|\r)/gm, NEWLINE_REPLACEMENT_STRING)
            .replace(/"/gm, DOUBLEQUOTE_REPLACEMENT_STRING)}"`; //TODO: REMOVE FUGLY FIX ONCE n3.js is not having issues with newlines or doublequotes anymore
        case "BlankNode":
          return x.nominalValue;
        default:
          throw new Error("Can't parse token: " + JSON.stringify(x));
      }
    };
    const tripleToString = t =>
      toToken(t.subject) +
      " " +
      toToken(t.predicate) +
      " " +
      toToken(t.object) +
      " " +
      (graphUri ? `<${graphUri}>` : "") +
      ".";

    return triples.map(tripleToString).join("\n");
  }

  won.validateEnvelopeDataForAtom = function(atomUri) {
    if (typeof atomUri === "undefined" || atomUri == null) {
      throw {
        message: "validateEnvelopeDataForAtom: atomUri must not be null",
      };
    }

    return Promise.resolve();
  };

  won.validateEnvelopeDataForConnection = async function(
    socketUri,
    targetSocketUri
  ) {
    if (
      typeof socketUri === "undefined" ||
      socketUri == null ||
      typeof targetSocketUri === "undefined" ||
      targetSocketUri == null
    ) {
      throw {
        message: "getEnvelopeDataforConnection: socketUris must not be null",
      };
    }

    return Promise.resolve();
  };

  won.getConnectionUrisWithStateByAtomUri = (atomUri, requesterWebId) => {
    return won
      .executeCrawlableQuery(
        won.queries["getAllMetaConnectionsOfAtom"],
        atomUri,
        requesterWebId
      )
      .then(result =>
        result.map(x => {
          return {
            connectionUri: x.connectionUri && x.connectionUri.value,
            connectionState: x.connectionState && x.connectionState.value,
            socketUri: x.socketUri && x.socketUri.value,
            socketType: x.socketType && x.socketType.value,
            atomUri: x.atomUri && x.atomUri.value,
            targetAtomUri: x.targetAtomUri && x.targetAtomUri.value,
            targetConnectionUri:
              x.targetConnectionUri && x.targetConnectionUri.value,
            targetSocketUri: x.targetSocketUri && x.targetSocketUri.value,
            targetSocketType: x.targetSocketType && x.targetSocketType.value,
            modified: x.modified && x.modified.value,
          };
        })
      );
  };

  /**
   * @param connectionUri
   * @param fetchParams See `ensureLoaded`.
   * @return {*} the connections predicates along with the uris of associated events
   */
  won.getConnectionWithEventUris = function(connectionUri, fetchParams) {
    if (!is("String", connectionUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          connectionUri
      );
    }
    return (
      won
        .getNode(connectionUri, fetchParams)
        //add the eventUris
        .then(connection =>
          Promise.all([
            Promise.resolve(connection),
            won.getNode(connection.messageContainer, fetchParams),
          ])
        )
        .then(([connection, messageContainer]) => {
          /*
                 * if there's only a single rdfs:member in the event
                 * container, getNode will not return an array, so we
                 * need to make sure it's one from here on out.
                 */
          connection.hasEvents = is("Array", messageContainer.member)
            ? messageContainer.member
            : [messageContainer.member];
          return connection;
        })
    );
  };

  /**
   * @param connectionUri
   * @param fetchParams See `ensureLoaded`.
   * @return {*} the connections predicates along with the uris of associated events
   */
  won.getConnectionWithEventUrisBySocket = function(
    senderSocketUri,
    targetSocketUri,
    fetchParams
  ) {
    if (!is("String", senderSocketUri) || !is("String", targetSocketUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          senderSocketUri +
          " or " +
          targetSocketUri
      );
    }

    fetchParams.socket = senderSocketUri;
    fetchParams.targetSocket = targetSocketUri;

    return (
      won
        .deleteDocumentFromStore(senderSocketUri.split("#")[0] + "/c")
        .then(() =>
          won.getNode(senderSocketUri.split("#")[0] + "/c", fetchParams)
        )
        //add the eventUris
        .then(jsonResp => jsonResp.member)
        .then(connUris => {
          let _connUris;
          if (is("String", connUris)) {
            _connUris = [connUris];
          } else {
            _connUris = connUris;
          }

          const newFetchParams = {
            requesterWebId: fetchParams.requesterWebId,
          };

          return _connUris.map(connUri => {
            return won.getConnectionWithEventUris(connUri, newFetchParams);
          });
        })
    );
  };

  won.getWonMessage = (msgUri, fetchParams) => {
    return won
      .getRawEvent(msgUri, fetchParams)
      .then(rawEvent => won.wonMessageFromJsonLd(rawEvent))
      .catch(e => {
        const msg = "Failed to get wonMessage " + msgUri + ".";
        e.message += msg;
        console.error(e.message);
        throw e;
      });
  };

  window.getWonMessage4dbg = won.getWonMessage;
  window.wonMessageFromJsonLd4dbg = won.wonMessageFromJsonLd;

  won.getRawEvent = (eventUri, fetchParams) => {
    return won
      .ensureLoaded(eventUri, fetchParams)
      .then(() => {
        return Promise.all(
          Array.from(privateData.documentToGraph[eventUri]).map(graphUri => {
            return won
              .getCachedGraphTriples(graphUri)
              .then(graphTriples =>
                jsonld.promises.fromRDF(
                  rdfstoreTriplesToString(graphTriples, graphUri),
                  {
                    format: "application/nquads",
                  }
                )
              )
              .then(parsedJsonLd => {
                if (parsedJsonLd.length !== 1) {
                  throw new Error(
                    "Got more or less than the expected one graph " +
                      JSON.stringify(parsedJsonLd)
                  );
                } else {
                  parsedJsonLd = parsedJsonLd[0];
                }
                let jsonLdString = JSON.stringify(parsedJsonLd)
                  .replace(NEWLINE_REPLACEMENT_PATTERN, "\\n")
                  .replace(DOUBLEQUOTE_REPLACEMENT_PATTERN, '\\"');

                return JSON.parse(jsonLdString); //TODO: REMOVE FUGLY FIX AND JUST RETURN parsedJsonLd ONCE n3.js is not having issues with newlines anymore
              });
          })
        );
      })
      .then(eventGraphs => {
        if (!is("Array", eventGraphs)) {
          throw new Error(
            "event graphs weren't an array. something didn't go as expected: " +
              JSON.stringify(eventGraphs)
          );
        }

        return {
          "@graph": eventGraphs,
          "@context": clone(won.defaultContext),
        };
      });
  };

  window.getRawEvent4dbg = won.getRawEvent;

  /**
   * Fetches the triples where URI is subject and add objects of those triples to the
   * resulting structure by the localname of the predicate.
   * The URI is added as property 'uri'.
   *
   * If a predicate occurs multiple times the objects (in the rdf sense) will be
   * grouped as an array. This is usually the case for rdfs:member, to give an example.
   *
   * NOTE: Atm it ignores prefixes which might lead to clashes.
   *
   * @param uri
   * @param fetchParams See `ensureLoaded`.
   */
  won.getNode = function(uri, fetchParams) {
    if (!uri) {
      return Promise.reject({ message: "getNode: uri must not be null" });
    }

    let releaseLock = undefined;

    return won
      .ensureLoaded(uri, fetchParams)
      .then(() => {
        const lock = getReadUpdateLockPerUri(uri);
        releaseLock = () => lock.releaseReadLock();
        return lock.acquireReadLock();
      })
      .then(() => {
        return new Promise((resolve, reject) => {
          privateData.store.node(uri, function(success, graph) {
            if (!success) {
              reject({
                message:
                  "Error loading node with attributes for URI " + uri + ".",
              });
            } else if (graph.length === 0) {
              /* TODO HACK / WORKAROUND
                             * try query + manual filter. it's slower but the store
                             * occasionally has hiccups where neither `store.node(uri)`
                             * nor `select * where {<uri> ?p ? o}` return triples, but
                             * `select * where { ?s ?p ?o }` contains the the desired
                             * triples. I couldn't find out what's the cause of this
                             * within a feasible time, but hope these issues will go
                             * away, when we'll get around to update the store to the
                             * newest version.
                             */
              privateData.store.graph((success, entireGraph) => {
                resolve(
                  entireGraph.triples.filter(
                    t => t.subject.nominalValue === uri
                  )
                );
              });
            } else {
              //.node(...) was successful. return the node's tripples
              resolve(graph.triples);
            }
          });
        });
      })
      .then(triples => {
        const node = {};
        triples.forEach(triple => {
          //TODO this cropping ignores prefixes, causing predicates/fields to clash!
          const propName = won.getLocalName(triple.predicate.nominalValue);
          if (node[propName]) {
            //encountered multiple occurances of the same predicate, e.g. rdfs:member
            if (!(node[propName] instanceof Array)) {
              //on the first 'clash', instantiate the predicate/property as array
              node[propName] = [node[propName]];
            }
            node[propName].push(triple.object.nominalValue);
          } else {
            node[propName] = triple.object.nominalValue;
          }
        });
        node.uri = uri;
        releaseLock && releaseLock();
        return node;
      })
      .catch(e => {
        releaseLock && releaseLock();
        rethrow(
          e,
          "Couldn't get node " + uri + " with params " + fetchParams + "\n"
        );
      });
  };

  /**
   * Deletes all triples belonging to that particular document (e.g. atom, event, etc)
   * from all graphs.
   */
  won.deleteDocumentFromStore = function(documentUri, removeCacheItem = true) {
    return won
      .getCachedGraphTriples(documentUri) // this retrieval requires addJsonLdData to save everything as a special graph equal to the documentUri
      .catch(e => {
        const msg =
          "Failed to retrieve triples for the document " + documentUri + ".";
        console.error(msg);
        e.message += msg;
        throw e;
      })
      .then(triples => {
        //remove entry from documentToGraph
        delete privateData.documentToGraph[documentUri];

        const urisOfContainedGraphs =
          privateData.documentToGraph[documentUri] || new Set();

        return Promise.all([
          won.deleteTriples(triples), // deletion from default graph
          won.deleteTriples(triples, documentUri), // deletion from document-graph

          //deletion from subgraphs
          ...Array.from(urisOfContainedGraphs).map(graphUri =>
            won.deleteTriples(
              triples,
              graphUri,
              success =>
                success
                  ? Promise.resolve()
                  : Promise.reject(
                      "Failed to delete the following triples from" +
                        " the graph " +
                        graphUri +
                        " contained in document " +
                        documentUri +
                        ": " +
                        JSON.stringify(triples)
                    )
            )
          ),
        ]);
      })
      .catch(e => {
        const msg =
          "Failed to delete the triples for the document " +
          documentUri +
          " from the default graph.";
        console.error(msg);
        e.message += msg;
        throw e;
      })
      .then(() => {
        if (removeCacheItem) {
          // e.g. `ensureLoaded` needs to clear the store but
          // not the cache-status, as it handles that itself
          cacheItemRemove(documentUri);
        }
      });
  };

  /**
   * @param {*} triples in rdfjs interface format
   * @param {string} graphUri if omitted, will remove from default graph
   */
  won.deleteTriples = function(triples, graphUri) {
    return new Promise(resolve => {
      const callback = success => {
        if (!success) {
          throw new Error();
        } else {
          resolve();
        }
      };
      try {
        if (graphUri) {
          privateData.store.delete(triples, graphUri, callback);
        } else {
          privateData.store.delete(triples, callback);
        }
      } catch (e) {
        rethrow(
          "Failed to delete the following triples: " + JSON.stringify(triples)
        );
      }
    });
  };

  won.getCachedGraphTriples = (graphUri, removeAtGraphTriples = true) =>
    rdfStoreGetGraph(privateData.store, graphUri).then(graph => {
      if (removeAtGraphTriples) {
        return (
          graph.triples
            // `store.graph` in `rdfStoreGetGraph` returns some
            // triples with `@graph` as predicate and a string
            // as value, which isn't proper json-ld, so we filter
            // them out here.
            .filter(t => getIn(t, ["predicate", "nominalValue"]) !== "@graph")
        );
      } else {
        return graph.triples;
      }
    });

  /**
   * Thin wrapper around `store.graph` that returns a promise.
   * @param store
   * @param {*} graphUri
   */
  function rdfStoreGetGraph(store, graphUri) {
    return new Promise(resolve => {
      const callback = (success, graph) => {
        if (success) {
          resolve(graph);
        } else {
          throw new Error("Got: " + JSON.stringify(graph));
        }
      };
      try {
        if (graphUri) {
          store.graph(graphUri, callback);
        } else {
          store.graph(callback);
        }
      } catch (e) {
        rethrow(e, "Failed to retrieve graph with uri " + graphUri + ".");
      }
    });
  }

  window.rdfStoreGetGraph4dbg = rdfStoreGetGraph;

  /**
   * Executes the specified crawlableQuery, returns a promise to its results, which may become available
   * after downloading the required content.
   */
  won.executeCrawlableQuery = function(
    crawlableQuery,
    baseUri,
    requesterWebId
  ) {
    const relevantResources = [];
    const recursionData = {};
    const MAX_RECURSIONS = 10;

    const executeQuery = function executeQuery(
      query,
      baseUri,
      relevantResources
    ) {
      query = query.replace(/::baseUri::/g, baseUri);
      const locks = getReadUpdateLocksPerUris(relevantResources);
      const promises = acquireReadLocks(locks);
      return Promise.all(promises).then(function() {
        const resultObject = {};
        try {
          privateData.store.execute(query, [], [], function(success, results) {
            if (
              rejectIfFailed(success, results, {
                message: "Error executing query.",
                allowNone: true,
                allowMultiple: true,
              })
            ) {
              return;
            }
            resultObject.results = results;
          });
          return resultObject.results;
        } catch (e) {
          rethrow("Could not execute query. Reason: " + e, e);
        } finally {
          //release the read locks
          locks.map(function(lock) {
            lock.releaseReadLock();
          });
        }
      });
    };

    const resolvePropertyPathsFromBaseUri = function resolvePropertyPathsFromBaseUri(
      propertyPaths,
      baseUri,
      relevantResources
    ) {
      const locks = getReadUpdateLocksPerUris(relevantResources);
      const promises = acquireReadLocks(locks);
      return Promise.all(promises).then(function() {
        try {
          const resolvedUris = [];
          propertyPaths.map(function(propertyPath) {
            const foundUris = won.resolvePropertyPathFromBaseUri(
              baseUri,
              propertyPath.propertyPath,
              propertyPath.prefixes,
              propertyPath.fragment
            );

            //resolve all property paths, add to 'resolvedUris'
            Array.prototype.push.apply(resolvedUris, foundUris);
          });
          return resolvedUris;
        } catch (e) {
          console.error(e);
        } finally {
          //release the read locks
          locks.map(function(lock) {
            lock.releaseReadLock();
          });
        }
      });
    };

    const resolveOrExecuteQuery = function resolveOrExecuteQuery(resolvedUris) {
      if (won.isNull(recursionData.depth)) {
        recursionData.depth = 0;
      }
      recursionData.depth++;
      if (
        won.containsAll(relevantResources, resolvedUris) ||
        recursionData.depth >= MAX_RECURSIONS
      ) {
        return executeQuery(crawlableQuery.query, baseUri, relevantResources);
      } else {
        Array.prototype.push.apply(relevantResources, resolvedUris);
        const loadedPromises = resolvedUris.map(x =>
          won.ensureLoaded(x, { requesterWebId })
        );
        return Promise.all(loadedPromises)
          .then(() =>
            resolvePropertyPathsFromBaseUri(
              crawlableQuery.propertyPaths,
              baseUri,
              resolvedUris
            )
          )
          .then(function(newlyResolvedUris) {
            return resolveOrExecuteQuery(newlyResolvedUris);
          });
      }
    };

    return resolveOrExecuteQuery([baseUri]);
  };

  /**
   * SPARQL queries and property paths for identifying the resources required for the query to work.
   * All URIs resolved by the specified property paths will locked (using the read-update-lock) and if
   * necessary, downloaded prior to execution of the query.
   *
   * For passing the baseUri to the query, it must contain the placeholder '::baseUri::' (without the quotes).
   *
   * @type {{connectionMessages: {query: string, propertyPaths: *[]}}}
   */
  won.queries = {
    /**
     * Despite the name, returns the connections fo the specified atom themselves. TODO rename
     */
    getAllMetaConnectionsOfAtom: {
      propertyPaths: [
        {
          prefixes:
            "prefix " +
            vocab.WON.prefix +
            ": <" +
            vocab.WON.baseUri +
            "> " +
            "prefix dct: <http://purl.org/dc/terms/> " +
            "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
          propertyPath: "won:connections",
        },
      ],
      query:
        "prefix won: <https://w3id.org/won/core#> \n" +
        "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
        "prefix dct: <http://purl.org/dc/terms/> \n" +
        "select " +
        "?connectionUri " +
        "?connectionState " +
        "?atomUri " +
        "?socketType " +
        "?socketUri " +
        "?targetAtomUri " +
        "?targetConnectionUri " +
        "?targetSocketType " +
        "?targetSocketUri " +
        "?modified " +
        "\n where { \n" +
        " <::baseUri::> a won:Atom; \n" +
        "           won:connections ?connections.\n" +
        "  ?connections rdfs:member ?connectionUri. \n" +
        "  ?connectionUri won:connectionState ?connectionState. \n" +
        "  ?connectionUri won:sourceAtom ?atomUri. \n" +
        "  ?connectionUri won:targetAtom ?targetAtomUri. \n" +
        "  ?connectionUri won:socket ?socketUri. \n" +
        "  ?socketUri won:socketDefinition ?socketType. \n" +
        "  ?connectionUri won:targetSocket ?targetSocketUri. \n" +
        "  ?connectionUri dct:modified ?modified. \n" +
        "  OPTIONAL { ?targetSocketUri won:socketDefinition ?targetSocketType. } \n" +
        "  OPTIONAL { ?connectionUri won:targetConnection ?targetConnectionUri } \n" +
        "} \n",
    },
  };
})();

/*
{
    "@id" : "conn:aavakck2xsm39hr9kxi2",
    "@type" : "won:Connection",
    "http://purl.org/dc/terms/modified" : {
      "@type" : "xsd:dateTime",
      "@value" : "2019-06-18T06:27:20.989Z"
    },
    "won:connectionState" : {
      "@id" : "won:Connected"
    },
    "won:socket" : {
      "@id" : "atom:z6ne170yrf0z#holdableSocket"
    },
    "won:sourceAtom" : {
      "@id" : "atom:z6ne170yrf0z"
    },
    "won:targetAtom" : {
      "@id" : "atom:sxxxgf2necv6"
    },
    "won:targetConnection" : {
      "@id" : "conn:totztqjd99h1vt73mi5n"
    },
    "won:targetSocket" : {
      "@id" : "atom:sxxxgf2necv6#holderSocket"
    }
  }, {
    "@id" : "conn:aboy09l0txxkewixlqtq",
    "@type" : "won:Connection",
    "http://purl.org/dc/terms/modified" : {
      "@type" : "xsd:dateTime",
      "@value" : "2019-06-18T06:27:46.278Z"
    },
    "won:connectionState" : {
      "@id" : "won:Suggested"
    },
    "won:socket" : {
      "@id" : "atom:z6ne170yrf0z#chatSocket"
    },
    "won:sourceAtom" : {
      "@id" : "atom:z6ne170yrf0z"
    },
    "won:targetAtom" : {
      "@id" : "atom:xbszwgx0ey23a9kcm283"
    },
    "won:targetSocket" : {
      "@id" : "atom:xbszwgx0ey23a9kcm283#ChatSocket"
    }
  }
* */

/**
 * Thin wrapper around `rdfstore.load(...)` that returns
 * a promise instead of requiring a callback.
 * @param {RdfStore} store
 * @param {String} mediaType
 * @param {Jsonld} jsonldData
 * @param {String} graphUri
 */
export async function loadIntoRdfStore(store, mediaType, jsonldData, graphUri) {
  return new Promise(resolve => {
    const callback = (success, results) => {
      if (success) {
        resolve();
      } else {
        throw new Error(JSON.stringify(results));
      }
    };

    try {
      if (graphUri) {
        store.load(mediaType, jsonldData, graphUri, callback); // add to graph of that uri
      } else {
        store.load(mediaType, jsonldData, callback); // add to default graph
      }
    } catch (e) {
      rethrow(e, "Failed to store json-ld data for " + graphUri);
    }
  });
}

/**
 * Thin wrapper around `store.execute`, that returns a promise.
 * @param {*} store
 * @param {*} sparqlQuery
 */
export async function executeQueryOnRdfStore(store, sparqlQuery) {
  return new Promise((resolve, reject) =>
    store.execute(sparqlQuery, (success, results) => {
      if (success) {
        resolve(results);
      } else {
        reject(`couldn't execute the following query: ` + sparqlQuery);
      }
    })
  );
}

window.groupByGraphs4dbg = groupByGraphs;
/**
 *
 * @param {*} jsonldData
 * @param {*} addDefaultContext
 */
function groupByGraphs(jsonldData, addDefaultContext = true) {
  const context = addDefaultContext
    ? Object.assign(clone(won.defaultContext), jsonldData["@context"])
    : jsonldData["@context"];

  const cleanUpGraph = graph => {
    const graphUri = graph["@id"];
    const graphWithContext = {
      "@graph": graph["@graph"],
      "@id": graphUri,
      "@context": context,
    };

    if (!graph["@graph"] || !graphUri || !context) {
      const msg =
        "Grouping-by-graph failed for the graph " +
        graphUri +
        " and context " +
        JSON.stringify(context) +
        " with the following jsonld: \n\n" +
        JSON.stringify(graphWithContext) +
        "\n\n";
      return Promise.reject(msg);
    } else {
      /*
            * the previous flattening would generate `{ "@id": "foo", "ex:bar": "someval"}` into 
            * `{ "@id": "foo", "ex:bar": { "@value": "someval"}}`. however, the rdfstore can't handle
            * nodes with `@value` but without `@type`, so we need to compact here first, to prevent
            * these from occuring.
            */
      return jsonld.promises
        .compact(graphWithContext, graphWithContext["@context"])
        .then(compactedGraph => {
          compactedGraph["@id"] = graphUri; // we want the long graph-uri, not the compacted one
          return compactedGraph;
        });
    }
  };

  return jsonld.promises
    .flatten(jsonldData) // flattening groups by graph as a side-effect
    .then(flattenedData => {
      return Promise.all(flattenedData.map(graph => cleanUpGraph(graph)));
    });
}

/**
 * Optionally prepends a string, and then throws
 * whatever it gets as proper javascript error.
 * Note, that throwing an error will also
 * reject in a `Promise`-constructor-callback.
 * @param {*} e
 * @param {*} prependedMsg
 */
function rethrow(e, prependedMsg = "") {
  prependedMsg = prependedMsg ? prependedMsg + "\n" : "";

  if (is("String", e)) {
    throw new Error(prependedMsg + e);
  } else if (e.stack && e.message) {
    // a class defined
    const g = new Error(prependedMsg + e.message);
    g.stack = e.stack;
    g.response = e.response; //we add the response so we can look up why a request threw an error

    throw g;
  } else {
    throw new Error(prependedMsg + JSON.stringify(e));
  }
}
