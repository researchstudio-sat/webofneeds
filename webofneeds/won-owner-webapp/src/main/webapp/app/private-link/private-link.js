/**
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/15/14
 * Time: 8:08 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner').controller('PrivateLinkCtrl', function ($scope, $location, userService, $rootScope) {

    $scope.title = 'New Flat, Need Furniture';
    $scope.img_path = '/images/thumbnail_demo.jpg';
    $rootScope.postClosed = false;
    $rootScope.postShouldBeClosed = false;
    $rootScope.postShouldBeReopened = false;
    $scope.showPublic = function() {
        return userService.isAuth();
    };

    //settings
    $scope.privateLink = 'https://won.com/la3f#private'; //todo set value normaly
    $scope.publicLink = 'http://www.webofneeds.org/'; //todo set value normaly;
    $scope.notificationEmail = '';
    $scope.notificationEmailValide = false;
    $scope.notificationChecks = {
        val1 : false,
        val2 : false,
        val3 : false
    };

    $scope.copyLinkToClipboard = function() {
        //todo maybe we can use http://zeroclipboard.org/
    };

    $scope.settingsCollapsed = false;
    $scope.settingsCollapseClick= function() {
        $scope.settingsCollapsed = !$scope.settingsCollapsed;
    };

    $scope.notifyMe = function() {
        $scope.notificationEmailValide = !$scope.notificationEmailValide;
        //todo send ro backend
    };
    $scope.changeEmailAddress = function() {
        $scope.notificationEmailValide = !$scope.notificationEmailValide;
    };

    //Post Options
    $scope.postOptionsCollapsed = false;
    $scope.postOptionsCollapseClick= function() {
        $scope.postOptionsCollapsed = !$scope.postOptionsCollapsed;
    };

    $scope.onClosePostClick= function() {
        $rootScope.postShouldBeClosed = !$rootScope.postShouldBeClosed;
        //todo send ro backend
    };

    $scope.onReopenPostClick= function() {
        $rootScope.postShouldBeReopened = !$rootScope.postShouldBeReopened;
        //todo send ro backend
    };

    $scope.onCopyAndPublishPostClick= function() {
        //todo send ro backend
    };

    //Messages Options
    $scope.conversationCollapseClick = function() {
        $scope.conversationCollapsed = !$scope.conversationCollapsed;
    };

    $scope.messageTypeColapsed = -1;
    $scope.conversationType0CollapseClick = function() {
        if($scope.messageTypeColapsed != 0) $scope.messageTypeColapsed = 0;
        else $scope.messageTypeColapsed = -1;
    };

    $scope.conversationType1CollapseClick = function() {
        if($scope.messageTypeColapsed != 1) $scope.messageTypeColapsed = 1;
        else $scope.messageTypeColapsed = -1;
    };

    $scope.conversationType2CollapseClick = function() {
        if($scope.messageTypeColapsed != 2) $scope.messageTypeColapsed = 2;
        else $scope.messageTypeColapsed = -1;
    };

    $scope.conversation = {
        title : 'Couch to give away',
        body : 'IKEA booshelf 4 chars and 1 table couch'
    };

    $scope.conversationRequest = {
        title : 'Sofa',
        body : 'PC chair mint new Holsten bed'
    };

    $scope.matches = {
        title : 'Couch Couch Couch',
        body : 'Couch table. Offer dinner table'
    };

    $scope.mesagesCollapsed = false;
    $scope.messagesCollapseClick = function() {
        $scope.mesagesCollapsed = !$scope.mesagesCollapsed;
    };

    //send new message
    $scope.newMessage = '';
    $scope.sendMessage = function() {
        //TODO logic
        $scope.newMessage = '';
    };
});

angular.module('won.owner').controller('CloseAndReopenPostCtrl', function ($scope,$route,$window,$location,userService, $rootScope) {

    $scope.close = false;
    $scope.reopen = false;
    $scope.error = '';


    //TODO logic
    /*onCloseResponse = function(result) {
        if (result.status == 'OK') {
           // $location.path('/');
            $scope.close = false;
        } else {
            $scope.error = result.message;
        }
    }     */

    $scope.onClickYes = function () {
        $scope.error = '';

        //userService.logIn($scope.user).then(onLoginResponse);
        if($rootScope.postShouldBeClosed) {
            //TODO logic
            $scope.close = true;
            $rootScope.postShouldBeClosed = false;
            $rootScope.postClosed = true;
        }

        //TODO logic
        if($rootScope.postShouldBeReopened){
            $scope.reopen = true;
            $rootScope.postShouldBeReopened = false;
            $rootScope.postClosed = false;
        }
    }

    $scope.onClickNo = function () {
        if($rootScope.postShouldBeClosed) {
            $scope.close = false;
            $rootScope.postShouldBeClosed = false;
        }

        if($rootScope.postShouldBeReopened){
            $scope.reopen = false;
            $rootScope.postShouldBeReopened = false;
        }
    }
});