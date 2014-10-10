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
 * Tracks the application state.
 * One responsibility is tracking which events were received but are still unread.
 * The main methods for this are
 * - addEvent(event): adds an event as (potentially) unread
 * - removeEvent(event) : removes the event
 * addEvent(event) must only be called once for each event.
 */
angular.module('won.owner').factory('applicationStateService', function (linkedDataService,utilService, $rootScope, $q) {

    //the service
    var applicationStateService = {}

    //private data only used inside the service
    var privateData = {};

    //filter for event types - used by the 'myfilter' method
    //the filter is a dictionary that is compared with elements of an array
    //if all values in the filter are equal to the values in the array object, the object is
    //copied to the result array.
    //if the value for a key in the filter is an array, only one of the values must match (i.e., OR semantics)
    privateData.filters = {};
    privateData.filters[won.UNREAD.TYPE.CREATED] =    { 'eventType' : won.EVENT.NEED_CREATED };
    privateData.filters[won.UNREAD.TYPE.HINT] =    { 'eventType' : won.EVENT.HINT_RECEIVED };
    privateData.filters[won.UNREAD.TYPE.MESSAGE] = { 'eventType' : won.EVENT.CONNECTION_MESSAGE_RECEIVED };
    privateData.filters[won.UNREAD.TYPE.CONNECT] = { 'eventType' : [won.EVENT.CONNECT_RECEIVED, won.EVENT.OPEN_RECEIVED] };
    privateData.filters[won.UNREAD.TYPE.CLOSE] =   { 'eventType' : won.EVENT.CLOSE_RECEIVED };

    //if we have a current need, that's its URI
    privateData.currentNeedURI = null;

    //all needs are stored in this array (in the form returned by linkedDataService.getNeed(uri)
    privateData.allNeeds = {};

    privateData.unreadEventsByNeedByType = {};

    privateData.unreadEventsByTypeByNeed = {
        'hint': {count:0, timestamp: new Date().getTime() },
        'connect': {count:0, timestamp: new Date().getTime()},
        'message': {count:0, timestamp: new Date().getTime()},
        'close': {count:0, timestamp: new Date().getTime()},
        'created': {count:0, timestamp: new Date().getTime()}
    };
    privateData.lastEventOfEachConnectionOfCurrentNeed = [];


    applicationStateService.processEventAndUpdateUnreadEventObjects = function(eventData){
        var eventType = eventData.eventType;
        var needURI = eventData.hasReceiverNeed;
        updateUnreadEventsByNeedByType(needURI, eventType, eventData);
        updateUnreadEventsByTypeByNeed(needURI, eventType, eventData);
    };

    // now just updates the counters,
    // in future might be changed to go through all events and check
    // if they are in 'unread' data structures, in which case remove them from there
    applicationStateService.setEventsAsReadForNeed = function(needURI, events){
        if(needURI in privateData.unreadEventsByNeedByType){
            for (var i = 0; i < won.UNREAD.TYPES.length; i++) {
                var eventType = won.UNREAD.TYPES[i];
            //for (var eventType in won.UNREAD.TYPES) {
                // by type
                privateData.unreadEventsByTypeByNeed[eventType].count-=privateData.unreadEventsByNeedByType[needURI][eventType].count;
                // by need
                privateData.unreadEventsByNeedByType[needURI][eventType].count = 0;
            }
        }
    }


    var createOrUpdateUnreadEntry = function(needURI, eventData, unreadEntry){

        if(unreadEntry == null || typeof unreadEntry === 'undefined'){
            unreadEntry = {};
            //unreadEntry.events = [];
            unreadEntry.count = 0;
        }
        unreadEntry.events.push(eventData);
        unreadEntry.timestamp=eventData.timeStamp;
        //unreadEntry.need = privateData.allNeeds[needURI];
        unreadEntry.count ++;
        return unreadEntry;
    };


    var getUnreadEventType = function(eventType){
        var unreadEventType = null;
        switch (eventType){
            case won.EVENT.HINT_RECEIVED:unreadEventType = won.UNREAD.TYPE.HINT;
                break;
            case won.EVENT.CONNECT_RECEIVED: unreadEventType = won.UNREAD.TYPE.CONNECT;
                break;
            case won.EVENT.OPEN_RECEIVED:unreadEventType = won.UNREAD.TYPE.CONNECT;
                break;
            //  case won.Event.Message_Rece_RECEIVED: privateData.unreadEventsByNeedByType[needURI].hint.push(eventData);
            case won.EVENT.CLOSE_RECEIVED: unreadEventType = won.UNREAD.TYPE.CLOSE;
                break;
            case won.EVENT.NEED_CREATED: unreadEventType = won.UNREAD.TYPE.CREATED;
                break;
            // case won.Event.HINT_RECEIVED: privateData.unreadEventsByNeedByType[needURI].hint.push(eventData);
        }
        return unreadEventType;
    }

    var getUnreadEventTypeFromHasMessageType = function(hasMessageType){
        var unreadEventType = null;
        switch (hasMessageType){
            case won.WONMSG.hintMessage:unreadEventType = won.UNREAD.TYPE.HINT;
                break;
            case won.WONMSG.connectMessage: unreadEventType = won.UNREAD.TYPE.CONNECT;
                break;
            case won.WONMSG.openMessage:unreadEventType = won.UNREAD.TYPE.CONNECT;
                break;
            case won.WONMSG.closeMessage: unreadEventType = won.UNREAD.TYPE.CLOSE;
                break;
            case won.WONMSG.createMessage: unreadEventType = won.UNREAD.TYPE.CREATED;
                break;
        }
        return unreadEventType;
    }


    var updateUnreadEventsByNeedByType = function (needURI, eventType,eventData){

        if(!(needURI in privateData.unreadEventsByNeedByType) ){
            var ts = new Date().getTime();
            var need = privateData.allNeeds[needURI];
            privateData.unreadEventsByNeedByType[needURI] = {
                'hint': {count:0, events: [], timestamp: ts, need: need},
                'connect': {count:0, events: [], timestamp: ts, need: need},
                'message': {count:0, events: [], timestamp: ts, need: need},
                'close': {count:0, events: [], timestamp: ts, need: need},
                'created': {count:0, events: [], timestamp: ts, need: need}
            }
            privateData.unreadEventsByNeedByType[needURI].need = need;
        }
        var now = new Date().getTime();
        createOrUpdateUnreadEntry(needURI, eventData, privateData.unreadEventsByNeedByType[needURI][getUnreadEventType(eventType)]);
        privateData.unreadEventsByNeedByType[needURI][getUnreadEventType(eventType)].timestamp = now;
        privateData.unreadEventsByNeedByType[needURI].timestamp = now;
        privateData.unreadEventsByNeedByType[needURI].count++;

    };

    var updateUnreadEventsByTypeByNeed = function (needURI,eventType, eventData){
        var unreadEventType = getUnreadEventType(eventType);
        var now = new Date().getTime();
        //var unreadEntry = createOrUpdateUnreadEntry(needURI,eventData, privateData.unreadEventsByTypeByNeed[unreadEventType][needURI]);
        //privateData.unreadEventsByTypeByNeed[unreadEventType][needURI] = unreadEntry;
        //privateData.unreadEventsByTypeByNeed[unreadEventType][needURI].timestamp = now;
        privateData.unreadEventsByTypeByNeed[unreadEventType].timestamp = now;
        privateData.unreadEventsByTypeByNeed[unreadEventType].count++;
    };

    var updatelatestEventsByNeedByConnection = function(){

    };
    applicationStateService.getUnreadEventsByNeed = function(){
        return privateData.unreadEventsByNeed;
    }
    applicationStateService.getUnreadEventsByNeedByType = function(){
        return privateData.unreadEventsByNeedByType;
    }
    applicationStateService.getUnreadEventsByType = function(){
        return privateData.unreadEventsByType;
    }
    applicationStateService.getUnreadEventsByTypeByNeed = function(){
        return privateData.unreadEventsByTypeByNeed;
    }



    /**
     * Removes an event - marking it as 'read', and flags the unreadObjects structure as dirty.
     * @param event
     */
    applicationStateService.removeEvent=function (event){
        console.log("removeEvent not yet implemented!!!");
    }

    /**
     * Sets the current need URI.
     * @param needURI
     */
    applicationStateService.setCurrentNeedURI = function(needURI){
        if (needURI != null && needURI != '' && needURI != privateData.currentNeedURI) {
            privateData.currentNeedURI = needURI;
            $rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);
        }
    }

    /**
     * Gets the current need URI.
     * @returns {null|*}
     */
    applicationStateService.getCurrentNeedURI = function(){
        return privateData.currentNeedURI;
    }

    /**
     * Gets a promise to the current need, if one is currently set. If no need is set,
     * the promise is rejected.
     */
    applicationStateService.getCurrentNeed = function(){
        var deferred = $q.defer();
        var needUri = privateData.currentNeedURI;
        if (needUri == null){
            deferred.reject("Cannot get current need: no need currently selected");
            return deferred.promise;
        }
        var need = privateData.allNeeds[needUri];
        if (need != null) {
            deferred.resolve(need)
            return deferred.promise;
        }
        linkedDataService.getNeed(needUri).then(function(need){
            deferred.resolve(need);
        }, function (reason){
            deferred.reject(reason);
        })
        return deferred.promise;
    }

    /**
     * Gets all needs.
     * @returns {Array}
     */
    applicationStateService.getAllNeeds = function(){
        return privateData.allNeeds;
    }

    /**
     * Adds a need.
     * @param need
     */
    applicationStateService.addNeed = function(need){
        privateData.allNeeds[need.uri] = need;
    }
    applicationStateService.addNeeds = function(needs){
        var needURIPromises = [];
        for(var i = 0;i<needs.data.length;i++){
            var needURI = needs.data[i];
            needURIPromises.push(linkedDataService.fetch(needURI)
                .then(function (value) {
                var deferred = $q.defer();
                linkedDataService.getNeed(needURI)
                    .then(function(need){
                        applicationStateService.addNeed(need)
                       deferred.resolve(need);
                    });
            }))
        }
        $q.all(needURIPromises);
    }
    applicationStateService.getAllNeedsCount = function(){
        return utilService.getKeySize(privateData.allNeeds);
    }
    /**
     * Fetches the unreadObjects structure, rebuilding it if it is dirty.
     * @returns
     */
    applicationStateService.getUnreadObjects = function(){
        if (privateData.unreadObjectsDirty) {
            updateUnreadObjects();
        }
        return privateData.unreadObjects;
    }

    /**
     * For the current need, fetch the latest event for each connection.
     * Returns a promise to the data. The promise is rejected if no need is currently selected.
     * @returns {Array}
     */
    applicationStateService.getLastEventOfEachConnectionOfCurrentNeed = function(){
        var deferred = $q.defer();
        if (privateData.currentNeedURI == null){
            deferred.reject("Cannot get latest events of current need: no need is currently selected");
            return deferred.promise;
        }
        linkedDataService.getLastEventOfEachConnectionOfNeed(applicationStateService.getCurrentNeedURI())
            .then(function(events){
                privateData.lastEventOfEachConnectionOfCurrentNeed = events;
                deferred.resolve(privateData.lastEventOfEachConnectionOfCurrentNeed)
            }, function(reason){
                deferred.reject(reason);
            });
        return deferred.promise;
    }


    return applicationStateService;
});