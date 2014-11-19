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

angular.module('won.owner').controller("HeaderCtrl",
    function ( $scope
             , $location
             , userService
             , linkedDataService
             , applicationStateService
             , $filter
             , $interval
             , $log
             ) {



    $log.debug("Initializing HeaderCtrl.")

    //after reloading, verify if the session's still valid
    //TODO move somewhere more appropriate (sadly in app.config the service isn't available yet)
    //userService.verifyAuth(); homepage doesn't load any more after using this here
    // due to the wait-for-all decorator? ("everything waits for the http request to return"?)

    $scope.isActive = function(where) {
		if ($location.path().indexOf(where) > -1) {
			return 'active';
		} else if(where == undefined && $location.path() == '/home') {
			return 'active';
		}
	};

    //TODO debug output. deletme
    $scope.$on('$locationChangeStart', function logRouteChange(e, to, from) {
        $log.debug("changing route: " + JSON.stringify(from)
                   + " to "  + JSON.stringify(to)
        )});

    $scope.redirectHome= function(){
        $location.path("/home");
    }

    $scope.goLandingPage = function() {
        $location.path("/");
    }

	/*$scope.showPublic = userService.isAuth().then( function(isAuth) {
        return !isAuth
    })*/
    $scope.showPublic = userService.isAuth() //TODO negate
        //return !userService.isAuth();

    $scope.checkRegistered = function(){
        return userService.getRegistered();
    };
    $scope.userdata = { username : userService.getUnescapeUserName()};

    /*
        TODO fix code, reintroduce
         */
        /*
    $scope.$watch(userService.isAuth, function(logged_in){
        if(logged_in){
            $scope.userdata = { username : userService.getUnescapeUserName()};
        }
    })
        */
    $scope.onDropDownClick=function(num){
        $scope.$parent.selectedType = num;
    }

	onResponseSignOut = function (result) {
		if (result.status == 'OK') {
			userService.resetAuth();
			$location.path("/");
		}
	};

	$scope.onClickSignOut = function() {
		applicationStateService.reset();
		linkedDataService.reset();
		userService.logOut().then(onResponseSignOut);
	};

});