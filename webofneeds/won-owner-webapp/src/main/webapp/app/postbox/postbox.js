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
angular.module('won.owner').controller('PostBoxCtrl', function ($scope,$interval, $location, userService, applicationStateService) {
    $scope.countOfAllUnreadMatchEvents = 0;
  //  $scope.emptyListText = 'You don\'t have posts yet. Fill it by creating new posts';
   // $scope.allUnreadEvents = applicationStateService.getUnreadObjects();
   /* $scope.updateCountOfAllUnreadMatchEvents= function(){
        var allMatchEvents = [];

        for(var i = 0; i<$scope.allNeedsWithUnreadNotifications.length;i++){
            var need = $scope.allNeedsWithUnreadNotifications[i];
            allMatchEvents.push(need.matches);
        }
        $scope.countOfAllUnreadMatchEvents = allMatchEvents.length;

    }                         */
    $scope.allNeeds = applicationStateService.getAllNeeds();


    /*
    $scope.$watchCollection('allNeedsWithUnreadNotifications',function(updated, old){
        console.log("Watching allNeedsWithUnreadNotifications collection: ", updated, old);
        $scope.updateCountOfAllUnreadMatchEvents();
    })*/


    /*
    $scope.AllNeedsWithUnreadNotifications = applicationStateService.fetchUnreadEventsForAllNeeds(); */
    /*
    $scope.$on(won.EVENT.HINT_RECEIVED, function(ngEvent, eventData) {
        $scope.AllNeedsWithUnreadNotifications= applicationStateService.fetchUnreadEventsForAllNeeds();
    });   */
  //  $scope.allPosts = applicationStateService.getAllNeeds();
	/*$scope.line = {
		type:'',
		title:'',
		newMessages:0,
		megaphone:0,
		puzzle:0,
		date:''
	};
                */
    //$scope.fetchNotifications();
    $scope.recordsToDisplay = 4;
    $scope.displayConfirmationDialog = false;
    var indexOfChosenDraft;
	$scope.search = '';

    // TODO call here backend method
    function deleteDraft(index) {
        if (index >= 0) {
            $scope.drafts.splice(index, 1);
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
    //need to fetch: need type, need title, if unread messages exist -> icon of message + number, need create date
   // $scope = {};
    //how to fetch all needs of one user?

    // for filtering: when number of unread notifications is 0 set appropriate value (convText, reqText or matchText) to empty string
/*	$scope.posts = [
		{type:'Want', title:'Want PS 4', msg:{convText: '', conversations : 0, reqText: '', requests: 0, matchText: '', matches: 0}, datetime: new Date('2014-08-25')},
		{type:'Change', title:'Looking for a flatscreen TV', msg:{convText: 'unread conversations', conversations : 3, reqText: '', requests: 0, matchText: 'unread matches', matches: 3}, datetime: new Date('2014-08-20')},
		{type:'Offer', title:'Offering IKEA couch', msg:{convText: 'unread conversations', conversations : 1, reqText: '', requests: 0, matchText: 'unread matches', matches: 5}, datetime: new Date('2014-08-15')},
		{type:'Together', title:'Collect items for Caritas asylum', msg:{convText: '', conversations : 0, reqText: 'unread requests', requests: 4, matchText: 'unread matches', matches: 1}, datetime: new Date('2014-07-14')},
        {type:'Change', title:'I want to change my mobile', msg:{convText: '', conversations : 0, reqText: '', requests: 0, matchText: '', matches: 0}, datetime: new Date('2014-05-25')},
        {type:'Together', title:'Clean the forest', msg:{convText: 'unread conversations', conversations : 10, reqText: '', requests: 0, matchText: '', matches: 0}, datetime: new Date('2014-08-26')},
        {type:'Offer', title:'Selling old laptop', msg:{convText: '', conversations : 0, reqText: 'unread requests', requests: 7, matchText: '', matches: 0}, datetime: new Date('2014-03-01')},
        {type:'Want', title:'Want a plane', msg:{convText: '', conversations : 0, reqText: '', requests: 0, matchText: 'unread matches', matches: 3}, datetime: new Date('2014-02-22')}
	];          */

    $scope.getTypePicURI = function (type) {
        if(type==won.WON.BasicNeedTypeDemand) return "/owner/images/type_posts/want.png";
        else if(type==won.WON.BasicNeedTypeCritique) return "/owner/images/type_posts/change.png";
        else if(type==won.WON.BasicNeedTypeSupply) return "/owner/images/type_posts/offer.png";
        else return "/owner/images/type_posts/todo.png";
    };

    // TODO call backend method here
    //need to fetch: need type, need title, need create date
    $scope.drafts = [
        {type:'Together', title:'Car sharing to Prague', datetime: new Date('2014-08-20')},
        {type:'Want', title:'Moved recently ...', datetime: new Date('2014-08-25')},
        {type:'Change', title:'Let\'s clean ...', datetime: new Date('2014-05-01')},
        {type:'Offer', title:'Friendly Bicycle ...', datetime: new Date('2014-07-10')},
        {type:'Offer', title:'Old children\'s clothes ..', datetime: new Date('2013-09-09')}
    ];

    // TODO call backend method here
    //need to fetch: need type, need title, need close date
    $scope.closedList = [
        {type:'Want', title:'Playing soccer together', datetime: new Date('2014-08-23')},
        {type:'Change', title:'Looking for a flatscreen TV', datetime: new Date('2014-08-20')},
        {type:'Together', title:'Go to the cinema', datetime: new Date('2014-07-14')}
    ];

    // data for notifications in menu, TODO call backend methods here and maybe more convenient controller
   /* $scope.conversations = [
        {type: 'Together', title:'Car sharing to Prague', msgs: 5},
        {type: 'Offer', title:'Friendly Bicycle ...', msgs: 4},
        {type: 'Want', title:'I want smartphone ...', msgs: 6},
        {type: 'Change', title:'Change my stamps ...', msgs: 3},
        {type: 'Together', title:'Study together ...', msgs: 2},
        {type: 'Offer', title:'Good guitar ...', msgs: 1}
    ];

    $scope.requests = [
        {type: 'Want', typeText:'Incoming Request', title:'Moved recently ...', msgs: 2},
        {type: 'Change', typeText:'Outgoing Request', title:'Let\'s clean ...', msgs: 3},
        {type: 'Together', typeText:'Incoming Request', title:'Bought new car ...', msgs: 1},
        {type: 'Offer', typeText:'Outgoing Request', title:'Let\'s grill ...', msgs: 5}
    ];

    $scope.matches = [
        {type: 'Change', title:'Old children\'s clothes ..', msgs: 3},
        {type: 'Offer', title:'Old men\'s clothes ..', msgs: 2}
    ];
             */
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

    $scope.draftsCollapsed = true;
    $scope.draftsCollapseClick = function () {
        $scope.draftsCollapsed = !$scope.draftsCollapsed;
    };

    $scope.closedCollapsed = true;
    $scope.closedCollapseClick = function () {
        $scope.closedCollapsed = !$scope.closedCollapsed;
    };

    $scope.clickOnRemoveButton = function (index) {
        $scope.displayConfirmationDialog = true;
        indexOfChosenDraft = index;
    }

    $scope.clickOnYesButton = function() {
        deleteDraft(indexOfChosenDraft);
        $scope.displayConfirmationDialog = false;
    }

    $scope.clickOnNoButton = function() {
        $scope.displayConfirmationDialog = false;
    }
});
angular.module('won.owner').controller('MatchCountCtrl', function ($scope,$interval, $location, userService, applicationStateService) {
    $scope.rowNeed = undefined;
    $scope.matchCount = 0;
    $scope.getMatchesForNeed = function(need){
        var unreadHintEventsForNeed = {};
        for(var i = 0; i< $scope.unreadObjects.byNeed.hint.length;i++){
            var unreadHint = $scope.unreadObjects.byNeed.hint[i];
            if(need.uri==unreadHint.need.uri ){
                unreadHintEventsForNeed.matchEvents = unreadHint.events;
            }

        }
        $scope.matchCount = unreadHintEventsForNeed.matchEvents.length;
        //return unreadHintEventsForNeed;
    }
    $scope.$watch('unreadObjects', function(newValue, oldValue){
        if($scope.rowNeed!=undefined){
            $scope.getMatchesForNeed($scope.rowNeed);
        }
    });
});