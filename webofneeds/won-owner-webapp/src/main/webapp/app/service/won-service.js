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
angular.module('won.owner').factory('wonService', function (messageService, $q) {

    wonService = {};
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
        //var messageURI = messageService.utils.getMessageURI(message);
        var callback = new messageService.MessageCallback(
            function (msg) {
                //check if the message we got (the create need response message) indicates that all went well
                console.log("got create need message response!");
                //TODO: if negative, use alternative need URI and send again
                //TODO: if positive, propagate positive response back to caller
                //TODO: fetch need data and store in local RDF store
                this.done = true;
                //WON.CreateResponse.equals(messageService.utils.getMessageType(msg)) &&
                //messageService.utils.getRefersToURIs(msg).contains(messageURI)

                //assume we can obtain a need URI and return it
                var needURI = "sadf"; //TODO: get needURI from result
                deferred.resolve(needURI);
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