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

angular.module('won.owner').factory('linkedDataService', function ($q, $rootScope,$log, utilService) {
    linkedDataService = {};

    var privateData = {};

    linkedDataService.reset = function() {
        privateData = {};
        //create an rdfstore-js based store as a cache for rdf data.
        privateData.store =  rdfstore.create();
        privateData.store.setPrefix("msg","http://purl.org/webofneeds/message#");
        privateData.store.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        privateData.store.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        privateData.store.setPrefix("xsd","http://www.w3.org/2001/XMLSchema#");
        privateData.store.setPrefix("won","http://purl.org/webofneeds/model#");

        privateData.readUpdateLocksPerUri = {}; //uri -> ReadUpdateLock
        privateData.cacheStatus = {} //uri -> [last access timestamp, 0 if dirty]
    }
    linkedDataService.reset();


    var createNameNodeInStore = function(uri){
        return privateData.store.rdf.createNamedNode(privateData.store.rdf.resolve(uri));
    }

    var getSafeValue = function(dataItem) {
        if (dataItem == null) return null;
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
            results = angular.isArray(promises) ? [] : {},
            handler = typeof errorHandler === 'function' ? errorHandler : function(x,y){};

        if (promises.length == 0) {
            deferred.reject(results);
        }

        angular.forEach(promises, function(promise, key) {
            promise.then(function(value) {
                successes++;
                if (results.hasOwnProperty(key)) return; //TODO: not sure if we need this
                results[key] = value;
                if (failures + successes >= numPromises) deferred.resolve(results);
            }, function(reason) {
                failures ++;
                $log.error("warning: promise failed. Reason " + JSON.stringify(reason));
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
     * then all writers are unblocked. The update function can be passed with every
     * acquireUpdateLock(function) call, but the function passed in the first call
     * since the last update is really used.
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
        this.updateInProgress = false;
        this.updateFunction = null;
    };
    ReadUpdateLock.prototype = {
        constructor: won.ReadUpdateLock,
        acquireReadLock: function(){
            var deferred = $q.defer();
            if (this.updateInProgress || this.blockedUpdaters.length > 0){
                //updates are already in progress or are waiting. block.
                $log.debug("rul:read:block:  " + this.uri);
                this.blockedReaders.push(deferred);
            } else {
                //nobody wishes to update the resource, the caller may read it
                //add the deferred execution to the blocked list, just in case
                //there are others blocket there, and then grant access to all
                $log.debug("rul:read:grant:  " + this.uri);
                this.blockedReaders.push(deferred);
                this.grantLockToReaders();
            }
            return deferred.promise;
        },
        runAsUpdate: function(updateFunction){
            if (this.updateFunction == null) {
                this.updateFunction = updateFunction;
            }
            var deferred = $q.defer();
            if (this.activeReaderCount > 0 ) {
                //readers are present, we have to wait till they are done
                $log.debug("rul:updt:block:  " + this.uri);
                this.blockedUpdaters.push(deferred);
            } else {
                $log.debug("rul:updt:grant:  " + this.uri);
                //add the deferred update to the list of blocked updates just
                //in case there are more, then grant the lock to all of them
                this.blockedUpdaters.push(deferred);
                this.grantLockToUpdaters();
            }
            return deferred.promise;
        },
        releaseReadLock: function(){
            this.activeReaderCount --;
            if (this.activeReaderCount < 0){
                throw {message: "Released a read lock that was never acquired"}
            } else if (this.activeReaderCount == 0) {
                //no readers currently have a lock: we can update - if we should
                this.grantLockToUpdaters();
            }
        },
        grantLockToUpdaters: function() {
            if (this.blockedUpdaters.length > 0 && ! this.updateInProgress) {
                $log.debug("rul:updt:all:    " + this.uri + "(unblocking " + this.blockedUpdaters.length +")");
                //there are blocked updaters. let them proceed.
                this.updateInProgress = true;
                var updatePromise = null;
                if (this.updateFunction != null){
                    updatePromise = this.updateFunction();
                    this.updateFunction = null;
                }
                if (updatePromise == null){
                    var deferred = $q.defer();
                    deferred.resolve();
                    updatePromise = deferred.promise;
                }
                var that = this;
                updatePromise.then(
                    function(value){
                        for (var i = 0; i < that.blockedUpdaters.length; i++) {
                            var deferredUpdate = that.blockedUpdaters[i];
                            deferredUpdate.resolve(value);
                            that.blockedUpdaters.splice(i, 1);
                            i--;
                        }
                        that.updateInProgress = false;
                    },
                    function(reason){
                        for (var i = 0; i < that.blockedUpdaters.length; i++) {
                            var deferredUpdate = that.blockedUpdaters[i];
                            deferredUpdate.reject(reason);
                            that.blockedUpdaters.splice(i, 1);
                            i--;
                        }
                        that.updateInProgress = false;
                    }
                );
            }
        },
        grantLockToReaders: function() {
            if (this.blockedReaders.length > 0) {
                $log.debug("rul:readers:all: " + this.uri + "(unblocking " + this.blockedReaders.length +")");
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

    var CACHE_DIRTY = -1;

    linkedDataService.cacheItemInsertOrOverwrite = function(uri){
        $log.debug("add to cache:    " + uri);
        privateData.cacheStatus[uri] = new Date().getTime();
    }

    linkedDataService.cacheItemIsLoaded = function(uri){
        var ret = typeof privateData.cacheStatus[uri] !== 'undefined';
        var retStr = (ret + "     ").substr(0,5);
        $log.debug("inCache: " + retStr + "   " + uri);
        return ret;
    }

    /**
     * Returns true iff the uri is loaded and marked as dirty. (i.e. last access
     * timestamp == CACHE_DIRTY ( == -1)
     * @param uri
     * @returns {boolean}
     */
    linkedDataService.cacheItemIsDirty = function(uri){
        var lastAccess = privateData.cacheStatus[uri];
        var ret = false;
        if (typeof lastAccess === 'undefined') {
            ret = false
        } else {
            ret = lastAccess == CACHE_DIRTY;
        }
        var retStr = (ret + "     ").substr(0,5);
        $log.debug("isDirty: " + retStr + "   " + uri);
        return ret;
    }

    /**
     * Returns true iff the uri is loaded and not marked as dirty.
     * @param uri
     * @returns {boolean}
     */
    linkedDataService.cacheItemIsOk = function(uri){
        var lastAccess = privateData.cacheStatus[uri];
        var ret = false;
        if (typeof lastAccess === 'undefined') {
            ret = false
        } else {
            ret = lastAccess != CACHE_DIRTY;
        }
        var retStr = (ret + "     ").substr(0,5);
        $log.debug("isCacheOk: " + retStr + " " + uri);
        return ret;
    }

    linkedDataService.cacheItemMarkAccessed = function(uri){
        var lastAccess = privateData.cacheStatus[uri];
        if (typeof lastAccess === 'undefined') {
            throw {message : "Trying to mark unloaded uri " + uri +" as accessed"}
        } else if (lastAccess === CACHE_DIRTY){
            throw {message : "Trying to mark uri " + uri +" as accessed, but it is already dirty"}
        }
        $log.debug("mark accessed:   " + uri);
        privateData.cacheStatus[uri] = new Date().getTime();
    }

    linkedDataService.cacheItemMarkDirty = function(uri){
        var lastAccess = privateData.cacheStatus[uri];
        if (typeof lastAccess === 'undefined') {
            return;
        }
        $log.debug("mark dirty:      " + uri);
        privateData.cacheStatus[uri] = CACHE_DIRTY;
    }

    linkedDataService.cacheItemRemove = function(uri){
        delete privateData.cacheStatus[uri];
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
    linkedDataService.invalidateCacheForNewConnection = function(connectionUri, needUri){
        if (connectionUri != null) {
            linkedDataService.cacheItemMarkDirty(connectionUri);
        }
        return linkedDataService.getNeedConnectionsUri(needUri).then(
            function(connectionsUri){
                if (connectionsUri != null){
                    linkedDataService.cacheItemMarkDirty(connectionsUri);
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
    linkedDataService.invalidateCacheForNewMessage = function(connectionUri){
        if (connectionUri != null) {
            linkedDataService.cacheItemMarkDirty(connectionUri);
        }
        return $q.when(true); //return a promise for chaining
    }
    linkedDataService.invalidateCacheForNeed = function(needUri){
        if (needUri != null) {
            linkedDataService.cacheItemMarkDirty(needUri);
            linkedDataService.cacheItemMarkDirty(needUri+'/connections/')
        }
        return $q.when(true); //return a promise for chaining
    }



    var getReadUpdateLockPerUri = function(uri){
        var lock = privateData.readUpdateLocksPerUri[uri];
        if (typeof lock === 'undefined' || lock == null) {
            lock = new ReadUpdateLock(uri);
            privateData.readUpdateLocksPerUri[uri] = lock;
        }
        return lock;
    }


    /**
     * Checks the query results (success, data) as returned by store.execute or store.node
     * and assuming that we are inside a deferred execution, calls $q.reject if the
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
            $log.error(options.message + " " + errorMessage);
            // TODO: this $q.reject seems to have no effect
            $q.reject(options.message + " " + errorMessage);
            return true;
        }
        return false;
    }


    /**
     * Deletes the node with specified uri from the local triplestore, then
     * fetches the linked data for the specified uri and saves it
     * in the local triplestore.
     * @param uri
     * @return a promise to a boolean which indicates success
     */
    linkedDataService.fetch = function(uri) {
        var tempUri = uri+'?prev='+new Date().getTime();
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "fetch: uri must not be null"};
        }
        $log.debug("fetch announced: " + uri);
        return getReadUpdateLockPerUri(uri)
            .runAsUpdate(
                function() {
                    var deferred = $q.defer();
                    $log.debug("updating:        " + uri);
                    try {
                        $log.debug("deleting :       " + uri);
                        var query = "delete where {<" + uri + "> ?anyP ?anyO}";
                        var failed = {};
                        privateData.store.execute(query, function (success, graph) {
                            if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
                                failed.failed = true;
                                return;
                            }
                            $log.debug("deleted:         " + uri)
                        });
                        if (failed.failed) {
                            return deferred.promise;
                        }
                        //the execute call above is not asynchronous, so we can safely continue outside the callback.
                        $log.debug("fetching:        " + uri);
                        privateData.store.load('remote', uri, function (success, results) {
                            $rootScope.$apply(function () {
                                if (success) {
                                    $log.debug("fetched:         " + uri)
                                    linkedDataService.cacheItemInsertOrOverwrite(uri);
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
                    return deferred.promise;
                }
            );
    }

    /**
     * Fetches the linked data for the specified URI and saves it in the local triplestore.
     * @param uri
     * @return a promise to a boolean which indicates success
     */
    linkedDataService.ensureLoaded = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "ensureLoaded: uri must not be null"};
        }
        $log.debug("ensuring loaded: " +uri);
        if (linkedDataService.cacheItemIsOk(uri)){
            var deferred = $q.defer();
            linkedDataService.cacheItemMarkAccessed(uri);
            deferred.resolve(uri);
            return deferred.promise;
        }
        //uri isn't loaded or needs to be refrehed. fetch it.
        return linkedDataService.fetch(uri);
    }

    /**
     * Saves the specified jsonld structure in the triple store with the specified default graph URI.
     * @param graphURI used if no graph URI is specified in the jsonld
     * @param jsonld the data
     */
    linkedDataService.storeJsonLdGraph = function(graphURI, jsonld) {
        if (typeof graphURI === 'undefined' || graphURI == null  ){
            throw {message : "storeJsonLdGraph: graphURI must not be null"};
        }
        privateData.store.load("application/ld+json", jsonld, graphURI, function (success, results) {});
    }

    /**
     * Loads the default data of the need with the specified URI into a js object.
     * @return the object or null if no data is found for that URI in the local datastore
     */
    linkedDataService.getNeed = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getNeed: uri must not be null"};
        }
       return linkedDataService.ensureLoaded(uri).then(
           function() {
               var lock = getReadUpdateLockPerUri(uri);
               return lock.acquireReadLock().then(
                   function () {
                       try {
                           var resultObject = null;
                           var query =
                               "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                               "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                               "prefix " + "dc" + ":<" + "http://purl.org/dc/elements/1.1/>\n" +
                               "prefix " + "geo" + ":<" + "http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                               "select ?basicNeedType ?title ?tags ?textDescription ?creationDate ?endTime ?recurInfinite ?recursIn ?startTime ?state where { " +
                               //TODO: add as soon as named graphs are handled by the rdf store
                               //
                               //                "<" + uri + ">" + won.WON.hasGraphCompacted + " ?coreURI ."+
                               //                "<" + uri + ">" + won.WON.hasGraphCompacted + " ?metaURI ."+
                               //                "GRAPH ?coreURI {"+,
                               "<" + uri + ">" + won.WON.isInStateCompacted + " ?state ." +
                               "<" + uri + ">" + won.WON.hasBasicNeedTypeCompacted + " ?basicNeedType ." +
                               "<" + uri + ">" + won.WON.hasContentCompacted + " ?content ." +
                               "?content dc:title ?title ." +
                               "OPTIONAL {?content " + won.WON.hasTagCompacted + " ?tags .}" +
                               "OPTIONAL {?content " + "geo:latitude" + " ?latitude .}" +
                               "OPTIONAL {?content " + "geo:longitude" + " ?longitude .}" +
                               "OPTIONAL {?content " + won.WON.hasEndTimeCompacted + " ?endTime .}" +
                               "OPTIONAL {?content " + won.WON.hasRecurInfiniteTimesCompacted + " ?recurInfinite .}" +
                               "OPTIONAL {?content " + won.WON.hasRecursInCompacted + " ?recursIn .}" +
                               "OPTIONAL {?content " + won.WON.hasStartTimeCompacted + " ?startTime .}" +
                               "OPTIONAL {?content " + won.WON.hasTagCompacted + " ?tags .}" +
                               "OPTIONAL {?content " + won.WON.hasTextDescriptionCompacted + " ?textDescription ." +
                               //TODO: add as soon as named graphs are handled by the rdf store
                               //                "}" +
                               //                "GRAPH ?metaURI {" +
                               "<" + uri + ">" + " <" + "http://purl.org/dc/terms/created" + "> " + "?creationDate ." +
                               "<" + uri + ">" + won.WON.hasConnectionsCompacted + " ?connections ." +
                               "<" + uri + ">" + won.WON.hasWonNodeCompacted + " ?wonNode ." +
                               "<" + uri + ">" + won.WON.isInStateCompacted + " ?state ." +
                               "OPTIONAL {<" + uri + "> " + won.WON.hasEventContainerCompacted + " ?eventContainer .}" +
                               "OPTIONAL {?eventContainer " + "rdfs:member" + " ?event .}" +
                               //TODO: add as soon as named graphs are handled by the rdf store
                               //                "}" +
                               "}}";
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
                           return $q.reject("could not load need " + uri + ". Reason: " + e);
                       } finally {
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
    linkedDataService.getUniqueObjectOfProperty = function(resourceURI, propertyURI){
        if (typeof resourceURI === 'undefined' || resourceURI == null  ){
            throw {message : "getUniqueObjectOfProperty: resourceURI must not be null"};
        }
        if (typeof propertyURI === 'undefined' || propertyURI == null  ){
            throw {message : "getUniqueObjectOfProperty: propertyURI must not be null"};
        }
        return linkedDataService.ensureLoaded(resourceURI).then(
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
                            return $q.reject("could not load object of property " + propertyURI + " of resource " + resourceURI + ". Reason: " + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                        return $q.reject("could not load object of property " + propertyURI + " of resource " + resourceURI);
                    }
                );
            })
    }

    linkedDataService.getWonNodeUriOfNeed = function(needUri){
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getWonNodeUriOfNeed: needUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(needUri, won.WON.hasWonNode)
            .then(
                function(result){return result;},
                function(reason) { return $q.reject("could not get WonNodeUri of Need " + needUri + ". Reason: " + reason)});
    }

    linkedDataService.getNeedUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getNeedUriOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(connectionUri, won.WON.belongsToNeed)
            .then(
                function(result) {
                    return result;
                },
                function(reason) {
                    return $q.reject("could not get need uri of connection " + connectionUri + ". Reason: " + reason)
                });
    }

    linkedDataService.getRemoteConnectionUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getRemoteConnectionUriOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(connectionUri, won.WON.hasRemoteConnection)
            .then(
                function(result){return result;},
                function(reason) { return $q.reject("could not get remote connection uri of connection " + connectionUri + ". Reason: " + reason)});
    }

    linkedDataService.getRemoteneedUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getRemoteneedUriOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(connectionUri, won.WON.hasRemoteNeed)
            .then(
                function(result){return result;},
                function(reason) { return $q.reject("could not get remote need uri of connection " + connectionUri + ". Reason: " + reason)});
    }

    linkedDataService.getEnvelopeDataForNeed=function(needUri){
        if(typeof needUri === 'undefined'||needUri == null){
            throw {message: "getEnvelopeDataForNeed: needUri must not be null"};

        }
        return linkedDataService.getWonNodeUriOfNeed(needUri)
            .then(function(wonNodeUri){
                var ret = {};
                ret[won.WONMSG.hasSender] = needUri;
                ret[won.WONMSG.hasSenderNeed] = needUri;
                ret[won.WONMSG.hasSenderNode] = wonNodeUri;
                ret[won.WONMSG.hasReceiverNeed] = needUri;
                ret[won.WONMSG.hasReceiverNode] = wonNodeUri;
                return ret;

            },function(reason) {
                //no connection found
                var deferred = $q.defer();
                var ret = {};
                ret[won.WONMSG.hasSender] = needUri;
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
    linkedDataService.getEnvelopeDataforConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getEnvelopeDataforConnection: connectionUri must not be null"};
        }
        return linkedDataService.getNeedUriOfConnection(connectionUri)
            .then(function(needUri) {
                return linkedDataService.getWonNodeUriOfNeed(needUri)
                    .then(function (wonNodeUri) {
                        return linkedDataService.getRemoteneedUriOfConnection(connectionUri)
                            .then(function(remoteneedUri){
                                return linkedDataService.getWonNodeUriOfNeed(remoteneedUri)
                                    .then(function(remoteWonNodeUri){
                                        //if the local connection was created through a hint message (most likely)
                                        //the remote connection is not known or doesn't exist yet. Hence, the next call
                                        //may or may not succeed.
                                        return linkedDataService.getRemoteConnectionUriOfConnection(connectionUri).then(
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
                                                var deferred = $q.defer();
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


    linkedDataService.getLastEventOfEachConnectionOfNeed = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getLastEventOfEachConnectionOfNeed: uri must not be null"};
        }
        return linkedDataService.getconnectionUrisOfNeed(uri)
            .then(function(conUris) {
                try {
                    var promises = [];
                    for (var conKey in conUris) {
                        promises.push(linkedDataService.getLastEventOfConnection(conUris[conKey]));
                    }
                    return somePromises(promises, function(key, reason){
                        won.reportError("could not fetch last event of connection " + conUris[key], reason);
                    }).then(function(val) {
                        return won.deleteWhereNull(val)
                    });
                } catch (e) {
                    return $q.reject("could not get last event of connection " + uri + ". Reason: " + e);
                }
            }
        );
    }



    linkedDataService.getLastEventOfConnection = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastEventOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getConnection(connectionUri)
            .then(function (connection) {
                return linkedDataService.getNeed(connection.hasRemoteNeed)
                    .then(function (need) {
                        return linkedDataService.getLastConnectionEvent(connectionUri)
                            .then(
                                function (event) {
                                    return {connection: connection, remoteNeed: need, event: event}
                                },function(reason){
                                    //remote need's won node may be offline - don't let that kill us
                                    var deferred = $q.defer();
                                    deferred.resolve(
                                        {connection: connection, remoteNeed: {'title': '[could not load]'}, event: event}
                                    );
                                    return deferred.promise;
                                });
                    });
            });
    }


    linkedDataService.getAllConnectionEvents = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getAllConnectionEvents: connectionUri must not be null"};
        }
        return linkedDataService.getAllConnectioneventUris(connectionUri)
            .then(function (eventUris) {
                try {
                    var eventPromises = [];
                    for (var evtKey in eventUris) {
                        eventPromises.push(linkedDataService.getConnectionEvent(eventUris[evtKey]));
                    }
                    return $q.all(eventPromises)
                } catch (e) {
                    return $q.reject("could not get all connection events for connection " + connectionUri + ". Reason: " + e);
                }
            });
    }

    linkedDataService.getLastConnectionEvent = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastConnectionEvent: connectionUri must not be null"};
        }
        return linkedDataService.getLastConnectioneventUri(connectionUri)
            .then(function (eventUri) {
                    return linkedDataService.getConnectionEvent(eventUri);
            })
    }


    /**
     * Loads all URIs of a need's connections.
     */
    linkedDataService.getconnectionUrisOfNeed = function(uri) {
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getconnectionUrisOfNeed: uri must not be null"};
        }
        return linkedDataService.ensureLoaded(uri).then(
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
                                        connectionsPromises.push(linkedDataService.ensureLoaded(connectionsURI).then(function (success) {
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
                            return $q.all(connectionsPromises)
                                .then(function (listOfLists) {
                                    //for each hasConnections triple (should only be one, but hey) we get a list of connections.
                                    //now flatten the list.
                                    var merged = [];
                                    merged = merged.concat.apply(merged, listOfLists);
                                    return merged;
                                });
                        } catch (e) {
                            $q.reject("could not get connection URIs of need + " + uri + ". Reason:" + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                    }
                )
            });

    }

    linkedDataService.getNeedConnectionsUri = function(needUri) {
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getConnectionsUri: needUri must not be null"};
        }
        return linkedDataService.ensureLoaded(needUri).then(
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
                            return $q.reject("could not get connection URIs of need + " + uri + ". Reason:" + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                    }
                )
            });
    }
    
    

    linkedDataService.getConnection = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getConnection: connectionUri must not be null"};
        }
        return linkedDataService.getNodeWithAttributes(connectionUri);
    }

    linkedDataService.getConnectionEvent = function(eventUri) {
        if (typeof eventUri === 'undefined' || eventUri == null  ){
            throw {message : "getConnectionEvent: eventUri must not be null"};
        }
        return linkedDataService.getNodeWithAttributes(eventUri);
    }



    linkedDataService.getAllConnectioneventUris = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getAllConnectioneventUris: connectionUri must not be null"};
        }
        return linkedDataService.ensureLoaded(connectionUri).then(
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
                           return $q.reject("Could not get all connection event URIs for connection " + connectionUri +". Reason: " + e);
                       } finally {
                           lock.releaseReadLock();
                       }
                   }
               );
            });
    }

    linkedDataService.crawlConnectionData = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "crawlConnectionData: connectionUri must not be null"};
        }
        return linkedDataService.ensureLoaded(connectionUri).then(
            function(){
                return linkedDataService.getAllConnectioneventUris(connectionUri).then(
                    function(uris){
                        var eventPromises = [];
                        for (key in uris){
                            eventPromises.push(linkedDataService.ensureLoaded(uris[key]));
                        }
                        return $q.all(eventPromises);
                    }
                );
            }
        );

    }

    linkedDataService.getLastConnectioneventUri = function(connectionUri) {
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getLastConnectioneventUri: connectionUri must not be null"};
        }
        return linkedDataService.crawlConnectionData(connectionUri).then(
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
                                "  ?eventUri msg:hasTimestamp ?timestamp; \n" +
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
                            return $q.reject("Could not get last connection event URI for connection " + connectionUri + ". Reason: " + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                    })
            }
        );

    }

    linkedDataService.getConnectionTextMessages = function(connectionUri) {
        return linkedDataService.crawlConnectionData(connectionUri).then(
            function collectTextMessages() {
                var lock = getReadUpdateLockPerUri(connectionUri);
                return lock.acquireReadLock().then(
                    function (success) {
                        try {
                            var textMessages = [];
                            var query =
                                "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                                "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                                "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                                    //note: we have to take the max timestamp as there might be multiple timestamps added to the
                                    //message dataset during processing
                                "select ?eventUri (max(?tmstmp) AS ?timestamp) ?text ?senderNeed where { " +
                                "<" + connectionUri + "> a " + won.WON.ConnectionCompacted + ";\n" +
                                won.WON.hasEventContainerCompacted + " ?container.\n" +
                                "?container rdfs:member ?eventUri. \n" +
                                "?eventUri won:hasTextMessage ?text; \n" +
                                "          msg:hasMessageType ?messageType; \n" +
                                "          msg:hasSenderNeed ?senderNeed. \n" +
                                " optional { " +
                                "  ?eventUri msg:hasTimestamp ?tmstmp .\n" +
                                " } \n" +
                                //filter added so we don't show the success/failure events as last events
                                " filter (?messageType != msg:SuccessResponse && ?messageType != msg:FailureResponse)" +
                                "} group by ?eventUri ?text ?senderNeed order by asc(?timestamp) ";


                            privateData.store.execute(query, [], [], function (success, results) {
                                if (rejectIfFailed(success, results, {message: "Error loading last connection event URI for connection " + connectionUri + ".", allowNone: true, allowMultiple: true})) {
                                    return;
                                }
                                for (var key in results) {
                                    var textMessage = {};
                                    var eventUri = getSafeValue(results[key].eventUri);
                                    var timestamp = getSafeValue(results[key].timestamp);
                                    var text = getSafeValue(results[key].text);
                                    var senderNeed = getSafeValue(results[key].senderNeed);
                                    if (eventUri != null && timestamp != null && text != null) {
                                        textMessage.eventUri = eventUri;
                                        textMessage.timestamp = timestamp;
                                        textMessage.text = text;
                                        textMessage.senderNeed = senderNeed;
                                        textMessages.push(textMessage);
                                    }
                                }
                            });
                            return textMessages;
                        } catch (e) {
                            return $q.reject("Could not get connection events' text messages " + connectionUri + ". Reason: " + e);
                        } finally {
                            lock.releaseReadLock();
                        }
                })
             }
        );
    }

    linkedDataService.getLastEventTypeBeforeTime = function(connectionUri, beforeTimestamp) {
        return linkedDataService.crawlConnectionData(connectionUri).then(
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
                            return $q.reject("Could not get connection event type before time " + connectionUri + ". Reason: " + e);
                        } finally {
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
    linkedDataService.getNodeWithAttributes = function(uri){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "getNodeWithAttributes: uri must not be null"};
        }
        return linkedDataService.ensureLoaded(uri).then(
            function(){
                var lock = getReadUpdateLockPerUri(uri);
                return lock.acquireReadLock().then(
                    function() {
                        $log.debug("getNodeWithAttrs:" + uri);
                        try {
                            var node = {};
                            privateData.store.node(uri, function (success, graph) {
                                if (graph.length == 0) {
                                    $log.error("warn: could not load any attributes for node with uri: " + uri);
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
                            return $q.reject("could not get node " + uri + "with attributes: " + e);
                        } finally {
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
    linkedDataService.deleteNode = function(uri){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "deleteNode: uri must not be null"};
        }
        $log.debug("deleting node:   " + uri);
        var deferred = $q.defer();
        var query = "delete where {<"+uri+"> ?anyP ?anyO}";
        //var query = "select ?anyO where {<"+uri+"> ?anyP ?anyO}";
        privateData.store.execute(query, function (success, graph) {
            if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
                return;
            } else {
                linkedDataService.cacheItemRemove(uri);
                deferred.resolve();
            }
        });
        return deferred.promise;
    }
    
    

    /**
     * Loads the default data of the need with the specified URI into a js object.
     * @return the object or null if no data is found for that URI in the local datastore
     */
    linkedDataService.getMessage = function(uri) {
        //TODO: SPARQL query that returns the common message properties
    }

    /**
     * Loads the hints for the need with the specified URI into an array of js objects.
     * @return the array or null if no data is found for that URI in the local datastore
     */
    linkedDataService.getHintsForNeed = function(uri) {
        //TODO: SPARQL query that returns an array of hints
    }

    /**
     * Loads the connections for the need with the specified URI into an array of js objects.
     * @return the array or null if no data is found for that URI in the local datastore
     */
    linkedDataService.getConnections = function(uri) {
        //TODO: SPARQL query that returns an array of connections
    }


    return linkedDataService;

});
