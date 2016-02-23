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
    checkHttpStatus,
    entries
} from '../utils';
import * as q from 'q';

import '../../scripts/rdfstore-js/rdf_store';
const rdfstore = window.rdfstore;

(function(){
    if(!won) won = {};

    const API_ENDPOINT = '/owner/rest/linked-data/?uri=';
    const AUTH_PARAMETER = '&requester='

    /**
     * This function is used to generate the query-strings.
     * Should anything about the way the API is accessed changed,
     * adapt this function.
     * @param dataUri
     * @param requesterWebId the auth-token for the post (NOT the sessionId-cookie)
     * @returns {string}
     */
    function queryString(dataUri, requesterWebId) {
        let requestUri = API_ENDPOINT + encodeURIComponent(dataUri);
        if (requesterWebId) {
            requestUri = requestUri + AUTH_PARAMETER+ encodeURIComponent(requesterWebId);
        }
        return requestUri;
    }

    var privateData = {};

    won.ld_reset = function() {
        privateData = {};
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
    }
    const CACHE_ITEM_STATE = { OK: 1, DIRTY: 2, UNRESOLVABLE: 3, FETCHING: 4};

    won.ld_reset();


    var createNameNodeInStore = function(uri){
        return privateData.store.rdf.createNamedNode(privateData.store.rdf.resolve(uri));
    }

    var getSafeValue = function(dataItem) {
        if (typeof dataItem === 'undefined') return null;
        if (dataItem == null) return null;
        if (typeof dataItem.value === 'undefined') return dataItem;
        if (dataItem.value != null) return dataItem.value;
        return null;
    }

    /**
     * Similar to q.all, takes an array of promises and returns a promise.
     * That promise will resolve if at least one of the promises succeeds.
     * The value with which it resolves it is an array of equal length as the input
     * containing either the resolve value of the promise or null if rejected.
     * If an errorHandler is specified, it is called with ([array key], [reject value]) of
     * each rejected promise.
     * @param promises
     */
    var somePromises = function(promises, errorHandler){
        var deferred = q.defer(),
            numPromises = promises.length,
            successes = 0,
            failures = 0,
            results = Array.isArray(promises) ? [] : {},
            handler = typeof errorHandler === 'function' ? errorHandler : function(x,y){};

        if (promises.length == 0) {
            deferred.reject(results);
        }

        promises.forEach(function(promise, key) {
            promise.then(function(value) {
                successes++;
                if (results.hasOwnProperty(key)) return; //TODO: not sure if we need this
                results[key] = value;
                if (failures + successes >= numPromises) deferred.resolve(results);
            }, function(reason) {
                failures ++;
                console.log("linkeddata-service-won.js: warning: promise failed. Reason " + JSON.stringify(reason));
                if (results.hasOwnProperty(key)) return; //TODO: not sure if we need this
                results[key] = null;
                handler(key, reason);
                if (failures >= numPromises) {
                    deferred.reject(results);
                } else if (failures + successes >= numPromises) {
                    deferred.resolve(results);
                }
            });
        });

        return deferred.promise;
    }

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
            var deferred = q.defer();
            if (this.updateInProgress || this.blockedUpdaters.length > 0){
                //updates are already in progress or are waiting. block.
                console.log("linkeddata-service-won.js: rul:read:block:  " + this.uri + " " + this.getLockStatusString());
                this.blockedReaders.push(deferred);
                this.grantLockToUpdaters();
            } else {
                //nobody wishes to update the resource, the caller may read it
                //add the deferred execution to the blocked list, just in case
                //there are others blocket there, and then grant access to all
                console.log("linkeddata-service-won.js: rul:read:grant:  " + this.uri + " " + this.getLockStatusString());
                this.blockedReaders.push(deferred);
                this.grantLockToReaders();
            }
            return deferred.promise;
        },
        acquireUpdateLock: function(){
            var deferred = q.defer();

            if (this.activeReaderCount > 0 ) {
                //readers are present, we have to wait till they are done
                console.log("linkeddata-service-won.js: rul:updt:block:  " + this.uri + " " + this.getLockStatusString());
                this.blockedUpdaters.push(deferred);
            } else {
                console.log("linkeddata-service-won.js: rul:updt:grant:  " + this.uri + " " + this.getLockStatusString());
                //add the deferred update to the list of blocked updates just
                //in case there are more, then grant the lock to all of them
                this.blockedUpdaters.push(deferred);
                this.grantLockToUpdaters();
            }

            return deferred.promise;
        },
        releaseReadLock: function(){
            console.log("linkeddata-service-won.js: rul:read:release:" + this.uri + " " + this.getLockStatusString());
            this.activeReaderCount --;
            if (this.activeReaderCount < 0){
                throw {message: "Released a read lock that was never acquired"}
            } else if (this.activeReaderCount == 0) {
                //no readers currently have a lock: we can update - if we should
                this.grantLockToUpdaters();
            }
        },
        releaseUpdateLock: function(){
            console.log("linkeddata-service-won.js: rul:updt:release:" + this.uri + " " + this.getLockStatusString());
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
                console.log("linkeddata-service-won.js: rul:updt:all:    " + this.uri + " " + this.getLockStatusString());
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
                console.log("linkeddata-service-won.js: rul:readers:all: " + this.uri + " " + this.getLockStatusString());
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



    var cacheItemInsertOrOverwrite = function(uri){
        console.log("linkeddata-service-won.js: add to cache:    " + uri);
        privateData.cacheStatus[uri] = {
            timestamp: new Date().getTime(),
            state: CACHE_ITEM_STATE.OK
        };
    }

    var cacheItemIsInState = function cacheItemIsInState(uri, state, nameOfState){
        var entry = privateData.cacheStatus[uri];
        var ret = false;
        if (typeof entry === 'undefined') {
            ret = false
        } else {
            ret = entry.state === state;
        }
        var retStr = (ret + "     ").substr(0,5);
        console.log("linkeddata-service-won.js: cacheSt: " + nameOfState + ":" +retStr + "   " + uri);
        return ret;
    }

    var cacheItemIsOkOrUnresolvableOrFetching = function cacheItemIsOkOrUnresolvableOrFetching(uri){
        var entry = privateData.cacheStatus[uri];
        var ret = false;
        if (typeof entry === 'undefined') {
            ret = false
        } else {
            ret = entry.state === CACHE_ITEM_STATE.OK || entry.state === CACHE_ITEM_STATE.UNRESOLVABLE || entry.state == CACHE_ITEM_STATE.FETCHING;
        }
        var retStr = (ret + "     ").substr(0,5);
        console.log("linkeddata-service-won.js: cacheSt: OK or Unresolvable:" +retStr + "   " + uri);
        return ret;
    }

    /**
     * Returns true iff the uri is loaded and marked as dirty.
     * @param uri
     * @returns {boolean}
     */
    var cacheItemIsDirty = function cacheItemIsDirty(uri){
        return cacheItemIsInState(uri, CACHE_ITEM_STATE.DIRTY, "dirty");
    }

    /**
     * Returns true iff the uri is loaded and marked ok.
     * @param uri
     * @returns {boolean}
     */
    var cacheItemIsOk = function cacheItemIsOk(uri){
        return cacheItemIsInState(uri, CACHE_ITEM_STATE.OK, "loaded");
    }

    /**
     * Returns true iff the uri is loaded and marked unresolvable.
     * @param uri
     * @returns {boolean}
     */
    var cacheItemIsUnresolvable = function cacheItemIsUnresolvable(uri){
        return cacheItemIsInState(uri, CACHE_ITEM_STATE.UNRESOLVABLE, "unresolvable");
    }

    var cacheItemMarkAccessed = function cacheItemMarkAccessed(uri){
        var entry = privateData.cacheStatus[uri];
        if (typeof entry === 'undefined') {
            throw {message : "Trying to mark unloaded uri " + uri +" as accessed"}
        } else if (entry.state === CACHE_ITEM_STATE.DIRTY){
            throw {message : "Trying to mark uri " + uri +" as accessed, but it is already dirty"}
        }
        console.log("linkeddata-service-won.js: mark accessed:   " + uri);
        privateData.cacheStatus[uri].timestamp = new Date().getTime();
    }

    var cacheItemMarkDirty = function cacheItemMarkDirty(uri){
        var entry = privateData.cacheStatus[uri];
        if (typeof entry === 'undefined') {
            return;
        }
        console.log("linkeddata-service-won.js: mark dirty:      " + uri);
        privateData.cacheStatus[uri].state = CACHE_ITEM_STATE.DIRTY;
    }

    var cacheItemMarkUnresolvable = function cacheItemMarkUnresolvable(uri){
        console.log("linkeddata-service-won.js: mark unres:      " + uri);
        privateData.cacheStatus[uri] = {timestamp: new Date().getTime(), state: CACHE_ITEM_STATE.UNRESOLVABLE};
    }

    var cacheItemMarkFetching = function cacheItemMarkFetching(uri){
        console.log("linkeddata-service-won.js: mark fetching:   " + uri);
        privateData.cacheStatus[uri] = {timestamp: new Date().getTime(), state: CACHE_ITEM_STATE.FETCHING};
    }

    var cacheItemRemove = function cacheItemRemove(uri){
        delete privateData.cacheStatus[uri];
    }


    /**
     * Method used for debugging pending locks.
     */
    won.getUnreleasedLocks = function(){
        var unreleasedLocks = [];
        for (key in privateData.readUpdateLocksPerUri){
            var lock = privateData.readUpdateLocksPerUri[key];
            if (lock.isLocked()){
                unreleasedLocks.push(lock);
            }
        }
        return unreleasedLocks;
    }

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
    }

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
        return q.when(true); //return a promise for chaining
    }
    won.invalidateCacheForNeed = function(needUri){
        if (needUri != null) {
            cacheItemMarkDirty(needUri);
            cacheItemMarkDirty(needUri+'/connections/')
        }
        return q.when(true); //return a promise for chaining
    }



    var getReadUpdateLockPerUri = function(uri){
        var lock = privateData.readUpdateLocksPerUri[uri];
        if (typeof lock === 'undefined' || lock == null) {
            lock = new ReadUpdateLock(uri);
            privateData.readUpdateLocksPerUri[uri] = lock;
        }
        return lock;
    }

    var getReadUpdateLocksPerUris = function(uris){
        var locks = [];
        uris.map(
            function(uri){
                locks.push(getReadUpdateLockPerUri(uri));
            }
        );
        return locks;
    }

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
    }


    /**
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
        var errorMessage = null;
        if (typeof options === 'undefined' || options == null) {
            options = {};
        }
        if (!options.message){
            options.message = "Query failed.";
        }
        if (!success){
            errorMessage = "Query failed: " + data;
        } else if (typeof options.allowNone !== undefined  && options.allowNone == false && data.length == 0){
            errorMessage = "No results found.";
        } else if (typeof options.allowMultiple !== undefined  && options.allowMultiple == false && data.length > 1){
            errorMessage = "More than one result found.";
        }
        if (errorMessage != null) {
            // observation: the error happens for #hasRemoteConnection property of suggested connection, but this
            // property is really not there (and should not be), so in that case it's not an error...
            console.log(options.message + " " + errorMessage);
            // TODO: this q.reject seems to have no effect
            q.reject(options.message + " " + errorMessage);
            return true;
        }
        return false;
    }


    /**
     * Adds the specified JSON-LD dataset to the store, identified by the specified uri.
     * The uri is used for cache control.
     */
    won.addJsonLdData = function(uri, data) {
        console.log("linkeddata-service-won.js: storing jsonld data for uri: " + uri);
        privateData.store.load("application/ld+json", data, function (success, results) {
            console.log("linkeddata-service-won.js: added jsonld data to rdf store, success: " + success);
            if (success) {
                cacheItemMarkAccessed(uri);
            }
        });
    }


    /**
     * Evaluates the specified property path via sparql on the default graph starting with the specified base uri.
     * Returns true if the query has at least one solution.
     * @param baseUri
     * @param propertyPath
     * @param optionalSparqlPrefixes
     * @returns {*}
     */
    won.canResolvePropertyPathFromBaseUri = function canResolvePropertyPath(baseUri, propertyPath, optionalSparqlPrefixes){
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
            "ASK where { \n" +
            "<" + baseUri +"> " + propertyPath + " ?target. \n" +
            "} ";
        var resultObject = {};
        privateData.store.execute(query, [], [], function (success, results) {
            resultObject.result = results;
        });
        return resultObject.result;
    }

    /**
     * Evaluates the specified property path via sparql on the default graph starting with the specified base uri.
     * Returns all solutions of the path.
     * @param baseUri
     * @param propertyPath
     * @param optionalSparqlPrefixes
     * @returns {*}
     */
    won.resolvePropertyPathFromBaseUri = function resolvePropertyPath(baseUri, propertyPath, optionalSparqlPrefixes){
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
            "<" + baseUri +"> " + propertyPath + " ?target. \n" +
            "} ";
        var resultObject = {};
        privateData.store.execute(query, [], [], function (success, results) {
            resultObject.result = [];
            results.forEach(function(elem){
                resultObject.result.push(elem.target.value);
            })
        });
        return resultObject.result;
    }

    /**
     * Fetches the linked data for the specified URI and saves it in the local triple-store.
     * @param uri
     * @param requesterWebId
     * @return {*}
     */
    won.ensureLoaded = function(uri, requesterWebId) {
        if (!uri) { throw {message : "ensureLoaded: uri must not be null"}; }

        console.log("linkeddata-service-won.js: ensuring loaded: " +uri);

        //we also allow unresolvable resources, so as to avoid re-fetching them.
        //we also allow resources that are currently being fetched.
        if (cacheItemIsOkOrUnresolvableOrFetching(uri)){
            return new Promise((resolve, reject) => {
                cacheItemMarkAccessed(uri);
                resolve(uri);
            });
        }
        //uri isn't loaded or needs to be refrehed. fetch it.
        cacheItemMarkFetching(uri);
        return won.fetch(uri, requesterWebId)
            .then(
                () => cacheItemMarkAccessed(uri),
                reason => cacheItemMarkUnresolvable(uri)
            )

    }

    /**
     * Fetches the rdf-node with the given uri from
     * the standard API_ENDPOINT.
     * @param uri
     * @param requesterWebId
     * @returns {*}
     */
    won.fetch = function(uri, requesterWebId) {
        var tempUri = uri+'?prev='+new Date().getTime();
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "fetch: uri must not be null"};
        }
        console.log("linkeddata-service-won.js: fetch announced: " + uri);
        const lock = getReadUpdateLockPerUri(uri);
        return lock.acquireUpdateLock().then(
                () => loadFromOwnServerIntoCache(uri, requesterWebId)
            ).then(dataset => {
                lock.releaseUpdateLock();
                return dataset;
            });
    }

    function loadFromOwnServerIntoCache(uri, requesterWebId) {
        return new Promise((resolve, reject) => {
            console.log("linkeddata-service-won.js: fetching:        " + uri);

            let requestUri = queryString(uri, requesterWebId);
            const find = '%3A';
            const re = new RegExp(find, 'g');
            requestUri = requestUri.replace(re, ':');

            fetch(requestUri, {
                method: 'get',
                credentials: 'include'
            })
            .then(dataset => dataset.json())
            .then(
                dataset => {
                    //make sure we've got a non-empty dataset
                    if (Object.keys(dataset).length === 0) {
                        reject("failed to load " + uri);
                    } else {
                        console.log("linkeddata-service-won.js: fetched:         " + uri)
                        won.addJsonLdData(uri, dataset);
                        resolve(dataset);
                    }
                  },
                e =>  reject(`failed to load ${uri} due to reason ${e}`)
            );
        });
    };

    /**
     * Saves the specified jsonld structure in the triple store with the specified default graph URI.
     * @param graphURI used if no graph URI is specified in the jsonld
     * @param jsonld the data
     */
    won.storeJsonLdGraph = function(graphURI, jsonld) {
        if (typeof graphURI === 'undefined' || graphURI == null  ){
            throw {message : "storeJsonLdGraph: graphURI must not be null"};
        }
        privateData.store.load("application/ld+json", jsonld, graphURI, function (success, results) {});
    }

    /**
     * Loads the default data of the need with the specified URI into a js object.
     * @return the object or null if no data is found for that URI in the local datastore
     */
    won.getNeed = function(needUri) {
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getNeed: uri must not be null"};
        }
       return won.ensureLoaded(needUri).then(
           function() {
               var lock = getReadUpdateLockPerUri(needUri);
               return lock.acquireReadLock().then(
                   function () {
                       try {
                           var resultObject = null;
                           var query =
                               "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                               "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                               "prefix " + "dc" + ": <" + "http://purl.org/dc/elements/1.1/>\n" +
                               "prefix " + "geo" + ": <" + "http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                               "prefix " + "rdfs" + ": <" + "http://www.w3.org/2000/01/rdf-schema#>\n" +
                               "select ?basicNeedType ?title ?tags ?textDescription ?creationDate ?endTime ?recurInfinite ?recursIn ?startTime ?state where { " +
                               //TODO: add as soon as named graphs are handled by the rdf store
                               //
                               //                "<" + uri + ">" + won.WON.hasGraphCompacted + " ?coreURI ."+
                               //                "<" + uri + ">" + won.WON.hasGraphCompacted + " ?metaURI ."+
                               //                "GRAPH ?coreURI {"+,
                               "<" + needUri + "> " + won.WON.isInStateCompacted + " ?state .\n" +
                               "<" + needUri + "> " + won.WON.hasBasicNeedTypeCompacted + " ?basicNeedType .\n" +
                               "<" + needUri + "> " + won.WON.hasContentCompacted + " ?content .\n" +
                               "?content dc:title ?title .\n" +
                               "OPTIONAL {?content " + won.WON.hasTagCompacted + " ?tags .}\n" +
                               "OPTIONAL {?content " + "geo:latitude" + " ?latitude .}\n" +
                               "OPTIONAL {?content " + "geo:longitude" + " ?longitude .}\n" +
                               "OPTIONAL {?content " + won.WON.hasEndTimeCompacted + " ?endTime .}\n" +
                               "OPTIONAL {?content " + won.WON.hasRecurInfiniteTimesCompacted + " ?recurInfinite .}\n" +
                               "OPTIONAL {?content " + won.WON.hasRecursInCompacted + " ?recursIn .}\n" +
                               "OPTIONAL {?content " + won.WON.hasStartTimeCompacted + " ?startTime .}\n" +
                               "OPTIONAL {?content " + won.WON.hasTextDescriptionCompacted + " ?textDescription .}\n" +
                               //TODO: add as soon as named graphs are handled by the rdf store
                               //                "}" +
                               //                "GRAPH ?metaURI {" +
                               "<" + needUri + "> " + " <" + "http://purl.org/dc/terms/created" + "> " + "?creationDate" +
                               " .\n" +
                               "<" + needUri + "> " + won.WON.hasConnectionsCompacted + " ?connections .\n" +
                               "<" + needUri + "> " + won.WON.hasWonNodeCompacted + " ?wonNode .\n" +
                               "OPTIONAL {<" + needUri + "> " + won.WON.hasEventContainerCompacted + " ?eventContainer" +
                               " .}\n" +
                               // if the triple below is used, we get > 1 result items, because usually here will be
                               // at least 2 events - need created and success response - therefore, it can be removed
                               // since we don't select ?event anyway here in this query:
                               //"OPTIONAL {?eventContainer " + "rdfs:member" + " ?event .}\n" +
                               //TODO: add as soon as named graphs are handled by the rdf store
                               //                "}" +
                               "}";
                           resultObject = {};
                           privateData.store.execute(query, [], [], function (success, results) {
                               if (rejectIfFailed(success, results, {message: "Could not load need " + needUri + ".", allowNone: false, allowMultiple: false})) {
                                   return;
                               }
                               var result = results[0];
                               resultObject.uri = needUri;
                               resultObject.basicNeedType = getSafeValue(result.basicNeedType);
                               resultObject.title = getSafeValue(result.title);
                               resultObject.tags = getSafeValue(result.tags);
                               resultObject.textDescription = getSafeValue(result.textDescription);
                               resultObject.creationDate = getSafeValue(result.creationDate);
                               resultObject.state = getSafeValue(result.state);
                           });
                           return resultObject;
                       } catch (e) {
                           return q.reject("could not load need " + needUri + ". Reason: " + e);
                       } finally {
                           //we don't need to release after a promise resolves because
                           //this function isn't deferred.
                           lock.releaseReadLock();
                       }
                   })
               });
    }

    /**
     * Utility method that first ensures the resourceURI is locally loaded, then fetches the object of the
     * specified property, which must be present only once.
     * @param resourceURI
     * @param propertyURI
     * @returns {*}
     */
    won.getUniqueObjectOfProperty = function(resourceURI, propertyURI){
        if (typeof resourceURI === 'undefined' || resourceURI == null  ){
            throw {message : "getUniqueObjectOfProperty: resourceURI must not be null"};
        }
        if (typeof propertyURI === 'undefined' || propertyURI == null  ){
            throw {message : "getUniqueObjectOfProperty: propertyURI must not be null"};
        }
        return won.ensureLoaded(resourceURI).then(
            function(){
                var lock = getReadUpdateLockPerUri(resourceURI);
                return lock.acquireReadLock().then(
                    function () {
                        try {
                            var resultData = {};
                            privateData.store.node(resourceURI, function (success, graph) {
                                if (rejectIfFailed(success, graph,{message : "Error loading object of property " + propertyURI + " of resource " + resourceURI + ".", allowNone : false, allowMultiple: true})){
                                    return;
                                }
                                var results = graph.match(resourceURI, propertyURI, null);
                                if (rejectIfFailed(success, results,{message : "Error loading object of property " + propertyURI + " of resource " + resourceURI + ".", allowNone : false, allowMultiple: false})){
                                    return;
                                }
                                resultData.result = results.triples[0].object.nominalValue;
                            });
                            return resultData.result;
                        } catch (e) {
                            return q.reject("could not load object of property " + propertyURI + " of resource " + resourceURI + ". Reason: " + e);
                        } finally {
                            //we don't need to release after a promise resolves because
                            //this function isn't deferred.
                            lock.releaseReadLock();
                        }
                        return q.reject("could not load object of property " + propertyURI + " of resource " + resourceURI);
                    }
                );
            })
    }

    won.getWonNodeUriOfNeed = function(needUri){
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getWonNodeUriOfNeed: needUri must not be null"};
        }
        return won.getUniqueObjectOfProperty(needUri, won.WON.hasWonNode)
            .then(
                function(result){return result;},
                function(reason) { return q.reject("could not get WonNodeUri of Need " + needUri + ". Reason: " + reason)});
    }

    won.getNeedUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getNeedUriOfConnection: connectionUri must not be null"};
        }
        return won.getUniqueObjectOfProperty(connectionUri, won.WON.belongsToNeed)
            .then(
                function(result) {
                    return result;
                },
                function(reason) {
                    return q.reject("could not get need uri of connection " + connectionUri + ". Reason: " + reason)
                });
    }

    won.getRemoteConnectionUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getRemoteConnectionUriOfConnection: connectionUri must not be null"};
        }
        return won.getUniqueObjectOfProperty(connectionUri, won.WON.hasRemoteConnection)
            .then(
                function(result){return result;},
                function(reason) { return q.reject("could not get remote connection uri of connection " + connectionUri + ". Reason: " + reason)});
    }

    won.getRemoteneedUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getRemoteneedUriOfConnection: connectionUri must not be null"};
        }
        return won.getUniqueObjectOfProperty(connectionUri, won.WON.hasRemoteNeed)
            .then(
                function(result){return result;},
                function(reason) { return q.reject("could not get remote need uri of connection " + connectionUri + ". Reason: " + reason)});
    }

    won.getEnvelopeDataForNeed=function(needUri){
        if(typeof needUri === 'undefined'||needUri == null){
            throw {message: "getEnvelopeDataForNeed: needUri must not be null"};

        }
        return won.getWonNodeUriOfNeed(needUri)
            .then(function(wonNodeUri){
                var ret = {};
                ret[won.WONMSG.hasSenderNeed] = needUri;
                ret[won.WONMSG.hasSenderNode] = wonNodeUri;
                ret[won.WONMSG.hasReceiverNeed] = needUri;
                ret[won.WONMSG.hasReceiverNode] = wonNodeUri;
                return ret;

            },function(reason) {
                //no connection found
                var deferred = q.defer();
                var ret = {};
                ret[won.WONMSG.hasSenderNeed] = needUri;
                ret[won.WONMSG.hasReceiverNeed] = needUri;
                return ret;
                deferred.resolve(ret);
                return deferred.promise;}
        )
    }
    /**
     * Fetches a structure that can be used directly (in a JSON-LD node) as the envelope data
     * to send a message via the specified connectionUri (that is interpreted as a local connection.
     * @param connectionUri
     * @returns a promise to the data
     */
    won.getEnvelopeDataforConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getEnvelopeDataforConnection: connectionUri must not be null"};
        }
        return won.getNeedUriOfConnection(connectionUri)
            .then(function(needUri) {
                return won.getWonNodeUriOfNeed(needUri)
                    .then(function (wonNodeUri) {
                        return won.getRemoteneedUriOfConnection(connectionUri)
                            .then(function(remoteneedUri){
                                return won.getWonNodeUriOfNeed(remoteneedUri)
                                    .then(function(remoteWonNodeUri){
                                        //if the local connection was created through a hint message (most likely)
                                        //the remote connection is not known or doesn't exist yet. Hence, the next call
                                        //may or may not succeed.
                                        return won.getRemoteConnectionUriOfConnection(connectionUri).then(
                                            function(remoteconnectionUri) {
                                                var ret = {};
                                                ret[won.WONMSG.hasSender] = connectionUri;
                                                ret[won.WONMSG.hasSenderNeed] = needUri;
                                                ret[won.WONMSG.hasSenderNode] = wonNodeUri;
                                                if (remoteconnectionUri != null) {
                                                    ret[won.WONMSG.hasReceiver] = remoteconnectionUri;
                                                }
                                                ret[won.WONMSG.hasReceiverNeed] = remoteneedUri;
                                                ret[won.WONMSG.hasReceiverNode] = remoteWonNodeUri;
                                                return ret;
                                            },function(reason) {
                                                //no connection found
                                                var deferred = q.defer();
                                                var ret = {};
                                                ret[won.WONMSG.hasSender] = connectionUri;
                                                ret[won.WONMSG.hasSenderNeed] = needUri;
                                                ret[won.WONMSG.hasSenderNode] = wonNodeUri;
                                                ret[won.WONMSG.hasReceiverNeed] = remoteneedUri;
                                                ret[won.WONMSG.hasReceiverNode] = remoteWonNodeUri;
                                                return ret;
                                                deferred.resolve(ret);
                                                return deferred.promise;
                                            });
                                    });
                            });
                    });
            });
    }

    /**
     * @param needUri
     * @return {*} the data of all connection-nodes referenced by that need
     */
    won.getConnectionsOfNeed = (needUri) =>
        won.getconnectionUrisOfNeed(needUri)
        .then(connectionUris => won.getNodes(connectionUris))


    /**
     * Loads all URIs of a need's connections.
     * @deprecated possible duplicate of `won.getConnectionsWithOwnNeed` (see there)
     */
    won.getconnectionUrisOfNeed = function(needUri) {
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getconnectionUrisOfNeed: uri must not be null"};
        }
        return won.ensureLoaded(needUri).then(
            function(){
                var lock = getReadUpdateLockPerUri(needUri);
                return lock.acquireReadLock().then(
                    function() {
                        try {
                            var subject = needUri;
                            var predicate = won.WON.hasConnections;
                            var connectionsPromises = [];
                            privateData.store.node(needUri, function (success, graph) {
                                var resultGraph = graph.match(subject, predicate, null);
                                if (resultGraph != null && resultGraph.length > 0) {
                                    for (key in resultGraph.triples) {
                                        var connectionsURI = resultGraph.triples[key].object.nominalValue;
                                        //TODO: here, we fetch, but if we knew that the connections container didn't change
                                        //we could just ensureLoaded. See https://github.com/researchstudio-sat/webofneeds/issues/109
                                        connectionsPromises.push(won.ensureLoaded(connectionsURI).then(function (success) {
                                            var connectionUris = [];
                                            privateData.store.node(connectionsURI, function (success, graph) {
                                                if (graph != null && graph.length > 0) {
                                                    var memberTriples = graph.match(connectionsURI, createNameNodeInStore("rdfs:member"), null);
                                                    for (var memberKey in memberTriples.triples) {
                                                        var member = memberTriples.triples[memberKey].object.nominalValue;
                                                        connectionUris.push(member);
                                                    }
                                                }
                                            });
                                            return connectionUris;
                                        }));
                                    }
                                }
                            });
                            return q.all(connectionsPromises)
                                .then(function (listOfLists) {
                                    //for each hasConnections triple (should only be one, but hey) we get a list of connections.
                                    //now flatten the list.
                                    var merged = [];
                                    merged = merged.concat.apply(merged, listOfLists);
                                    return merged;
                                });
                        } catch (e) {
                            q.reject("could not get connection URIs of need + " + needUri + ". Reason:" + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                    }
                )
            });

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
                            var result = {}
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
     * @param connectionUri
     * @param requesterWebId
     * @return {*} the most recent event for that connection as a full object.
     */
    won.getLatestEventOfConnection = (connectionUri, requesterWebId) =>
        won.getEventsOfConnection(connectionUri, requesterWebId)
            //find latest event:
            .then(eventsLookup => {
                let latestEvent = {};
                for(let [eventUri, event] of entries(eventsLookup)) {
                    const latestEventTime = Number.parseInt(latestEvent.hasReceivedTimestamp)
                    const eventTime = Number.parseInt(event.hasReceivedTimestamp)
                    latestEvent = latestEventTime >= eventTime ? latestEvent : event;
                }
                return latestEvent;
            });

    /**
     * Returns all events associated with a given connection
     * in a promise for an object of (eventUri -> eventData)
     * @param connectionUri
     * @param requesterWebId
     */
    won.getEventsOfConnection = function(connectionUri, requesterWebId) {
        return won.getEventUrisOfConnection(connectionUri, requesterWebId)
            .then(eventUris => won.urisToLookupMap(eventUris,
                    eventUri => won.getNode(eventUri, requesterWebId))
            )
            .catch(e => `Could not get all events of connection ${connectionUri}. Reason: ${e}`);
    };

    /**
     * Returns the uris of all events associated with a given connection
     * @param connectionUri
     * @param requesterWebId
     * @returns promise for an array strings (the uris)
     */
    won.getEventUrisOfConnection = function(connectionUri, requesterWebId) {
        if (!connectionUri ){
            throw {
                message : `getEventUrisOfConnection: connectionUri must not be null. Got: "${connectionUri}"`
            };
        }
        return won.getNodeWithAttributes(connectionUri, requesterWebId)
            .then(connection => connection.hasEventContainer)
            .then(eventContainerUri => won.getNodeWithAttributes(eventContainerUri, requesterWebId))
            .then(eventContainer => eventContainer.member)
            .catch(e => `Could not get all events of connection ${connectionUri}. Reason: ${e}`);
    }


    /**
     * @deprecated this function probably doesn't work anymore. rather use getEventsOfConnection
     *
     * @param connectionUri
     * @param requesterWebId
     * @returns {*}
     */
    won.getAllConnectioneventUris = function(connectionUri, requesterWebId) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getAllConnectioneventUris: connectionUri must not be null"};
        }
        //TODO ensure that the eventcontainer is loaded
        return won.ensureLoaded(connectionUri, requesterWebId).then(
            function(){
               var lock = getReadUpdateLockPerUri(connectionUri);
               return lock.acquireReadLock().then(
                   function() {
                       try {
                           var eventUris = [];
                           var query =
                               "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                               "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                               "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                               "select ?eventUri where { " +
                               "<" + connectionUri + "> a " + won.WON.ConnectionCompacted + ";\n" +
                               won.WON.hasEventContainerCompacted + " ?container.\n" +
                               "?container rdfs:member ?eventUri. \n" +
                               "}";
                           privateData.store.execute(query, [], [], function (success, results) {
                               if (rejectIfFailed(success, results,{message : "Error loading all connection event URIs for connection " + connectionUri +".", allowNone : false, allowMultiple: true})){
                                   return;
                               }
                               for (var key in results) {
                                   var eventUri = getSafeValue(results[key].eventUri);
                                   if (eventUri != null) {
                                       eventUris.push(eventUri);
                                   }
                               }
                           });
                           return eventUris;
                       } catch (e) {
                           return q.reject("Could not get all connection event URIs for connection " + connectionUri +". Reason: " + e);
                       } finally {
                           //we don't need to release after a promise resolves because
                           //this function isn't deferred.
                           lock.releaseReadLock();
                       }
                   }
               );
            });
    }

    won.crawlConnectionData = function(connectionUri, requesterWebId){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "crawlConnectionData: connectionUri must not be null"};
        }
        return won.ensureLoaded(connectionUri, requesterWebId).then(
            function(){
                return won.getAllConnectioneventUris(connectionUri, requesterWebId).then(
                    function(uris){
                        var eventPromises = [];
                        for (key in uris){
                            eventPromises.push(won.ensureLoaded(uris[key]));
                        }
                        return q.all(eventPromises);
                    }
                );
            }
        );

    }

    won.getLastConnectioneventUri = function(connectionUri, requesterWebId) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastConnectioneventUri: connectionUri must not be null"};
        }
        return won.crawlConnectionData(connectionUri, requesterWebId).then(
            function() {
                var lock = getReadUpdateLockPerUri(connectionUri);
                return lock.acquireReadLock().then(
                    function (success) {
                        try {
                            var resultObject = {};
                            var query =
                                "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                                "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                                "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                                "select ?eventUri where { " +
                                "<" + connectionUri + "> a " + won.WON.ConnectionCompacted + ";\n" +
                                won.WON.hasEventContainerCompacted + " ?container.\n" +
                                "?container rdfs:member ?eventUri. \n" +
                                " optional { " +
                                "  ?eventUri msg:hasReceivedTimestamp ?timestamp; \n" +
                                "            msg:hasMessageType ?messageType .\n" +
                                //filter added so we don't show the success/failure events as last events
                                " filter (?messageType != msg:SuccessResponse && ?messageType != msg:FailureResponse)" +
                                " } \n" +
                                " optional { " +
                                "  ?eventUri msg:hasCorrespondingRemoteMessage ?remoteEventUri. \n" +
                                "  ?remoteEventUri msg:hasReceivedTimestamp ?timestamp; \n" +
                                "            msg:hasMessageType ?messageType .\n" +
                                //filter added so we don't show the success/failure events as last events
                                " filter (?messageType != msg:SuccessResponse && ?messageType != msg:FailureResponse)" +
                                " } \n" +
                                "} " +
                                "order by desc(?timestamp) limit 1";
                            privateData.store.execute(query, [], [], function (success, results) {
                                if (rejectIfFailed(success, results, {message: "Error loading last connection event URI for connection " + connectionUri + ".", allowNone: false, allowMultiple: false})) {
                                    return;
                                }
                                for (var key in results) {
                                    var eventUri = getSafeValue(results[key].eventUri);
                                    if (eventUri != null) {
                                        resultObject.eventUri = eventUri;
                                        return;
                                    }
                                }
                            });
                            return resultObject.eventUri;
                        } catch (e) {
                            return q.reject("Could not get last connection event URI for connection " + connectionUri + ". Reason: " + e);
                        } finally {
                            //we don't need to release after a promise resolves because
                            //this function isn't deferred.
                            lock.releaseReadLock();
                        }
                    })
            }
        );

    }


    won.getLastEventOfEachConnectionOfNeed = function(needUri, requesterWebId) {
        //fetch all connection uris of the need
        var allConnectionsPromise = won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], needUri, requesterWebId);
        return allConnectionsPromise.then(
            function getLastEventForConnections(connectionsData){
                return somePromises(
                    //for each connection uri:
                    connectionsData.map(
                        function(conData){
                            return won.executeCrawlableQuery(
                                        won.queries["getLastEventUriOfConnection"],
                                        conData.connection.value,
                                        requesterWebId
                                ).then(function(eventUriResult){
                                            return q.all(
                                                [won.getNodeWithAttributes(eventUriResult[0].eventUri.value, requesterWebId),
                                                 won.getNodeWithAttributes(conData.connection.value),
                                                 won.getNeed(conData.remoteNeed.value)
                                                ]
                                            )
                                        }
                                ).then(function (result) {
                                            //make a nice structure for the data
                                            return {
                                                event: result[0],
                                                connection: result[1],
                                                remoteNeed: result[2]
                                            }
                                        });
                        }
                    )
                )
            });
    }

     won.getConnectionTextMessages = function(connectionUri, requesterWebId) {
        var queryResultPromise = won.executeCrawlableQuery(won.queries["getConnectionTextMessages"], connectionUri, requesterWebId);
        return queryResultPromise.then(
                function processConnectionTextMessages(results){
                        var textMessages = [];
                        for (var key in results) {
                            var textMessage = {};
                            var eventUri = getSafeValue(results[key].eventUri);
                            var timestamp = getSafeValue(results[key].receivedTimestamp);
                            var text = getSafeValue(results[key].text);
                            var senderNeed = getSafeValue(results[key].senderNeed);
                            var ownNodeResponseType = getSafeValue(results[key].ownNodeResponseType);
                            var remoteNodeResponseType = getSafeValue(results[key].remoteNodeResponseType);
                            var sender = getSafeValue(results[key].sender);
                            var isOwnMessage = sender == connectionUri;
                            var commState = "pending";
                            if (isOwnMessage){
                                if (ownNodeResponseType == won.WONMSG.successResponse && remoteNodeResponseType == won.WONMSG.successResponse){
                                    commState = "sent";
                                } else if (ownNodeResponseType == won.WONMSG.failureResponse) {
                                    commState = "failed"
                                } else if (remoteNodeResponseType == won.WONMSG.failureResponse) {
                                    commState = "failed"
                                }
                            } else {
                                commState = "";
                            }
                            if (eventUri != null && timestamp != null && text != null) {
                                textMessage.eventUri = eventUri;
                                textMessage.timestamp = timestamp;
                                textMessage.text = text;
                                textMessage.senderNeed = senderNeed;
                                textMessage.communicationState = commState;
                                textMessages.push(textMessage);
                            }

                        }
                        return textMessages;
                    });
    }

    won.getLastEventTypeBeforeTime = function(connectionUri, beforeTimestamp, requesterWebId) {
        return won.crawlConnectionData(connectionUri, requesterWebId).then(
            function queryLastEventBeforeTime() {
                var lock = getReadUpdateLockPerUri(connectionUri);
                return lock.acquireReadLock().then(
                    function (success) {
                        try {
                            var lastEventTypeBeforeTime;
                            var filterPart = "";
                            if (typeof beforeTimestamp != 'undefined') {
                                filterPart = " filter ( ?timestamp > " + timestamp + " ) \n";
                            }
                            var query =
                                "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                                "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                                "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                                "select distinct ?msgType where { " +
                                "<" + connectionUri + "> a " + won.WON.ConnectionCompacted + ";\n" +
                                won.WON.hasEventContainerCompacted + " ?container.\n" +
                                "?container rdfs:member ?eventUri. \n" +
                                "?eventUri won:hasMessageType ?msgType. \n" +
                                " optional { " +
                                "  ?eventUri msg:hasTimestamp ?timestamp .\n" +
                                " } \n" +
                                filterPart + //" filter ( ?timestamp > " + timestamp + " ) \n" +
                                "} order by desc(?timestamp) ";//limit " + limit;

                            privateData.store.execute(query, [], [], function (success, results) {
                                if (rejectIfFailed(success, results, {message: "Error loading last connection event URI for connection " + connectionUri + ".", allowNone: true, allowMultiple: true})) {
                                    return;
                                }
                                for (var key in results) {
                                    var msgType = getSafeValue(results[key].msgType);
                                    if (msgType != null) {
                                        lastEventTypeBeforeTime = msgType;
                                    }
                                    break;
                                }
                            });
                            return lastEventTypeBeforeTime;
                        } catch (e) {
                            return q.reject("Could not get connection event type before time " + connectionUri + ". Reason: " + e);
                        } finally {
                            //we don't need to release after a promise resolves because
                            //this function isn't deferred.
                            lock.releaseReadLock();
                        }
                    })
            }
        );
    }

    /**
     * Maps `getNodeWithAttributes` over the list of uris and
     * collects the results.
     * @param uris array of strings
     * @param requesterWebId map/object of (uri -> node's data)
     */
    won.getNodes = function(uris, requesterWebId) {
        return won.urisToLookupMap(uris, uri => won.getNode(uri, requesterWebId));
    };

    //aliases (formerly functions that were just pass-throughs)
    won.getConnection =
    won.getConnectionEvent =
    won.getNodeWithAttributes =
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
     * @param requesterWebId
     */
    won.getNode = function(uri, requesterWebId) {
        if(!uri) {
            return Promise.reject({message : "getNode: uri must not be null"})
        }


        let releaseLock = undefined;

        const nodePromise = won.ensureLoaded(uri, requesterWebId)
            .then(() => {
                const lock = getReadUpdateLockPerUri(uri);
                releaseLock = () => lock.releaseReadLock();
                return lock.acquireReadLock();
            })
            .then(() => {
                console.log("linkeddata-service-won.js: getNode:" + uri);
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
                            console.log('linkeddata-service-won.js: warn: could not ' +
                                'load any attributes for node with uri: ', uri,
                                '. Trying sparql-query + result.filter workaround.');
                            privateData.store.graph((success, entireGraph) => {
                                resolve(entireGraph.triples.filter(t => t.subject.nominalValue === uri))
                            })
                        } else {
                            //graph.triples[0].object.nominalValue
                            //graph.triples[0].subject.nominalValue
                            //graph.triples[0].predicate.nominalValue
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
            .catch(releaseLock);

        return nodePromise;
    }

    /**
     * Deletes all triples where the specified uri is the subect. May have side effects on concurrent
     * reads on the rdf store if called without a read lock.
     */
    won.deleteNode = function(uri){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "deleteNode: uri must not be null"};
        }
        console.log("linkeddata-service-won.js: deleting node:   " + uri);
        var deferred = q.defer();
        var query = "delete where {<"+uri+"> ?anyP ?anyO}";
        //var query = "select ?anyO where {<"+uri+"> ?anyP ?anyO}";
        privateData.store.execute(query, function (success, graph) {
            if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
                return;
            } else {
                cacheItemRemove(uri);
                deferred.resolve();
            }
        });
        return deferred.promise;
    }
    
    won.getConnectionWithOwnAndRemoteNeed= function(ownNeedUri,remoteNeedUri){
        return won.getconnectionUrisOfNeed(ownNeedUri).then(connectionUris=>{
            let data = Q.defer()
            let promises=[]
            connectionUris.forEach(connection=>{
                promises.push(won.ensureLoaded(connection))
            })
            Q.all(promises).then(results=>{
                let query="prefix msg: <http://purl.org/webofneeds/message#> \n"+
                    "prefix won: <http://purl.org/webofneeds/model#> \n" +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n"+
                    "select ?connection \n" +
                    " where { \n" +
                    "?connection a won:Connection; \n" +
                    "              won:belongsToNeed <" +ownNeedUri +"> ; \n" +
                    "              won:hasRemoteNeed <" +remoteNeedUri +"> ."+
                    "} \n"

                privateData.store.execute(query,[],[],function(success,results){
                    if (rejectIfFailed(success, results, {message: "Error loading connection for need " + ownNeedUri, allowNone: true, allowMultiple: true})) {
                        return;
                    }
                    if(results.length ===1){
                        let connection = null;
                        won.getConnection(results[0].connection.value).then(connectionData=>{
                            return data.resolve(connectionData)
                        })
                    }
                })
            })
            return data.promise;
        })
    }
    /**
     * Loads the hints for the need with the specified URI into an array of js objects.
     * @return the array or null if no data is found for that URI in the local datastore
     */
    //TODO refactor this method.
    won.getConnectionInStateForNeedWithRemoteNeed = function(needUri,connectionState) {

        return won.getconnectionUrisOfNeed(needUri).then(function(connectionUris){
            let promises=[];
            connectionUris.forEach(function(connection){
                let resultObject = {}
                let data = Q.defer()
                promises.push(data.promise)
                won.getConnection(connection).then(function(connectionData){
                    resultObject.connection = connectionData;
                    let query="prefix msg: <http://purl.org/webofneeds/message#> \n"+
                        "prefix won: <http://purl.org/webofneeds/model#> \n" +
                        "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n"+
                        "select ?remoteNeed \n" +
                        " where { \n" +
                        "<"+connectionData.uri+"> a won:Connection; \n" +
                        "              won:belongsToNeed <" +needUri +"> ; \n" +
                        "              won:hasRemoteNeed ?remoteNeed; \n"+
                        "              won:hasConnectionState "+ connectionState +". \n"+
                        "} \n"

                    privateData.store.execute(query,[],[],function(success,results){
                        if (rejectIfFailed(success, results, {message: "Error loading connection for need " + needUri + "in state"+connectionState+".", allowNone: true, allowMultiple: true})) {
                            return;
                        }
                        let needs = []
                        let ownNeedPromise = won.getNeed(needUri);
                        needs.push(ownNeedPromise);
                        let remoteNeedPromise = won.getNeed(results[0].remoteNeed.value)
                         needs.push(remoteNeedPromise)
                        Q.all(needs).then(function(needData){
                            resultObject.ownNeed = needData[0]
                            resultObject.remoteNeed=needData[1]
                            return data.resolve(resultObject );
                        })



                    })
                })
            })
            return Q.all(promises)
        })

    }


    /**
     * Takes an array of uris, performs the lookup function on each
     * of them seperately, collects the results and builds an map/object
     * with the uris as keys and the results as values.
     * @param uris
     * @param asyncLookupFunction
     * @return {*}
     */
    won.urisToLookupMap = function(uris, asyncLookupFunction) {
        const asyncLookups = uris.map(uri => asyncLookupFunction(uri));
        return Promise.all(asyncLookups).then( dataObjects => {
            const lookupMap = {};
            //make sure there's the same
            for (let i = 0; i < uris.length; i++) {
                lookupMap[uris[i]] = dataObjects[i];
            }
            return lookupMap;
        });
    }

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
            console.log("linkeddata-service-won.js: executing query: \n"+query);
            var locks = getReadUpdateLocksPerUris(relevantResources);
            var promises = acquireReadLocks(locks);
            return q.all(promises).then(
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
                        console.log("linkeddata-service-won.js: Could not execute query. Reason: " + e);
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
        }

        var resolvePropertyPathsFromBaseUri = function resolvePropertyPathsFromBaseUri(propertyPaths, baseUri, relevantResources){
            console.log("linkeddata-service-won.js: resolving " + propertyPaths.length + " property paths on baseUri " + baseUri);
            var locks = getReadUpdateLocksPerUris(relevantResources);
            var promises = acquireReadLocks(locks);
            return q.all(promises).then(
                function () {
                    try {
                        var resolvedUris = [];
                        propertyPaths.map(
                            function(propertyPath){
                                console.log("linkeddata-service-won.js: resolving property path: " + propertyPath.propertyPath);
                                var foundUris = won.resolvePropertyPathFromBaseUri(
                                        baseUri,
                                        propertyPath.propertyPath,
                                        propertyPath.prefixes);

                                //resolve all property paths, add to 'resolvedUris'
                                Array.prototype.push.apply(resolvedUris, foundUris);
                                console.log("linkeddata-service-won.js: resolved to " + foundUris.length + " resources (total " + resolvedUris.length+")");
                        });
                        return resolvedUris;
                    }catch(e){
                        console.log(e)
                    } finally {
                        //release the read locks
                        locks.map(
                            function(lock){
                                lock.releaseReadLock();
                            }
                        );
                    }
                });
        }

        var resolveOrExecuteQuery = function resolveOrExecuteQuery(resolvedUris){
            if (won.isNull(recursionData.depth)){
                recursionData.depth = 0;
            }
            console.log("linkeddata-service-won.js: crawlableQuery:resolveOrExecute depth=" + recursionData.depth + ", resolvedUris=" + JSON.stringify(resolvedUris)+", relevantResources=" + JSON.stringify(relevantResources));
            recursionData.depth++;
            if (won.containsAll(relevantResources, resolvedUris) || recursionData.depth >= MAX_RECURSIONS) {
                console.log("linkeddata-service-won.js: crawlableQuery:resolveOrExecute crawling done");
                return executeQuery(crawlableQuery.query, baseUri, relevantResources);
            } else {
                console.log("linkeddata-service-won.js: crawlableQuery:resolveOrExecute resolving property paths ...");
                Array.prototype.push.apply(relevantResources, resolvedUris);
                var loadedPromises = relevantResources.map(function(x){ return won.ensureLoaded(x, requesterWebId)});
                return q.all(loadedPromises)
                    .then(
                        function (x) {
                            return resolvePropertyPathsFromBaseUri(crawlableQuery.propertyPaths, baseUri, relevantResources);
                        })
                    .then(
                        function(newlyResolvedUris) {
                            return resolveOrExecuteQuery(newlyResolvedUris);
                        });
            }
        }

        return resolveOrExecuteQuery([baseUri]);

    }

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
        "getConnectionTextMessages" : {
            propertyPaths : [
                { prefixes :
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                  propertyPath : "won:hasEventContainer"
                },
                { prefixes :
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                    propertyPath : "won:hasEventContainer/rdfs:member"
                }
            ],
        query:
        //note: we have to take the max timestamp as there might be multiple timestamps added to the
        //message dataset during processing
            "prefix msg: <http://purl.org/webofneeds/message#> \n"+
                "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n"+
                "select distinct ?eventUri ?receivedTimestamp ?text ?senderNeed ?sender ?ownNodeResponseType ?remoteNodeResponseType\n" +
                " where { \n" +
                "  <::baseUri::> a won:Connection; \n" +
                "  won:hasEventContainer ?container.\n" +
                " {\n" +
                "  ?container rdfs:member ?eventUri.\n" +
                "  ?eventUri msg:hasReceivedTimestamp ?receivedTimestamp;\n" +
                " } union {\n" +
                "  ?container rdfs:member/msg:hasCorrespondingRemoteMessage ?eventUri.\n" +
                "  ?eventUri msg:hasReceivedTimestamp ?receivedTimestamp;\n" +
                " }\n" +
                "  ?eventUri msg:hasMessageType ?messageType;\n" +
                "       won:hasTextMessage ?text;\n" +
                "       msg:hasSenderNeed ?senderNeed;\n" +
                "       msg:hasSender ?sender.\n" +
                " optional { \n" +
                "   ?ownNodeResponse msg:isResponseTo ?eventUri; \n" +
                "                    msg:hasMessageType ?ownNodeResponseType. \n" +
                " } \n" +
                " optional { \n" +
                "   ?remoteNodeResponse msg:isRemoteResponseTo ?eventUri; \n" +
                "                    msg:hasMessageType ?remoteNodeResponseType. \n" +
                " } \n" +
                " filter (?messageType != msg:SuccessResponse && ?messageType != msg:FailureResponse)\n" +
                "}\n" +
                "order by desc(?receivedTimestamp)"
        },
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


        "getLastEventUriOfConnection" : {
        propertyPaths : [
            { prefixes :
                "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                propertyPath : "won:hasEventContainer"
            },
            { prefixes :
                "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> " +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> " +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ",
                propertyPath : "won:hasEventContainer/rdfs:member"
            }
        ],
        query:
            "prefix msg: <http://purl.org/webofneeds/message#> \n" +
            "prefix won: <http://purl.org/webofneeds/model#> \n" +
            "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
            "select ?eventUri  \n" +
            "where { \n" +
            " {\n" +
            "  <::baseUri::> a won:Connection; \n" +
            "  won:hasEventContainer ?container.\n" +
            "  ?container rdfs:member ?eventUri.\n" +
            "  ?eventUri msg:hasReceivedTimestamp ?receivedTimestamp.\n" +
            "  ?eventUri msg:hasMessageType ?messageType.\n" +
            " } union {\n" +
            "  <::baseUri::> a won:Connection; \n" +
            "  won:hasEventContainer ?container.\n" +
            "  ?container rdfs:member/msg:hasCorrespondingRemoteMessage ?eventUri.\n" +
            "  ?eventUri msg:hasReceivedTimestamp ?receivedTimestamp.\n" +
            "  ?eventUri msg:hasMessageType ?messageType.\n" +
            " }\n" +
            " filter (?messageType != msg:SuccessResponse)\n" +
            "}\n" +
            "order by desc(?receivedTimestamp) limit 1 \n"

            }
    }
})();
