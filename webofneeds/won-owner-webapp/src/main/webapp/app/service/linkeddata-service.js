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

angular.module('won.owner').factory('linkedDataService', function ($q, $rootScope) {
    linkedDataService = {};

    var privateData = {};

    //create an rdfstore-js based store as a cache for rdf data.
    privateData.store =  rdfstore.create();
    privateData.store.setPrefix("msg","http://purl.org/webofneeds/message#");
    privateData.store.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    privateData.store.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    privateData.store.setPrefix("xsd","http://www.w3.org/2001/XMLSchema#");
    privateData.store.setPrefix("won","http://purl.org/webofneeds/model#");


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
            counter = 0,
            results = angular.isArray(promises) ? [] : {},
            handler = typeof errorHandler === 'function' ? errorHandler : function(x,y){};

        angular.forEach(promises, function(promise, key) {
            counter++;
            promise.then(function(value) {
                if (results.hasOwnProperty(key)) return;
                results[key] = value;
                if (!(--counter)) deferred.resolve(results);
            }, function(reason) {
                console.log("warning: promise failed. Reason " + JSON.stringify(reason));
                if (results.hasOwnProperty(key)) return;
                results[key] = null;
                handler(key, reason);
                if (!(--counter)) deferred.reject("all promises failed");
            });
        });

        if (counter === 0) {
            deferred.resolve(results);
        }

        return deferred.promise;
    }

    privateData.deferredsForUrisBeingFetched = {}; //uri -> list of promise

    var isBeingFetched = function(uri){
        return typeof privateData.deferredsForUrisBeingFetched[uri] !== 'undefined' && privateData.deferredsForUrisBeingFetched[uri] != null;
    }

    var addDeferredForUriBeingFetched = function(deferred, uri){
        var deferreds = privateData.deferredsForUrisBeingFetched[uri]
        if (typeof deferreds === 'undefined' || deferreds == null){
            privateData.deferredsForUrisBeingFetched[uri] = [];
            deferreds = privateData.deferredsForUrisBeingFetched[uri];
            console.log("new fetch:       " + uri);
        } else {
            console.log("already fetching: " + uri);
        }
        deferreds.push(deferred);
    }
    
    var resolveDeferredsForUrisBeingFetched = function(uri, value){
        var deferreds = privateData.deferredsForUrisBeingFetched[uri]
        if (deferreds != null) {
            for (key in deferreds){
                console.log("delivering reslt:" + uri);
                deferreds[key].resolve(value);
            }
        }
        delete privateData.deferredsForUrisBeingFetched[uri];
    }

    var rejectDeferredsForUrisBeingFetched = function(uri, value){
        var deferreds = privateData.deferredsForUrisBeingFetched[uri]
        if (typeof deferreds !== 'undefined' && deferreds != null) {
            for (key in deferreds){
                deferreds[key].reject("failed to load uri: " + uri +". Reason: "+ value);
            }
        }
        delete privateData.deferredsForUrisBeingFetched[uri];
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
            errorMessage = "Query failed.";
        } else if (typeof options.allowNone !== undefined  && options.allowNone == false && data.length == 0){
            errorMessage = "No results found.";
        } else if (typeof options.allowMultiple !== undefined  && options.allowMultiple == false && data.length > 1){
            errorMessage = "More than one result found.";
        }
        if (errorMessage != null){
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
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "fetch: uri must not be null"};
        }
        console.log("fetch starts:    " + uri);
        var deferred = $q.defer();
        //check if this is the first requrest
        var first = ! isBeingFetched(uri);
        //add the deferred to the list of deferreds for the uri
        addDeferredForUriBeingFetched(deferred, uri);
        if (first) {
            //actually do load data
            try {
                console.log("deleting :       " + uri);
                var query = "delete where {<"+uri+"> ?anyP ?anyO}";
                privateData.store.execute(query, function (success, graph) {

                    if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
                        return;
                    }
                    console.log("deleted:         " + uri)
                });
                //the execute call above is not asynchronous, so we can safely continue outside the callback.
                console.log("fetching:        " + uri);
                privateData.store.load('remote', uri, function (success, results) {
                    $rootScope.$apply(function () {
                        if (success) {
                            console.log("fetched:         " + uri)
                            resolveDeferredsForUrisBeingFetched(uri, success);
                        } else {
                            rejectDeferredsForUrisBeingFetched(uri, "failed to load " + uri);
                        }
                    });
                });
            } catch (e) {
                $rootScope.$apply(function () {
                    rejectDeferredsForUrisBeingFetched(uri, e);
                });
            }
        }
        return deferred.promise;
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
        console.log("ensuring loaded: " +uri);

        var deferred = $q.defer();
        if (isBeingFetched(uri)){
            //The uri is being fetched. Add the deferred object to the list of deferred objects that
            //will be resolved/rejected in response to fetching the uri
            addDeferredForUriBeingFetched(deferred, uri);
            return deferred;
        }
        // The uri is not being fetched. Check if it exists as subject in the store, if not, fetch it.
        privateData.store.node(uri, function (success, mygraph) {
            if (success && mygraph.triples.length > 0) {
                deferred.resolve(true);
            } else {
                deferred.resolve(false);
            }
        });
        return deferred.promise.then(
            function(isAlreadyLoaded){
                if (isAlreadyLoaded) {
                    return true;
                } else {
                    return linkedDataService.fetch(uri);
                }
            }
        );
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
       return linkedDataService.ensureLoaded(uri).then(function(){
            try {
                var resultObject = null;
                var query =
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                    "prefix " + "dc" + ":<" + "http://purl.org/dc/elements/1.1/>\n" +
                    "prefix " + "geo" + ":<" + "http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                    "select ?basicNeedType ?title ?tags ?textDescription ?creationDate ?endTime ?recurInfinite ?recursIn ?startTime where { " +
                    //TODO: add as soon as named graphs are handled by the rdf store
                    //
                    //                "<" + uri + ">" + won.WON.hasGraphCompacted + " ?coreURI ."+
                    //                "<" + uri + ">" + won.WON.hasGraphCompacted + " ?metaURI ."+
                    //                "GRAPH ?coreURI {"+
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
                    if (rejectIfFailed(success, results,{message : "Could not load need " + uri +".", allowNone : false, allowMultiple: false})){
                        return;
                    }
                    var result = results[0];
                    resultObject.uri = uri;
                    resultObject.basicNeedType = getSafeValue(result.basicNeedType);
                    resultObject.title = getSafeValue(result.title);
                    resultObject.tags = getSafeValue(result.tags);
                    resultObject.textDescription = getSafeValue(result.textDescription);
                    resultObject.creationDate = getSafeValue(result.creationDate);
                });
                return resultObject;
            } catch (e){
                $q.reject("could not load need " + uri + ". Reason: " + e);
            }
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
        return linkedDataService.ensureLoaded(resourceURI)
            .then(function () {
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
                    $q.reject("could not load object of property " + propertyURI + " of resource " + resourceURI + ". Reason: " + e);
                }
                $q.reject("could not load object of property " + propertyURI + " of resource " + resourceURI);
            });
    }

    linkedDataService.getWonNodeUriOfNeed = function(needUri){
        if (typeof needUri === 'undefined' || needUri == null  ){
            throw {message : "getWonNodeUriOfNeed: needUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(needUri, won.WON.hasWonNode)
            .then(
                function(result){return result;},
                function(reason) { $q.reject("could not get WonNodeUri of Need " + needUri + ". Reason: " + reason)});
    }

    linkedDataService.getneedUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getneedUriOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(connectionUri, won.WON.belongsToNeed)
            .then(
                function(result){return result;},
                function(reason) { $q.reject("could not get need uri of connection " + connectionUri + ". Reason: " + reason)});
    }

    linkedDataService.getRemoteconnectionUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getRemoteconnectionUriOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(connectionUri, won.WON.hasRemoteConnection)
            .then(
                function(result){return result;},
                function(reason) { $q.reject("could not get remote connection uri of connection " + connectionUri + ". Reason: " + reason)});
    }

    linkedDataService.getRemoteneedUriOfConnection = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "getRemoteneedUriOfConnection: connectionUri must not be null"};
        }
        return linkedDataService.getUniqueObjectOfProperty(connectionUri, won.WON.hasRemoteNeed)
            .then(
                function(result){return result;},
                function(reason) { $q.reject("could not get remote need uri of connection " + connectionUri + ". Reason: " + reason)});
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
        return linkedDataService.getneedUriOfConnection(connectionUri)
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
                                        return linkedDataService.getRemoteconnectionUriOfConnection(connectionUri).then(
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
                    }).then(function(val) { return won.deleteWhereNull(val)});
                } catch (e) {
                    $q.reject("could not get last event of connection " + uri + ". Reason: " + e);
                }
            }
        );
    }

    /**
     * Finds the resource that denotes the connection's event container and fetches its
     * content. Used for refreshing information about connection events.
     * TODO: improve so as to load only new events
     * TODO: currently this does not work as the connection event container is a blank node.
     * --> we have to fetch the whole connection
     *
     * @param connectionUri
     * @returns {*}
     */
    linkedDataService.refreshConnectionEventContainer = function (connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "refreshConnectionEventContainer: connectionUri must not be null"};
        }
        return linkedDataService.ensureLoaded(connectionUri).then(function(success) {
            try {
                var subject = connectionUri;
                var predicate = won.WON.hasEventContainer;
                privateData.store.node(connectionUri, function (success, graph) {
                    var resultGraph = graph.match(subject, predicate, null);
                    if (resultGraph != null && resultGraph.length > 0) {
                        for (key in resultGraph.triples) {
                            var containerURI = resultGraph.triples[key].object.nominalValue;
                            //TODO: here, we fetch, but if we knew that the connections container didn't change
                            //we could just ensureLoaded. See https://github.com/researchstudio-sat/webofneeds/issues/109
                            return linkedDataService.fetch(containerURI);
                        }
                    }
                });
            } catch (e) {
                $q.reject("could not refresh connection event container for connection " + connectionUri +". Reason:", e);
            }
        });
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
                    $q.reject("could not get all connection events for connection " + connectionUri + ". Reason: " + e);
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
        return linkedDataService.ensureLoaded(uri).then(function(success) {
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
                            connectionsPromises.push(linkedDataService.fetch(connectionsURI).then(function (success) {
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
            }
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
        return linkedDataService.fetch(connectionUri).then(function(success) {
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
                $q.reject("Could not get all connection event URIs for connection " + connectionUri +". Reason: " + e);
            }
        });
    }

    linkedDataService.crawlConnectionData = function(connectionUri){
        if (typeof connectionUri === 'undefined' || connectionUri == null  ){
            throw {message : "crawlConnectionData: connectionUri must not be null"};
        }
        return linkedDataService.fetch(connectionUri).then(
            function(){
                return linkedDataService.getAllConnectioneventUris(connectionUri).then(
                    function(uris){
                        var eventPromises = [];
                        for (key in uris){
                            eventPromises.push(linkedDataService.fetch(uris[key]));
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
        return linkedDataService.crawlConnectionData(connectionUri).then(function(success) {

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
                    "  ?eventUri msg:hasTimestamp ?timestamp .\n" +
                    " } \n" +
                    "} order by desc(?timestamp) limit 1";
                privateData.store.execute(query, [], [], function (success, results) {
                    if (rejectIfFailed(success, results,{message : "Error loading last connection event URI for connection " + connectionUri +".", allowNone : false, allowMultiple: false})){
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
                $q.reject("Could not get last connection event URI for connection " + connectionUri +". Reason: " + e);
            }
        });
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
        return linkedDataService.ensureLoaded(uri).then(function(success) {
            console.log("getNodeWithAttrs:" + uri);
            try {
                var node = {};
                privateData.store.node(uri, function (success, graph) {
                    if (rejectIfFailed(success, graph,{message : "Error loading node with attributes for URI " + uri+".", allowNone : false, allowMultiple: true})){
                        return;
                    }
                    for (key in graph.triples) {
                        var propName = won.getLocalName(graph.triples[key].predicate.nominalValue);
                        node[propName] = graph.triples[key].object.nominalValue;
                    }
                });
                if (node.length == 0){
                    console.log("warn: could not load any attributes for uri: " + uri);
                }
                node.uri = uri;
                return node;
            } catch (e) {
                $q.reject("could not get node " + uri + "with attributes: " + e);
            }
        });
    }

    /**
     * Deletes all triples where the specified uri is the subect.
     */
    linkedDataService.deleteNode = function(uri){
        if (typeof uri === 'undefined' || uri == null  ){
            throw {message : "deleteNode: uri must not be null"};
        }
        console.log("deleting node:   " + uri);
        var deferred = $q.defer();
        var query = "delete where {<"+uri+"> ?anyP ?anyO}";
        privateData.store.execute(query, function (success, graph) {
            if (rejectIfFailed(success, graph, {message: "Error deleting node with URI " + uri + "."})) {
                return;
            } else {
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
