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
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/15/14
 * Time: 8:08 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner').controller('PostBoxCtrl', function ($scope,$interval, $location, userService, applicationStateService, applicationControlService) {
    $scope.countOfAllUnreadMatchEvents = 0;
    // TODO if we want to remember the last sortedField selected by the user,
    // we should store it in e.g. applicationStateService
    $scope.sortedField = 'creationDate';
    $scope.reversedSort = true;


    $scope.hasNeeds = function () {
        return applicationStateService.getAllNeedsCount() > 0;
    }

    $scope.recordsToDisplay = 4;

	$scope.search = '';



    $scope.setSortParams = function(fieldName) {
        if ($scope.sortedField == fieldName) {
            $scope.reversedSort = !$scope.reversedSort;
        } else {
            $scope.reversedSort = false;
            $scope.sortedField = fieldName;
        }
    }

    $scope.resizableColumns = function (id) {
        var pressed = false;
        var start = undefined;
        var startX, startWidth;

        $('#' + id + ' th').mousedown(function(e) {
            start = $(this);
            pressed = true;
            startX = e.pageX;
            startWidth = $(this).width();
            $(start).addClass("resizing");
        });

        $(document).mousemove(function(e) {
            if(pressed) {
                $(start).width(startWidth+(e.pageX-startX));
            }
        });

        $(document).mouseup(function() {
            if(pressed) {
                $(start).removeClass("resizing");
                pressed = false;
            }
        });
    }



    // TODO call backend method here
    //need to fetch: need type, need title, need close date
    $scope.closedList = [
        {type:'Want', title:'Playing soccer together', datetime: new Date('2014-08-23')},
        {type:'Change', title:'Looking for a flatscreen TV', datetime: new Date('2014-08-20')},
        {type:'Together', title:'Go to the cinema', datetime: new Date('2014-07-14')}
    ];


	$scope.clickOnMessage = function () {
		//TODO Put here logic
	};

	$scope.clickOnMegaphone = function () {
		//TODO Put here logic
	};

	$scope.clickOnPuzzle = function () {
		//TODO Put here logic
	};

	$scope.showPublic = function () {
		return userService.isAuth();
	};

    $scope.inboxCollapsed = false;
    $scope.inboxCollapseClick = function () {
        $scope.inboxCollapsed = !$scope.inboxCollapsed;
    };


    $scope.closedCollapsed = true;
    $scope.closedCollapseClick = function () {
        $scope.closedCollapsed = !$scope.closedCollapsed;
    };

    $scope.clickOnNeedPrivateLink = function(clickedNeed) {
        applicationStateService.setCurrentNeedURI(clickedNeed.uri);
        $location.path("/private-link")
    }
});