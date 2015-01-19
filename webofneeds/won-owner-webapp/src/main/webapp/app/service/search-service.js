angular.module('won.owner').factory('searchService', function ($window, $http, $log, $q, $location, $rootScope, applicationStateService, utilService) {
    var searchService = {};


    searchService.search = function(searchText){
        $http.get(
            '/matcher/searchJson/',
            {params:{q:searchText}}
        ).then(
            function (results) {
                var promises = [];
                angular.forEach(results.data,function(result){
                    promises.push(linkedDataService.invalidateCacheForNeed(result['matchURI']));
                })
                applicationStateService.addSearchResults(results.data,promises);
                $location.url('search');
                $rootScope.$broadcast(won.EVENT.WON_SEARCH_RECEIVED, results.data);


            },
            function (response) {
                switch(response.status) {
                    case 403:
                        // normal error
                        return {status: "ERROR", message: "getting search result failed"};
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "getting search results failed"};
                        break;
                }
            }
        )
    }
    return searchService;
});

