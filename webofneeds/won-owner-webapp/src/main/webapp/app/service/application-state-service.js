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
angular.module('won.owner').factory('applicationStateService', function (linkedDataService, $rootScope) {

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
    privateData.filters[won.UNREAD.TYPE.HINT] =    { 'eventType' : won.EVENT.HINT_RECEIVED };
    privateData.filters[won.UNREAD.TYPE.MESSAGE] = { 'eventType' : won.EVENT.CONNECTION_MESSAGE_RECEIVED };
    privateData.filters[won.UNREAD.TYPE.CONNECT] = { 'eventType' : [won.EVENT.CONNECT_RECEIVED, won.EVENT.OPEN_RECEIVED] };
    privateData.filters[won.UNREAD.TYPE.CLOSE] =   { 'eventType' : won.EVENT.CLOSE_RECEIVED };

    //if we have a current need, that's its URI
    privateData.currentNeedURI = null;

    //all needs are stored in this array (in the form returned by linkedDataService.getNeed(uri)
    privateData.allNeeds = [];



    //contains all unread events
    privateData.unreadEvents = [];

    //contains 'unread objects' - events, eventcounts, needs etc. see prepareEmptyUnreadObjects() below to get an idea
    privateData.unreadObjects = {};
    //dirty flag for the unread objects.
    privateData.unreadObjectsDirty = false;

    var prepareEmptyUnreadObjects = function() {
        return {
            'all': {
                'hint': {'count': 0, 'events': []},
                'connect': {'count': 0, 'events': []},
                'message': {'count': 0, 'events': []},
                'close': {'count': 0, 'events': []}
            },
            byNeed: {
                'hint': [],
                'connect': [],
                'message': [],
                'close': []
            }
        };
    }
    privateData.unreadObjects = prepareEmptyUnreadObjects();

    //helper function: is x an array?
    var isArray = function(x){
        return Object.prototype.toString.call( x ) === '[object Array]';
    }

    /**
     * filters the array of js objects by testing if all the dictionary values found in data are
     * present in the current array object
     * @param array
     * @param data  containing key/value pairs. if the value is an array, one of the elements must
     * match the value found in the array element for the key.
     */
    var myfilter = function(array, data) {
        var ret = [];
        outer:
        for (var i = 0; i < array.length; i++) {
            for (field in data) {
                if (array[i][field] === 'undefined'){
                    continue;
                }
                if (isArray(data[field])){
                    for (var j = 0; j < data[field].length; j++){
                        if (array[i][field] === data[field][j] ){
                            ret.push(array[i]);
                            continue outer;
                        }
                    }
                }
                if (array[i][field] === data[field]){
                    ret.push(array[i]);
                    continue outer;
                }
            }
        }
        return ret;
    };

    /**
     * Reconstructs the unreadObjects structure. May be expensive.
     */
    var updateUnreadObjects = function(){
        var newObjects = prepareEmptyUnreadObjects(); //array used in the GUI to show message counts
        for (key in privateData.filters) {
            var filtered = myfilter(privateData.unreadEvents, privateData.filters[key]);
            //first, count and add to 'all' group (count and list of events)
            newObjects[won.UNREAD.GROUP.ALL][key] = {
                count: filtered.length,
                events: filtered
            };
            //now, filter again and add to 'byNeed' group
            newObjects[won.UNREAD.GROUP.BYNEED][key] = [];
            //now filter again by need URI
            for (var i = 0; i < privateData.allNeeds.length; i++) {
                var currentNeed = privateData.allNeeds[i];
                var needFilter = {}
                needFilter['hasReceiverNeed'] = currentNeed.uri;
                var filteredAgain = myfilter(filtered, needFilter);
                newObjects[won.UNREAD.GROUP.BYNEED][key].push(
                    {need: currentNeed,
                     count: filteredAgain.length,
                     events: filteredAgain}
                );
                //TODO: do we need the same for connection URIs?
            }
        }
        privateData.unreadObjects = newObjects;
        privateData.unreadObjectsDirty = false;
    };


    /**
     * Adds a new, unread event and flags the unreadObjects structure as dirty.
     * @param event
     */
    applicationStateService.addEvent=function (event){
        privateData.unreadEvents.push(event);
        privateData.unreadObjectsDirty = true;
    }

    /**
     * Removes an event - marking it as 'read', and flags the unreadObjects structure as dirty.
     * @param event
     */
    applicationStateService.removeEvent=function (event){
        for(var i = 0; i < privateData.unreadEvents.length; i++){
            if (privateData.unreadEvents[i].uri === event.uri){
                privateData.unreadEvents.splice(i,1);
                i--;
            }
        }
        privateData.unreadObjectsDirty = true;
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
        privateData.allNeeds.push(need);
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


    return applicationStateService;
});