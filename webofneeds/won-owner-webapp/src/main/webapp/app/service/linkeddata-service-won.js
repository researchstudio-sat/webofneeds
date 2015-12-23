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
import * as q from 'q';
(function(){
    if(!won) won = {};

    var privateData = {};

    won.ld_reset = function() {
        privateData = {};
        //create an rdfstore-js based store as a cache for rdf data.
        privateData.store =  rdfstore.create();
        privateData.store.setPrefix("msg","http://purl.org/webofneeds/message#");
        privateData.store.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        privateData.store.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        privateData.store.setPrefix("xsd","http://www.w3.org/2001/XMLSchema#");
        privateData.store.setPrefix("won","http://purl.org/webofneeds/model#");

        privateData.readUpdateLocksPerUri = {}; //uri -> ReadUpdateLock
        privateData.cacheStatus = {} //uri -> {timestamp, cacheItemState}
    }
    var CACHE_ITEM_STATE = { OK: 1, DIRTY: 2, UNRESOLVABLE: 3, FETCHING: 4};

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
     * Similar to $q.all, takes an array of promises and returns a promise.
     * That promise will resolve if at least one of the promises succeeds.
     * The value with which it resolves it is an array of equal length as the input
     * containing either the resolve value of the promise or null if rejected.
     * If an errorHandler is specified, it is called with ([array key], [reject value]) of
     * each rejected promise.
     * @param promises
     */
    var somePromises = function(promises, errorHandler){
        var deferred = $q.defer(),
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
                console.log("warning: promise failed. Reason " + JSON.stringify(reason));
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
            var deferred = $q.defer();
            if (this.updateInProgress || this.blockedUpdaters.length > 0){
                //updates are already in progress or are waiting. block.
                console.log("rul:read:block:  " + this.uri + " " + this.getLockStatusString());
                this.blockedReaders.push(deferred);
                this.grantLockToUpdaters();
            } else {
                //nobody wishes to update the resource, the caller may read it
                //add the deferred execution to the blocked list, just in case
                //there are others blocket there, and then grant access to all
                console.log("rul:read:grant:  " + this.uri + " " + this.getLockStatusString());
                this.blockedReaders.push(deferred);
                this.grantLockToReaders();
            }
            return deferred.promise;
        },
        acquireUpdateLock: function(){
            var deferred = q.defer();

            if (this.activeReaderCount > 0 ) {
                //readers are present, we have to wait till they are done
                console.log("rul:updt:block:  " + this.uri + " " + this.getLockStatusString());
                this.blockedUpdaters.push(deferred);
            } else {
                console.log("rul:updt:grant:  " + this.uri + " " + this.getLockStatusString());
                //add the deferred update to the list of blocked updates just
                //in case there are more, then grant the lock to all of them
                this.blockedUpdaters.push(deferred);
                this.grantLockToUpdaters();
            }

            return deferred.promise;
        },
        releaseReadLock: function(){
            console.log("rul:read:release:" + this.uri + " " + this.getLockStatusString());
            this.activeReaderCount --;
            if (this.activeReaderCount < 0){
                throw {message: "Released a read lock that was never acquired"}
            } else if (this.activeReaderCount == 0) {
                //no readers currently have a lock: we can update - if we should
                this.grantLockToUpdaters();
            }
        },
        releaseUpdateLock: function(){
            console.log("rul:updt:release:" + this.uri + " " + this.getLockStatusString());
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
                console.log("rul:updt:all:    " + this.uri + " " + this.getLockStatusString());
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
                console.log("rul:readers:all: " + this.uri + " " + this.getLockStatusString());
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
        console.log("add to cache:    " + uri);
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
        console.log("cacheSt: " + nameOfState + ":" +retStr + "   " + uri);
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
        console.log("cacheSt: OK or Unresolvable:" +retStr + "   " + uri);
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
        console.log("mark accessed:   " + uri);
        privateData.cacheStatus[uri].timestamp = new Date().getTime();
    }

    var cacheItemMarkDirty = function cacheItemMarkDirty(uri){
        var entry = privateData.cacheStatus[uri];
        if (typeof entry === 'undefined') {
            return;
        }
        console.log("mark dirty:      " + uri);
        privateData.cacheStatus[uri].state = CACHE_ITEM_STATE.DIRTY;
    }

    var cacheItemMarkUnresolvable = function cacheItemMarkUnresolvable(uri){
        console.log("mark unres:      " + uri);
        privateData.cacheStatus[uri] = {timestamp: new Date().getTime(), state: CACHE_ITEM_STATE.UNRESOLVABLE};
    }

    var cacheItemMarkFetching = function cacheItemMarkFetching(uri){
        console.log("mark fetching:   " + uri);
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
     * Invalidates the appropriate linked data cache items such that all information about a
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
        if (connectionUri != null) {
            cacheItemMarkDirty(connectionUri);
        }
        return won.getNeedConnectionsUri(needUri).then(
            function(connectionsUri){
                if (connectionsUri != null){
                    cacheItemMarkDirty(connectionsUri);
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
        locks = [];
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
        acquiredLocks = [];
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
        console.log("storing jsonld data for uri: " + uri);
        privateData.store.load("application/ld+json", data, function (success, results) {
            console.log("added jsonld data to rdf store, success: " + success);
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
            for (key in results) {
                resultObject.result.push(results[key].target.value);
            }
        });
        return resultObject.result;
    }

    /**
     * Deletes the node with specified uri from the local triplestore, then
     * fetches the linked data for the specified uri and saves it
     * in the local triplestore.
     * @param uri
     * @return a promise to a boolean which indicates success
     */
    won.fetch = function(uri, requesterWebId) {
        var tempUri = uri+'?prev='+new Date().getTime();
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "fetch: uri must not be null"};
        }
        console.log("fetch announced: " + uri);
        var lock = getReadUpdateLockPerUri(uri);
        return lock.acquireUpdateLock().then(
                function() {
                    // We can use loadFromURI() when we are able to supply our web-id with the call to linked data uri:
                    // return loadFromURI(uri);
                    // In the meanwhile, we use our owner-server as an intermediary that can access remote linked data
                    // on behalf of the need with its web-id, because it has the private key of that need identity:
                    return loadFromOwnServer(uri, requesterWebId);
                }
        ).then(() =>
            {console.log("FINALLY")
                lock.releaseUpdateLock()})

        //make sure we only release the lock when our main promise resolves
/*        ["finally"](function(){
            lock.releaseUpdateLock();
        });*/
    }

    var loadFromOwnServer = function(uri, requesterWebId) {
        var promise = new Promise(function(resolve,reject){});

        console.log("updating:        " + uri);
        try {
            /*
             TODO: uncommenting the delete block is experimental. Using it is not exactly safe, either, as we risk to delete triples that the subsequent fetch will not restore.

             console.log("deleting :       " + uri);
             var query = "delete where {<" + uri + "> ?anyP ?anyO}";
             var failed = {};
             privateData.store.execute(query, function (success, graph) {
             if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
             failed.failed = true;
             return;
             }
             console.log("deleted:         " + uri)
             });
             if (failed.failed) {
             return deferred.promise;
             }                   */
            //the execute call above is not asynchronous, so we can safely continue outside the callback.
            console.log("fetching:        " + uri);
            fetchLinkedDataFromOwnServer(uri, requesterWebId).then(
                function success(dataset) {
                    if (Object.keys(dataset).length === 0 ) {
                        promise.reject("failed to load " + uri);
                    } else {
                        console.log("fetched:         " + uri)
                        won.addJsonLdData(uri, dataset);
                        promise.resolve(uri);
                    }
                },
                function failure(data) {
                    promise.reject("failed to load " + uri);
                }
            );
        } catch (e) {
            $rootScope.$apply(function () {
                promise.reject("failed to load " + uri + ". Reason: " + e);
            });
        }
        return promise;
    }

    var fetchLinkedDataFromOwnServer = function(dataUri, requesterWebId) {
        var requestUri = '/owner/rest/linked-data/?uri=' + encodeURIComponent(dataUri);
        //if (requesterWebId != null) {
        //    requestUri = requestUri + "&requester=" + encodeURIComponent(requesterWebId);
        //}#var requestUri = '/owner/rest/linked-data/
       // var requestUrl = new URL('/owner/rest/linked-data/')
        //var params = {uri:dataUri}
        if (typeof requesterWebId != 'undefined' && requesterWebId != null) {
            requestUri = requestUri + "&reqeuster="+encodeURIComponent(requesterWebId);
        }
        var find = '%3A';
        var re = new RegExp(find, 'g');


        requestUri = requestUri.replace(re,':')
        return fetch(requestUri, {
            method: 'get',
            credentials: 'include'
        }).then(
            function success(response){
                return response.data;
            },
            function failure(response){
                console.log("ERROR: could not fetched linked data " + dataUri + " for need " + requesterWebId)
                return {};
            }
        )
    }

    var loadFromURI = function(uri) {
        var deferred = q.defer();
        console.log("updating:        " + uri);
        try {
            /*
             TODO: uncommenting the delete block is experimental. Using it is not exactly safe, either, as we risk to delete triples that the subsequent fetch will not restore.

             console.log("deleting :       " + uri);
             var query = "delete where {<" + uri + "> ?anyP ?anyO}";
             var failed = {};
             privateData.store.execute(query, function (success, graph) {
             if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
             failed.failed = true;
             return;
             }
             console.log("deleted:         " + uri)
             });
             if (failed.failed) {
             return deferred.promise;
             }                   */
            //the execute call above is not asynchronous, so we can safely continue outside the callback.
            console.log("fetching:        " + uri);
            privateData.store.load('remote', uri, function (success, results) {
                $rootScope.$apply(function () {
                    if (success) {
                        console.log("fetched:         " + uri)
                        cacheItemInsertOrOverwrite(uri);
                        deferred.resolve(uri);
                    } else {
                        deferred.reject("failed to load " + uri);
                    }
                });
            });
        } catch (e) {
            $rootScope.$apply(function () {
                deferred.reject("failed to load " + uri + ". Reason: " + e);
            });
        }
        var promise = deferred.promise;
        return promise;
    }

    /**
     * Fetches the linked data for the specified URI and saves it in the local triplestore if necessary.
     * Note: this method does not grant the caller any locks. This has to be done by the caller after calling this method.
     * @param uri
     * @return a promise to a boolean which indicates success
     */
    won.ensureLoaded = function(uri, requesterWebId) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "ensureLoaded: uri must not be null"};
        }
        console.log("ensuring loaded: " +uri);
        //we also allow unresolvable resources, so as to avoid re-fetching them.
        //we also allow resources that are currently being fetched.
        if (cacheItemIsOkOrUnresolvableOrFetching(uri)){
            var deferred = q.defer();
            cacheItemMarkAccessed(uri);
            deferred.resolve(uri);
            return deferred.promise;
        }
        //uri isn't loaded or needs to be refrehed. fetch it.
        cacheItemMarkFetching(uri);
        return won.fetch(uri, requesterWebId)
            .then(
                function(x){
                    cacheItemMarkAccessed(uri);
                },
                function(reason){
                    cacheItemMarkUnresolvable(uri);
                }
            )

    }

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
    won.getNeed = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getNeed: uri must not be null"};
        }
       return won.ensureLoaded(uri).then(
           function() {
               var lock = getReadUpdateLockPerUri(uri);
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
                               "<" + uri + "> " + won.WON.isInStateCompacted + " ?state .\n" +
                               "<" + uri + "> " + won.WON.hasBasicNeedTypeCompacted + " ?basicNeedType .\n" +
                               "<" + uri + "> " + won.WON.hasContentCompacted + " ?content .\n" +
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
                               "<" + uri + "> " + " <" + "http://purl.org/dc/terms/created" + "> " + "?creationDate" +
                               " .\n" +
                               "<" + uri + "> " + won.WON.hasConnectionsCompacted + " ?connections .\n" +
                               "<" + uri + "> " + won.WON.hasWonNodeCompacted + " ?wonNode .\n" +
                               "OPTIONAL {<" + uri + "> " + won.WON.hasEventContainerCompacted + " ?eventContainer" +
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
                               if (rejectIfFailed(success, results, {message: "Could not load need " + uri + ".", allowNone: false, allowMultiple: false})) {
                                   return;
                               }
                               var result = results[0];
                               resultObject.uri = uri;
                               resultObject.basicNeedType = getSafeValue(result.basicNeedType);
                               resultObject.title = getSafeValue(result.title);
                               resultObject.tags = getSafeValue(result.tags);
                               resultObject.textDescription = getSafeValue(result.textDescription);
                               resultObject.creationDate = getSafeValue(result.creationDate);
                               resultObject.state = getSafeValue(result.state);
                           });
                           return resultObject;
                       } catch (e) {
                           return q.reject("could not load need " + uri + ". Reason: " + e);
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

/*
    won.getLastEventOfEachConnectionOfNeed = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getLastEventOfEachConnectionOfNeed: uri must not be null"};
        }
        return won.getconnectionUrisOfNeed(uri)
            .then(function(conUris) {
                try {
                    var promises = [];
                    for (var conKey in conUris) {
                        promises.push(won.getLastEventOfConnection(conUris[conKey]));
                    }
                    return somePromises(promises, function(key, reason){
                        won.reportError("could not fetch last event of connection " + conUris[key], reason);
                    }).then(function(val) {
                        return won.deleteWhereNull(val)
                    });
                } catch (e) {
                    return q.reject("could not get last event of connection " + uri + ". Reason: " + e);
                }
            }
        );
    }
  */


    won.getLastEventOfConnection = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastEventOfConnection: connectionUri must not be null"};
        }
        return won.getConnection(connectionUri)
            .then(function (connection) {
                return won.getNeed(connection.hasRemoteNeed)
                    .then(function (need) {
                        return won.getLastConnectionEvent(connectionUri)
                            .then(
                                function (event) {
                                    return {connection: connection, remoteNeed: need, event: event}
                                },function(reason){
                                    //remote need's won node may be offline - don't let that kill us
                                    var deferred = q.defer();
                                    deferred.resolve(
                                        {connection: connection, remoteNeed: {'title': '[could not load]'}, event: event}
                                    );
                                    return deferred.promise;
                                });
                    });
            });
    }


    won.getAllConnectionEvents = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getAllConnectionEvents: connectionUri must not be null"};
        }
        return won.getAllConnectioneventUris(connectionUri)
            .then(function (eventUris) {
                try {
                    var eventPromises = [];
                    for (var evtKey in eventUris) {
                        eventPromises.push(won.getConnectionEvent(eventUris[evtKey]));
                    }
                    return q.all(eventPromises)
                } catch (e) {
                    return q.reject("could not get all connection events for connection " + connectionUri + ". Reason: " + e);
                }
            });
    }

    won.getLastConnectionEvent = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastConnectionEvent: connectionUri must not be null"};
        }
        return won.getLastConnectioneventUri(connectionUri)
            .then(function (eventUri) {
                    return won.getConnectionEvent(eventUri);
            })
    }


    /**
     * Loads all URIs of a need's connections.
     */
    won.getconnectionUrisOfNeed = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getconnectionUrisOfNeed: uri must not be null"};
        }
        return won.ensureLoaded(uri).then(
            function(){
                var lock = getReadUpdateLockPerUri(uri);
                return lock.acquireReadLock().then(
                    function() {
                        try {
                            var subject = uri;
                            var predicate = won.WON.hasConnections;
                            var connectionsPromises = [];
                            privateData.store.node(uri, function (success, graph) {
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
                            q.reject("could not get connection URIs of need + " + uri + ". Reason:" + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                    }
                )
            });

    }

    won.getNeedConnectionsUri = function(needUri) {
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
    
    

    won.getConnection = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getConnection: connectionUri must not be null"};
        }
        return won.getNodeWithAttributes(connectionUri);
    }

    won.getConnectionEvent = function(eventUri) {
        if (typeof eventUri === 'undefined' || eventUri == null  ){
            throw {message : "getConnectionEvent: eventUri must not be null"};
        }
        return won.getNodeWithAttributes(eventUri);
    }



    won.getAllConnectioneventUris = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getAllConnectioneventUris: connectionUri must not be null"};
        }
        return won.ensureLoaded(connectionUri).then(
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

    won.crawlConnectionData = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "crawlConnectionData: connectionUri must not be null"};
        }
        return won.ensureLoaded(connectionUri).then(
            function(){
                return won.getAllConnectioneventUris(connectionUri).then(
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

    won.getLastConnectioneventUri = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastConnectioneventUri: connectionUri must not be null"};
        }
        return won.crawlConnectionData(connectionUri).then(
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
        var allConnectionsPromise = won.executeCrawlableQuery(queries["getAllConnectionUrisOfNeed"], needUri, requesterWebId);
        return allConnectionsPromise.then(
            function getLastEventForConnections(connectionsData){
                return somePromises(
                    //for each connection uri:
                    connectionsData.map(
                        function(conData){
                            return won.executeCrawlableQuery(
                                        queries["getLastEventUriOfConnection"],
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
        var queryResultPromise = won.executeCrawlableQuery(queries["getConnectionTextMessages"], connectionUri, requesterWebId);
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

    won.getLastEventTypeBeforeTime = function(connectionUri, beforeTimestamp) {
        return won.crawlConnectionData(connectionUri).then(
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
     * Fetches the triples where URI is subject and add objects of those triples to the
     * resulting structure by the localname of the predicate.
     * The URI is added as property 'uri'.
     * @param eventUri
     */
    won.getNodeWithAttributes = function(uri, requesterWebId){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getNodeWithAttributes: uri must not be null"};
        }
        return won.ensureLoaded(uri, requesterWebId).then(
            function(){
                var lock = getReadUpdateLockPerUri(uri);
                return lock.acquireReadLock().then(
                    function() {
                        console.log("getNodeWithAttrs:" + uri);
                        try {
                            var node = {};
                            privateData.store.node(uri, function (success, graph) {
                                if (graph.length == 0) {
                                    console.log("warn: could not load any attributes for node with uri: " + uri);
                                }
                                if (rejectIfFailed(success, graph,{message : "Error loading node with attributes for URI " + uri+".", allowNone : false, allowMultiple: true})){
                                    return;
                                }
                                for (key in graph.triples) {
                                    var propName = won.getLocalName(graph.triples[key].predicate.nominalValue);
                                    node[propName] = graph.triples[key].object.nominalValue;
                                }
                            });
                            node.uri = uri;
                            return node;
                        } catch (e) {
                            return q.reject("could not get node " + uri + "with attributes: " + e);
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
     * Deletes all triples where the specified uri is the subect. May have side effects on concurrent
     * reads on the rdf store if called without a read lock.
     */
    won.deleteNode = function(uri){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "deleteNode: uri must not be null"};
        }
        console.log("deleting node:   " + uri);
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
    
    

    /**
     * Loads the default data of the need with the specified URI into a js object.
     * @return the object or null if no data is found for that URI in the local datastore
     */
    won.getMessage = function(uri) {
        //TODO: SPARQL query that returns the common message properties
    }

    /**
     * Loads the hints for the need with the specified URI into an array of js objects.
     * @return the array or null if no data is found for that URI in the local datastore
     */
    won.getHintsForNeed = function(uri) {
        //TODO: SPARQL query that returns an array of hints
    }

    /**
     * Loads the connections for the need with the specified URI into an array of js objects.
     * @return the array or null if no data is found for that URI in the local datastore
     */
    won.getConnections = function(uri) {
        //TODO: SPARQL query that returns an array of connections
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
            console.log("executing query: \n"+query);
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
                        console.log("Could not execute query. Reason: " + e);
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
            console.log("resolving " + propertyPaths.length + " property paths on baseUri " + baseUri);
            var locks = getReadUpdateLocksPerUris(relevantResources);
            var promises = acquireReadLocks(locks);
            return q.all(promises).then(
                function () {
                    try {
                        var resolvedUris = [];
                        propertyPaths.map(
                            function(propertyPath){
                                console.log("resolving property path: " + propertyPath.propertyPath);
                                var foundUris = linkedDataService
                                    .resolvePropertyPathFromBaseUri(
                                        baseUri,
                                        propertyPath.propertyPath,
                                        propertyPath.prefixes);

                                //resolve all property paths, add to 'resolvedUris'
                                Array.prototype.push.apply(resolvedUris, foundUris);
                                console.log("resolved to " + foundUris.length + " resources (total " + resolvedUris.length+")");
                        });
                        return resolvedUris;
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
            console.log("crawlableQuery:resolveOrExecute depth=" + recursionData.depth + ", resolvedUris=" + JSON.stringify(resolvedUris)+", relevantResources=" + JSON.stringify(relevantResources));
            recursionData.depth++;
            if (won.containsAll(relevantResources, resolvedUris) || recursionData.depth >= MAX_RECURSIONS) {
                console.log("crawlableQuery:resolveOrExecute crawling done");
                return executeQuery(crawlableQuery.query, baseUri, relevantResources);
            } else {
                console.log("crawlableQuery:resolveOrExecute resolving property paths ...");
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
    var queries = {
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

        // get each connection of the specified need
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
                "select ?connection ?need ?remoteNeed  \n" +
                " where { \n" +
                " <::baseUri::> a won:Need; \n" +
                "           won:hasConnections ?connections.\n" +
                "  ?connections rdfs:member ?connection. \n" +
                "  ?connection won:belongsToNeed ?need; \n" +
                "              won:hasRemoteNeed ?remoteNeed. \n"+
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
