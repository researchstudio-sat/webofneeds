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
        return privateData.deferredsForUrisBeingFetched[uri] != null;
    }

    var addDeferredForUriBeingFetched = function(deferred, uri){
        var deferreds = privateData.deferredsForUrisBeingFetched[uri]
        if (deferreds == null){
            privateData.deferredsForUrisBeingFetched[uri] = [];
            deferreds = privateData.deferredsForUrisBeingFetched[uri];
        }
        deferreds.push(deferred);
    }
    
    var resolveDeferredsForUrisBeingFetched = function(uri, value){
        var deferreds = privateData.deferredsForUrisBeingFetched[uri]
        if (deferreds != null) {
            for (key in deferreds){
                deferreds[key].resolve(value);
            }
        }
        delete privateData.deferredsForUrisBeingFetched[uri];
    }

    var rejectDeferredsForUrisBeingFetched = function(uri, value){
        var deferreds = privateData.deferredsForUrisBeingFetched[uri]
        if (deferreds != null) {
            for (key in deferreds){
                deferreds[key].reject("failed to load uri: " + uri +". Reason: "+ value);
            }
        }
        delete privateData.deferredsForUrisBeingFetched[uri];
    }



    /**
     * Fetches the linked data for the specified URI and saves it in the local triplestore.
     * @param uri
     * @return a promise to a boolean which indicates success
     */
    linkedDataService.fetch = function(uri) {
        var deferred = $q.defer();
        //check if this is the first requrest
        var first = ! isBeingFetched(uri);
        //add the deferred to the list of deferreds for the uri
        addDeferredForUriBeingFetched(deferred, uri);
        if (first) {
            //actually do load data
            try {
                console.log("fetching linked data: " + uri);
                privateData.store.load('remote', uri, function (success, results) {
                    $rootScope.$apply(function() {
                        if (success) {
                            resolveDeferredsForUrisBeingFetched(uri, success);
                        } else {
                            rejectDeferredsForUrisBeingFetched(uri, "failed to load " + uri);
                        }
                    });
                });
            } catch (e) {
                $rootScope.$apply(function() {
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
        var deferred = $q.defer();
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
        privateData.store.load("application/ld+json", jsonld, graphURI, function (success, results) {});
    }

    /**
     * Loads the default data of the need with the specified URI into a js object.
     * @return the object or null if no data is found for that URI in the local datastore
     */
    linkedDataService.getNeed = function(uri) {
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

                    if (!success) {
                        $q.reject("query for need " + uri + " failed.");
                        return;
                    }
                    //use only first result!
                    if (results.length == 0) {
                        $q.reject("query for need " + uri + " returned no results");
                        return;
                    }
                    if (results.length > 1) {
                        $q.reject("query for need " + uri + " returned more than one result");
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

    linkedDataService.getWonNodeOfNeed = function(needUri){
        return linkedDataService.ensureLoaded(needUri).then(
          function(){
            try{
                resultData = {};
                privateData.store.node(needUri, function(success, graph){
                    if (!success) {
                        $q.reject("failed to load node " + needUri);
                        return;
                    }
                    if (graph.triples.length == 0){
                        $q.reject("loading node " + needUri + " returned an empty graph");
                        return;
                    }
                    var results = graph.match(needUri, won.WON.hasWonNode, null);
                    if (results.length == 0){
                        $q.reject("loading won:hasWonNode triples " + needUri + " returned no triples");
                        return;
                    }
                    if (results.length > 1){
                        $q.reject("loading won:hasWonNode triples " + needUri + " returned more than 1 triple: " + JSON.stringify(results.triples));
                        return;
                    }
                    resultData.wonNodeUri = results.triples[0].object.nominalValue;
                });
                return resultData.wonNodeUri;
            } catch (e){
                $q.reject("could not get WoN node for need " + needUri + ". Reason: " + e);
            }
            $q.reject("unable to load WoN node for need " + needUri);
          }
        );
    }


    linkedDataService.getLastEventOfEachConnectionOfNeed = function(uri) {
        return linkedDataService.getConnectionURIsOfNeed(uri)
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

    linkedDataService.getLastEventOfConnection = function(connectionUri) {
        return linkedDataService.getConnection(connectionUri)
            .then(function (connection) {
                return linkedDataService.getNeed(connection.hasRemoteNeed)
                    .then(function (need) {
                        return linkedDataService.getLastConnectionEvent(connectionUri)
                            .then(function (event) {
                                return {connection: connection, remoteNeed: need, event: event}
                            });
                    });
            });
    }

    linkedDataService.getAllConnectionEvents = function(connectionUri) {
        return linkedDataService.getConnectionEventUris(connectionUri)
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
        return linkedDataService.getLastConnectionEventUri(connectionUri)
            .then(function (eventUri) {
                    return linkedDataService.getConnectionEvent(eventUri);
            })
    }


    /**
     * Loads all URIs of a need's connections.
     */
    linkedDataService.getConnectionURIsOfNeed = function(uri) {
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
                            connectionsPromises.push(linkedDataService.ensureLoaded(connectionsURI).then(function (success) {
                                var connectionURIs = [];
                                privateData.store.node(connectionsURI, function (success, graph) {
                                    if (graph != null && graph.length > 0) {
                                        var memberTriples = graph.match(connectionsURI, createNameNodeInStore("rdfs:member"), null);
                                        for (var memberKey in memberTriples.triples) {
                                            var member = memberTriples.triples[memberKey].object.nominalValue;
                                            connectionURIs.push(member);
                                        }
                                    }
                                });
                                return connectionURIs;
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
        return linkedDataService.getNodeWithAttributes(connectionUri);
    }

    linkedDataService.getConnectionEvent = function(eventUri) {
        return linkedDataService.getNodeWithAttributes(eventUri);
    }

    linkedDataService.getAllConnectionEventUris = function(connectionURI) {
        return linkedDataService.ensureLoaded(connectionURI).then(function(success) {
            try {
                var eventURIs = [];
                var query =
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "select ?eventURI where { " +
                    "<" + connectionURI + "> a " + won.WON.ConnectionCompacted + ";\n" +
                    won.WON.hasEventContainerCompacted + " ?container.\n" +
                    "?container rdfs:member ?eventURI. \n" +
                    "}";
                privateData.store.execute(query, [], [], function (success, results) {
                    if (success) {
                        for (var key in results) {
                            var eventURI = getSafeValue(results[key].eventURI);
                            if (eventURI != null) {
                                eventURIs.push(eventURI);
                            }
                        }
                    }
                });
                return eventURIs;
            } catch (e) {
                $q.reject("Could not get all connection event URIs for connection " + connectionURI +". Reason: " + e);
            }
        });
    }

    linkedDataService.getLastConnectionEventUri = function(connectionURI) {
        return linkedDataService.ensureLoaded(connectionURI).then(function(success) {
            try {
                var resultObject = {};
                //TODO: use event with highest timestamp
                var query =
                    "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                    "prefix " + won.WON.prefix + ": <" + won.WON.baseUri + "> \n" +
                    "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "select ?eventURI where { " +
                    "<" + connectionURI + "> a " + won.WON.ConnectionCompacted + ";\n" +
                    won.WON.hasEventContainerCompacted + " ?container.\n" +
                    "?container rdfs:member ?eventURI. \n" +
                    "} limit 1";
                privateData.store.execute(query, [], [], function (success, results) {
                    if (success) {
                        for (var key in results) {
                            var eventURI = getSafeValue(results[key].eventURI);
                            if (eventURI != null) {
                                resultObject.eventURI = eventURI;
                                return;
                            }
                        }
                    }
                });
                return resultObject.eventURI;
            } catch (e) {
                $q.reject("could not get last connection event URI for connection " + connectionURI +". Reason: " + e);
            }
        });
    }

    /**
     * Fetches the triples where URI is subject and add objects of those triples to the
     * resulting structure by the localname of the predicate.
     * The URI is added as property 'uri'.
     * @param eventURI
     */
    linkedDataService.getNodeWithAttributes = function(uri){
        return linkedDataService.ensureLoaded(uri).then(function(success) {
            try {
                var node = {};
                privateData.store.node(uri, function (success, graph) {
                    if (graph != null && graph.length > 0) {
                        for (key in graph.triples) {
                            var propName = won.getLocalName(graph.triples[key].predicate.nominalValue);
                            node[propName] = graph.triples[key].object.nominalValue;
                        }
                    }
                });
                node.uri = uri;
                return node;
            } catch (e) {
                $q.reject("could not get node " + uri + "with attributes: " + e);
            }
        });
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
