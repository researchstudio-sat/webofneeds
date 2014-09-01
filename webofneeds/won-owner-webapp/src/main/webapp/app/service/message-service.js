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

    //currently registered callbacks
    var callbacks = [];

    //array holding messages waiting to be sent. The sendMessage function never
    //blocks, but when the socket isn't connected, the service will try to connect
    //and send the message later.
    var pendingOutMessages = [];

    attachListenersToSocket = function(newsocket){
        newsocket.onopen = function () {
            console.log("SockJS connection has been established!")
            var i = 0;
            while (pendingOutMessages.length > 0){
                var msg = pendingOutMessages.pop();
                console.log("sending pending message no " + (++i));
                socket.send(msg);
            }
        }

        newsocket.onmessage = function (msg) {
            //first, run callbacks registered inside the service:
            console.log("SockJS message received!")
            for(var i = 0; i < callbacks.length; i++) {
                callbacks[i].handleMessage(msg);
            }
            console.log("Received data: "+msg);
            $rootScope.$apply(function () {
                $rootScope.$broadcast("WonMessageReceived", msg);
            });
        };

        newsocket.onclose = function () {
            console.log("SockJS connection closed");
        };
    }

    isConnected = function(){
        return socket != null && socket.readyState == SockJS.OPEN ;
    }

    isConnecting = function(){
        return socket != null && socket.readyState == SockJS.CONNECTING;
    }

    isConnectedOrConnecting = function(){
        return socket != null && (socket.readyState == SockJS.OPEN || socket.readyState == SockJS.CONNECTING) ;
    }

    isClosingOrClosed= function(){
        return socket != null && (socket.readyState == SockJS.CLOSED || socket.readyState == SockJS.CLOSING) ;
    }

    isConnecting = function(){
        return socket != null && socket.readyState == SockJS.CONNECTING;
    }

    createSocket = function() {
        var options = {debug: true};
        var url = 'http://localhost:8080/owner/msg'; //TODO: get socket URI from server through JSP
        socket = new SockJS(url, null, options);
        attachListenersToSocket(socket);
    }

    getSocket = function(){
        if (isConnectedOrConnecting()) {
            return socket;
        }
        return createSocket();
    }

    enqueueMessage = function(msg) {
        if (isConnected()) {
            //just to be sure, test if the connection is established now and send instead of enqueue
            socket.send(msg);
        } else {
            pendingOutMessages.push(msg);
        }
    }

    var socket = getSocket();

    messageService.closeConnection = function () {
        if (socket != null && ! isClosingOrClosed()) {
            socket.close();
        }
    }

    messageService.sendMessage = function(msg) {
        var jsonMsg = JSON.stringify(msg);
        if (isConnected()) {
            getSocket().send(jsonMsg);
        } else {
            createSocket();
            enqueueMessage(jsonMsg);
        }
    };


    messageService.addMessageCallback = function(callback) {
        if (typeof callback.handleMessage !== 'function') {
            throw new TypeError("callback must provide function 'handleMessage(object)'");
        }
        callbacks.push(callback);
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
     *  callback will be unregistered if shouldUnregisterTest returns true or is omitted.
     * @constructor
     */
    messageService.MessageCallback = function MessageCallback(action){
        this.action = action;
        this.shouldUnregisterTest = function(msg){ return false; };
        this.shouldHandleTest = function(msg){ return true; };
    }

    messageService.MessageCallback.prototype = {
        constructor: messageService.MessageCallback,
        shouldHandle: function(msg) {
            var ret = this.shouldHandleTest(msg);
            console.log("interested in message: " + ret)
            return ret;
        },
        performAction: function(msg) {
            console.log("performing action for message " + JSON.stringify(msg));
            this.action(msg);
        },
        shouldUnregister: function(msg) {
            var ret = this.shouldUnregisterTest(msg);
            console.log("should unregister: " + ret);
            return ret;
        },
        handleMessage: function(msg) {
            if (this.shouldHandle(msg)) {
                this.performAction(msg);
            }
            if (this.shouldUnregister(msg)){
               this.unregister();
            }
        },
        unregister: function() {
            console.log("removing message callback: " + this);
            //remove the callback
            var index = callbacks.indexOf(this);
            if (index > -1) {
                callbacks = callbacks.splice(index, 1);
            }
        }
    };

    return messageService;
});
