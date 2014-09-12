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

angular.module('won.owner').controller("HeaderCtrl", function($scope,$location, userService, linkedDataService, applicationStateService, $filter) {

    $scope.allNeeds = applicationStateService.getAllNeeds();
    $scope.eventNotifications = [];
    $scope.getUnreadEvents = function(){
        var unread = [];
        for(var i = 0; $scope.allNeeds.length >i; i++){
            var matchFilterType = [won.WON.HintCompacted];
            var conversationFilterType = [won.WON.OwnerMessageCompacted, won.WON.PartnerMessageCompacted];
            var requestFilterType =  [won.WON.OwnerOpenCompacted, won.WON.PartnerOpenCompacted];
          //  var events = linkedDataService.getAllEvents($scope.allNeeds[i].needURI);
            var need = $scope.allNeeds[i];

            var events =  [
                { eventType: won.WON.HintCompacted , title:'Car sharing to Prague', timeStamp: new Date('2014-08-25 14:30'), msg:'This is a Hint '},
                { eventType: won.WON.PartnerOpenCompacted, title:'Moved recently ...', timeStamp:new Date('2014-08-20'), msg:'This is a Connection Request'}];
            for(var j = 0; events.length;j++){
                if(applicationStateService.getReadEvents().indexOf(events[j])==-1)
                {
                    unread.push(matches[j].eventURI);
                }
            }

            var matches = $filter('messageTypeFilter')(unread,matchFilterType);
            var conversations = $filter('messageTypeFilter')(unread, conversationFilterType);
            var requests = $filter('messageTypeFilter')(unread, requestFilterType)
            need.matches = matches;
            need.conversations = conversations;
            need.requests = requests;
            $scope.eventNotifications.push(need);
        }
        //return unread;
    }
    $scope.getUnreadEvents();



	$scope.isActive = function(where) {
		if ($location.path().indexOf(where) > -1) {
			return 'active';
		} else if(where == undefined && $location.path() == '/') {
			return 'active';
		}
	};

    $scope.authenticated = false;

	$scope.showPublic = function() {
        $scope.authenticated = !userService.isAuth();
		return  $scope.authenticated;
	};

    $scope.checkRegistered = function(){
        return userService.getRegistered();
    };
    $scope.userdata = { username : userService.getUnescapeUserName()};
    $scope.message = "successfully registered";

    $scope.$watch(userService.isAuth, function(logged_in){
        if(logged_in){
            $scope.userdata = { username : userService.getUnescapeUserName()};
            $scope.authenticated = true;
            //$window.location.reload();
          //  $location.path('/');

      //      $window.location.reload();
       //     $scope.showPublic();
      //      $scope.$apply();
        }
    })

	onResponseSignOut = function (result) {
		if (result.status == 'OK') {
			userService.resetAuth();
			$location.path("/");
		}
	};

	$scope.onClickSignOut = function() {
		userService.logOut().then(onResponseSignOut);
	};

});