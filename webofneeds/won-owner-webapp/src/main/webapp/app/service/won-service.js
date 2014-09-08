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
 * Created by syim on 08.08.2014.
 */
angular.module('won.owner').factory('wonService', function (messageService, $q, linkedDataService, $rootScope) {

    wonService = {};

    /**
     * helper function for accessin JSON-LD data
     */
    matchFirstObject = function(jsonld, subject, predicate){
        var deferred = $q.defer();
        console.log("obtaining data from " + jsonld);
        var ret = {};
        ret.result = null;
        var store = rdfstore.create();
        store.setPrefix("ex", "http://example.org/people/");
        store.setPrefix("wonmsg","http://purl.org/webofneeds/message#");
        store.load("application/ld+json", jsonld, "ex:test", function(success, results) {
            if (!success) {
                deferred.reject("could not parse jsonld " + jsonld);
            }
            console.log("success:" + success + ", results: " + results);
            var sub = subject == null ? null:store.rdf.createNamedNode(store.rdf.resolve(subject));
            var pred = predicate == null ? null:store.rdf.createNamedNode(store.rdf.resolve(predicate));
            store.graph("ex:test", function(success, mygraph) {
                if (!success) {
                    deferred.reject("could not match subject " + subject + " predicate " + predicate);
                }
                var resultGraph = mygraph.match(sub, pred, null);
                if (resultGraph != null && resultGraph.triples.length > 0) {
                    deferred.resolve(resultGraph.triples[0].object.nominalValue);
                }
            });
        });
        return deferred.promise;
    };


    /**
     * Creates a need and returns a Promise to the URI of the newly created need (which may differ from the one
     * specified in the need object here.
     * @param needAsJsonLd
     * @returns {*}
     */
    wonService.createNeed = function(needAsJsonLd) {
        var deferred = $q.defer();
        var message = new won.CreateMessageBuilder(needAsJsonLd)
            .addMessageGraph()
            .eventURI("34543242134")//TODO: generate event URI here
            .hasSenderNeed()
            .hasReceiverNode("http://localhost:8080/won")//TODO: pass node to function
            .build();
        //TODO: obtain message URI so we can wait for a dedicated response
        var callback = new messageService.MessageCallback(
            function (event, msg) {
                //check if the message we got (the create need response message) indicates that all went well
                console.log("got create need message response!");
                //TODO: if negative, use alternative need URI and send again
                //TODO: if positive, propagate positive response back to caller
                //fetch need data and store in local RDF store
                //get URI of newly created need from message
                needURIPromise = matchFirstObject(msg.data, null, "wonmsg:hasReceiverNeed", null);
                //load the data into the local rdf store and publish NeedCreatedEvent when done
                needURIPromise.then(
                    function(value) {
                        var needURI = value;
                        console.log("need uri:" + needURI);
                        linkedDataService.fetch(needURI)
                            .then(
                            function (value) {
                                eventData = {};
                                eventData.needURI = needURI;
                                eventData.type = won.EVENT.NEED_CREATED;
                                //publish a needCreatedEvent
                                $rootScope.$broadcast(won.EVENT.NEED_CREATED, eventData);
                                //inform the caller of the new need URI
                                deferred.resolve(needURI);
                            },
                            function (reason) {
                                deferred.reject(reason);
                            }, null
                        );
                    },
                    function(reason){
                        console.log("could not get receiver need uri. Reason:" + reason);
                    }
                    ,null
                );
                this.done = true;
            });
        callback.done = false;
        callback.shouldHandleTest = function (event, msg) {
            return true;
        };
        callback.shouldUnregisterTest = function(event, msg) {
            return this.done;
        };

        messageService.addMessageCallback(callback);
        try {
            messageService.sendMessage(message);
        } catch (e) {
            deferred.reject(e);
        }
        return deferred.promise;
    }

    wonService.connect = function(connectionAsJsonLd, need1, need2){
        var deferred = $q.defer();
        var message = new won.ConnectMessageBuilder(connectionAsJsonLd)
            .addMessageGraph()
            .eventURI("2345432343")  //TODO: generate event URI here
            .hasSenderNeed(need1)
            .hasSenderNode("http://localhost:8080/won")
            .hasReceiverNeed(need2)
            .hasReceiverNode("http://localhost:8080/won")
            .sender()
            .receiver()
            .build();
        var callback = new messageService.MessageCallback(
            function (event, msg) {
                //check if the message we got (the create need response message) indicates that all went well
                console.log("got connect needs message response!");
                //TODO: if negative, use alternative need URI and send again
                //TODO: if positive, propagate positive response back to caller
                //TODO: fetch need data and store in local RDF store
                this.done = true;
                //WON.CreateResponse.equals(messageService.utils.getMessageType(msg)) &&
                //messageService.utils.getRefersToURIs(msg).contains(messageURI)

                //assume we can obtain a need URI and return it
                var connectionURI = "sadf"; //TODO: get needURI from result
                deferred.resolve(connectionURI);
            });
        callback.done = false;
        callback.shouldHandleTest = function (msg) {
            return true;
        };
        callback.shouldUnregisterTest = function(msg) {
            return this.done;
        };

        messageService.addMessageCallback(callback);
        try {
            messageService.sendMessage(message);
        } catch (e) {
            deferred.reject(e);
        }
        return deferred.promise;
    }



    return wonService;
});