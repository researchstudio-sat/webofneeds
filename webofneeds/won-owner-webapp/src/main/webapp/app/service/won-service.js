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
angular.module('won.owner').factory('wonService', function (
    messageService,
    $q,
    $http,
    $location,
    linkedDataService,
    $rootScope,
    applicationStateService,
    utilService) {

    var wonService = {};
    var privateData = {}
    //set a default WoN node uri, but immediately replace by the one obtained from the server
    privateData.defaultWonNodeUri = $location.protocol()+"://"+$location.host()+"/won/resource";
    $http.get("appConfig/getDefaultWonNodeUri")
        .success(
        function onGetDefaultWonNodeUri(data, status, headers, config) {
            if (status == 200){
                console.log("setting default won node uri to value obtained from server: " + JSON.stringify(data));
                privateData.defaultWonNodeUri = JSON.parse(data);
            } else {
                console.log("warn: error obtaining default won node uri, http status=" + status);
            }
        })
        .error(
        function onGetDefaultWonNodeUriError(data, status, headers, config) {
            console.log("warn: error obtaining default won node uri, http status=" + status);
        }
        );




    //private functions that handle incoming messages
    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processHintMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        linkedDataService.invalidateCacheForNewConnection(eventData.hasReceiver, eventData.hasReceiverNeed)
            ['finally'](function(){
                eventData.matchScore = eventData.framedMessage[won.WON.hasMatchScoreCompacted];
                eventData.matchCounterpartURI = won.getSafeJsonLdValue(eventData.framedMessage[won.WON.hasMatchCounterpart]);
                //add some properties to the eventData so as to make them easily accessible to consumers
                //of the hint event
                if (eventData.matchCounterpartURI != null) {
                    //load the data of the need the hint is about, if required
                    //linkedDataService.ensureLoaded(eventData.uri);
                    linkedDataService.ensureLoaded(eventData.matchCounterpartURI);
                }
                console.log("publishing angular event");
                $rootScope.$broadcast(eventData.eventType, eventData);
            });
    }

    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processConnectMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        linkedDataService.invalidateCacheForNewConnection(eventData.hasReceiver, eventData.hasReceiverNeed).
            then(function(){
                console.log("publishing angular event");
                $rootScope.$broadcast(eventData.eventType, eventData);
            });
    }
    var processConnectSentMessage = function(eventData, message){
        console.log("processing ConnectSent Message");
        linkedDataService.invalidateCacheForNewMessage(eventData.hasSender)
            .then(function(){
                console.log("publishing angular event");
                $rootScope.$broadcast(eventData.eventType, eventData);
            });

    }

    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processOpenMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.hasReceiver;
        linkedDataService.invalidateCacheForNewMessage(connectionURI)
            .then(function(){
                console.log("publishing angular event");
                $rootScope.$broadcast(eventData.eventType, eventData);
            });
    }
    var processErrorMessage = function(eventData, message){
        eventData.commState = won.COMMUNUCATION_STATE.NOT_TRANSMITTED;

    }
    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processCloseMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.hasReceiver;
        linkedDataService.invalidateCacheForNewMessage(connectionURI)
            .then(function(){
                console.log("publishing angular event");
                $rootScope.$broadcast(eventData.eventType, eventData);
            });
    }

    var processConnectionMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.hasReceiver;
        linkedDataService.invalidateCacheForNewMessage(connectionURI)
            .then(function(){
                console.log("publishing angular event");
                $rootScope.$broadcast(eventData.eventType, eventData);
            })
    }


    //mapping between message type and eventType/handler combination
    var messageTypeToEventType = {};
    messageTypeToEventType[won.WONMSG.hintMessageCompacted] = {eventType: won.EVENT.HINT_RECEIVED, handler: processHintMessage};
    messageTypeToEventType[won.WONMSG.connectMessageCompacted] = {eventType: won.EVENT.CONNECT_RECEIVED,handler:processConnectMessage};
    messageTypeToEventType[won.WONMSG.connectSentMessageCompacted] = {eventType: won.EVENT.CONNECT_SENT, handler: processConnectSentMessage}
    messageTypeToEventType[won.WONMSG.openMessageCompacted] = {eventType: won.EVENT.OPEN_RECEIVED, handler:processOpenMessage};
    messageTypeToEventType[won.WONMSG.closeMessageCompacted] = {eventType: won.EVENT.CLOSE_RECEIVED, handler:processCloseMessage};
    messageTypeToEventType[won.WONMSG.connectionMessageCompacted] = {eventType: won.EVENT.CONNECTION_MESSAGE_RECEIVED, handler:processConnectionMessage};
    messageTypeToEventType[won.WONMSG.needStateMessageCompacted] = {eventType: won.EVENT.NEED_STATE_MESSAGE_RECEIVED, handler:null};
    messageTypeToEventType[won.WONMSG.errorMessageCompacted] = {eventType: won.EVENT.NOT_TRANSMITTED, handler:processErrorMessage}

    //callback to be used in message-service to handle incoming messages
    var createIncomingMessageCallback = function() {
        var incomingMessageHandler = new messageService.MessageCallback(
            function (event, msg) {
                console.log("processing incoming message");
                var configForEvent = messageTypeToEventType[event.hasMessageType];
                //only do something if a type/handler combination is registered
                if (configForEvent.eventType != null) {
                    event.eventType = configForEvent.eventType;
                    event.timestamp = new Date().getTime();
                    console.log("incoming message: \n  ", JSON.stringify(msg));
                    if (configForEvent.handler != null) {
                        //the handler is responsible for broadcasting the event!
                        configForEvent.handler(event, msg);
                    } else {
                        //publish angular event
                        console.log("publishing angular event");
                        $rootScope.$broadcast(event.eventType, event);
                    }

                } else {
                    console.log("Not handling message of type " + event.hasMessageType + " in incomingMessageHandler");
                }
            });
        //handle all incoming messages that are mapped to events in the messageTypeToEventType map
        incomingMessageHandler.shouldHandleTest = function (event, msg){
            var messageType = event.hasMessageType;
            if (typeof messageType === 'undefined') return false;
            var eventTypeConfig = messageTypeToEventType[messageType];
            if (typeof eventTypeConfig === 'undefined') return false;
            return true;
        };
        //never unregister
        incomingMessageHandler.shouldUnregisterTest = function(event, msg) {
            return false;
        };
        incomingMessageHandler.id = "incomingMessageHandler";
        incomingMessageHandler.equals = function(other){
            return other != null && other.id != null && other.id === this.id;
        }
        return incomingMessageHandler;
    }

    //add callback to messageService
    messageService.addMessageCallback(createIncomingMessageCallback());

    wonService.getDefaultWonNodeUri = function(){
        return privateData.defaultWonNodeUri;
    }

    /**
     * Creates a need and returns a Promise to the URI of the newly created need (which may differ from the one
     * specified in the need object here.
     * @param needAsJsonLd
     * @returns {*}
     */
    wonService.createNeed = function(needAsJsonLd) {
        var deferred = $q.defer();
        var needData = won.clone(needAsJsonLd);
        //TODO: Fix hard-coded URIs here!
        var eventUri = privateData.defaultWonNodeUri + "/event/" + utilService.getRandomInt(1,9223372036854775807);
        var needUri = needData['@graph'][0]['@graph'][0]['@id'];
        if (needUri == null || 0 === needUri.length) {
            needUri =  privateData.defaultWonNodeUri + "/need/" + utilService.getRandomInt(1,9223372036854775807);
            needData['@graph'][0]['@graph'][0]['@id'] = needUri;
        }
        var wonNode = privateData.defaultWonNodeUri;
        needData['@graph'][0]['@id'] = needUri + "/core/#data";
        var message = new won.MessageBuilder(won.WONMSG.createMessage, needData)
            .eventURI(eventUri)
            .hasReceiverNode(wonNode)
            .hasSenderNeed(needUri)
            .build();


        var callback = new messageService.MessageCallback(
            function (event, msg) {
                //check if the message we got (the create need response message) indicates that all went well
                console.log("got create need message response for need " + event.hasReceiverNeed);
                //TODO: if negative, use alternative need URI and send again
                //fetch need data and store in local RDF store
                //get URI of newly created need from message

                //load the data into the local rdf store and publish NeedCreatedEvent when done
                var needURI = event.hasReceiverNeed;
                linkedDataService.fetch(needURI)
                    .then(
                    function (value) {
                        console.log("publishing angular event");
                        var eventData = won.clone(event);
                        eventData.eventType = won.EVENT.NEED_CREATED;
                        eventData.needURI = needURI;
                        linkedDataService.getNeed(needURI)
                            .then(function(need){
                                applicationStateService.addNeed(need)
                                $rootScope.$broadcast(won.EVENT.NEED_CREATED, eventData);
                                deferred.resolve(needURI);
                            });
                    },
                    function (reason) {
                        deferred.reject(reason);
                    }, null
                );
                this.done = true;
            });
        callback.done = false;
        callback.msgURI = eventUri;
        callback.shouldHandleTest = function (event, msg) {
            var ret = event.refersTo == this.msgURI;
            console.log("event " + event.uri + " refers to event " + this.msgURI + ": " + ret);
            return ret;
        };
        callback.shouldUnregisterTest = function(event, msg) {
            return this.done;
        };
        callback.equals = function(other){
            return other != null && other.msgURI != null && other.msgURI === this.msgURI;
        }

        //find out which message uri was created and use it for the callback's shouldHandleTest
        //so we can wait for a dedicated response
        console.log("adding message callback listening for refersToURI " + callback.msgURI);
        messageService.addMessageCallback(callback);
        try {
            messageService.sendMessage(message);
        } catch (e) {
            deferred.reject(e);
        }
        return deferred.promise;
    }

    /**
     * Creates a connection on behalf of need1 to need2. If a connection between the same facets
     * as defined here already exists, the WoN node will return an error message.
     * @param need1
     * @param need2
     */
    wonService.connect = function(need1, need2, textMessage){
        var deferred = $q.defer();
        var sendConnect = function(need1, need2, wonNodeUri1, wonNodeUri2, textMessage) {
            //TODO: use event URI pattern specified by WoN node
            var envelopeData = {};
            envelopeData[won.WONMSG.hasSender]=need1;
            envelopeData[won.WONMSG.hasSenderNeed] = need1;
            envelopeData[won.WONMSG.hasSenderNode] = wonNodeUri1;
            envelopeData[won.WONMSG.hasReceiverNeed] = need2;
            envelopeData[won.WONMSG.hasReceiverNode] = wonNodeUri2;
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);

            var message = new won.MessageBuilder(won.WONMSG.connectMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasFacet(won.WON.OwnerFacet)
                .hasRemoteFacet(won.WON.OwnerFacet)
                .hasTextMessage(textMessage)
                .build();


            var callback = new messageService.MessageCallback(
                function (event, msg) {
                    //check if the message we got (the create need response message) indicates that all went well
                    console.log("got connect needs message response! TODO: check for connect response!");
                    //TODO: if negative, use alternative need URI and send again
                    //TODO: if positive, propagate positive response back to caller
                    //TODO: fetch need data and store in local RDF store
                    this.done = true;
                    //WON.CreateResponse.equals(messageService.utils.getMessageType(msg)) &&
                    //messageService.utils.getRefersToURIs(msg).contains(messageURI)

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
                setTimeout(
                    function(){
                        //linkedDataService.fetch(connection.uri);
                        //console.log("publishing angular event");
                        //$rootScope.$broadcast(won.EVENT.CLOSE_SENT, eventData);
                        var messageTemp = new won.MessageBuilder(won.WONMSG.connectSentMessage)
                            .eventURI(eventUri)
                            .forEnvelopeData(envelopeData)
                            .hasFacet(won.WON.OwnerFacet)
                            .hasRemoteFacet(won.WON.OwnerFacet)
                            .build();
                        var eventData = getEventData(messageTemp);
                        //  eventData.eventType = messageTypeToEventType[eventData.hasMessageType];
                        eventData.eventType = won.EVENT.CONNECT_SENT;
                        eventData.commState = won.COMMUNUCATION_STATE.PENDING;
                        linkedDataService.fetch(eventData.hasSender)
                            .then(
                            function (value) {
                                linkedDataService.fetch(eventUri)
                                    .then(
                                    function(value2) {
                                        console.log("publishing angular event");

                                        //eventData.eventType = won.EVENT.CLOSE_SENT;
                                        deferred.resolve(eventUri);
                                        eventData.timestamp = new Date().getTime();
                                        $rootScope.$broadcast(won.EVENT.CONNECT_SENT, eventData);
                                        //$rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);

                                    }, won.reportError("cannot fetch closed event " + eventUri)
                                );
                            }, won.reportError("cannot fetch closed connection " +eventUri)
                        );

                    }, 3000);
            } catch (e) {
                deferred.reject(e);
               console.log("could not connect " + need1 + " and " + need2 + ". Reason" + e);
            }
        }

        //fetch the won nodes of both needs
        linkedDataService.getWonNodeUriOfNeed(need1).then(
            function (wonNodeUri1) {
                return linkedDataService.getWonNodeUriOfNeed(need2).then(
                    function(wonNodeUri2){
                        sendConnect(need1, need2, wonNodeUri1, wonNodeUri2);
                    }
                );
            },
            won.reportError("cannot connect need " + need1 + " and " + need2)
        );
        return deferred.promise;
    }
    //TODO: only added for testing, remove it afterwards
    var getEventData = function(json) {
        console.log("getting data from jsonld message");
        var eventData = {};
        //call handler if there is one - it may modify the event object
        //frame the incoming jsonld to get the data that interest us
        var frame = {"@context": {
            "won": "http://purl.org/webofneeds/model#",
            "msg": "http://purl.org/webofneeds/message#" //message is the default vocabulary
        },
            "msg:hasMessageType": { }
        };
        //copy data from the framed message to the event object
        var framedMessage = jsonld.frame(json, frame);
        for (key in framedMessage) {
            var propName = won.getLocalName(key);
            if (propName != null && !won.isJsonLdKeyword(propName)) {
                eventData[propName] = won.getSafeJsonLdValue(framedMessage[key]);
            }
        }
        eventData.uri = won.getSafeJsonLdValue(framedMessage);
        eventData.framedMessage = framedMessage;
        console.log("done copying the data to the event object, returning the result");

        return eventData;
    }
    /**
     * Opens the existing connection specified by connectionUri.
     * @param need1
     * @param need2
     */
    wonService.openSuggestedConnection = function(connectionUri, textMessage){

        var sendOpen = function(envelopeData) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.connectMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasFacet(won.WON.OwnerFacet)
                .hasRemoteFacet(won.WON.OwnerFacet)
                .hasTextMessage(textMessage)
                .build();
            var callback = new messageService.MessageCallback(
                function (event, msg) {
                    //check if the message we got (the create need response message) indicates that all went well
                    console.log("got connect needs message response! TODO: check for connect response!");
                    //TODO: if negative, use alternative need URI and send again
                    //TODO: if positive, propagate positive response back to caller
                    //TODO: fetch need data and store in local RDF store
                    this.done = true;
                    //WON.CreateResponse.equals(messageService.utils.getMessageType(msg)) &&
                    //messageService.utils.getRefersToURIs(msg).contains(messageURI)
                    //$rootScope.$broadcast(won.EVENT.OPEN_SENT, eventData);

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
                // ideally should be in callback, but since there is no response coming,
                // right now I put it in timeout, so that the close event and connection
                // update has some time to be saved on the won-node before it gets fetched here.
                // As soon as getting the response for this sent message work, and the equivalent to
                // below implementation inside the callback works,  all the setTimeout() with its
                // inside should be removed
                setTimeout(
                    function(){
                        //linkedDataService.fetch(connection.uri);
                        //console.log("publishing angular event");
                        //$rootScope.$broadcast(won.EVENT.CLOSE_SENT, eventData);
                        var messageTemp = new won.MessageBuilder(won.WONMSG.connectSentMessage)
                            .eventURI(eventUri)
                            .forEnvelopeData(envelopeData)
                            .hasFacet(won.WON.OwnerFacet)
                            .hasRemoteFacet(won.WON.OwnerFacet)
                            .build();
                        var eventData = getEventData(messageTemp);
                      //  eventData.eventType = messageTypeToEventType[eventData.hasMessageType];
                        eventData.eventType = won.EVENT.CONNECT_SENT;
                        eventData.commState = won.COMMUNUCATION_STATE.PENDING;
                        linkedDataService.fetch(eventData.hasSender)
                            .then(
                            function (value) {
                                linkedDataService.fetch(eventUri)
                                    .then(
                                    function(value2) {
                                        console.log("publishing angular event");

                                        //eventData.eventType = won.EVENT.CLOSE_SENT;
                                        eventData.timestamp = new Date().getTime();
                                        $rootScope.$broadcast(won.EVENT.CONNECT_SENT, eventData);
                                        //$rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);

                                    }, won.reportError("cannot fetch closed event " + eventUri)
                                );
                            }, won.reportError("cannot fetch closed connection " +eventUri)
                        );

                    }, 3000);


            //    var handleIncomingMessage = createIncomingMessageCallback();
            //    handleIncomingMessage.action(getEventData(message), message);



            } catch (e) {
                var eventData = {"uri":eventUri,"commState":won.COMMUNUCATION_STATE.NOT_CONNECTED};
                $rootScope.$broadcast(won.EVENT.NO_CONNECTION, eventData);
                console.log("could not open suggested connection " + connectionUri + ". Reason" + e);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataforConnection(connectionUri)
            .then(function(envelopeData){
                sendOpen(envelopeData);
            },
            won.reportError("cannot open suggested connection " + connectionUri)
        );

    }

    /**
     * Opens the existing connection specified by connectionUri.
     * @param need1
     * @param need2
     */
    wonService.open = function(msgToOpenFor, textMessage){

        var sendOpen = function(envelopeData, eventToOpenFor) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.openMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasFacet(won.WON.OwnerFacet)
                .hasRemoteFacet(won.WON.OwnerFacet)
                .hasTextMessage(textMessage)
                .build();
            var callback = new messageService.MessageCallback(
                function (event, msg) {
                    //check if the message we got (the create need response message) indicates that all went well
                    console.log("got connect needs message response! TODO: check for connect response!");
                    //TODO: if negative, use alternative need URI and send again
                    //TODO: if positive, propagate positive response back to caller
                    //TODO: fetch need data and store in local RDF store
                    this.done = true;
                    //WON.CreateResponse.equals(messageService.utils.getMessageType(msg)) &&
                    //messageService.utils.getRefersToURIs(msg).contains(messageURI)

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

                setTimeout(
                    function(){
                        var messageTemp = new won.MessageBuilder(won.WONMSG.openSentMessage)
                            .eventURI(eventUri)
                            .forEnvelopeData(envelopeData)
                            .hasFacet(won.WON.OwnerFacet)
                            .hasRemoteFacet(won.WON.OwnerFacet)
                            .build();
                        var eventData = getEventData(messageTemp);
                        //  eventData.eventType = messageTypeToEventType[eventData.hasMessageType];
                        eventData.eventType = won.EVENT.OPEN_SENT;
                        eventData.commState = won.COMMUNUCATION_STATE.PENDING;

                        linkedDataService.fetch(msgToOpenFor.connection.uri)
                            .then(
                            function (value) {
                                linkedDataService.fetch(eventUri)
                                    .then(
                                    function(value2) {
                                        console.log("publishing angular event");
                                        linkedDataService.invalidateCacheForNewMessage(eventData.hasSender);
                                        //eventData.eventType = won.EVENT.CLOSE_SENT;
                                        $rootScope.$broadcast(won.EVENT.OPEN_SENT, eventData);
                                        //$rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);

                                    }, won.reportError("cannot fetch closed event " + eventUri)
                                );
                            }, won.reportError("cannot fetch closed connection " + msgToOpenFor.connection.uri)
                        );

                    }, 3000);

            } catch (e) {
                console.log("could not open " + msgToOpenFor.connection.uri + ". Reason" + e);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataforConnection(msgToOpenFor.connection.uri)
            .then(function(envelopeData){
                sendOpen(envelopeData, msgToOpenFor.event);
            },
            won.reportError("cannot open connection " + msgToOpenFor.connection.uri)
        );

    }


    /**
     * Closes the existing connection specified by connectionUri.
     * @param need1
     * @param need2
     */
    wonService.closeConnection = function(msgToClose, textMessage){

        var sendClose = function(envelopeData, eventDataToClose) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.closeMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasFacet(won.WON.OwnerFacet)
                .hasRemoteFacet(won.WON.OwnerFacet)
                .hasTextMessage(textMessage)
                .build();
            var callback = new messageService.MessageCallback(
                function (event, msg) {
                    // TODO: deal with failures to create close event response
                    // (e.g. if uri is in use, one needs to resent the event with new uri)
                    // TODO: when this works (when won-node starts sending responses to
                    // close connection messages), comment the code from timeout below,
                    // and check if this works or make it work, and when it works - remove
                    // the timeout code
                    console.log("got close connection message response! TODO: check for close connection response!");
                    this.done = true;
                    // fetch the updated connection and created close event from the won-node
                    var uriOfUpdatedConnectionPromise = linkedDataService.fetch(msgToClose.connection.uri);
                    var uriOfCloseEventPromise = linkedDataService.fetch(eventUri)
                    // when uri promises are resolved, broadcast the event close sent event (actually
                    // it is 'close sent response success received' event...)
                    $q.all([uriOfUpdatedConnectionPromise, uriOfCloseEventPromise])
                        .then(function(result) {
                            linkedDataService.invalidateCacheForNewMessage(eventData.hasSender);
                            $rootScope.$broadcast(won.EVENT.CLOSE_SENT, eventDataToClose);
                        },
                        function(errors) {
                            won.reportError("cannot fetch closed connection " + msgToClose.connection.uri);
                        });
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
                // ideally should be in callback, but since there is no response coming,
                // right now I put it in timeout, so that the close event and connection
                // update has some time to be saved on the won-node before it gets fetched here.
                // As soon as getting the response for this sent message work, and the equivalent to
                // below implementation inside the callback works,  all the setTimeout() with its
                // inside should be removed
                setTimeout(
                    function(){
                        //linkedDataService.fetch(connection.uri);
                        //console.log("publishing angular event");
                        //$rootScope.$broadcast(won.EVENT.CLOSE_SENT, eventData);

                        linkedDataService.fetch(msgToClose.connection.uri)
                            .then(
                            function (value) {
                                linkedDataService.fetch(eventUri)
                                    .then(
                                    function(value2) {
                                        console.log("publishing angular event");

                                        //eventData.eventType = won.EVENT.CLOSE_SENT;
                                        $rootScope.$broadcast(won.EVENT.CLOSE_SENT, eventDataToClose);
                                        //$rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);

                                    }, won.reportError("cannot fetch closed event " + eventUri)
                                );
                            }, won.reportError("cannot fetch closed connection " + msgToClose.connection.uri)
                        );

                    }, 3000);
            } catch (e) {
                console.log("could not open suggested connection " + connection.uri + ". Reason" + e);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataforConnection(msgToClose.connection.uri)
            .then(function(envelopeData){
                sendClose(envelopeData, msgToClose.event);
            },
            won.reportError("cannot close connection " + msgToClose.connection.uri)
        );

    }


    wonService.textMessage = function(text, connectionUri){

        var sendTextMessage = function(envelopeData) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);

            var message = new won.MessageBuilder(won.WONMSG.connectionMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .addContentGraphData(won.WON.hasTextMessage, text)
                .build();
            var callback = new messageService.MessageCallback(
                function (event, msg) {
                    //check if the message we got (the create need response message) indicates that all went well
                    console.log("got connect needs message response! TODO: check for connect response!");
                    //TODO: if negative, use alternative need URI and send again
                    //TODO: if positive, propagate positive response back to caller
                    //TODO: fetch need data and store in local RDF store
                    this.done = true;
                    //WON.CreateResponse.equals(messageService.utils.getMessageType(msg)) &&
                    //messageService.utils.getRefersToURIs(msg).contains(messageURI)

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
                setTimeout(
                    function(){

                        //linkedDataService.fetch(connection.uri);
                        //console.log("publishing angular event");
                        //$rootScope.$broadcast(won.EVENT.CLOSE_SENT, eventData);
                        var messageTemp = new won.MessageBuilder(won.WONMSG.connectionMessageSentMessage)
                            .eventURI(eventUri)
                            .forEnvelopeData(envelopeData)
                            .hasFacet(won.WON.OwnerFacet)
                            .hasRemoteFacet(won.WON.OwnerFacet)
                            .build();
                        var eventData = getEventData(messageTemp);
                        //  eventData.eventType = messageTypeToEventType[eventData.hasMessageType];
                        eventData.eventType = won.EVENT.CONNECTION_MESSAGE_SENT;
                        eventData.commState = won.COMMUNUCATION_STATE.PENDING;
                        linkedDataService.fetch(eventData.hasSender)
                            .then(
                            function (value) {
                                linkedDataService.fetch(eventUri)
                                    .then(
                                    function(value2) {
                                        console.log("publishing angular event");
                                        linkedDataService.invalidateCacheForNewMessage(eventData.hasSender);
                                        //eventData.eventType = won.EVENT.CLOSE_SENT;
                                        eventData.timestamp = new Date().getTime();
                                        $rootScope.$broadcast(won.EVENT.CONNECTION_MESSAGE_SENT,eventData);
                                        //$rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);

                                    }, won.reportError("cannot fetch closed event " + eventUri)
                                );
                            }, won.reportError("cannot fetch closed connection " +eventUri)
                        );

                    }, 3000);

            } catch (e) {
                console.log("could not open " + connectionUri + ". Reason" + e);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataforConnection(connectionUri)
            .then(function(envelopeData){
                sendTextMessage(envelopeData);
            },
            won.reportError("cannot open connection " + connectionUri)
        );

    }

    return wonService;
});