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
    linkedDataService,
    $rootScope,
    applicationStateService,
    utilService) {

    var wonService = {};




    //private functions that handle incoming messages
    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processHintNotificationMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.hasReceiver;
        if (connectionURI != null) {
            linkedDataService.ensureLoaded(connectionURI);
        }
        //extract hint information from message
        //call handler if there is one - it may modify the event object

        eventData.matchScore = eventData.framedMessage[won.WON.hasMatchScoreCompacted];
        eventData.matchCounterpartURI = won.getSafeJsonLdValue(eventData.framedMessage[won.WON.hasMatchCounterpart]);
        //add some properties to the eventData so as to make them easily accessible to consumers
        //of the hint event
        if (eventData.matchCounterpartURI != null) {
            //load the data of the need the hint is about, if required
            linkedDataService.ensureLoaded(eventData.matchCounterpartURI);
        }
    }

    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processConnectMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        //workaround until we get the newly created connection URI from our WoN node inside the event: don't do anything here
        // --> in getLastEventForEachConnection... the 'connections' container is reloaded and the connect event should be loaded, too
        //extract hint information from message
        //call handler if there is one - it may modify the event object
        eventData.remoteNeed = won.getSafeJsonLdValue(eventData.framedMessage[won.WONMSG.hasSenderNeed]);
        //wonService.open(eventData.framedMessage[won.WONMSG.hasReceiver()]);
    }

    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processOpenMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.hasLocalConnection;
        if (connectionURI != null) {
            linkedDataService.fetch(connectionURI);
        }
        //extract hint information from message
        //call handler if there is one - it may modify the event object
        eventData.remoteNeed = won.getSafeJsonLdValue(eventData.framedMessage[won.WONMSG.hasSenderNeed]);
        wonService.sendTextMessage(eventData.framedMessage[won.WONMSG.hasReceiver()], "Hi! this is an automatic greeting!");
    }
    /**
     * Updates the local triple store with the data contained in the hint message.
     * @param eventData event object that is passed as additional argument to $rootScope.$broadcast.
     * @param message the complete message data as received from the WoN node.
     */
    var processCloseMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.receiverURI;
        if (connectionURI != null) {
            linkedDataService.fetch(connectionURI);
        }
        //TODO update remove connection from local store if not already removed
    }


    //mapping between message type and eventType/handler combination
    var messageTypeToEventType = {};
    messageTypeToEventType[won.WONMSG.hintNotificationMessageCompacted] = {eventType: won.EVENT.HINT_RECEIVED, handler: processHintNotificationMessage};
    messageTypeToEventType[won.WONMSG.connectMessageCompacted] = {eventType: won.EVENT.CONNECT_RECEIVED,handler:processConnectMessage};
    messageTypeToEventType[won.WONMSG.openMessageCompacted] = {eventType: won.EVENT.OPEN_RECEIVED, handler:processOpenMessage};
    messageTypeToEventType[won.WONMSG.closeMessageCompacted] = {eventType: won.EVENT.CLOSE_RECEIVED, handler:processCloseMessage};
    messageTypeToEventType[won.WONMSG.connectionMessageCompacted] = {eventType: won.EVENT.CONNECTION_MESSAGE_RECEIVED, handler:null};
    messageTypeToEventType[won.WONMSG.needStateMessageCompacted] = {eventType: won.EVENT.NEED_STATE_MESSAGE_RECEIVED, handler:null};

    //callback to be used in message-service to handle incoming messages
    var createIncomingMessageCallback = function() {
        var incomingMessageHandler = new messageService.MessageCallback(
            function (event, msg) {
                console.log("processing incoming message");
                var configForEvent = messageTypeToEventType[event.hasMessageType];
                //only do something if a type/handler combination is registered
                if (configForEvent.eventType != null) {
                    event.eventType = configForEvent.eventType;
                    if (configForEvent.handler != null) {
                        configForEvent.handler(event, msg);
                    }
                    //publish angular event
                    console.log("incoming message: \n  ", JSON.stringify(msg) + "\npublishing angular event");
                    event.timestamp = new Date().getTime();
                    $rootScope.$broadcast(event.eventType, event);
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
        var eventUri = "http://localhost:8080/won/resource/event/" + utilService.getRandomInt(1,9223372036854775807);
        var needUri = "http://localhost:8080/won/resource/need/" + utilService.getRandomInt(1,9223372036854775807);
        var wonNode = "http://localhost:8080/won";
        needData['@graph'][0]['@id'] = needUri + "/core/#data";
        needData['@graph'][0]['@graph'][0]['@id'] = needUri;
        var message = new won.MessageBuilder(won.WONMSG.createMessage, needData)
            .eventURI(eventUri)
            .hasReceiverNode(wonNode)
            .hasSenderNeed(needUri)
            .hasTimestamp()
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
    wonService.connect = function(need1, need2){

        var sendConnect = function(need1, need2, wonNodeUri1, wonNodeUri2) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = wonNodeUri1+ "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.connectionMessage)
                .eventURI(eventUri)
                .hasSenderNeed(need1)
                .hasSenderNode(wonNodeUri1)
                .hasReceiverNeed(need2)
                .hasReceiverNode(wonNodeUri2)
                .hasTimestamp()
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
            } catch (e) {
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

    }

    /**
     * Opens the existing connection specified by connectionUri.
     * @param need1
     * @param need2
     */
    wonService.openSuggestedConnection = function(connectionUri){

        var sendOpen = function(envelopeData) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.connectMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasTimestamp()
                .hasFacet(won.WON.OwnerFacet)
                .hasRemoteFacet(won.WON.OwnerFacet)
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
            } catch (e) {
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
    wonService.open = function(connectionUri){

        var sendOpen = function(envelopeData) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.openMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasTimestamp()
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
            } catch (e) {
                console.log("could not open " + connectionUri + ". Reason" + e);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataforConnection(connectionUri)
            .then(function(envelopeData){
                sendOpen(envelopeData);
            },
            won.reportError("cannot open connection " + connectionUri)
        );

    }


    /**
     * Closes the existing connection specified by connectionUri.
     * @param need1
     * @param need2
     */
    wonService.closeConnection = function(msgToClose){

        var sendClose = function(envelopeData, eventDataToClose) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.closeMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasTimestamp()
                .hasFacet(won.WON.OwnerFacet)
                .hasRemoteFacet(won.WON.OwnerFacet)
                .build();
            var callback = new messageService.MessageCallback(
                function (event, msg) {
                    //TODO: move here the code from timeout when won-node will
                    // be changed to send  close connection message response
                    console.log("got close connection message response! TODO: check for close connection response!");
                    this.done = true;

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


    wonService.textMessage = function(text){

        var sendTextMessage = function(envelopeData) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);

            var message = new won.MessageBuilder(won.WONMSG.openMessage)
                .eventURI(eventUri)
                .hasTimestamp()
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