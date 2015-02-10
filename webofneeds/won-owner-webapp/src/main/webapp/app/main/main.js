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

angular.module('won.owner').controller("MainCtrl", function($scope,$location, applicationStateService, applicationControlService, $rootScope, $log, messageService, wonService, userService) {
    //we use the messageService dependency in order to force websocket creation from the very beginning
    //we use the wonService dependency because it initializes messageService's callback (addMessageCallback)
    $rootScope.$on("$routeChangeSuccess", function(event, currentRoute, previousRoute) {
        $rootScope.title = currentRoute.title;
    });
    $scope.selectedType = -1;
    $scope.applicationStateService = applicationStateService;
    $scope.unreadEventsByNeedByType = applicationStateService.getUnreadEventsByNeedByType();
    $scope.unreadEventsByTypeByNeed = applicationStateService.getUnreadEventsByTypeByNeed();
    $scope.allDrafts = applicationStateService.getAllDrafts();
    $scope.allNeeds = applicationStateService.getAllNeeds();
    //TODO; reafactor this. move it to applicationStateService. it is used by private link controller
    //allow acces to service methods from angular expressions:
    $scope.openNeedDetailView = applicationControlService.openNeedDetailView;
    $scope.getTypePicURI = applicationControlService.getTypePicURI;
    $scope.clickOnGetStarted = applicationControlService.goHome;
    $scope.clickOnWon = applicationControlService.goHome;
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

    var reloadCurrentNeedData = function(){
        applicationStateService.getCurrentNeed()
            .then(
            function success(need) {
                $scope.currentNeed = need;
            },
            function error(respond) {
                // it is expected in case the applicationstate
                // gets reset and there is no current need
                $scope.currentNeed = {};
            });
        applicationStateService.getLastEventOfEachConnectionOfCurrentNeed()
            .then(
            function success(events) {
                $scope.lastEventOfEachConnectionOfCurrentNeed = events;
            },function error(events) {
                $scope.lastEventOfEachConnectionOfCurrentNeed = [];
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

    // This is moved to another function that is a promise function  
//    $scope.$on(won.EVENT.USER_SIGNED_IN, function(event){
//       messageService.reconnect();
//        applicationStateService.reset();
//        userService.fetchPostsAndDrafts();
//    });
    $scope.$on('RenderFinishedEvent', function(event){
        $log.debug("render finished event") ;
    });

    // This is moved to another function that is a promise function  
//    $scope.$on(won.EVENT.USER_SIGNED_OUT, function(event){
//        messageService.reconnect();
//    });
    
    $scope.$on(won.EVENT.WON_SEARCH_RECEIVED,function(ngEvent, event){
       $log.debug("search received");
    });

    // This is probably a temporary solution for socket disconnecting with code 1011,
    // which includes session timeout reason. Probably the right way
    // would be that the server-side handles redirect to a timeout page...
    $scope.$on(won.EVENT.WEBSOCKET_CLOSED_UNEXPECTED, function(event){
        // if the gui part thinks we are authenticated but the server says not,
        // it most probably means there was a session timeout.
        // TODO: possible handling is asking user to re-login
        var wasAuth = userService.isAuth();
        userService.verifyAuth().then(function handleUnexpectedWebsocketClose(authenticated){
            if (authenticated) {
                $log.warn("WEBSOCKET CLOSED code 1011, user is still authenticated. Reconnecting.");
                messageService.reconnect();
            } else if (wasAuth) {
                $log.warn("WEBSOCKET CLOSED code 1011, user was authenticated. Logging out.");
                userService.logOutAndSetUpApplicationState().then(onResponseSignOut);
            } else {
                $log.warn("WEBSOCKET CLOSED code 1011, user was not authenticated. Reconnecting.");
                messageService.reconnect();
            }
        });
    });

    var onResponseSignOut = function (result) {
       $location.url("/");
    };

});