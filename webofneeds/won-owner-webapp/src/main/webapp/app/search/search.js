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

angular.module('won.owner').controller('SearchCtrl', function ($scope, $location,$log,$routeParams,$window,searchService, linkedDataService, mapService, applicationStateService, applicationControlService) {

    $scope.results = applicationStateService.getSearchResults();
    $scope.search = {};
    $scope.search.title = $routeParams.term;
    $scope.search.type = parseInt($routeParams.type);

    $scope.columnNum = 2;
    $scope.$on(won.EVENT.WON_SEARCH_RECEIVED,function(ngEvent, event){
        $scope.results.push(event);
    })
    // TODO LOGIC
    $scope.relatedSearchTerms = [];
    $scope.extractRelatedSearches = function(){
        angular.forEach($scope.results,function(res){
            if(res[won.WON.searchResultPreview][won.WON.hasContent][won.WON.hasTag]!= undefined){
                var tags = res[won.WON.searchResultPreview][won.WON.hasContent][won.WON.hasTag]['@value'].split(",");

                for(var i = 0;i<tags.length;i++){
                    var tag = tags[i];

                    var contains = false;
                    for(var j = 0; j<$scope.relatedSearchTerms.length;j++){
                        if($scope.relatedSearchTerms[j][0]==tag){
                            $scope.relatedSearchTerms[j][1]=$scope.relatedSearchTerms[j]+1;
                            contains = true;
                            break;
                        }
                    }
                    if(contains == false){
                        $scope.relatedSearchTerms.push([tag,0]);
                    }

                    /*iif(tag in $scope.relatedSearchTerms){
                     $scope.relatedSearchTerms[tag]= $scope.relatedSearchTerms[tag] +1;
                     }else{
                     $scope.relatedSearchTerms[tag]=0;
                     }*/
                }
                $scope.relatedSearchTerms.sort(function(a,b){
                    a = a[1];
                    b = b[1];
                    return a < b ? -1 : (a > b ? 1 : 0);
                })
            }


        })


    }
    $scope.extractRelatedSearches();


    $scope.createNewPost = function () {
        //TODO put title from search
        $location.url('/create-need/1/-1/' + $scope.search.title);
    }
    $scope.redirectToCreatePost = function(){
        $window.open('./#/create-need/1/-1/'+$scope.search.title, '_blank');
    };
    $scope.getRelatedSearches = function(){

        return $scope.relatedSearchTerms;
    }
    $scope.newSearch = function(term){
        $scope.search.title = term;
        searchService.search($scope.search.type, term,applicationControlService.getNeedType($scope.search.type));
    }

});
angular.module('won.owner').controller('SearchResultCtrl', function($scope, $log,applicationStateService){
    $scope.res = {};


    $scope.res.uri = $scope.result[won.WON.searchResultURI]['@id'];
    $scope.res.title = $scope.result[won.WON.searchResultPreview][won.WON.hasContent][won.defaultContext.dc+"title"]["@value"];
    $scope.res.type = $scope.result[won.WON.searchResultPreview][won.WON.hasBasicNeedType]['@id'];
    //$scope.res.need = linkedDataService.getNeed($scope.res.uri);


})
app.directive(('relatedSearches'), function relatedSearchesFct(){
    var dtv = {
        restrict: 'E',
        scope : {
            terms : '=',
            clickOnItem : '&'
        },
        templateUrl: "app/search/related-searches.html",
        controller: function($scope){

        }

    }
    return dtv;
})
app.directive(('searchResult'), function searchResultFct($log, applicationStateService){

    var dtv = {
        restrict: 'E',
        scope : {
            results : '=',
            columnNum : '@'
        },
        templateUrl: "app/search/search-result.html",
        link: function(scope, elem, attr){
            scope.counter = 0;
            scope.preparedResults = [];
            var prepareResults = function(){
                var rowCount = 0;
                for(var i = 0;i<scope.results.length;i++){
                    scope.results[i].id = i;
                    if(i%scope.columnNum==0){
                        rowCount = rowCount+1;
                        var row = [];
                        row.push(scope.results[i]);
                        scope.preparedResults.push(row);
                    }else{
                        scope.preparedResults[scope.preparedResults.length-1].push(scope.results[i]);
                    }
                }

            }
            prepareResults();
        },
        controller: function($scope){
            var selectedResult = 0;//default
            $scope.selectedNeed = {};

            $scope.getCurrentNeed=function(){
                return applicationStateService.getCurrentNeed();
            }
            $scope.selected = function(num) {
                if (selectedResult == num){
                    return "col-md-12 thumbnail-selected"
                }else return "col-md-12 thumbnail-non-selected"
            }
            $scope.select=function(num){
                selectedResult = num;
                try{
                    applicationStateService.setCurrentNeedURI($scope.results[selectedResult][won.WON.searchResultURI]['@id']);


                    linkedDataService.getNeed($scope.results[selectedResult][won.WON.searchResultURI]['@id']).then(
                    function success(need) {
                        $scope.selectedNeed = need;
                    },
                    function error(response) {
                        $scope.selectedNeed = null;
                        $log.debug("failed to select search result, probably the need doesn't exist any more")
                    })
                } catch (e){
                    $log.debug("failed to set current need uri. probably there's no search result")
                }
            }
            $scope.select(selectedResult);
        }
    }
    return dtv;
})

