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


    getEventData = function(json){
        console.log("getting data from jsonld message");
        var eventData = {};
        //call handler if there is one - it may modify the event object
        //frame the incoming jsonld to get the data that interest us
        var frame = {"@context" : {
            "won":"http://purl.org/webofneeds/model#",
            "msg":"http://purl.org/webofneeds/message#" //message is the default vocabulary
        },
            "msg:hasMessageType": { }
        };
        //copy data from the framed message to the event object
        var framedMessage = jsonld.frame(json, frame);

        eventData.messageType = won.getSafeURI(framedMessage[won.WONMSG.hasMessageTypeCompacted]);
        eventData.receiverURI = won.getSafeURI(framedMessage[won.WONMSG.hasReceiverCompacted]);
        eventData.receiverNeedURI = won.getSafeURI(framedMessage[won.WONMSG.hasReceiverNeedCompacted]);
        eventData.receiverNodeURI = won.getSafeURI(framedMessage[won.WONMSG.hasReceiverNodeCompacted]);
        eventData.senderURI = won.getSafeURI(framedMessage[won.WONMSG.hasSenderCompacted]);
        eventData.senderNeedURI = won.getSafeURI(framedMessage[won.WONMSG.hasSenderNeedCompacted]);
        eventData.senderNodeURI = won.getSafeURI(framedMessage[won.WONMSG.hasSenderNodeCompacted]);
        eventData.refersToURI = won.getSafeURI(framedMessage[won.WONMSG.refersToCompacted]);
        eventData.responseState = won.getSafeURI(framedMessage[won.WONMSG.hasResponseStateCompacted]);
        eventData.eventURI = won.getSafeURI(framedMessage);
        eventData.framedMessage = framedMessage;
        console.log("done copying the data to the event object, returning the result");

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
                console.log("SockJS message received")
                var event = getEventData(jsonld);
                //call all registered callbacks
                console.log("SockJS message is of type " + event.messageType + ", starting to process callbacks");
                for (var i = 0; i < privateData.callbacks.length; i++) {
                    console.log("processing messaging callback " + (i + 1) + " of " + privateData.callbacks.length);
                    try {
                        var myJsonld = JSON.parse(JSON.stringify(jsonld));
                        var myEvent = JSON.parse(JSON.stringify(event));
                        var callback = privateData.callbacks[i];
                        callback.handleMessage(myEvent, myJsonld);
                    } catch(e) {
                        console.log("error processing messaging callback " + i + ": " + JSON.stringify(e));
                    }
                    try {
                        if (callback.shouldUnregister(myEvent, myJsonld)) {
                            //delete current callback from array
                            privateData.callbacks.splice(i,1);
                            i--;
                        }
                    } catch (e) {
                        console.log("error while deciding whether to unregister callback " + i + ": "+ JSON.stringify(e));
                    }
                    console.log("done processing callback ");
                }
                console.log("done processing all callbacks ");
            });
        };

        newsocket.onclose = function () {
            console.log("SockJS connection closed");
            //TODO: reconnect when connection is lost
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
        var url = 'http://localhost:8080/owner/msg'; //TODO: get socket URI from server
        privateData.socket = new SockJS(url, null, options);
        attachListenersToSocket(privateData.socket);
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
