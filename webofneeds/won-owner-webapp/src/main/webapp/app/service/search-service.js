angular.module('won.owner').factory('searchService', function ($window, $http, $log, $q, $location, $rootScope, applicationStateService, utilService) {
    var searchService = {};


    searchService.search = function(type,searchText){
        $http.get(
            '/matcher/search/',
            {
                headers : {
                    'Accept' : 'application/ld+json'
                },
                params:{q:searchText}
            }
        ).then(
            function (results) {
                var frame = [{
                    "@context": {
                        "dc": "http://purl.org/dc/elements/1.1/",
                        "ex": "http://example.org/vocab#",
                        "xsd": "http://www.w3.org/2001/XMLSchema#",
                        "won":"http://purl.org/webofneeds/model#"
                    },
                    "@type": "won:Match"
                }]

                var framedResult = jsonld.frame(results.data, frame);
                framedResult = jsonld.expand(framedResult);

                var promises = [];
                angular.forEach(framedResult,function(res){
                    promises.push(linkedDataService.invalidateCacheForNeed(res[won.WON.searchResultURI]));
                })
                applicationStateService.addSearchResults(framedResult,promises);
                $location.url('search/'+type+'/'+searchText);
                $rootScope.$broadcast(won.EVENT.WON_SEARCH_RECEIVED, framedResult);


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

