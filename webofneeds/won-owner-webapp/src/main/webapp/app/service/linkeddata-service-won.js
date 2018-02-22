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
import {
    entries,
    urisToLookupMap,
    is,
    clone,
    contains,
} from '../utils.js';

import rdfstore from 'rdfstore-js';
import jld from 'jsonld';
import won from './won.js';

/**
 * Thin wrapper around `rdfstore.load(...)` that returns 
 * a promise instead of requiring a callback.
 * @param {RdfStore} store 
 * @param {String} mediaType 
 * @param {Jsonld} jsonldData 
 * @param {String} graphUri 
 */
export function rdfStoreLoadPromise(store, mediaType, jsonldData, graphUri) {

    return new Promise((resolve, reject) => {
        const callback = (success, results) => {
            if (success) {
                //console.log('linkeddata-serice-won.js: finished storing triples ', data);
                resolve();
            } else {
                console.error('Failed to store json-ld data for ' + uri);
                reject('Failed to store json-ld data for ' + uri);
            }
        }

        if(graphUri) {
            store.load("application/ld+json", jsonldData, graphUri, callback); // add to graph of that uri
        } else {
            store.load("application/ld+json", jsonldData, callback); // add to default graph
        }
    });
}

(function(){

    /**
     * paging parameters as found
     * [here](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-node-webapp/doc/linked-data-paging.md)
     * @type {string[]}
     */
    const legitQueryParameters = ['p', 'resumebefore', 'resumeafter', 'type', 'state', 'timeof', 'deep'];
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
     *                 the event-container)
     *         * paging parameters as found
     *           [here](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-node-webapp/doc/linked-data-paging.md)
     * @returns {string}
     */
    function queryString(dataUri, queryParams = {}) {
        let queryOnOwner = '/owner/rest/linked-data/?';
        if(queryParams.requesterWebId) {
            queryOnOwner +=
                'requester=' +
                encodeURIComponent(queryParams.requesterWebId) +
                '&';
        }

        // The owner hands this part -- the one in the `uri=` paramater -- directly to the node.
        let queryOnNode = dataUri;
        let firstParam = true;
        for(let [paramName, paramValue] of entries(queryParams)) {
            if(contains(legitQueryParameters, paramName)) {
                queryOnNode = queryOnNode + (firstParam ? '?' : '&');
                firstParam = false;
                queryOnNode = queryOnNode + paramName + '=' + paramValue;
            }
        }

        let query = queryOnOwner +
            'uri=' + encodeURIComponent(queryOnNode);

        // server can't resolve uri-encoded colons. revert the encoding done in `queryString`.
        query = query.replace(new RegExp('%3A', 'g'), ':');

        return query;
    }

    var privateData = {};

    won.clearStore = function () {
        //create an rdfstore-js based store as a cache for rdf data.
        privateData.store =  rdfstore.create();
        window.store4dbg = privateData.store; //TODO deletme
        window.ldspriv4dbg = privateData; //TODO deletme
        privateData.store.setPrefix("msg","http://purl.org/webofneeds/message#");
        privateData.store.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        privateData.store.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        privateData.store.setPrefix("xsd","http://www.w3.org/2001/XMLSchema#");
        privateData.store.setPrefix("won","http://purl.org/webofneeds/model#");

        privateData.readUpdateLocksPerUri = {}; //uri -> ReadUpdateLock
        privateData.cacheStatus = {} //uri -> {timestamp, cacheItemState}
    };
    /**
     * OK: fully fetched
     * DIRTY: has changed
     * UNRESOLVABLE: sthg went wrong during fetching
     * FETCHING: a request has been sent but not returned yet
     * PARTIALLY_FETCHED: the request succeeded but only a part of the ressource was requested (i.e. ld-pagination was used)
     * @type {{OK: number, DIRTY: number, UNRESOLVABLE: number, FETCHING: number, PARTIALLY_FETCHED: number}}
     */
    const CACHE_ITEM_STATE = { OK: 1, DIRTY: 2, UNRESOLVABLE: 3, FETCHING: 4, PARTIALLY_FETCHED: 5};


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
    var ReadUpdateLock = function(uri){
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
        isLocked: function() {
          return this.blockedUpdaters.length > 0
              || this.blockedReaders.length > 0
              || this.activeReaderCount > 0
              || this.activeUpdaterCount > 0;
        },
        getLockStatusString: function(){
            return "[blockedUpdaters: "+this.blockedUpdaters.length
                + ", blockedReaders: "+ this.blockedReaders.length
                + ", activeUpdaters:" + this.activeUpdaterCount
                + ", activeReaders: " + this.activeReaderCount
                + "]";
        },
        acquireReadLock: function(){
            let deferred = {};
            let promise = new Promise((resolve, reject) => { deferred = {resolve, reject} });
            if (this.updateInProgress || this.blockedUpdaters.length > 0){
                //updates are already in progress or are waiting. block.
                //console.log("linkeddata-service-won.js: rul:read:block:  " + this.uri + " " + this.getLockStatusString());
                this.blockedReaders.push(deferred);
                this.grantLockToUpdaters();
            } else {
                //nobody wishes to update the resource, the caller may read it
                //add the deferred execution to the blocked list, just in case
                //there are others blocket there, and then grant access to all
                //console.log("linkeddata-service-won.js: rul:read:grant:  " + this.uri + " " + this.getLockStatusString());
                this.blockedReaders.push(deferred);
                this.grantLockToReaders();
            }
            return promise;
        },
        acquireUpdateLock: function(){
            let deferred = {};
            let promise = new Promise((resolve, reject) => {deferred = {resolve, reject}});

            if (this.activeReaderCount > 0 ) {
                //readers are present, we have to wait till they are done
                //console.log("linkeddata-service-won.js: rul:updt:block:  " + this.uri + " " + this.getLockStatusString());
                this.blockedUpdaters.push(deferred);
            } else {
                //console.log("linkeddata-service-won.js: rul:updt:grant:  " + this.uri + " " + this.getLockStatusString());
                //add the deferred update to the list of blocked updates just
                //in case there are more, then grant the lock to all of them
                this.blockedUpdaters.push(deferred);
                this.grantLockToUpdaters();
            }

            return promise;
        },
        releaseReadLock: function(){
            //console.log("linkeddata-service-won.js: rul:read:release:" + this.uri + " " + this.getLockStatusString());
            this.activeReaderCount --;
            if (this.activeReaderCount < 0){
                throw {message: "Released a read lock that was never acquired"}
            } else if (this.activeReaderCount == 0) {
                //no readers currently have a lock: we can update - if we should
                this.grantLockToUpdaters();
            }
        },
        releaseUpdateLock: function(){
            //console.log("linkeddata-service-won.js: rul:updt:release:" + this.uri + " " + this.getLockStatusString());
            this.activeUpdaterCount --;
            if (this.activeUpdaterCount < 0){
                throw {message: "Released an update lock that was never acquired"}
            } else if (this.activeUpdaterCount == 0) {
                //no readers currently have a lock: we can update - if we should
                this.updateInProgress = false;
                this.grantLockToReaders();
            }
        },
        grantLockToUpdaters: function() {
            if (this.blockedUpdaters.length > 0 && ! this.updateInProgress) {
                //console.log("linkeddata-service-won.js: rul:updt:all:    " + this.uri + " " + this.getLockStatusString());
                //there are blocked updaters. let them proceed.
                this.updateInProgress = true;
                for (var i = 0; i < this.blockedUpdaters.length; i++) {
                    var deferredUpdate = this.blockedUpdaters[i];
                    this.activeUpdaterCount ++;
                    deferredUpdate.resolve();
                    this.blockedUpdaters.splice(i, 1);
                    i--;
                }
            }
        },
        grantLockToReaders: function() {
            if (this.blockedReaders.length > 0) {
                //console.log("linkeddata-service-won.js: rul:readers:all: " + this.uri + " " + this.getLockStatusString());
                //there are blocked readers. let them proceed.
                for (var i = 0; i < this.blockedReaders.length; i++) {
                    var deferredRead = this.blockedReaders[i];
                    this.activeReaderCount++;
                    deferredRead.resolve();
                    this.blockedReaders.splice(i, 1);
                    i--;
                }
            }
        }

    };

    /**
     * We got the rdf but didn't know the uri of the resource
     * first (e.g. due to server-side bundling like http-2)
     * Preferably use `cacheItemMarkAccessed` as it's
     * undefined-check might catch some bugs early.
     *
     * @param uri
     */
    var cacheItemInsertOrOverwrite = function(uri, partial){
        //console.log("linkeddata-service-won.js: add to cache:    " + uri);
        privateData.cacheStatus[uri] = {
            timestamp: new Date().getTime(),
            state: partial? CACHE_ITEM_STATE.PARTIALLY_FETCHED : CACHE_ITEM_STATE.OK
        };
    };

    var cacheItemIsOkOrUnresolvableOrFetching = function cacheItemIsOkOrUnresolvableOrFetching(uri){
        var entry = privateData.cacheStatus[uri];
        var ret = false;
        if (typeof entry === 'undefined') {
            ret = false
        } else {
            ret =
                entry.state === CACHE_ITEM_STATE.OK ||
                entry.state === CACHE_ITEM_STATE.UNRESOLVABLE ||
                entry.state === CACHE_ITEM_STATE.FETCHING;
        }
        var retStr = (ret + "     ").substr(0,5);
        //console.log("linkeddata-service-won.js: cacheSt: OK or Unresolvable:" +retStr + "   " + uri);
        return ret;
    };


    var cacheItemMarkAccessed = function cacheItemMarkAccessed(uri){
        var entry = privateData.cacheStatus[uri];
        if (typeof entry === 'undefined') {
            const message = "Trying to mark unloaded uri " + uri +" as accessed";
            //console.error(message);
            throw { message }
        } else if (entry.state === CACHE_ITEM_STATE.DIRTY){
            const message = "Trying to mark uri " + uri +" as accessed, but it is already dirty";
            //console.error(message);
            throw { message };
        }
        //console.log("linkeddata-service-won.js: mark accessed:   " + uri);
        privateData.cacheStatus[uri].timestamp = new Date().getTime();
    };

    var cacheItemMarkDirty = function cacheItemMarkDirty(uri){
        var entry = privateData.cacheStatus[uri];
        if (typeof entry === 'undefined') {
            return;
        }
        //console.log("linkeddata-service-won.js: mark dirty:      " + uri);
        privateData.cacheStatus[uri].state = CACHE_ITEM_STATE.DIRTY;
    };

    var cacheItemMarkUnresolvable = function cacheItemMarkUnresolvable(uri, reason){
        //console.log("linkeddata-service-won.js: mark unres:      " + uri);
        privateData.cacheStatus[uri] = {timestamp: new Date().getTime(), state: CACHE_ITEM_STATE.UNRESOLVABLE};
        console.error("Couldn't resolve " + uri + ". reason: ", JSON.stringify(reason));
    };

    var cacheItemMarkFetching = function cacheItemMarkFetching(uri){
        //console.log("linkeddata-service-won.js: mark fetching:   " + uri);
        privateData.cacheStatus[uri] = {timestamp: new Date().getTime(), state: CACHE_ITEM_STATE.FETCHING};
    };

    var cacheItemRemove = function cacheItemRemove(uri){
        delete privateData.cacheStatus[uri];
    };

    /**
     * Invalidates the appropriate linked data cache items (i.e. the set of connections
     * associated with a need) such that all information about a
     * newly created connection is loaded. Should be called when receiving hint or connect.
     *
     * Note that this causes an asynchronous call - the cache items may only be invalidated
     * after some delay.
     *
     * @param connectionUri - the uri of the new connection
     * @param needUri - the uri of the need that now has a new connection
     * @return a promise so the caller can chain promises after this one
     */
    won.invalidateCacheForNewConnection = function(connectionUri, needUri){
        if (connectionUri) {
            cacheItemMarkDirty(connectionUri);
        }
        return getConnectionContainerOfNeed(needUri).then(
            function(connectionContainerUri){
                if (connectionContainerUri != null){
                    cacheItemMarkDirty(connectionContainerUri);
                }
            }
        );
    };

    /**
     * Invalidates the appropriate linked data cache items such that all information about a
     * newly received connection message is loaded. Should be called when receiving open, close, or message.
     *
     * Note that this causes an asynchronous call - the cache items may only be invalidated
     * after some delay.
     *
     * @param connectionUri - the uri of the connection
     * @return a promise so that the caller can chain another promise
     */
    won.invalidateCacheForNewMessage = function(connectionUri){
        if (connectionUri != null) {
            cacheItemMarkDirty(connectionUri);
        }
        return won.getNode(connectionUri)
            .then(connection => {
                if(connection.hasEventContainer) {
                    cacheItemMarkDirty(connection.hasEventContainer);
                }
            });
    };
    won.invalidateCacheForNeed = function(needUri){
        if (needUri != null) {
            cacheItemMarkDirty(needUri);
            cacheItemMarkDirty(needUri+'/connections')
        }
        return Promise.resolve(true); //return a promise for chaining
    };

    var getReadUpdateLockPerUri = function(uri){
        var lock = privateData.readUpdateLocksPerUri[uri];
        if (typeof lock === 'undefined' || lock == null) {
            lock = new ReadUpdateLock(uri);
            privateData.readUpdateLocksPerUri[uri] = lock;
        }
        return lock;
    };

    var getReadUpdateLocksPerUris = function(uris){
        var locks = [];
        //console.log("uris",uris);
        uris.map(
            function(uri){
                locks.push(getReadUpdateLockPerUri(uri));
            }
        );
        return locks;
    };

    /**
     * Acquires all locks, returns an array of promises.
     * @param locks
     * @returns {Array|*}
     */
    var acquireReadLocks = function acquireReadLocks(locks){
        var acquiredLocks = [];
        locks.map(
            function(lock){
                var promise = lock.acquireReadLock();
                acquiredLocks.push(promise);
            }
        );
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
    var rejectIfFailed = function(success, data, options){
        const rejectionMessage = buildRejectionMessage(success, data, options);
        if(rejectionMessage) {
            // observation: the error happens for #hasRemoteConnection property of suggested connection, but this
            // property is really not there (and should not be), so in that case it's not an error...
            //console.log(rejectionMessage);
            // TODO: this q.reject seems to have no effect
            q.reject(rejectionMessage);
            return true;
        }
        return false;
    };

    function buildRejectionMessage (success, data, options) {
        let errorMessage = null;
        if (typeof options === 'undefined' || options == null) {
            options = {};
        }
        if (!options.message){
            options.message = "Query failed.";
        }
        if (!success){
            errorMessage = "Query failed: " + data;
        } else if (
            typeof options.allowNone !== undefined
            && options.allowNone == false
            && data.length == 0){
                errorMessage = "No results found.";
        } else if (
            typeof options.allowMultiple !== undefined
            && options.allowMultiple == false
            && data.length > 1){
                errorMessage = "More than one result found.";
        }
        if (errorMessage === null) {
            return '';
        } else {
            // observation: the error happens for #hasRemoteConnection property of suggested connection, but this
            // property is really not there (and should not be), so in that case it's not an error...
            //console.error(options.message + " " + errorMessage);
            return options.message + " " + errorMessage;
        }
    }


    /**
     * Evaluates the specified property path via sparql on the default graph starting with the specified base uri.
     * Returns all solutions of the path.
     * @param baseUri
     * @param propertyPath
     * @param optionalSparqlPrefixes
     * @returns {*}
     */
    won.resolvePropertyPathFromBaseUri = function resolvePropertyPath(baseUri, propertyPath, optionalSparqlPrefixes, optionalSparqlFragment){
        var query = "";
        if (won.isNull(baseUri)){
            throw new Error("cannot evaluate property path: baseUri is null");
        }
        if (won.isNull(propertyPath)){
            throw new Error("cannot evaluate property path: propertyPath is null");
        }
        if (!won.isNull(optionalSparqlPrefixes)){
            query = query + optionalSparqlPrefixes;
        }
        query = query +
            "SELECT ?target where { \n" +
            "<::baseUri::> " + propertyPath + " ?target. \n";
        if (!won.isNull(optionalSparqlFragment)){
        	query = query + optionalSparqlFragment;
        }
        query = query + "} ";
        query = query.replace(/\:\:baseUri\:\:/g, baseUri);
        var resultObject = {};
        privateData.store.execute(query, [], [], function (success, results) {
            resultObject.result = [];
            results.forEach(function(elem){
                resultObject.result.push(elem.target.value);
            })
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
    won.ensureLoaded = function(uri, fetchParams = {}) {
        if (!uri) { throw {message : "ensureLoaded: uri must not be null"}; }

        //console.log("linkeddata-service-won.js: ensuring loaded: " +uri);

        //we allow suppressing the fetch - this is used when the data to be accessed is
        //known to be present in the local store
        if (fetchParams.doNotFetch){
            cacheItemInsertOrOverwrite(uri,false);
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
        if ( cacheItemIsOkOrUnresolvableOrFetching(uri) ) {
            cacheItemMarkAccessed(uri);
            return Promise.resolve(uri);
        }


        cacheItemMarkFetching(uri);
        return won.fetch(uri, fetchParams )
            .then(
                (dataset) => {
                    if( !(fetchParams && fetchParams.deep) ) {
                        cacheItemInsertOrOverwrite(uri, partialFetch);
                        return uri;
                    } else {
                        return selectLoadedResourcesFromDataset(dataset)
                            .then(allLoadedResources => {
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
                                    )
                                });
                                return allLoadedResources;
                            }
                        )
                    }
                },
                reason => cacheItemMarkUnresolvable(uri, reason)
            )

    };

    /**
     * Fetches the rdf-node with the given uri from
     * the standard API_ENDPOINT.
     * @param uri
     * @param params: see `loadFromOwnServerIntoCache`
     * @returns {*}
     */
    won.fetch = function(uri, params) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "fetch: uri must not be null"};
        }
        //console.log("linkeddata-service-won.js: fetch announced: " + uri);
        const lock = getReadUpdateLockPerUri(uri);
        return lock.acquireUpdateLock().then(
                () => loadFromOwnServerIntoCache(uri, params)
            ).then(dataset => {
                lock.releaseUpdateLock();
                return dataset;
            })
            .catch(error => {
                lock.releaseUpdateLock();
                throw({msg: 'Failed to fetch ' + uri, causedBy: error});
            });
    };

    function fetchesPartialRessource(requestParams) {
        if(requestParams.pagingSize) {
                return true;
            } else {
                const qp = requestParams.queryParams || requestParams;
                return !!(qp['p'] ||
                          qp['resumebefore'] ||
                          qp['resumeafter'] ||
                          qp['type'] ||
                          qp['state'] ||
                          qp['timeof']);
            }
    };

    function selectLoadedResourcesFromDataset(dataset) {
        /*
         * create a temporary store to load the dataset into
         * so we only query over the new triples
         */
        const tmpstore = rdfstore.create();

        //TODO avoid duplicate parsing of the dataset
        const storeWithDatasetP = new Promise((resolve, reject) =>
                tmpstore.load('application/ld+json', dataset,
                    (success, results) => success ?
                        resolve(tmpstore) :
                        reject(`couldn't load dataset for ${needUri} into temporary store.`)
                )
        );

        const allLoadedResourcesP = storeWithDatasetP.then(tmpstore => {
            const queryPromise = new Promise((resolve, reject) =>
                    //TODO use the existing constants for prefixes
                    //TODO eliminate redundant queries
                    //TODO rdf:type for connectionContainer?
                    tmpstore.execute(`
                prefix won: <http://purl.org/webofneeds/model#>
                prefix msg: <http://purl.org/webofneeds/message#>
                prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>
                prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                select distinct ?s where {
                    { ?s rdf:type won:Need } union
                    { ?s won:hasConnections ?o } union

                    { ?s rdf:type won:Connection } union
                    { ?s won:hasEventContainer ?o } union

                    { ?s rdfs:member ?o } union
                    { ?s rdf:type won:EventContainer } union

                    { ?s rdf:type msg:FromOwner } union
                    { ?s rdf:type msg:FromSystem } union
                    { ?s rdf:type msg:FromExternal } union
                    { ?s msg:hasMessageType ?o } union
                    { ?s won:hasCorrespondingRemoteMessage ?o } union
                    { ?s won:hasReceiver ?o }.
                }`,
                        (success, results) => success ?
                            resolve(results) :
                            reject(`couldn't execute query for ${needUri} on temporary store for ${uri}`)
                    )
            );
            //final cleanup and return
            return queryPromise.then(queryResults => queryResults.map(r => r.s.value))
        });

        return allLoadedResourcesP;
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
     * @return {Promise}
     */
    function loadFromOwnServerIntoCache(uri, params) { //requesterWebId, queryParams) {
        let requestUri = queryString(uri, params);

        //console.log("linkeddata-service-won.js: fetching:        " + requestUri);

        const datasetP = fetch(requestUri, {
            method: 'get',
            credentials: "same-origin",
            headers: {
                'Accept': 'application/ld+json',
                'Prefer': params.pagingSize?
                    `return=representation; max-member-count="${ params.pagingSize }"` :
                    undefined,
            }
        })
        .then(response => {
            if (response.status === 200)
                return response;
            else
                throw new Exception(`${response.status} - ${response.statusText}`);
        })
        .then(dataset =>
            dataset.json())
        .then( dataset =>
            //make sure we've got a non-empty dataset
            Object.keys(dataset).length === 0 ?
                Promise.reject("failed to load " + uri + ": Object.keys(dataset).length == 0") :
                dataset
        )
        .then(dataset =>
            Promise.resolve()
            .then(() => {
                if(!fetchesPartialRessource(params)) {
                    /* as paging is only used for containers
                     * and they don't lose entries, we can
                     * simply merge on top of the already
                     * loaded triples below. So we skip removing
                     * the previously loaded data. For everything
                     * remove any remaining stale data: */
                    won.deleteNode(uri)
                }
            })
            .then(() =>
                won.addJsonLdData(dataset)
            )
            .then(() => dataset)
        )
        .catch(e =>
            Promise.reject(`failed to load ${uri} due to reason: "${e}"`)
        );

        return datasetP;
    };

    /**
     * Adds the specified JSON-LD dataset to the store, once to the default graph and once to 
     * the individual graphs contained in the data set.
     */
    won.addJsonLdData = function(data) {
        if(!window.jsonLdFiles4dbg) { window.jsonLdFiles4dbg = [] } window.jsonLdFiles4dbg.push(data); //TODO deleteme

        const context = data['@context'];

        const graphsAddedSeperatelyP = Promise.resolve();

        const triplesAddedToDefaultGraphP = rdfStoreLoadPromise(privateData.store, 'application/ld+json', data);

        return Promise
            .all([graphsAddedSeperatelyP, triplesAddedToDefaultGraphP])
            .then(() => undefined); // no return value beyond success
    };

    /**
     * Loads the need-data without following up
     * with a request for the connection-container
     * to get the connection-uris. Thus it's faster.
     */
    won.getNeed = function(needUri) {
        return won.ensureLoaded(needUri)
            .then(() => selectNeedData(needUri, privateData.store));
    };

    window.selectNeedData4dbg = needUri => selectNeedData(needUri, privateData.store);
    function selectNeedData(needUri, store) {
/*
        let query = `
            prefix won: <http://purl.org/webofneeds/model#>
            prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            prefix dct: <http://purl.org/dc/terms/>
            construct {
                <${needUri}> ?b ?c.
                ?c ?d ?e.
                ?e ?f ?g.
                ?g ?h ?i.
                ?i ?j ?k.
                ?k ?l ?m.

            } where {
                {
                    <${needUri}> dct:created ?c.
                    <${needUri}> ?b ?c.
                } UNION {
                     <${needUri}> won:isInState ?c.
                     <${needUri}> ?b ?c.
                } UNION {
                     <${needUri}> won:hasFlag ?c.
                     <${needUri}> ?b ?c.
                }

                UNION
                {
                    <${needUri}> won:hasConnections ?c.
                    <${needUri}> ?b ?c.
                    optional {?c ?d ?e.}
                }

                UNION
                {
                    <${needUri}> won:hasEventContainer ?c.
                    <${needUri}> ?b ?c.
                    optional {?c ?d ?e.}
                }

                UNION {
                  <${needUri}> ?b ?c. filter (?b = won:is || ?b = won:seeks)
                  ?c ?d ?e.
                  ?e ?f ?g.
                  ?g ?h ?i.
                  ?i ?j ?k.
                } UNION {
                  <${needUri}> ?b ?c. filter (?b = won:is || ?b = won:seeks)
                  ?c ?d ?e.
                  ?e ?f ?g.
                  ?g ?h ?i.
                } UNION {
                  <${needUri}> ?b ?c. filter (?b = won:is || ?b = won:seeks)
                  ?c ?d ?e.
                  ?e ?f ?g.
                } UNION {
                  <${needUri}> ?b ?c. filter (?b = won:is || ?b = won:seeks)
                  ?c ?d ?e.
                }  
            }
            `

*/
        let propertyTree = {
            prefixes: {
                "won" : "http://purl.org/webofneeds/model#",
                "rdf" : "http://www.w3.org/2000/01/rdf-schema#",
                "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
                "dct" : "http://purl.org/dc/terms/",
                "dc"  : "http://purl.org/dc/elements/1.1/",
                "s"   : "http://schema.org/"
            },

            roots: [
                {node: "won:hasWonNode"},
                {node: "won:isInState"},
                {node: "won:hasFlag"},
                {node: "won:hasMatchingContext"},
                {node: "won:hasFacet"},
                {node: "dct:created"},
                {
                    node: "won:hasEventContainer",
                    children: [
                        {node: "rdfs:member"}
                    ]
                },
                {
                    node: "won:hasConnections",
                    children: [
                        {node: "rdfs:member"}
                    ]
                },
                {
                    node: "won:seeks",
                    children: [
                        {node: "dc:title"},
                        {node: "dc:description"},
                        {node: "won:hasTag"},
                        {
                            node: "won:hasLocation",
                            children: [
                                {node: "s:name"},
                                {node: "rdf:type"},
                                {
                                    node: "s:geo",
                                    children: [
                                        {node: "rdf:type"},
                                        {node: "s:latitude"},
                                        {node: "s:longitude"}
                                    ]
                                },
                                {
                                    node: "won:hasBoundingBox",
                                    children: [
                                        {
                                            node: "won:hasNorthWestCorner",
                                            children: [
                                                {node: "rdf:type"},
                                                {node: "s:latitude"},
                                                {node: "s:longitude"}
                                            ]
                                        },
                                        {
                                            node: "won:hasSouthEastCorner",
                                            children: [
                                                {node: "rdf:type"},
                                                {node: "s:latitude"},
                                                {node: "s:longitude"}
                                            ]
                                        }

                                    ]
                                }
                            ]
                        }
                    ]
                },
                {
                    node: "won:is",
                    children: [
                        {node: "dc:title"},
                        {node: "dc:description"},
                        {node: "won:hasTag"},
                        {
                            node: "won:hasLocation",
                            children: [
                                {node: "s:name"},
                                {node: "rdf:type"},
                                {
                                    node: "s:geo",
                                    children: [
                                        {node: "rdf:type"},
                                        {node: "s:latitude"},
                                        {node: "s:longitude"}
                                    ]
                                },
                                {
                                    node: "won:hasBoundingBox",
                                    children: [
                                        {node: "rdf:type"},
                                        {
                                            node: "won:hasNorthWestCorner",
                                            children: [
                                                {node: "rdf:type"},
                                                {node: "s:latitude"},
                                                {node: "s:longitude"}
                                            ]
                                        },
                                        {
                                            node: "won:hasSouthEastCorner",
                                            children: [
                                                {node: "rdf:type"},
                                                {node: "s:latitude"},
                                                {node: "s:longitude"}
                                            ]
                                        }

                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        };


        /*for (let j = 0; j < 1; j++) {
            let rep = 1
            let start = performance.now();
            for (let i = 0; i < rep; i++) {
                store.execute(query, (success, resultGraph) => {
               });
            }
            let time = performance.now() - start;
            let format = new Intl.NumberFormat("en-US",{minimumFractionDigits:2, maximumFractionDigits:2, useGrouping:false})
            let pad = '         ';
            let needPad = '                                                                   ';

            let timeStr = format.format(time);
            let timePerNeedStr = format.format(time / rep);
            console.log("executed sparql code for " + (needPad + needUri).slice(-needPad.length) + " (run " + j + ") " + rep + " times in " + (pad + timeStr).slice(-pad.length) + " millis (" + (pad + timePerNeedStr).slice(-pad.length) + " millis per query)");
            start = performance.now();
            for (let i = 0; i < rep; i++) {
                let result = loadStarshapedGraph(store, needUri, propertyTree);
            }
            time = performance.now() - start;
            timeStr = format.format(time);
            timePerNeedStr = format.format(time / rep);
            console.log("executed custom code for " + (needPad + needUri).slice(-needPad.length) + " (run " + j + ") " + rep + " times in " + (pad + timeStr).slice(-pad.length) + " millis (" + (pad + timePerNeedStr).slice(-pad.length) + " millis per query)");
        }*/
        const needJsonLdP = new Promise((resolve, reject) => {

                const resultGraph = loadStarshapedGraph(store, needUri, propertyTree);
                const needJsonLdP = triples2framedJson(needUri, resultGraph.triples, {
                    /* frame */
                    "@id": needUri, // start the framing from this uri. Otherwise will generate all possible nesting-variants.
                    "@context": {
                        "msg": "http://purl.org/webofneeds/message#",
                        "woncrypt": "http://purl.org/webofneeds/woncrypt#",
                        "xsd": "http://www.w3.org/2001/XMLSchema#",
                        "cert": "http://www.w3.org/ns/auth/cert#",
                        "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
                        "sig": "http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#",
                        "geo": "http://www.w3.org/2003/01/geo/wgs84_pos#",
                        "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                        "won": "http://purl.org/webofneeds/model#",
                        "ldp": "http://www.w3.org/ns/ldp#",
                        "sioc": "http://rdfs.org/sioc/ns#",
                        "dc": "http://purl.org/dc/elements/1.1/",
                        "dct": "http://purl.org/dc/terms/",
                        "s": "http://schema.org/",
                    },
                });
                resolve(needJsonLdP);
            }
        ).then(needJsonLd => {
            // usually the need-data will be in a single object in the '@graph' array.
            // We can flatten this and still have valid json-ld
            const simplified = needJsonLd['@graph'][0];
            if(!simplified) {
                if(!needJsonLd || needJsonLd['@graph'].length === 0) {
                    console.error('Received empty graph ', needJsonLd, ' for need ', needUri);
                }

                //doesn't contain graph. probably already simplified.
                return needJsonLd;
            } else {
                simplified['@context'] = needJsonLd['@context'];
                return simplified;
            }
        }).then(needJsonLd => {
            /*
             * The framing algorithm doesn't use arrays if there's
             * only a single `rdfs:member`/element in the list :|
             * Thus, we need to manually make sure all uses of
             * `rdfs:member` have an array as value.
             */
            ensureRdfsMemberArrays(needJsonLd);
            return needJsonLd;
        });

        return needJsonLdP;
    }

    //taken from https://www.w3.org/TR/rdf-interfaces/#triple-filters
    won.tripleFilters = {
        s: function(s) { return function(t) { return t.subject.equals(s); }; },
        p: function(p) { return function(t) { return t.predicate.equals(p); }; },
        o: function(o) { return function(t) { return t.object.equals(o); }; },
        sp: function(s,p) { return function(t) { return t.subject.equals(s) && t.predicate.equals(p); }; },
        so: function(s,o) { return function(t) { return t.subject.equals(s) && t.object.equals(o); }; },
        po: function(p,o) { return function(t) { return t.predicate.equals(p) && t.object.equals(o); }; },
        spo: function(s,p,o) { return function(t) { return t.subject.equals(s) && t.predicate.equals(p) && t.object.equals(o); }; },
        describes: function(v) { return function(t) { return t.subject.equals(v) || t.object.equals(v); }; },
        type: function(o) {
            var type = rdf.resolve("rdf:type");
            return function(t) { return t.predicate.equals(type) && t.object.equals(o); };
        }
    };


    // loads all triples starting from start uri, using each array in 'paths' like a
    // property path, collecting all reachable triples
    // returns a JS RDF Interfaces Graph object
    function loadStarshapedGraph(store, startUri, tree) {
        let prefixes = tree.prefixes;
        if (prefixes != null) {
            for (let key in prefixes) {
                if (prefixes.hasOwnProperty(key) && (! store.rdf.prefixes.get(key)) ) {
                    store.rdf.prefixes.set(key, prefixes[key]);
                }
            }
        }
        try {
            let startNode = store.rdf.createNamedNode(startUri);
            let tmpResult  = { res : null };
            store.graph( (success, result) => tmpResult.res = result);
            let dataGraph = dropUnnecessaryTriples(store, tmpResult.res, tree.roots);
            let resultGraph = new store.rdf.api.Graph();
            for (let i = 0; i < tree.roots.length; i++) {
                let subResult = loadStarshapedGraph_internal(store, dataGraph, startNode, tree.roots[i]);
                resultGraph.addAll(subResult);
            }
            return resultGraph;
        } catch (e){
            console.error("error executing custom select function: " + e);
        }
    }

    function dropUnnecessaryTriples(store, graph, roots){
        "use strict";
        let usedProperties = collectProperties(store, roots);
        //let usedPropertiesString = usedProperties.reduce( (acc, val) => acc + val);
        return graph.filter( triple => {
            let pred = triple.predicate;
            //return usedPropertiesString.indexOf(pred.nominalValue) > -1;
            return usedProperties.includes(pred.nominalValue);
        })
    }

    function collectProperties(store, node) {
        let usedProperties = [];
        if (Array.isArray(node)){
            node.forEach(element => {
                if (typeof element === "string") {
                    //we are processing an array of properties
                    let resolvedProperty = store.rdf.resolve(element);
                    if (!usedProperties.includes(resolvedProperty)){
                        usedProperties.push(resolvedProperty);
                    }
                } else {
                    //we are processing an array of tree nodes
                    let foundProperties = collectProperties(store, element);
                    foundProperties.forEach(prop => {
                        if (!usedProperties.includes(prop)) {
                            usedProperties.push(prop);
                        }
                    })
                }
            });
        } else if (node.hasOwnProperty("usedProperties")) {
            return node.usedProperties;
        } else {
            //we cache the recursion result in the object nodes of the structure
            //we assume an object with fields 'node' and 'children'
            if (node.hasOwnProperty("children")){
                let foundProperties = collectProperties(store, node.children);
                foundProperties.forEach(prop => {
                    if (!usedProperties.includes(prop)){
                        usedProperties.push(prop);
                    }
                });
            }
            if (node.hasOwnProperty("node")){
                let resolvedProperty = store.rdf.resolve(node.node);
                if (!usedProperties.includes(resolvedProperty)){
                    usedProperties.push(resolvedProperty);
                }
            }
            node.usedProperties = usedProperties;
        }
        return usedProperties;
    }



    /**
     * Returns an RDFJSInterface.Graph containing all triples reachable from the start node
     * using the specified property tree.
     */
    function loadStarshapedGraph_internal(store, dataGraph, startNode, tree) {
        let resultGraph = new store.rdf.api.Graph();
        // convert node path into an array if it isn't one already
        //   (this allows for specifying a property path instead of just one property  )
        let path = Array.isArray(tree.node) ? tree.node : [ tree.node];

        // call this function for all elements and collect the results
        let pathResult = loadGraphForPath(store, dropUnnecessaryTriples(store, dataGraph, path), startNode, path);
        resultGraph.addAll(pathResult.graph);
        // recurse
        let subtreeStartNodes = pathResult.leafNodes;
        if (tree.hasOwnProperty("children")) {
            let children = tree.children;
            for (let i = 0; i < children.length; i++) {
                let child = children[i];
                for (let j = 0; j < subtreeStartNodes.length; j++) {
                    let subtreeStartNode = subtreeStartNodes[j];
                    let subResult = loadStarshapedGraph_internal(store, dropUnnecessaryTriples(store, dataGraph, child), subtreeStartNode, child);
                    resultGraph.addAll(subResult);
                }
            }
        }
        return resultGraph;
    }

    /**
     * Returns a structure {"graph": RDFJSInterface.Graph, "leafNodes": <Array of RDFJSInterface.Node>}.
     * The graph contains all triples reachable from the start node given the property path.
     * @param startNode
     * @param path
     */
    function loadGraphForPath(store, dataGraph, startNode, path) {
        if (path.length == 0) return null;
        let localResultGraph = dataGraph.filter(won.tripleFilters.sp(startNode, store.rdf.createNamedNode(path[0])));
        let localLeafNodes = getObjectsFromGraph(localResultGraph);
        if (path.length == 1) {
            // last path element - the leaf in the current branch.
            // if we find a value for this property, the whole branch will retun a result
            return {
                graph : localResultGraph,
                leafNodes : localLeafNodes
            };
        } else {
            let resultGraph = new store.rdf.api.Graph();
            let resultLeafNodes = [];
            for (i = 0; i < localLeafNodes.length; i++) {
                let newStartNode = localLeafNodes[i];
                let newPath = path.splice(1, path.length - 1);
                let subResult = loadGraphForPath(store, newStartNode, newPath);
                resultGraph.addAll(subResult.graph);
                resultLeafNodes.push(subResult.leafNodes);
            }
            return {graph : resultGraph, leafNodes: resultLeafNodes};
        }
    }

    function getObjectsFromGraph(graph){
        let objects = [];
        graph.forEach(triple => { objects.push(triple.object)});
        return objects;
    }



    function triples2framedJson(needUri, triples, frame) {
        const jsonldjsQuads = {
            // everything in our rdfstore is in the default-graph atm
            '@default': triples.map(triple => ({
                subject: rdfstorejsToJsonldjs(triple.subject),
                predicate: rdfstorejsToJsonldjs(triple.predicate),
                object: rdfstorejsToJsonldjs(triple.object),
                //object.datatype: ? //TODO
            }))
        };

        //console.log('jsonldjsQuads: ', jsonldjsQuads);

        const context = frame['@context']? clone(frame['@context']) : {}; //TODO
        context.useNativeTypes = true; //do some of the parsing from strings to numbers

        const jsonLdP = jld.promises
            .fromRDF(jsonldjsQuads, context)
            .then(complexJsonLd => {
                //the framing algorithm expects an js-object with an `@graph`-property
                const complexJsonLd_ = complexJsonLd['@graph'] ?
                    complexJsonLd :
                    {'@graph': complexJsonLd};

                //console.log('complexJsonLd_: ', complexJsonLd_);
                return jld.promises.frame(complexJsonLd_, frame);
            })
            .then(framed => {
                //console.log('framed: ', framed);
                return framed;
            })
            .catch(err => {
                console.error('Failed to frame need-data.', needUri, err);
                throw err;
            });

        return jsonLdP;

    }

    /**
     * Receives a subject, predicate or object in rdfstorejs-style
     * and returns it's pendant in jsonldjs-style.
     * @param element
     * @return {{value: *, type: undefined}}
     */
    function rdfstorejsToJsonldjs(element){
        const result = {
            value: element.nominalValue,
            type: undefined,
        };

        switch(element.interfaceName) {
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
                throw new Exception("Encountered triple with object of unknown type: "
                    + t.object.interfaceName + "\n" +
                    t.subject.nominalValue + " " +
                    t.predicate.nominalValue + " " +
                    t.object.nominalValue + " "
                );
        }

        return result;
    }


    /**
     * Impure function, that all cases of `rdfs:member` have. This
     * is necessary as the framing-algorithm doesn't use arrays in cases,
     * where there's only a single `rdfs:member` property.
     * an array as value.
     * @param needJsonLd
     * @param visited
     */
    function ensureRdfsMemberArrays(needJsonLd, visited = new Set()) {
        if(visited.has(needJsonLd)) return;
        visited.add(needJsonLd);

        for( var k of Object.keys(needJsonLd)) {
            if(k === "rdfs:member" && !is('Array', needJsonLd[k])) needJsonLd[k] = [needJsonLd[k]];
            ensureRdfsMemberArrays(needJsonLd[k], visited)
        }
    }

    won.getEnvelopeDataForNeed=function(needUri, nodeUri){
        if(typeof needUri === 'undefined'||needUri == null){
            throw {message: "getEnvelopeDataForNeed: needUri must not be null"};
        }

        let ret = {};
        ret[won.WONMSG.hasSenderNeed] = needUri;
        ret[won.WONMSG.hasReceiverNeed] = needUri;

        if(!(typeof nodeUri === 'undefined'||nodeUri == null)) {
            ret[won.WONMSG.hasSenderNode] = nodeUri;
            ret[won.WONMSG.hasReceiverNode] = nodeUri;
        }

        return Promise.resolve(ret);
    };

    won.getEnvelopeDataforNewConnection = function(ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri) {
        if (!ownNeedUri){
        	console.log("no own need uri");
            throw {message : "getEnvelopeDataforNewConnection: ownNeedUri must not be null"};
        }
        if (!theirNeedUri){
        	console.log("no remote need uri");
            throw {message : "getEnvelopeDataforNewConnection: theirNeedUri must not be null"};
        }
        return {
            [won.WONMSG.hasSenderNeed]: ownNeedUri,
            [won.WONMSG.hasSenderNode]: ownNodeUri,
            [won.WONMSG.hasReceiverNeed]: theirNeedUri,
            [won.WONMSG.hasReceiverNode]: theirNodeUri,
        }
    };


    /**
     * Fetches a structure that can be used directly (in a JSON-LD node) as the envelope data
     * to send a message via the specified connectionUri (that is interpreted as a local connection.
     * @param connectionUri
     * @returns a promise to the data
     */
    won.getEnvelopeDataforConnection = async function(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getEnvelopeDataforConnection: connectionUri must not be null"};
        }

        const ret = {
            [won.WONMSG.hasSender]: connectionUri,
            [won.WONMSG.hasSenderNeed]: ownNeedUri,
            [won.WONMSG.hasSenderNode]: ownNodeUri,
            [won.WONMSG.hasReceiverNeed]: theirNeedUri,
            [won.WONMSG.hasReceiverNode]: theirNodeUri,
        };
        try {
            if (theirConnectionUri) {
                ret[won.WONMSG.hasReceiver] = theirConnectionUri;
            }
        } catch(err){}
        return Promise.resolve(ret);
    };

    /**
     * @param needUri
     * @return {*} the data of all connection-nodes referenced by that need
     */
    won.getConnectionsOfNeed = (needUri, requesterWebId = needUri) =>
        won.getConnectionUrisOfNeed(needUri, requesterWebId,false)
        .then(connectionUris =>
            urisToLookupMap(
                connectionUris,
                uri => won.getConnectionWithEventUris(uri, { requesterWebId })
            )
        );

    /*
     * Loads all URIs of a need's connections.
     */
    won.getConnectionUrisOfNeed = (needUri, requesterWebId, includeClosed=false) =>
    	{
    		if (includeClosed) {
    			return won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], needUri, requesterWebId)
            		.then(
            			(result) =>  result.map( x => x.connection.value));
    		} else {
    			return won.executeCrawlableQuery(won.queries["getUnclosedConnectionUrisOfActiveNeed"], needUri, requesterWebId)
        		.then(
        			(result) =>  result.map( x => x.connection.value));
    		}
    	}	

    /**
     *
     * @param needUri
     * @returns {*} the Uri of the connection container (read: set) of
     *              connections for the given need
     */
    function getConnectionContainerOfNeed(needUri) {
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getConnectionsUri: needUri must not be null"};
        }
        return won.ensureLoaded(needUri).then(
            function(){
                var lock = getReadUpdateLockPerUri(needUri);
                return lock.acquireReadLock().then(
                    function() {
                        try {
                            var subject = needUri;
                            var predicate = won.WON.hasConnections;
                            var result = {};
                            privateData.store.node(needUri, function (success, graph) {
                                var resultGraph = graph.match(subject, predicate, null);
                                if (!rejectIfFailed(success, resultGraph, {allowMultiple:false, allowNone: false, message:"Failed to load connections uri of need " + needUri})){
                                        result.result = resultGraph.triples[0].object.nominalValue;
                                }
                            });
                            return result.result;
                        } catch (e) {
                            return q.reject("could not get connection URIs of need + " + uri + ". Reason:" + e);
                        } finally {
                            //we don't need to release after a promise resolves because
                            //this function isn't deferred.
                            lock.releaseReadLock();
                        }
                    }
                )
            });
    }
    
    /**
     * Returns all events associated with a given connection
     * in a promise for an object of (eventUri -> eventData)
     * @param connectionUri
     * @param fetchParams See `ensureLoaded`.
     */
    won.getEventsOfConnection = function(connectionUri, fetchParams) {
        return won.getEventUrisOfConnection(connectionUri, fetchParams)
            .then(eventUris => urisToLookupMap(eventUris,
                    eventUri => won.getEvent(eventUri, fetchParams))
            )
    };

    /**
     * Returns the uris of all events associated with a given connection
     * @param connectionUri
     * @param fetchParams See `ensureLoaded`.
     * @returns promise for an array strings (the uris)
     */
    won.getEventUrisOfConnection = function(connectionUri, fetchParams) {
        return won.getConnectionWithEventUris(connectionUri,  fetchParams)
            .then(connection => connection.hasEvents);
    };


    /**
     * @param connectionUri
     * @param fetchParams See `ensureLoaded`.
     * @return {*} the connections predicates along with the uris of associated events
     */
    won.getConnectionWithEventUris = function(connectionUri, fetchParams) {
        if(!is('String', connectionUri)) {
            throw new Error('Tried to request connection infos for sthg that isn\'t an uri: ' + connectionUri);
        }
        return won.getNode(connectionUri, fetchParams)
            //add the eventUris
            .then(connection => Promise.all([
                Promise.resolve(connection),
                won.getNode(connection.hasEventContainer, fetchParams)
            ]))
            .then( ([connection, eventContainer]) => {
                /*
                 * if there's only a single rdfs:member in the event
                 * container, getNode will not return an array, so we
                 * need to make sure it's one from here on out.
                 */
                connection.hasEvents = is('String', eventContainer.member) ?
                    [eventContainer.member] :
                    eventContainer.member;
                return connection;
            })
    };

    //aliases (formerly functions that were just pass-throughs)
    won.getEvent = (uri, fetchParams) => won.getNode(uri, fetchParams)
            .then(event => {
                // framing will find multiple timestamps (one from each node and owner) -> only use latest for the client
                if(is('Array', event.hasReceivedTimestamp)) {
                    const latestFirst = event.hasReceivedTimestamp.sort((x,y) => new Date(y) - new Date(x));
                    event.hasReceivedTimestamp = new Date(latestFirst[0]);
                } else {
                    event.hasReceivedTimestamp = new Date(event.hasReceivedTimestamp);
                }


                if(!event.hasCorrespondingRemoteMessage) {
                    return event;
                } else {
                    if (event.isRemoteResponseTo) {
                        //we can't access the remote message of a remote response. just use the event
                        return event;
                    }
                    /*
                    * there's some messages (e.g. incoming connect) where there's
                    * vital information in the correspondingRemoteMessage. So
                    * we fetch it here.
                    */
                    fetchParams.doNotFetch = true;
                    return won
                        .getNode(event.hasCorrespondingRemoteMessage, fetchParams)
                        .then(correspondingEvent => {
                            if (correspondingEvent.type) {
                                //if we have at least a type attribute, we add the remote event to the
                                //local event. if not, it is just an URI.
                                event.hasCorrespondingRemoteMessage = correspondingEvent;
                            }
                            return event;
                        })
                }
            });

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
     * @param eventUri
     * @param fetchParams See `ensureLoaded`.
     */
    won.getNode = function(uri, fetchParams) {
        if(!uri) {
            return Promise.reject({message : "getNode: uri must not be null"})
        }

        let releaseLock = undefined;

        const nodePromise = won.ensureLoaded(uri, fetchParams)
            .then(() => {
                const lock = getReadUpdateLockPerUri(uri);
                releaseLock = () => lock.releaseReadLock();
                return lock.acquireReadLock();
            })
            .then(() => {
                //console.log("linkeddata-service-won.js: getNode:" + uri);
                return new Promise((resolve, reject) => {
                    privateData.store.node(uri, function (success, graph) {
                        if(!success) {
                            reject({
                                message: "Error loading node with attributes for URI " + uri + "."
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
                            /*console.log('linkeddata-service-won.js: warn: could not ' +
                                'load any attributes for node with uri: ', uri,
                                '. Trying sparql-query + result.filter workaround.');*/
                            privateData.store.graph((success, entireGraph) => {
                                resolve(entireGraph.triples.filter(t => t.subject.nominalValue === uri))
                            })
                        } else {
                            //.node(...) was successful. return the node's tripples
                            resolve(graph.triples);
                        }
                    })
                })
            })
            .then(triples => {
                const node = {};
                triples.forEach(triple => {
                    //TODO this cropping ignores prefixes, causing predicates/fields to clash!
                    const propName = won.getLocalName(triple.predicate.nominalValue);
                    if(node[propName]) {
                        //encountered multiple occurances of the same predicate, e.g. rdfs:member
                        if(!(node[propName] instanceof Array)) {
                            //on the first 'clash', instantiate the predicate/property as array
                            node[propName] = [ node[propName] ];
                        }
                        node[propName].push(triple.object.nominalValue);
                    } else {
                        node[propName] = triple.object.nominalValue;
                    }
                });
                node.uri = uri;
                releaseLock();
                return node;
            })
            .catch(e => {
                releaseLock();
                throw new Exception("Couldn't get node " +
                    uri + " with params " + fetchParams, e);
            });

        return nodePromise;
    };

    /**
     * Deletes all triples where the specified uri is the subect. May have side effects on concurrent
     * reads on the rdf store if called without a read lock.
     */
    won.deleteNode = function(uri){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "deleteNode: uri must not be null"};
        }
        //console.log("linkeddata-service-won.js: deleting node:   " + uri);
        const query = "delete where {<"+uri+"> ?anyP ?anyO}";
        //var query = "select ?anyO where {<"+uri+"> ?anyP ?anyO}";
        return new Promise((resolve, reject) => {
            privateData.store.execute(query, function (success, graph) {
                const rejMsg = buildRejectionMessage(
                    success,
                    graph, {
                        message: "Error deleting node with URI " + uri + "."
                    }
                );
                if (rejMsg) {
                    reject(rejMsg);
                } else {
                    cacheItemRemove(uri);
                    resolve();
                }
            });
        });
    };

    won.getConnectionWithOwnAndRemoteNeed = function(ownNeedUri, remoteNeedUri) {
        return won.getConnectionsOfNeed(ownNeedUri).then(connections => {
            for(let connectionUri of Object.keys(connections)) {
                if(connections[connectionUri].hasRemoteNeed === remoteNeedUri) {
                    return connections[connectionUri];
                }
            }
            throw new Error("Couldn't find connection between own need <" +
               ownNeedUri + "> and remote need <" + remoteNeedUri + ">.");
        })
    };
    

    /**
     * Executes the specified crawlableQuery, returns a promise to its results, which may become available
     * after downloading the required content.
     */
    won.executeCrawlableQuery = function (crawlableQuery, baseUri, requesterWebId) {
        var relevantResources = [];
        var recursionData = {};
        var MAX_RECURSIONS = 10;

        var executeQuery = function executeQuery(query, baseUri, relevantResources){
            query = query.replace(/\:\:baseUri\:\:/g, baseUri);
            //console.log("linkeddata-service-won.js: executing query: \n"+query);
            var locks = getReadUpdateLocksPerUris(relevantResources);
            var promises = acquireReadLocks(locks);
            return Promise.all(promises).then(
                function () {
                    var resultObject = {};
                    try {
                        privateData.store.execute(query, [], [], function (success, results) {
                            if (rejectIfFailed(success, results, {message: "Error executing query.", allowNone: true, allowMultiple: true})) {
                                return;
                            }
                            resultObject.results = results;
                        });
                        return resultObject.results;
                    } catch (e) {
                        //console.log("linkeddata-service-won.js: Could not execute query. Reason: " + e);
                        return q.reject("Could not execute query. Reason: " + e);
                    } finally {
                        //release the read locks
                        locks.map(
                            function(lock){
                                lock.releaseReadLock();
                            }
                        );
                    }
                }
            )
        };

        var resolvePropertyPathsFromBaseUri = function resolvePropertyPathsFromBaseUri(propertyPaths, baseUri, relevantResources){
            //console.log("linkeddata-service-won.js: resolving " + propertyPaths.length + " property paths on baseUri " + baseUri);
            var locks = getReadUpdateLocksPerUris(relevantResources);
            var promises = acquireReadLocks(locks);
            return Promise.all(promises).then(
                function () {
                    try {
                        var resolvedUris = [];
                        propertyPaths.map(
                            function(propertyPath){
                                //console.log("linkeddata-service-won.js: resolving property path: " + propertyPath.propertyPath);
                                var foundUris = won.resolvePropertyPathFromBaseUri(
                                        baseUri,
                                        propertyPath.propertyPath,
                                        propertyPath.prefixes,
                                        propertyPath.fragment);

                                //resolve all property paths, add to 'resolvedUris'
                                Array.prototype.push.apply(resolvedUris, foundUris);
                                //console.log("linkeddata-service-won.js: resolved to " + foundUris.length + " resources (total " + resolvedUris.length+")");
                        });
                        return resolvedUris;
                    }catch(e){
                        console.error(e)
                    } finally {
                        //release the read locks
                        locks.map(
                            function(lock){
                                lock.releaseReadLock();
                            }
                        );
                    }
                });
        };

        var resolveOrExecuteQuery = function resolveOrExecuteQuery(resolvedUris){
            if (won.isNull(recursionData.depth)){
                recursionData.depth = 0;
            }
            //console.log("linkeddata-service-won.js: crawlableQuery:resolveOrExecute depth=" + recursionData.depth + ", resolvedUris=" + JSON.stringify(resolvedUris)+", relevantResources=" + JSON.stringify(relevantResources));
            recursionData.depth++;
            if (won.containsAll(relevantResources, resolvedUris) || recursionData.depth >= MAX_RECURSIONS) {
                //console.log("linkeddata-service-won.js: crawlableQuery:resolveOrExecute crawling done");
                return executeQuery(crawlableQuery.query, baseUri, relevantResources);
            } else {
                //console.log("linkeddata-service-won.js: crawlableQuery:resolveOrExecute resolving property paths ...");
                Array.prototype.push.apply(relevantResources, resolvedUris);
                var loadedPromises = relevantResources.map(x => won.ensureLoaded(x, { requesterWebId }) );
                return Promise.all(loadedPromises)
                    .then(
                        function (x) {
                            return resolvePropertyPathsFromBaseUri(crawlableQuery.propertyPaths, baseUri, relevantResources);
                        })
                    .then(
                        function(newlyResolvedUris) {
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
         * Despite the name, returns the connections fo the specified need themselves. TODO rename
         */
        "getAllConnectionUrisOfNeed" : {
            propertyPaths : [
                { prefixes :
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                        "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                        "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                    propertyPath : "won:hasConnections"
                },
                { prefixes :
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                        "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                        "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                    propertyPath : "won:hasConnections/rdfs:member"
                }
            ],
        query:
                "prefix msg: <http://purl.org/webofneeds/message#> \n"+
                "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n"+
                "select ?connection ?need ?remoteNeed ?connectionState  \n" +
                " where { \n" +
                " <::baseUri::> a won:Need; \n" +
                "           won:hasConnections ?connections.\n" +
                "  ?connections rdfs:member ?connection. \n" +
                "  ?connection won:belongsToNeed ?need; \n" +
                "              won:hasRemoteNeed ?remoteNeed; \n"+
            "                  won:hasConnectionState ?connectionState. \n"+
                "} \n"

        },
        /**
         * Despite the name, returns the connections fo the specified need themselves. TODO rename
         */
        "getUnclosedConnectionUrisOfActiveNeed" : {
            propertyPaths : [
                { prefixes :
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                        "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                        "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                    propertyPath : "won:hasConnections",
                    fragment: " filter exists {<::baseUri::> won:isInState won:Active} "
                },
                { prefixes :
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                        "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                        "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                    propertyPath : "won:hasConnections/rdfs:member",
                    fragment: " filter exists {<::baseUri::> won:isInState won:Active} "
                }
            ],
        query:
                "prefix msg: <http://purl.org/webofneeds/message#> \n"+
                "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n"+
                "select ?connection ?need ?remoteNeed ?connectionState  \n" +
                " where { \n" +
                " <::baseUri::> a won:Need; \n" +
                "           won:hasConnections ?connections; \n" +
                "           won:isInState ?needState.\n " +
                "  ?connections rdfs:member ?connection. \n" +
                "  ?connection won:belongsToNeed ?need; \n" +
                "              won:hasRemoteNeed ?remoteNeed; \n"+
                "                  won:hasConnectionState ?connectionState. \n"+
                "  filter ( ?connectionState != won:Closed && ?needState = won:Active) \n" +
                "} \n"

        },
    }
})();
