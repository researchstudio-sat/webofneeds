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
angular.module('won.owner').factory('messageService', function ($http, $q,$log, $rootScope, $interval, $location) {
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



    var getEventData = function(json){
        $log.debug("getting data from jsonld message");
        var eventData = {};
        //call handler if there is one - it may modify the event object
        //frame the incoming jsonld to get the data that interest us
        var frame = {
            "@context" : {
                "won":"http://purl.org/webofneeds/model#",
                "msg":"http://purl.org/webofneeds/message#"
            },
            "@type": "msg:FromOwner"
        }

        //copy data from the framed message to the event object
        var framedMessage = jsonld.frame(json, frame);

        if (framedMessage == null){
            //not FromSystem? maybe it's FromSystem?
            frame['@type'] = "msg:FromSystem";
            //copy data from the framed message to the event object
            framedMessage = jsonld.frame(json, frame);
       }

        if (framedMessage == null){
            //not FromSystem? maybe it's FromExternal?
            frame['@type'] = "msg:FromExternal";
            //copy data from the framed message to the event object
            framedMessage = jsonld.frame(json, frame);
        }

        for (key in framedMessage){
            var propName = won.getLocalName(key);
            if (propName != null && ! won.isJsonLdKeyword(propName)) {
                eventData[propName] = won.getSafeJsonLdValue(framedMessage[key]);
            }
        }
        eventData.uri = won.getSafeJsonLdValue(framedMessage);
        eventData.framedMessage = framedMessage;
        $log.debug("done copying the data to the event object, returning the result");

        return eventData;
    }




    var enqueueMessage = function(msg) {
        if (isConnected()) {
            $log.debug("sending message instead of enqueueing");
            //just to be sure, test if the connection is established now and send instead of enqueue
            privateData.socket.send(msg);
        } else {
            $log.warn("socket not connected yet, enqueueing");
            privateData.pendingOutMessages.push(msg);
        }
    }
    var attachListenersToSocket = function(newsocket){
        //TODO: register listeners for incoming messages

        newsocket.onopen = function () {
            $rootScope.$apply(function() {
                $log.debug("SockJS connection has been established!");
                var i = 0;
                while (privateData.pendingOutMessages.length > 0) {
                    var msg = privateData.pendingOutMessages.shift();
                    $log.debug("sending pending message no " + (++i));
                    privateData.socket.send(msg);
                }
            });
        }

        newsocket.onmessage = function (msg) {
            $rootScope.$apply(function() {
                //first, run callbacks registered inside the service:
                var jsonld = JSON.parse(msg.data);
                $log.debug("SockJS message received")
                var event = getEventData(jsonld);
                //call all registered callbacks
                $log.debug("SockJS message is of type " + event.hasMessageType + ", starting to process callbacks");
                for (var i = 0; i < privateData.callbacks.length; i++) {
                    $log.debug("processing messaging callback " + (i + 1) + " of " + privateData.callbacks.length);
                    try {
                        var myJsonld = JSON.parse(JSON.stringify(jsonld));
                        var myEvent = JSON.parse(JSON.stringify(event));
                        var callback = privateData.callbacks[i];
                        callback.handleMessage(myEvent, myJsonld);
                    } catch(e) {
                        $log.error("error processing messaging callback " + i + ": " + JSON.stringify(e));
                    }
                    try {
                        if (callback.shouldUnregister(myEvent, myJsonld)) {
                            //delete current callback from array
                            privateData.callbacks.splice(i,1);
                            i--;
                        }
                    } catch (e) {
                        $log.error("error while deciding whether to unregister callback " + i + ": "+ JSON.stringify(e));
                    }
                    $log.debug("done processing callback ");
                }
                $log.debug("done processing all callbacks ");
            });
        };

        newsocket.onclose = function (e) {
            $log.debug("SockJS connection closed ", e);
            //TODO: reconnect when connection is lost?
            if (e.code === 1011) { // unexpected server condition - happens when the user's session times out
                $rootScope.$apply(function () {
                    $rootScope.$broadcast(won.EVENT.WEBSOCKET_CLOSED_UNEXPECTED);
                });
            }
        };

    }

    var isConnected = function(){
        return privateData.socket != null && privateData.socket.readyState == SockJS.OPEN ;
    }

    var isConnecting = function(){
        return privateData.socket != null && privateData.socket.readyState == SockJS.CONNECTING;
    }

    var isConnectedOrConnecting = function(){
        return privateData.socket != null && (privateData.socket.readyState == SockJS.OPEN || privateData.socket.readyState == SockJS.CONNECTING) ;
    }

    var isClosingOrClosed= function(){
        return privateData.socket != null && (privateData.socket.readyState == SockJS.CLOSED || privateData.socket.readyState == SockJS.CLOSING) ;
    }

    var isConnecting = function(){
        return privateData.socket != null && privateData.socket.readyState == SockJS.CONNECTING;
    }
    var createSocket = function() {
        var options = {debug: true};
        //TODO: this depends on the owner application being deployed under the context root '/owner/'! Change!
        var url = '/owner/msg';
        privateData.socket = new SockJS(url, null, options);
        attachListenersToSocket(privateData.socket);
    }







    messageService.closeConnection = function () {
        if (privateData.socket != null && ! isClosingOrClosed()) {
            $log.debug("closing Websocket via messageService.closeConnection()");
            privateData.socket.close();
        }
    }

    messageService.reconnect = function(){
        $log.debug("reconnecting Websocket");
        messageService.closeConnection();
        createSocket();
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
        for (key in privateData.callbacks){
            if (callback.equals(privateData.callbacks[key])){
                $log.debug("prevented duplicate callback registration");
                return;
            }
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
            $log.debug("interested in message: " + ret)
            return ret;
        },
        performAction: function(event, msg) {
            $log.debug("performing action for message " + event.uri);
            this.action(event, msg);
        },
        shouldUnregister: function(event, msg) {
            var ret = this.shouldUnregisterTest(event, msg);
            $log.debug("should unregister: " + ret);
            return ret;
        },
        handleMessage: function(event, msg) {
            if (this.shouldHandle(event, msg)) {
                this.performAction(event, msg);
            }
        },
        /**
         * Equals method for testing if callbacks are identical. Before a callback
         * is registered, the callback is compared with all registerd callbacks using this
         * method. If it returns true, the callback is not added.
         * Should be overwritten by callbacks that want to avoid duplicate reactions.
         * in cases where multiple callbacks
         * @param other
         * @returns {boolean}
         */
        equals: function(other){
            return this === other;
        }

    };

    createSocket();

    return messageService;
});
