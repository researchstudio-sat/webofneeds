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
angular.module('won.owner').factory('messageService', function ($http, $q) {
    var messageService = {};
    // Keep all pending requests here until they get responses
    var callbacks ={};
    //Create a unique callback ID to map requests to responses
    var currentCallbackId = 0;
    var options = {debug: true};
    var url = 'http://localhost:8080/owner/msg';
    var socket = new SockJS(url, null, options);
  /*  var sendMessage = function () {

        var message = document.getElementById("messageText").value;

        socket.send(message);
    };         */
    socket.onopen = function () {
        console.log("connection has been established!")
    }

    socket.onmessage = function (event) {
        console.log("Received data: "+event.data);

    };

    socket.onclose = function () {
        console.log("Lost connection")
    };

    messageService.closeConnection = function () {
        socket.close;
    }

    messageService.sendMessage = function(dataset){

      /*  var message = {
            method:methodName,
            body: messageBody
        }
        if(socket==null||socket.readyState==3){
            socket.connect();
        }             */
        socket.send(JSON.stringify(dataset));

    };
    return messageService;
});