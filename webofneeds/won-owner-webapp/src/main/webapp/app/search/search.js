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

angular.module('won.owner').controller('SearchCtrl', function ($scope, $location,$log, mapService, applicationStateService) {

    $scope.results = applicationStateService.getSearchResults();
    $scope.$on(won.EVENT.WON_SEARCH_RECEIVED,function(ngEvent, event){
        event.data = linkedDataService.getNeed(event.matchUrl());
        $scope.results.push(event);
    })
    // TODO LOGIC
    $scope.relatedTags = ['Sony', 'Tv', 'Samsung', 'LCD'];

    //TODO LOGIC
    $scope.searching = {type:'others offer', title:'Frilly pink cat unicorn'};

    $scope.createNewPost = function () {
        //TODO put title from search
        $location.url('/create-need/1//' + $scope.searching.title);
    }
});

