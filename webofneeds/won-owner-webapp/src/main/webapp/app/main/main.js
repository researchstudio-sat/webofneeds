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

angular.module('won.owner').controller("MainCtrl", function($scope,$location, applicationStateService) {
    $scope.wonNodeURI = "http://localhost:8080/won";
    $scope.needURIPath = "/resource/need";
    $scope.connectionURIPath = "/connection";

    $scope.allNeedsWithUnreadNotifications = [];

    $scope.$on(won.EVENT.HINT_RECEIVED, function(ngEvent, eventData) {
        $scope.allNeedsWithUnreadNotifications= applicationStateService.fetchUnreadEventsForAllNeeds();
    });
    $scope.fetchNotifications = function(){
        $scope.allNeedsWithUnreadNotifications = applicationStateService.fetchUnreadEventsForAllNeeds();
        return $scope.allNeedsWithUnreadNotifications;
    }
    $scope.$on(won.EVENT.HINT_RECEIVED, function(ngEvent, eventData) {
        $scope.allNeedsWithUnreadNotifications= applicationStateService.fetchUnreadEventsForAllNeeds();

    });
    $scope.$on(won.EVENT.NEED_CREATED, function(ngEvent, eventData) {
        $scope.allNeedsWithUnreadNotifications= applicationStateService.fetchUnreadEventsForAllNeeds();

    });
});