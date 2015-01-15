angular.module('won.owner').factory('searchService', function ($window, $http, $log, $q, $rootScope, applicationStateService, utilService) {
    var searchService = {};


    searchService.search = function(searchText){
        $http.get(
            '/matcher/search/',
            searchText
        ).then(
            function (results) {
                // when we were having a bug that results in redirect here returning owner web-page
                // instead of array of needs, we still get the http status 200 at this point. This
                // is because the redirect is already handled by the browser by this time and the
                // redirected page returns OK 200 that we see here. Therefore, here is just a hack
                // to check if the response data is not a string (we except array here).
                if(utilService.isString(needs.data)) {
                    // unexpected response data, probably a redirect to another web-page
                    $log.error("ERROR: unexpected response data for /owner/rest/needs/");
                    return {status: "ERROR", message: "unexpected response data"};
                } else {
                    if (needs.data.length>0){
                        applicationStateService.addNeeds(needs);
                    }
                    // success
                    return {status:"OK"};
                }

            },
            function (response) {
                switch(response.status) {
                    case 403:
                        // normal error
                        return {status: "ERROR", message: "getting needs of a user failed"};
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "getting needs of a user failed"};
                        break;
                }
            }
        )
    }
});

