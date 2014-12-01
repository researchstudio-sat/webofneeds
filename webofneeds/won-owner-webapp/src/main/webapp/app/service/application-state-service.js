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
 //TODO this object/service imho tries to do three separate things (which conflicts with the single responsibility principle):
 // * managing events (as an event-queue)
 // * managing information about the currently viewed need (that's what we've got a browser/routing for)
 // * holding information about the loaded needs/drafts (we got a user object elsewhere for that)
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
    privateData.filters[won.UNREAD.TYPE.MESSAGE] = { 'eventType' : [won.EVENT.CONNECTION_MESSAGE_RECEIVED,won.EVENT.OPEN_RECEIVED]};
    privateData.filters[won.UNREAD.TYPE.CONNECT] = { 'eventType' : won.EVENT.CONNECT_RECEIVED };
    privateData.filters[won.UNREAD.TYPE.CLOSE] =   { 'eventType' : won.EVENT.CLOSE_RECEIVED };

    /**
    * Empties the current lists of needs, drafts and events (call e.g. when logging out so no data's left)
    * (or creates those if they didn't exist before for some reason)
    //TODO initialize these from the server here if the session's still "logged in"?
     * Resets the services internal data-structure to actual values.
     * @param needURIs any needs from previous sessions, fetched from the server
     */
    applicationStateService.reset = function() {
        //if we have a current need, that's its URI
        privateData.currentNeedURI = null;
        privateData.currentEvent = null;
        //all needs are stored in this array (in the form returned by linkedDataService.getNeed(uri)
        utilService.removeAllProperties(privateData.allNeeds);
        utilService.removeAllProperties(privateData.allDrafts);

        utilService.removeAllProperties(privateData.unreadEventsByNeedByType);

        applicationStateService.resetUnreadEventsByTypeByNeed();
        privateData.lastEventOfEachConnectionOfCurrentNeed.splice(0, privateData.lastEventOfEachConnectionOfCurrentNeed.length);
    }
    applicationStateService.init = function(){
        privateData.allNeeds = {}
        privateData.allDrafts = {};

        privateData.allClosed = [
            {type:'Want', title:'Playing soccer together', datetime: new Date('2014-08-23')},
            {type:'Change', title:'Looking for a flatscreen TV', datetime: new Date('2014-08-20')},
            {type:'Together', title:'Go to the cinema', datetime: new Date('2014-07-14')}
        ];
        privateData.currentNeedURI = null;
        privateData.currentEvent = null;

        privateData.unreadEventsByNeedByType = {};
        privateData.unreadEventsByTypeByNeed = {
            'hint': {count: 0, timestamp: new Date().getTime() },
            'connect': {count: 0, timestamp: new Date().getTime()},
            'message': {count: 0, timestamp: new Date().getTime()},
            'close': {count: 0, timestamp: new Date().getTime()},
            'created': {count: 0, timestamp: new Date().getTime()}
        }
        privateData.lastEventOfEachConnectionOfCurrentNeed = [];
    }
    applicationStateService.resetUnreadEventsByTypeByNeed= function(){
        Object.keys(privateData.unreadEventsByTypeByNeed).forEach(function(element, index, array){
            privateData.unreadEventsByTypeByNeed[element].count = 0;
            privateData.unreadEventsByTypeByNeed.timestamp = new Date().getTime();
        })
    }
    applicationStateService.init();
    applicationStateService.setCurrentConnectionURI= function (connectionURI){
        privateData.currentEvent = connectionURI;
    }
    applicationStateService.getCurrentConnectionURI = function(){
        return privateData.currentEvent;
    }
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
    /**
     *  reset count of a specific event type for a need
     * @param needURI
     * @param eventType
     */
    applicationStateService.resetUnreadEventsForByNeedByType = function (needURI, eventType){
        privateData.unreadEventsByNeedByType[needURI][eventType].count=0;
        privateData.unreadEventsByNeedByType[needURI][eventType].events=[];
        privateData.unreadEventsByNeedByType[needURI][eventType].timestamp = new Date().getTime();
    }
    /**
     * reset all counts of a specific event type for all needs
     * @param eventType
     */
    applicationStateService.setEventsAsReadForByNeedByType = function(eventType){
        Object.keys(privateData.unreadEventsByNeedByType).forEach(function(element, index, array){
            applicationStateService.resetUnreadEventsForByNeedByType(element, eventType);
        } )
    }

    applicationStateService.setEventsAsReadForType=function(eventType){
        privateData.unreadEventsByTypeByNeed[eventType].count = 0;
        privateData.unreadEventsByTypeByNeed[eventType].timestamp = new Date().getTime();

    }
    var createOrUpdateUnreadEntry = function(needURI, eventData, unreadEntry){

        if(unreadEntry == null || typeof unreadEntry === 'undefined'){
            unreadEntry = {"events" : []};
            //unreadEntry.events = [];
            unreadEntry.count = 0;
        }
        unreadEntry.events.push(eventData);
        unreadEntry.timestamp=eventData.timestamp;
        unreadEntry.need = privateData.allNeeds[needURI];
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
            case won.EVENT.CONNECT_SENT: unreadEventType = won.UNREAD.TYPE.CONNECT;
                break;
            case won.EVENT.OPEN_RECEIVED:unreadEventType = won.UNREAD.TYPE.MESSAGE;
                break;
            case won.EVENT.OPEN_SENT:unreadEventType = won.UNREAD.TYPE.MESSAGE;
                break;
            //  case won.Event.Message_Rece_RECEIVED: privateData.unreadEventsByNeedByType[needURI].hint.push(eventData);
            case won.EVENT.CLOSE_RECEIVED: unreadEventType = won.UNREAD.TYPE.CLOSE;
                break;
            case won.EVENT.NEED_CREATED: unreadEventType = won.UNREAD.TYPE.CREATED;
                break;
            case won.EVENT.CONNECTION_MESSAGE_RECEIVED: unreadEventType = won.UNREAD.TYPE.MESSAGE;
                break;
            case won.EVENT.CONNECTION_MESSAGE_SENT: unreadEventType = won.UNREAD.TYPE.MESSAGE;
                break;
            // case won.Event.HINT_RECEIVED: privateData.unreadEventsByNeedByType[needURI].hint.push(eventData);
        }
        return unreadEventType;
    }

    applicationStateService.getUnreadEventTypeFromHasMessageType = function(hasMessageType){
        var unreadEventType = null;
        switch (hasMessageType){
            case won.WONMSG.hintMessage:unreadEventType = won.UNREAD.TYPE.HINT;
                break;
            case won.WONMSG.connectMessage: unreadEventType = won.UNREAD.TYPE.CONNECT;
                break;
            case won.WONMSG.connectionMessage: unreadEventType = won.UNREAD.TYPE.MESSAGE;
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

        if(eventType != undefined && eventData != undefined ) {
            var now = new Date().getTime();
            createOrUpdateUnreadEntry(needURI, eventData, privateData.unreadEventsByNeedByType[needURI][getUnreadEventType(eventType)]);
            privateData.unreadEventsByNeedByType[needURI][getUnreadEventType(eventType)].timestamp = now;
            privateData.unreadEventsByNeedByType[needURI].timestamp = now;
            privateData.unreadEventsByNeedByType[needURI].count++;
        }

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
     * used for updating unread event notifications.
     * e.g. if connect_received event is received, but there's still a hint event of the affected need of the incoming event is still in the unread event container,
     * it should be removed.
      * @param eventType
     * @returns {string}
     */
    applicationStateService.getPreviousUnreadState = function(eventType){
        switch(eventType){
            case won.EVENT.CONNECT_RECEIVED:
                return won.UNREAD.TYPE.HINT;
            case won.EVENT.CONNECTION_MESSAGE_RECEIVED:
                return won.UNREAD.TYPE.CONNECT;
        }
    }
    /**
     * Removes an event - marking it as 'read', and flags the unreadObjects structure as dirty.
     * @param event
     */
    //TODO: handling unread event and notifications shall be refactored. maybe with a state machine?
    applicationStateService.decreaseUnreadEventCountByTypeByNeed = function(unreadEventType){
        if(privateData.unreadEventsByTypeByNeed[unreadEventType].count>0){
            privateData.unreadEventsByTypeByNeed[unreadEventType].count--;
        }
    }
    /**
     * unread event handling method.
     * if there's still a unread event of the previous "state" in the unread event containers, when the a new event is received,
     * it shall be removed.
     * the previous unread event might already have been removed, if the user has already clicked on the event in the private link page.
     *
     * @param eventData
     */
    applicationStateService.removePreviousUnreadEventIfExists = function(eventData){
        var receiverNeed = eventData.hasReceiverNeed;
        var receiverConnection = eventData.hasReceiver;
        applicationStateService.removeEvent(applicationStateService.getPreviousUnreadState(eventData.eventType),receiverConnection);
    }

    applicationStateService.removeEvent=function (unreadEventType, connectionURI){
        console.log("removing Event");
        if(unreadEventType == undefined){
            unreadEventType = applicationStateService.getUnreadEventTypeFromHasMessageType(event.event.hasMessageType);
        }
        var allEventsOfTypeOfNeed=privateData.unreadEventsByNeedByType[privateData.currentNeedURI][unreadEventType].events;
        for(var i =0; i<allEventsOfTypeOfNeed.length;i++){
            if(allEventsOfTypeOfNeed[i].hasReceiver == connectionURI){
                allEventsOfTypeOfNeed.splice(i,1);
                privateData.unreadEventsByNeedByType[privateData.currentNeedURI][unreadEventType].count--;
                privateData.unreadEventsByTypeByNeed[unreadEventType].timestamp = new Date();
                applicationStateService.decreaseUnreadEventCountByTypeByNeed(unreadEventType);
            }
        }

        privateData.unreadEventsByNeedByType[privateData.currentNeedURI][unreadEventType].timestamp = new Date().getTime();
    }
    /*
    applicationStateService.removeEvent=function (event){
        console.log("removing Event");
        var allEventsOfTypeOfNeed=privateData.unreadEventsByNeedByType[privateData.currentNeedURI][getUnreadEventTypeFromHasMessageType(event.event.hasMessageType)].events;
        for(var i =0; i<allEventsOfTypeOfNeed.length;i++){
            if(allEventsOfTypeOfNeed[i].uri == event.event.uri){
                allEventsOfTypeOfNeed.splice(i,1);
                privateData.unreadEventsByNeedByType[privateData.currentNeedURI][getUnreadEventTypeFromHasMessageType(event.event.hasMessageType)].count--;
                privateData.unreadEventsByTypeByNeed[getUnreadEventTypeFromHasMessageType(event.event.hasMessageType)].timestamp = new Date();
                applicationStateService.decreaseUnreadEventCountByTypeByNeed(getUnreadEventTypeFromHasMessageType(event.event.hasMessageType))
            }
        }

        privateData.unreadEventsByNeedByType[privateData.currentNeedURI][getUnreadEventTypeFromHasMessageType(event.event.hasMessageType)].timestamp = new Date().getTime();
    }
                  */
    /**
     * Sets the current need URI.
     * @param needURI
     */
    applicationStateService.setCurrentNeedURI = function(needURI){ //TODO this should be done via routing
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
     * Gets the draft by its URI.
     * @param draftURI
     * @returns draft
     */
    applicationStateService.getDraft = function(draftURI){
        return privateData.allDrafts[draftURI];
    }

    /**
     * Removes the draft by its URI.
     * @param draftURI
     */
    applicationStateService.removeDraft = function(draftURI){
        delete privateData.allDrafts[draftURI];
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
    applicationStateService.getAllDrafts = function(){
        return privateData.allDrafts;
    }
    applicationStateService.getAllClosed = function(){
        return privateData.allClosed;
    }
    /**
     * Adds a need.
     * @param need
     */
    applicationStateService.addNeed = function(need){
        updateUnreadEventsByNeedByType(need.uri);
        privateData.allNeeds[need.uri] = need;
    }
    applicationStateService.addDraft = function(draft){
        var draftLd = JSON.parse(draft.draft);
        var draftBuilderObject = new window.won.DraftBuilder(draftLd).setContext();
        var menuposition = draftBuilderObject.getCurrentMenuposition();
        var currentStep = draftBuilderObject.getCurrentStep();
        var timestamp = draftBuilderObject.getLastSavedTimestamp();
        var draftObj = draftBuilderObject.getDraftObject();
        var draftObjWithMetaInfo = draftObj;
        draftObjWithMetaInfo.uri = draft.draftURI;
        draftObjWithMetaInfo.meta = {}
        draftObjWithMetaInfo.meta.uri = draft.draftURI;
        draftObjWithMetaInfo.meta.menuposition = menuposition;
        draftObjWithMetaInfo.meta.currentStep = currentStep;
        draftObjWithMetaInfo.meta.timestamp = timestamp;
        privateData.allDrafts[draft.draftURI] = draftObjWithMetaInfo;

    }
    applicationStateService.addNeeds = function(needs){
        var needURIPromises = [];
        for(var i = 0;i<needs.data.length;i++){
            var needURI = needs.data[i];
            needURIPromises.push(linkedDataService.getNeed(needURI).then(
                function(need){
                   applicationStateService.addNeed(need)
                   return need;
                })
            )
        }
        $q.all(needURIPromises);
    }
    applicationStateService.addDrafts = function(drafts){
        for(var i = 0; i<drafts.data.length;i++){
            applicationStateService.addDraft(drafts.data[i]);
        }
    }
    applicationStateService.getAllNeedsCount = function(){
        return utilService.getKeySize(privateData.allNeeds);
    }
    applicationStateService.getAllDraftsCount = function(){
        return utilService.getKeySize(privateData.allDrafts);
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