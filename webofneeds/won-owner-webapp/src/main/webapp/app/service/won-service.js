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
    utilService,
    $log) {

    var wonService = {};
    var privateData = {}
    //set a default WoN node uri, but immediately replace by the one obtained from the server
    privateData.defaultWonNodeUri = $location.protocol()+"://"+$location.host()+"/won/resource";
    $http.get("appConfig/getDefaultWonNodeUri")
        .success(
        function onGetDefaultWonNodeUri(data, status, headers, config) {
            if (status == 200){
                $log.debug("setting default won node uri to value obtained from server: " + JSON.stringify(data));
                privateData.defaultWonNodeUri = JSON.parse(data);
            } else {
                $log.error("Error obtaining default won node uri, http status=" + status);

            }
        })
        .error(
        function onGetDefaultWonNodeUriError(data, status, headers, config) {
            $log.error("Error obtaining default won node uri, http status=" + status);
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
                $log.debug("Broadcasting angular event " + eventData.eventType);
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
                $log.debug("Broadcasting angular event " + eventData.eventType);
                $rootScope.$broadcast(eventData.eventType, eventData);
            });
    }
    var processConnectSentMessage = function(eventData, message){
        $log.debug("processing ConnectSent Message");
        linkedDataService.invalidateCacheForNewMessage(eventData.hasSender)
            .then(function(){
                $log.debug("Broadcasting angular event " + eventData.eventType);
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
                $log.debug("Broadcasting angular event " + eventData.eventType);
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
                $log.debug("Broadcasting angular event " + eventData.eventType);
                $rootScope.$broadcast(eventData.eventType, eventData);
            });
    }
    var processCloseNeedMessage = function(eventData, message){
        var needURI = eventData.hasReceiver;
        linkedDataService.invalidateCacheForNewMessage(needURI)
            .then(function(){
                $log.debug("Broadcasting angular event "+eventData.eventType);
                $rootScope.$broadcast(eventData.eventType, eventData);
            })
    }

    var processConnectionMessage = function(eventData, message) {
        //load the data of the connection that the hint is about, if required
        var connectionURI = eventData.hasReceiver;
        linkedDataService.invalidateCacheForNewMessage(connectionURI)
            .then(function(){
                $log.debug("Broadcasting angular event " + eventData.eventType);
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
    messageTypeToEventType[won.WONMSG.closeNeedMessageCompacted] = {eventType: won.EVENT.CLOSE_NEED_RECEIVED, handler:processCloseNeedMessage};
    messageTypeToEventType[won.WONMSG.connectionMessageCompacted] = {eventType: won.EVENT.CONNECTION_MESSAGE_RECEIVED, handler:processConnectionMessage};
    messageTypeToEventType[won.WONMSG.needStateMessageCompacted] = {eventType: won.EVENT.NEED_STATE_MESSAGE_RECEIVED, handler:null};
    messageTypeToEventType[won.WONMSG.errorMessageCompacted] = {eventType: won.EVENT.NOT_TRANSMITTED, handler:processErrorMessage}

    //callback to be used in message-service to handle incoming messages
    var createIncomingMessageCallback = function() {
        var incomingMessageHandler = new messageService.MessageCallback(
            function (event, msg) {
                $log.debug("processing incoming message");
                var configForEvent = messageTypeToEventType[event.hasMessageType];
                //only do something if a type/handler combination is registered
                if (configForEvent.eventType != null) {
                    event.eventType = configForEvent.eventType;
                    event.timestamp = new Date().getTime();
                    $log.debug("incoming message: \n  " + JSON.stringify(msg));
                    if (configForEvent.handler != null) {
                        //the handler is responsible for broadcasting the event!
                        configForEvent.handler(event, msg);
                    } else {
                        //publish angular event
                        $log.debug("Broadcasting angular event " + event.eventType);
                        $rootScope.$broadcast(event.eventType, event);
                    }

                } else {
                    $log.warn("Not handling message of type " + event.hasMessageType + " in incomingMessageHandler");
                }
            });
        //handle all incoming messages that are mapped to events in the messageTypeToEventType map
        incomingMessageHandler.shouldHandleTest = function (event, msg){
            var messageType = event.hasMessageType;
            if (typeof messageType === 'undefined') return false;
            var eventTypeConfig = messageTypeToEventType[messageType];
            if (typeof eventTypeConfig === 'undefined') return false;
            // YP: the next line is added, so that the incoming message is ignored in case it refers to a local need
            // that the local application state doesn't know about (e.g., see issue #196). This line can be removed
            // if we change the notification handling to insure that the user's needs are synced between his sessions
            if (!(event.hasReceiverNeed in applicationStateService.getAllNeeds())) return false;
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


    var isSuccessMessage = function isSuccessMessage(event) {
        return event.hasMessageType === won.WONMSG.successResponseCompacted;
    }


    /**
     * Having received a response, and having transformed it to the specified
     * event, set the commState property according to whether the response represents success or failure.
     * Does this for a message that is directed at an owned need (create, activate, deactivate).
     * @param event
     */
    var setCommStateFromResponseForLocalNeedMessage = function setCommStateFromResponseForLocalNeedMessage(event) {
        if (isSuccessMessage(event)){
            event.commState = won.COMMUNUCATION_STATE.ACCEPTED;
        } else {
            event.commState = won.COMMUNUCATION_STATE.NOT_TRANSMITTED;
        }
    }

    var createMessageCallbackForLocalNeedMessage = function createMessageCallbackForLocalNeedMessage(eventUri, eventType){
        var callback = new messageService.MessageCallback(
            function(event,msg){
                $log.debug("got response for " + eventType +": " + event.hasMessageType);
                var eventData = won.clone(event);
                setCommStateFromResponseForLocalNeedMessage(eventData);
                eventData.eventType = eventType;
                linkedDataService.invalidateCacheForNeed(needURI)
                    .then(
                    function (value) {
                        linkedDataService.fetch(eventUri)
                            .then(
                            function (value2) {
                                linkedDataService.invalidateCacheForNewMessage(eventData.hasSender);
                                $log.debug("Broadcasting angular event " + eventType + " commState:" + eventData.commState);
                                $rootScope.$broadcast(eventType, eventData);
                            }, won.reportError("cannot fetch event " + eventUri)
                        );
                    }, won.reportError("cannot fetch event " + needURI)
                );
                this.done = true;
            }
        );
        callback.done = false;
        callback.msgURI = eventUri;
        callback.shouldHandleTest = function(event,msg){
            var ret = event.isResponseTo == this.msgURI;
            $log.debug("event "+event.uri + "refers to event "+ this.msgURI+": "+ret);
            return ret;
        };
        callback.shouldUnregisterTest = function(msg){
            return this.done;
        };
        return callback;
    }

    /**
     * Having received a response, and having transformed it to the specified
     * event, set the commState property according to whether the response represents success or failure.
     * Does this for a message that is directed at a remote need (connect/open/connectionMessage/close).
     * @param event
     * @parem isResponseFromRemoteNode - if true, the response is interpreted as coming from the remote node
     */
    var setCommStateFromResponseForRemoteNeedMessage = function setCommStateFromResponseForRemoteNeedMessage(event, isResponseFromRemoteNode) {
        if (isResponseFromRemoteNode) {
            if (isSuccessMessage(event)){
                event.commState = won.COMMUNUCATION_STATE.ACCEPTED;
            } else {
                event.commState = won.COMMUNUCATION_STATE.NOT_CONNECTED;
            }
        } else {
            if (isSuccessMessage(event)) {
                event.commState = won.COMMUNUCATION_STATE.PENDING;
            } else {
                event.commState = won.COMMUNUCATION_STATE.NOT_TRANSMITTED;
            }
        }
    }

    var createMessageCallbackForRemoteNeedMessage = function createMessageCallbackForRemoteNeedMessage(eventUri, eventType){
        var callback = new messageService.MessageCallback(
            function (event, msg) {
                $log.debug("got response for " + eventType +": " + event.hasMessageType);
                var eventData = won.clone(event);
                eventData.eventType = eventType;
                var isRemoteResponse = false;
                if (event.isRemoteResponseTo == this.msgURI){
                    isRemoteResponse = true;
                    this.gotResponseFromRemoteNode = true;
                } else if (event.isResponseTo == this.msgURI) {
                    isRemoteResponse = false;
                    this.gotResponseFromOwnNode = true;
                } else {
                    $log.error("event is not a response to " + this.msgURI +" : " + JSON.stringify(event));
                    return;
                }
                if (!isSuccessMessage(event)){
                    this.done = true;
                } else {
                    this.done = this.gotResponseFromOwnNode && this.gotResponseFromRemoteNode;
                }
                setCommStateFromResponseForRemoteNeedMessage(eventData, isRemoteResponse);
                linkedDataService.fetch(eventData.hasSender)
                    .then(
                    function (value) {
                        linkedDataService.fetch(eventUri)
                            .then(
                            function(value2) {
                                linkedDataService.invalidateCacheForNewMessage(eventData.hasSender);
                                eventData.timestamp = new Date().getTime();
                                $log.debug("Broadcasting angular event " + eventType + ", commState:" + eventData.commState);
                                $rootScope.$broadcast(eventType,eventData);
                            }, won.reportError("cannot fetch event " + eventUri)
                        );
                    }, won.reportError("cannot fetch connection for event " + eventUri)
                );
            });
        callback.gotResponseFromOwnNode = false;
        callback.gotResponseFromRemoteNode = false;
        callback.done = false;
        callback.msgURI = eventUri;
        callback.shouldHandleTest = function (event, msg) {
            var ret = event.isResponseTo == this.msgURI || event.isRemoteResponseTo == this.msgURI;
            $log.debug("event " + event.uri + " refers to event " + this.msgURI + ": " + ret);
            return ret;
        };
        callback.shouldUnregisterTest = function(msg) {
            return this.done;
        };
        return callback;
    }


    wonService.activateNeed = function(needURI){
        var sendActivateNeed = function(envelopeData,needUri){
            var eventUri = envelopeData[won.WONMSG.hasSenderNode]+"/event/"+utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.activateNeedMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .build();
            var callback = createMessageCallbackForLocalNeedMessage(eventUri, won.EVENT.ACTIVATE_NEED_SENT);
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
                //now tell listeners that we sent the message
                var eventData = {}
                eventData.eventType = won.EVENT.ACTIVATE_NEED_SENT;
                eventData.commState = won.COMMUNUCATION_STATE.PENDING;
                $rootScope.$broadcast(won.EVENT.ACTIVATE_NEED_SENT, eventData);
            }catch (e) {
                $log.warn("could not activate " + needURI + ". Reason" + e);
            }

        }
        linkedDataService.getEnvelopeDataForNeed(needURI)
            .then(function(envelopeData){
                sendActivateNeed(envelopeData, needURI);
            },
            won.reportError("cannot open connection " + needURI)
        );
    }


    wonService.closeNeed = function(needURI){
        var sendCloseNeed = function(envelopeData, eventToOpenFor) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  utilService.getRandomInt(1,9223372036854775807);
            var message = new won.MessageBuilder(won.WONMSG.closeNeedMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .build();
            var callback = createMessageCallbackForLocalNeedMessage(eventUri, won.EVENT.CLOSE_NEED_SENT);
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
            } catch (e) {
                $log.warn("could not open " + needURI + ". Reason" + e);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataForNeed(needURI)
            .then(function(envelopeData){
                sendCloseNeed(envelopeData, needURI);
            },
            won.reportError("cannot open connection " + needURI)
        );

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

        //TODO: this callback could be changed to be the same as activate/deactivate, but the special code (updateing the applicationStateService) needs to be moved to another place
        var callback = new messageService.MessageCallback(
            function (event, msg) {
                //check if the message we got (the create need response message) indicates that all went well
                $log.debug("got response for CREATE: " + event.hasMessageType);
                //TODO: if negative, use alternative need URI and send again
                //fetch need data and store in local RDF store
                //get URI of newly created need from message

                //load the data into the local rdf store and publish NeedCreatedEvent when done
                var needURI = event.hasReceiverNeed;
                linkedDataService.fetch(needURI)
                    .then(
                    function (value) {
                        var eventData = won.clone(event);
                        eventData.eventType = won.EVENT.NEED_CREATED;
                        setCommStateFromResponseForLocalNeedMessage(eventData);
                        eventData.needURI = needURI;
                        linkedDataService.getNeed(needURI)
                            .then(function(need){
                                applicationStateService.addNeed(need);
                                $log.debug("Broadcasting angular event " + won.EVENT.NEED_CREATED);
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
            var ret = event.isResponseTo == this.msgURI;
            $log.debug("event " + event.uri + " refers to event " + this.msgURI + ": " + ret);
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
        $log.debug("adding message callback listening for isResponseTo " + callback.msgURI);
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
            applicationStateService.setCurrentNeedURI(need1);
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
            var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CONNECT_SENT)
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
            } catch (e) {
                deferred.reject(e);
                $log.warn("could not connect " + need1 + " and " + need2 + ". Reason" + e);
            }
        }

        //fetch the won nodes of both needs
        linkedDataService.getWonNodeUriOfNeed(need1).then(
            function (wonNodeUri1) {
                return linkedDataService.getWonNodeUriOfNeed(need2).then(
                    function(wonNodeUri2){
                        sendConnect(need1, need2, wonNodeUri1, wonNodeUri2, textMessage);
                    }
                );
            },
            won.reportError("cannot connect need " + need1 + " and " + need2)
        );
        return deferred.promise;
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
                .hasFacet(won.WON.OwnerFacet) //TODO: looks like a copy-paste-leftover from connect
                .hasRemoteFacet(won.WON.OwnerFacet) //TODO: looks like a copy-paste-leftover from connect
                .hasTextMessage(textMessage)
                .build();
            var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CONNECT_SENT)
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
            } catch (e) {
                var eventData = {"uri":eventUri,"commState":won.COMMUNUCATION_STATE.NOT_CONNECTED};
                $log.warn("could not open suggested connection " + connectionUri + ". Reason" + e);
                $log.debug("Broadcasting angular event " + won.EVENT.NO_CONNECTION);
                $rootScope.$broadcast(won.EVENT.NO_CONNECTION, eventData);
            }
        }

        //fetch all data needed
        linkedDataService.getEnvelopeDataforConnection(connectionUri)
            .then(function(envelopeData){
                sendOpen(envelopeData);
            },
            // TODO the ux needs to react to such errors. E.g. now, if the user tries to
            // send a request conversation message, this error occurs, but in the gui nothing happens
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
                .hasFacet(won.WON.OwnerFacet) //TODO: looks like a copy-paste-leftover from connect
                .hasRemoteFacet(won.WON.OwnerFacet)//TODO: looks like a copy-paste-leftover from connect
                .hasTextMessage(textMessage)
                .build();
            var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.OPEN_SENT);
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
            } catch (e) {
                $log.warn("could not open " + msgToOpenFor.connection.uri + ". Reason" + e);
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
                .hasTextMessage(textMessage)
                .build();
            var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CLOSE_SENT);
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
            } catch (e) {
                $log.warn("could not open suggested connection " +  msgToClose.connection.uri + ". Reason" + e);
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
            var callback = createMessageCallbackForRemoteMessage(eventUri, won.EVENT.CONNECTION_MESSAGE_SENT);
            messageService.addMessageCallback(callback);
            try {
                messageService.sendMessage(message);
            } catch (e) {
                $log.warn("could not open " + connectionUri + ". Reason" + e);
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