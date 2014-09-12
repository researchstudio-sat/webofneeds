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
 * Created by LEIH-NB on 09.09.2014.
 */

angular.module('won.owner').factory('applicationStateService', function (linkedDataService, $filter) {
    var currentNeedURI = null;
    var allNeeds = [];
    var applicationStateService = {}
    var readEvents = [];
    var allNeedsWithUnreadEvents = [];

    applicationStateService.getAllNeedsWithUnreadEvents= function(){
        return allNeedsWithUnreadEvents;
    }
    applicationStateService.getReadEvents= function(){
        return readEvents;
    }
    applicationStateService.addReadEvent=function (eventURI){
        readEvents.push(eventURI);
    }
    applicationStateService.setCurrentNeedURI = function(needURI){
        currentNeedURI = needURI;
    }

    applicationStateService.getCurrentNeedURI = function(){

        return currentNeedURI;
    }

    applicationStateService.getAllNeeds = function(){
        return allNeeds;
    }

    applicationStateService.addNeed = function(need){
        allNeeds.push(need);
    }
    applicationStateService.fetchUnreadEventsForAllNeeds= function(){
            var needsWithUnreadEvents=[];

            var allNeeds = this.getAllNeeds();
            for(var i = 0; allNeeds.length >i; i++){
                var unread = [];
                var matchFilterType = [won.WON.HintCompacted];
                var conversationFilterType = [won.WON.OwnerMessageCompacted, won.WON.PartnerMessageCompacted];
                var requestFilterType =  [won.WON.OwnerOpenCompacted, won.WON.PartnerOpenCompacted];
                //  var events = linkedDataService.getAllEvents($scope.allNeeds[i].needURI);
                var need = allNeeds[i];
                //TODO: events shall be retrieved with linked data service
                var events =  [
                    { eventType: won.WON.HintCompacted , eventURI : "http://event1.com", title:'Car sharing to Prague', timeStamp: new Date('2014-08-25 14:30'), msg:'This is a Hint '},
                    { eventType: won.WON.PartnerOpenCompacted,eventURI : "http://event2.com" ,title:'Moved recently ...', timeStamp:new Date('2014-08-20'), msg:'This is a Connection Request'}];
                for(var j = 0; events.length>j;j++){
                    if(this.getReadEvents().indexOf(events[j].eventURI)==-1)
                    {
                        unread.push(events[j]);
                    }
                }
                    var matches = $filter('messageTypeFilter')(unread,matchFilterType);
                    var conversations = $filter('messageTypeFilter')(unread, conversationFilterType);
                    var requests = $filter('messageTypeFilter')(unread, requestFilterType)
                need.matches = matches;
                need.conversations = conversations;
                need.requests = requests;
                needsWithUnreadEvents.push(need);
            }
            //return unread;
           allNeedsWithUnreadEvents = needsWithUnreadEvents;
        return allNeedsWithUnreadEvents;
    }


    return applicationStateService;
});