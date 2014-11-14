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
 * Created by LEIH-NB on 12.09.2014.
 */
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

angular.module('won.owner').controller("MainCtrl", function($scope,$location, applicationStateService, applicationControlService, $rootScope, $log, messageService, userService) {
    //we use the messageService dependency in order to force websocket creation from the very beginning
    $scope.selectedType = -1;
    $scope.applicationStateService = applicationStateService;
    $scope.unreadEventsByNeedByType = applicationStateService.getUnreadEventsByNeedByType();
    $scope.unreadEventsByTypeByNeed = applicationStateService.getUnreadEventsByTypeByNeed();
    $scope.allDrafts = applicationStateService.getAllDrafts();
    //TODO; reafactor this. move it to applicationStateService. it is used by private link controller
    //allow acces to service methods from angular expressions:
    $scope.openNeedDetailView = applicationControlService.openNeedDetailView;
    $scope.getTypePicURI = applicationControlService.getTypePicURI;
    $scope.currentNeed = {};
    $scope.lastEventOfEachConnectionOfCurrentNeed = [];
    $scope.eventCommState = {};
    var reloadCurrentNeedDataIfNecessary = function(uriOfChangeNeed){
        var currentNeedURI = applicationStateService.getCurrentNeedURI()
        if (currentNeedURI == null ) return; //can't update: no need selected
        if (uriOfChangeNeed == null || currentNeedURI === uriOfChangeNeed){
            reloadCurrentNeedData();
        }
    }

    $scope.goHome = function(){
        $location.path("/home");
    }

    var reloadCurrentNeedData = function(){
        applicationStateService.getCurrentNeed()
            .then(function (need) {
                $scope.currentNeed = need;
            });
        applicationStateService.getLastEventOfEachConnectionOfCurrentNeed()
            .then(function (events) {
                $scope.lastEventOfEachConnectionOfCurrentNeed = events;
            });
    }


    var addEventAsUnreadEvent = function(eventData){
        applicationStateService.processEventAndUpdateUnreadEventObjects(eventData);
        $scope.unreadEventsByNeedByType = applicationStateService.getUnreadEventsByNeedByType();
        $scope.unreadEventsByTypeByNeed = applicationStateService.getUnreadEventsByTypeByNeed();
    }

    $scope.$on(won.EVENT.HINT_RECEIVED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasReceiverNeed);
    });
    $scope.$on(won.EVENT.NEED_CREATED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasReceiverNeed);
    });
    $scope.$on(won.EVENT.CONNECT_RECEIVED, function(ngEvent, eventData) {
        $scope.checkIfMessageViewIsOpen(eventData);
        //unread events of previous event state, in case of "connect received" it's 'hint' unread events.
        applicationStateService.removePreviousUnreadEventIfExists(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasReceiverNeed);
    });
    $scope.$on(won.EVENT.CONNECT_SENT, function(ngEvent, eventData) {
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
       // addEventAsUnreadEvent(eventData);
        reloadCurrentNeedDataIfNecessary(eventData.hasSenderNeed);
        $scope.eventCommState[eventData.uri] = eventData.commState;
      // $scope.changeLastEventOfConnection(connectionURI, eventData);
    });
    $scope.$on(won.EVENT.NO_CONNECTION, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasSenderNeed);
        $scope.eventCommState[eventData.uri] = eventData.commState;
        // $scope.changeLastEventOfConnection(connectionURI, eventData);
    });
    $scope.$on(won.EVENT.OPEN_SENT, function(ngEvent, eventData) {
        //addEventAsUnreadEvent(eventData);
        //applicationStateService.removeEvent(eventData);
        reloadCurrentNeedData();
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
    });
    $scope.$on(won.EVENT.OPEN_RECEIVED, function(ngEvent, eventData) {
        //a receiving event is not a unread event if current need is equal to receiver need
        $scope.checkIfMessageViewIsOpen(eventData);

        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasReceiverNeed);
    });

    $scope.$on(won.EVENT.CLOSE_RECEIVED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasReceiverNeed);
    });

    $scope.$on(won.EVENT.CLOSE_SENT, function(ngEvent, eventData) {
        //removeEventFromUnreadAndUpdateUnreadObjects(eventData);
        //applicationStateService.removeEvent(eventData);
        reloadCurrentNeedData();
    });
    $scope.checkIfMessageViewIsOpen = function(eventData){

            if(eventData.hasReceiver!=applicationStateService.getCurrentConnectionURI()){
                addEventAsUnreadEvent(eventData);
            }

    }
    $scope.$on(won.EVENT.CONNECTION_MESSAGE_RECEIVED, function(ngEvent, eventData) {
        $scope.checkIfMessageViewIsOpen(eventData);

        applicationStateService.removePreviousUnreadEventIfExists(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasReceiverNeed);
    });
    $scope.$on(won.EVENT.CONNECTION_MESSAGE_SENT, function(ngEvent, eventData) {
        //addEventAsUnreadEvent(eventData);
        //for now, just update the current need data. Later, we can alter just the entry for
        // the one connection we are processing the event for.
        reloadCurrentNeedDataIfNecessary(eventData.hasSenderNeed);
    });


    $scope.$on(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED, function(event){
        reloadCurrentNeedData();
    });

    $scope.$on(won.EVENT.USER_SIGNED_IN, function(event){
       messageService.reconnect();
       userService.fetchPostsAndDrafts();
        applicationStateService.reset();
    });
    $scope.$on('RenderFinishedEvent', function(event){
       console.log("render finished event") ;
    });
    $scope.$on(won.EVENT.USER_SIGNED_OUT, function(event){
        messageService.reconnect();
    });

});