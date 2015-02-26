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
             , $window
             , $modal //TODO testing
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

    $scope.goLandingPage = function() {
        $location.url("/");
    }

    $scope.loginModal = function() {
        $modal.open({
            //template: ' <div class="modal-dialog"><div class="modal-content"><div class="modal-header">blah</div><div class="modal-body">blah</div><div class="modal-footer">blah</div></div></div>',
            templateUrl: 'app/sign-in/sign-in-modal-content.html',
            size: 'sm'
        });

    };

	$scope.showPublic = function() {
        return !userService.isAuth();
	};

    $scope.showAccountUser = function() {
        return userService.isAccountUser();
    };

    $scope.showPrivateLinkUser = function() {
        return userService.isPrivateUser();
    };

    $scope.checkRegistered = function () {
        return userService.isRegistered();
    };

    var getDisplayUserName = function () {
        $scope.userdata = { username: userService.getUnescapeUserName()};
        if (userService.isPrivateUser()) {
            $scope.userdata.username = "private user";
        }
    }
    getDisplayUserName();


    $scope.$watch(userService.isAuth, function(logged_in){
        if(logged_in){
            getDisplayUserName();
        }
    })
    $scope.onDropDownClick=function(num){
        $scope.$parent.selectedType = num;
    }

	onResponseSignOut = function (result) {
		if (result.status == 'OK') {
			$scope.goLandingPage();
		}
	};

	$scope.onClickSignOut = function() {
		applicationStateService.reset();
		userService.logOutAndSetUpApplicationState().then(onResponseSignOut);
	};

});