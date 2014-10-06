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

angular.module('won.owner').controller("MainCtrl", function($scope,$location, applicationStateService, applicationControlService) {
    $scope.wonNodeURI = "http://localhost:8080/won";
    $scope.needURIPath = "/resource/need";
    $scope.connectionURIPath = "/connection";
    $scope.selectedType = -1;
    $scope.unreadEventsByNeedByType = applicationStateService.getUnreadEventsByNeedByType();
    $scope.unreadEventsByTypeByNeed = applicationStateService.getUnreadEventsByTypeByNeed();
    $scope.latestEventsByNeedByConnection = {};
    //allow acces to service methods from angular expressions:
    $scope.openNeedDetailView = applicationControlService.openNeedDetailView;

    addEventAsUnreadEvent = function(eventData){


        applicationStateService.processEventAndUpdateUnreadEventObjects(eventData);
        $scope.unreadEventsByNeedByType = applicationStateService.getUnreadEventsByNeedByType();
        $scope.unreadEventsByTypeByNeed = applicationStateService.getUnreadEventsByTypeByNeed();

    }

    $scope.$on(won.EVENT.HINT_RECEIVED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);

    });
    $scope.$on(won.EVENT.NEED_CREATED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);

    });
    $scope.$on(won.EVENT.CONNECT_RECEIVED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);

    });
    $scope.$on(won.EVENT.OPEN_SENT, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);
    });
    $scope.$on(won.EVENT.OPEN_RECEIVED, function(ngEvent, eventData) {

        addEventAsUnreadEvent(eventData);
    });

    $scope.$on(won.EVENT.CLOSE_RECEIVED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);
    });

    $scope.$on(won.EVENT.CONNECTION_MESSAGE_RECEIVED, function(ngEvent, eventData) {
        addEventAsUnreadEvent(eventData);

    });

});