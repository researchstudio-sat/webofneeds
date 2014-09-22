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
angular.module('won.owner').factory('wonService', function (messageService, $q, linkedDataService, $rootScope,applicationStateService) {

    wonService = {};




    //private functions that handle incoming messages
    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    processHintNotificationMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.receiverURI;
        if (connectionURI != null) {
            linkedDataService.ensureLoaded(connectionURI);
        }
        //extract hint information from message
        //call handler if there is one - it may modify the event object

        eventData.matchScore = eventData.framedMessage[won.WON.hasMatchScoreCompacted];
        eventData.matchCounterpartURI = won.getSafeURI(eventData.framedMessage[won.WON.hasMatchCounterpart]);
        //add some properties to the eventData so as to make them easily accessible to consumers
        //of the hint event
        if (eventData.matchCounterpartURI != null) {
            //load the data of the need the hint is about, if required
            linkedDataService.ensureLoaded(eventData.matchCounterpartURI);
        }
    }


    //mapping between message type and eventType/handler combination
    messageTypeToEventType = {};
    messageTypeToEventType[won.WONMSG.hintNotificationMessageCompacted] = {eventType: won.EVENT.HINT_RECEIVED, handler: processHintNotificationMessage};
    messageTypeToEventType[won.WONMSG.connectMessageCompacted] = {eventType: won.EVENT.CONNECT_RECEIVED,handler:null};
    messageTypeToEventType[won.WONMSG.openMessageCompacted] = {eventType: won.EVENT.OPEN_RECEIVED, handler:null};
    messageTypeToEventType[won.WONMSG.closeMessageCompacted] = {eventType: won.EVENT.CLOSE_RECEIVED, handler:null};
    messageTypeToEventType[won.WONMSG.connectionMessageCompacted] = {eventType: won.EVENT.CONNECTION_MESSAGE_RECEIVED, handler:null};
    messageTypeToEventType[won.WONMSG.needStateMessageCompacted] = {eventType: won.EVENT.NEED_STATE_MESSAGE_RECEIVED, handler:null};

    //callback to be used in message-service to handle incoming messages
    createIncomingMessageCallback = function() {
        var incomingMessageHandler = new messageService.MessageCallback(
            function (event, msg) {
                console.log("processing incoming message");
                var configForEvent = messageTypeToEventType[event.messageType];
                //only do something if a type/handler combination is registered
                if (configForEvent.eventType != null) {
                    event.eventType = configForEvent.eventType;
                    //store event in local triple store
                    linkedDataService.storeJsonLdGraph(event.eventURI, msg);

                    if (configForEvent.handler != null) {
                        configForEvent.handler(event, msg);
                    }
                    //publish angular event
                    console.log("incoming message: \n  ", JSON.stringify(msg) + "\npublishing angular event");
                    $rootScope.$broadcast(event.eventType, event);
                } else {
                    console.log("Not handling message of type " + event.messageType + " in incomingMessageHandler");
                }
            });
        //handle all incoming messages that are mapped to events in the messageTypeToEventType map
        incomingMessageHandler.shouldHandleTest = function (event, msg){
            var messageType = event.messageType;
            if (typeof messageType === 'undefined') return false;
            var eventTypeConfig = messageTypeToEventType[messageType];
            if (typeof eventTypeConfig === 'undefined') return false;
            return true;
        };
        //never unregister
        incomingMessageHandler.shouldUnregisterTest = function(event, msg) {
            return false;
        };
        return incomingMessageHandler;
    }

    //add callback to messageService
    messageService.addMessageCallback(createIncomingMessageCallback());

    /**
     * helper function for accessin JSON-LD data
     */
    matchFirstObject = function(jsonld, subject, predicate){
        var deferred = $q.defer();
        var ret = {};
        ret.result = null;
        var store = rdfstore.create();
        store.setPrefix("ex", "http://example.org/people/");
        store.setPrefix("msg","http://purl.org/webofneeds/message#");
        store.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        store.load("application/ld+json", jsonld, "ex:test", function(success, results) {
            if (!success) {
                deferred.reject("could not parse jsonld " + jsonld);
            }
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
     * helper function for accessin JSON-LD data
     */
    matchFirstSubject = function(jsonld, predicate, object){
        var deferred = $q.defer();
        var ret = {};
        ret.result = null;
        var store = rdfstore.create();
        store.setPrefix("ex", "http://example.org/people/");
        store.setPrefix("msg","http://purl.org/webofneeds/message#");
        store.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        store.load("application/ld+json", jsonld, function(success, results) {
            if (!success) {
                deferred.reject("could not parse jsonld " + jsonld);
            }
            var obj = object == null ? null:store.rdf.createNamedNode(store.rdf.resolve(object));
            var pred = predicate == null ? null:store.rdf.createNamedNode(store.rdf.resolve(predicate));
            store.graph( function(success, mygraph) {
                if (!success) {
                    deferred.reject("could not match predicate " + predicate + " object " + object);
                }
                var resultGraph = mygraph.match(null, pred, obj);
                if (resultGraph != null && resultGraph.triples.length > 0) {
                    deferred.resolve(resultGraph.triples[0].subject.nominalValue);
                } else {
                    deferred.reject("could not match predicate " + predicate + " object " + object);
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
            .eventURI("34543242134")//TODO: generate event URI here
            .hasReceiverNode("http://localhost:8080/won")//TODO: pass node to function
            .build();


        var callback = new messageService.MessageCallback(
            function (event, msg) {
                //check if the message we got (the create need response message) indicates that all went well
                console.log("got create need message response for need " + event.receiverNeedURI);
                //TODO: if negative, use alternative need URI and send again
                //fetch need data and store in local RDF store
                //get URI of newly created need from message

                //load the data into the local rdf store and publish NeedCreatedEvent when done
                var needURI = event.receiverNeedURI;
                linkedDataService.fetch(needURI)
                    .then(
                    function (value) {
                        console.log("publishing angular event");
                        eventData = won.clone(event);
                        eventData.eventType = won.EVENT.NEED_CREATED;
                        eventData.needURI = needURI;

                        $rootScope.$broadcast(won.EVENT.NEED_CREATED, eventData);

                        //inform the caller of the new need URI
                        deferred.resolve(needURI);
                    },
                    function (reason) {
                        deferred.reject(reason);
                    }, null
                );
                this.done = true;
            });
        callback.done = false;
        callback.msgURI = null;
        callback.shouldHandleTest = function (event, msg) {
            var ret = event.refersToURI == this.msgURI;
            console.log("event " + event.eventURI + " refers to event " + this.msgURI + ": " + ret);
            return ret;
        };
        callback.shouldUnregisterTest = function(event, msg) {
            return this.done;
        };

        //find out which message uri was created and use it for the callback's shouldHandleTest
        //so we can wait for a dedicated response
        //TODO: get the event URI from where we generate it in the first place! this is unsafe!
        matchFirstSubject(message, won.WONMSG.hasMessageTypeCompacted, null)
            .then(function(uri) {
                callback.msgURI = uri;
            })
            .then(function() {
                console.log("adding message callback listening for refersToURI " + callback.msgURI);
                messageService.addMessageCallback(callback);
                try {
                    messageService.sendMessage(message);
                } catch (e) {
                    deferred.reject(e);
                }
            });
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


    /*********************
     * Angular event handling
     */



    return wonService;
});