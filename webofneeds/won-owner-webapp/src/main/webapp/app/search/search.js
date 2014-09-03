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

angular.module('won.owner').controller('SearchCtrl', function ($scope, $location, mapService) {
    $scope.results = [
        {id:1},
        {id:2},
        {id:3}
    ];

    //TODO LOGIC
    $scope.searching = {type:'others offer', title:'Frilly pink cat unicorn'};

    $scope.createNewPost = function () {
        //TODO put title from search
        $location.path('/create-need/1//' + $scope.searching.title);
    }
});

angular.module('won.owner').directive('wonPostDetail', function factory(userService) {
    return {
        restrict:'AE',
        templateUrl:"app/post-detail/post-detail.html",
        scope:{}
    }
});
