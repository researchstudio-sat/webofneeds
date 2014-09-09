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
angular.module('won.owner').factory('messageService', function ($http, $q, $rootScope) {
    //the service object we're constructing here
    var messageService = {};

    //private data of the service
    var privateData = {};

    //currently registered callbacks
    privateData.callbacks = [];

    //array holding messages waiting to be sent. The sendMessage function never
    //blocks, but when the socket isn't connected, the service will try to connect
    //and send the message later.
    privateData.pendingOutMessages = [];

    getSafeValue = function(dataItem) {
        if (dataItem == null) return null;
        if (dataItem.value != null) return dataItem.value;
        return null;
    }

    getEventData = function(json){
        console.log("getting data from jsonld message");
        var eventData = {};
        rdfstore.create(function (store) {
            store.setPrefix("wonmsg", "http://purl.org/webofneeds/message#");
            store.load("application/ld+json", json, function (success, results) {});

            var query =
                "prefix " + won.WONMSG.prefix + ": <" + won.WONMSG.baseUri + "> \n" +
                "SELECT ?msg ?receiver ?receiverNeed ?receiverNode ?sender ?senderNeed ?senderNode ?messageType ?refersTo ?responseState where {" +
                "?msg " + won.WONMSG.hasMessageTypePropertyCompacted + " ?messageType." +
                " OPTIONAL { " +
                "?msg " + won.WONMSG.hasReceiverCompacted + " ?receiver ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.hasReceiverNeedCompacted + " ?receiverNeed ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.hasReceiverNodeCompacted + " ?receiverNode ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.hasSenderCompacted + " ?sender ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.hasSenderNeedCompacted + " ?senderNeed ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.hasSenderNodeCompacted + " ?senderNode ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.refersToCompacted + " ?refersTo ." +
                "} OPTIONAL { " +
                "?msg " + won.WONMSG.hasResponseStatePropertyCompacted + " ?responseState ." +
                "}" +
                "}";
            store.execute(query, function (success, results) {
                if (!success) {
                    return;
                }
                //use only first result!
                if (results.length == 0) {
                    return;
                }
                if (results.length > 1) {
                    console.log("more than 1 solution found for message property query!");
                }
                var result = results[0];
                eventData.messageType = getSafeValue(result.messageType);
                eventData.receiverURI = getSafeValue(result.receiver);
                eventData.receiverNeedURI = getSafeValue(result.receiverNeed);
                eventData.receiverNodeURI = getSafeValue(result.receiverNode);
                eventData.senderURI = getSafeValue(result.sender);
                eventData.senderNeedURI = getSafeValue(result.senderNeed);
                eventData.senderNodeURI = getSafeValue(result.senderNode);
                eventData.refersToURI = getSafeValue(result.refersTo);
                eventData.responseState = getSafeValue(result.responseState);
                eventData.eventURI = getSafeValue(result.msg);
                console.log("done copying the data to the event object, returning the result");
            });
        });
        return eventData;
    }

    enqueueMessage = function(msg) {
        if (isConnected()) {
            console.log("sending message instead of enqueueing");
            //just to be sure, test if the connection is established now and send instead of enqueue
            privateData.socket.send(msg);
        } else {
            console.log("socket not connected yet, enqueueing");
            privateData.pendingOutMessages.push(msg);
        }
    }
    attachListenersToSocket = function(newsocket){
        //TODO: register listeners for incoming messages

        newsocket.onopen = function () {
            $rootScope.$apply(function() {
                console.log("SockJS connection has been established!");
                var i = 0;
                while (privateData.pendingOutMessages.length > 0) {
                    var msg = privateData.pendingOutMessages.shift();
                    console.log("sending pending message no " + (++i));
                    privateData.socket.send(msg);
                }
            });
        }

        newsocket.onmessage = function (msg) {
            $rootScope.$apply(function() {
                //first, run callbacks registered inside the service:
                var jsonld = JSON.parse(msg.data);
                console.log("SockJS message received!")
                var event = getEventData(jsonld);
                //call all registered callbacks
                console.log("starting to process callbacks");
                var callbacksToKeep = [];
                for (var i = 0; i < privateData.callbacks.length; i++) {
                    console.log("processing messaging callback " + (i + 1) + " of " + privateData.callbacks.length);
                    try {
                        var myJsonld = JSON.parse(JSON.stringify(jsonld));
                        var myEvent = JSON.parse(JSON.stringify(event));
                        var callback = privateData.callbacks[i];
                        callback.handleMessage(myEvent, myJsonld);
                        if (!callback.shouldUnregister(myEvent, myJsonld)) {
                            callbacksToKeep.push(callback);
                        }
                    } catch (e) {
                        console.log("error processing messaging callback " + i + ", ignoring");
                    }
                    console.log("done processing callback ")
                }
                privateData.callbacks = callbacksToKeep;
            });
        };

        newsocket.onclose = function () {
            console.log("SockJS connection closed");
        };
    }

    isConnected = function(){
        return privateData.socket != null && privateData.socket.readyState == SockJS.OPEN ;
    }

    isConnecting = function(){
        return privateData.socket != null && privateData.socket.readyState == SockJS.CONNECTING;
    }

    isConnectedOrConnecting = function(){
        return privateData.socket != null && (privateData.socket.readyState == SockJS.OPEN || privateData.socket.readyState == SockJS.CONNECTING) ;
    }

    isClosingOrClosed= function(){
        return privateData.socket != null && (privateData.socket.readyState == SockJS.CLOSED || privateData.socket.readyState == SockJS.CLOSING) ;
    }

    isConnecting = function(){
        return privateData.socket != null && privateData.socket.readyState == SockJS.CONNECTING;
    }
    createSocket = function() {
        var options = {debug: true};
        var url = 'http://localhost:8080/owner/msg'; //TODO: get socket URI from server through JSP
        privateData.socket = new SockJS(url, null, options);
        attachListenersToSocket(privateData.socket);
        //TODO attach additional listener that creates the correct angular events (create is msg-response, i need to code events for simply incoming)
        messageService.addMessageCallback(newMatchesCallback())
    }

    newMatchesCallback = function() {
        return new messageService.MessageCallback(function(event, msg){
            console.log("in newMatchesCallback-Action");
            switch(event.messageType) {
                case won.WONMSG.hintMessage:
                    console.log("Got a hint ", JSON.stringify(msg));
                    break;
                default:
                    console.log("Got a yet unhandled message. ", JSON.stringify(msg));

            }
        });

    }




    messageService.closeConnection = function () {
        console.log("closing Websocket via messageService.closeConnection()");
        if (privateData.socket != null && ! isClosingOrClosed()) {
            privateData.socket.close();
        }
    }

    messageService.sendMessage = function(msg) {
        var jsonMsg = JSON.stringify(msg);
        if (isConnected()) {
            privateData.socket.send(jsonMsg);
        } else {
            if (!isConnecting()) {
                createSocket();
            }
            enqueueMessage(jsonMsg);
        }
    };


    messageService.addMessageCallback = function(callback) {
        if (typeof callback.handleMessage !== 'function') {
            throw new TypeError("callback must provide function 'handleMessage(object)'");
        }
        privateData.callbacks.push(callback);
    }

    /**
     * Callback class for receiving WoN messages via the messaging service. Will be called to handle each
     * message until the unregister() function is called.
     *
     * @param action required. Callback that is called with the message as only parameter.
     *
     * Additional callbacks can be set:
     * shouldHandleTest: . Function that gets the message as only parameter and returns a boolean.
     *  action will only be executed if shouldHandleTest returns true or is omitted.
     * shouldUnregisterTest: . Function that gets the message as only parameter and returns a boolean.
     *  callback will be unregistered if shouldUnregisterTest returns true.
     * @constructor
     */
    messageService.MessageCallback = function MessageCallback(action){
        this.action = action;
        this.shouldUnregisterTest = function(event, msg){ return false; };
        this.shouldHandleTest = function(event, msg){ return true; };
    }

    messageService.MessageCallback.prototype = {
        constructor: messageService.MessageCallback,
        shouldHandle: function(event, msg) {
            var ret = this.shouldHandleTest(event, msg);
            console.log("interested in message: " + ret)
            return ret;
        },
        performAction: function(event, msg) {
            console.log("performing action for message " + event.eventURI);
            this.action(event, msg);
        },
        shouldUnregister: function(event, msg) {
            var ret = this.shouldUnregisterTest(event, msg);
            console.log("should unregister: " + ret);
            return ret;
        },
        handleMessage: function(event, msg) {
            if (this.shouldHandle(event, msg)) {
                this.performAction(event, msg);
            }
        }
    };

    return messageService;
});
