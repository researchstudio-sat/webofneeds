/**
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/15/14
 * Time: 8:08 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner')
    .controller('PrivateLinkCtrl', function ($scope, $location, userService, $rootScope,applicationStateService, linkedDataService) {

    $scope.need = {};
    $scope.events = [];

    $scope.$on(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED, function(event){
        if (applicationStateService.getCurrentNeedURI() != $scope.need.uri && ! $scope.initializing){
            $scope.initialize();
        }
    });

        // all types of messages will be shown when the page is loaded
     var msgFilterCriteria = [1, 2, 3];
     $scope.initialize = function() {
         $scope.need = {};
         linkedDataService.getNeed(applicationStateService.getCurrentNeedURI())
             .then(function(need){
               $scope.need = need;
             })
             .then(function(){
                 if ($scope.need.uri != null) {
                     linkedDataService.getLastEventOfEachConnectionOfNeed($scope.need.uri).then(function(allEvents){
                         $scope.events.splice(0,$scope.events.length);
                         for (key in allEvents){
                             $scope.events.push(allEvents[key]);
                         }
                         //$scope.events = allEvents;
                     })
                 }
             });
        //$scope.need.matches = linkedDataService.getMatches($scope.need.uri);

        //
        // $scope.need.events = linkedDataService.getAllEvents($scope.need.uri);

        $scope.currentEventType = [
         won.WONMSG.connectionMessage,
         won.WONMSG.connectMessage,
         won.WONMSG.openMessage,
         won.WONMSG.hintMessage];


        //$scope.title = 'New Flat, Need Furniture';
        $scope.img_path = '/owner/images/thumbnail_demo.jpg';
        $rootScope.postClosed = false;
        $rootScope.postShouldBeClosed = false;
        $rootScope.postShouldBeReopened = false;


        //settings
        $scope.privateLink = 'https://won.com/la3f#private'; //todo set value normaly
        $scope.publicLink = 'http://www.webofneeds.org/'; //todo set value normaly;
        $scope.notificationEmail = '';
        $scope.notificationEmailValide = false;
        $scope.notificationChecks = {
            val1: false,
            val2: false,
            val3: false
        };

        $scope.prevMessageId = null;
        $scope.chosenMessage = null;
        $scope.rateValue = 0;
        $scope.showConfirmationDialogForDeclineRequest = false;
        $scope.showConfirmationDialogForCancelRequest = false;
        $scope.showEditButtons = false;
        $scope.showPencil = true;
        $scope.textAreaContent = '';
        $scope.showConfirmationDialogForRemoveMatch = false;

        $scope.showWarningForRating = false;
        $scope.showMatchControl = false;
    }


    $scope.showPublic = function() {
        return userService.isAuth();
    };

    $scope.copyLinkToClipboard = function() {
        //todo maybe we can use http://zeroclipboard.org/
    };


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
    $scope.newMessage = '';
    $scope.mesagesCollapsed = false;
    $scope.messageTypeColapsed = -1;
    $scope.settingsCollapsed = false;
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
    /*
    $scope.messages = [
        {id:1, type: 1, typeText:'Conversation', title:'Car sharing to Prague', datetime: new Date('2014-08-25 14:30'), msg:'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.'},
        {id:2, type: 2, typeText:'Incoming Request', title:'Moved recently ...', datetime:new Date('2014-08-20')},
        {id:3, type: 2, typeText:'Outgoing Request', title:'Let\'s clean ...', datetime:new Date('2014-06-28')},
        {id:4, type: 1, typeText:'Conversation', title:'Friendly Bicycle ...', datetime:new Date('2014-04-15 10:11'), msg:'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.'},
        {id:5, type: 3, typeText:'Matches', title:'Old children\'s clothes ..', datetime:new Date('2013-09-10')},
        {id:6, type: 2, typeText:'Incoming Request', title:'Bought new car ...', datetime:new Date('2014-03-01')},
        {id:7, type: 2, typeText:'Outgoing Request', title:'Let\'s grill ...', datetime:new Date('2014-06-19')},
        {id:8, type: 3, typeText:'Matches', title:'Old men\'s clothes ..', datetime:new Date('2014-02-10')}
    ];
    */

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

    /*$scope.getFilter = function() {
        alert($scope.messageTypeColapsed ) ;
        if($scope.messageTypeColapsed == -1) return "";
        else if($scope.messageTypeColapsed == 0) return "| filter: {type: message}";
        else if($scope.messageTypeColapsed == 1) return "| filter: {type: request}";
        else return "| filter: {type: match}";
    };   */

    $scope.getIconClass = function (typeText) {
        if (typeText=='Conversation') return 'fa fa-comment-o fa-lg';
        else if (typeText=='Incoming Request') return 'fa fa-share fa-lg';
        else if (typeText=='Outgoing Request') return 'fa fa-reply fa-lg';
        else return 'fa fa-puzzle-piece fa-lg';
    };

    /*$scope.messageFilter = function(msg) {
        return msgFilterCriteria.indexOf(msg.type) >= 0 ? true : false;
    } */
    $scope.changeEventTypeButtonsDisplay = function(buttonId){
        var button = $('#' + buttonId);
        if (button.hasClass('btn-success')) {
            button.removeClass('btn-success').addClass('btn-default');
        } else {
            button.removeClass('btn-default').addClass('btn-success');
        }
    }

        /**
         * Appends each element of the array to $scope.currentEventType if it is not
         * yet in it, otherwise it removes the element.
         * @param types
         */
    var toggleEventTypes = function(types){
        var list = $scope.currentEventType;
        for (key in types){
            var elem = types[key];
            var pos = list.indexOf(elem);
            if (pos == -1){
                list.push(elem);
            } else {
                list.splice(pos,1);
            }
        }
    }

    $scope.clickOnMessageButton = function(buttonId) {
        switch (buttonId) {
            case 1:
                toggleEventTypes([won.WONMSG.connectionMessage]);
                break;
            case 2:
                toggleEventTypes([won.WONMSG.connectMessage, won.WONMSG.openMessage]);
                break;
            case 3:
                toggleEventTypes([won.WONMSG.hintMessage]);
                break;
            default:
                console.log("not handling message button id " + buttonId)  ;
        }
        $scope.changeEventTypeButtonsDisplay(buttonId);
    }

    $scope.conversationCollapseClick = function() {
        $scope.conversationCollapsed = !$scope.conversationCollapsed;
    };


    $scope.messagesCollapseClick = function() {
        $scope.mesagesCollapsed = !$scope.mesagesCollapsed;
    };

    //send new message

    $scope.sendMessage = function() {
        //TODO logic
        $scope.newMessage = '';
    };


    // helper function to get message according to its id from messages
    function getEventById(msgId) {
        for(var i = 0; i < $scope.need.events.length; i++) {
            if ($scope.need.events[i].id == msgId) return $scope.need.events[i];
        }
        // should not get here
    }
    $scope.initRater = function() {
        $("#rater").rating({
            starCaptions: function(val) {
                $scope.rateValue = val;
                return val;
            }
        });
    }

    $scope.clickOnTitle = function(msgId) {
        // msgId can't be null here
        if ($scope.prevMessageId == msgId) {
            $scope.chosenMessage = $scope.chosenMessage == null ? getEventById(msgId) : null;
        } else {
            $scope.chosenMessage = getEventById(msgId);
        }
        $scope.prevMessageId = msgId;
    }

    $scope.showConversations = function() {
        if($scope.chosenMessage != null){
            if($scope.chosenMessage.typeText == 'Conversation') return true;
        }else return false;
    }

    $scope.showIncomingRequests = function() {
        if($scope.chosenMessage != null){
            if($scope.chosenMessage.typeText == 'Incoming Request') return true;
        }else return false;
    }

    $scope.showOutgoingRequests = function() {
        if($scope.chosenMessage != null){
            if($scope.chosenMessage.typeText == 'Outgoing Request') return true;
        }else return false;
    }

    $scope.showMatches = function() {
        if($scope.chosenMessage != null){
            if($scope.chosenMessage.typeText == 'Matches') return true;
        }else return false;
    }

    // Incoming Requests
    $scope.clickOnDeclineForInRequest = function() {
        console.log('decline clicked');
        $scope.showConfirmationDialogForDeclineRequest = true;
    }

    $scope.clickOnAcceptForInRequest = function() {
        console.log('accept clicked');
        // TODO add parameter for displaying specific stuff on private-link page
        console.log('redirect: /private-link');
        $location.path('/private-link');
    }

    $scope.clickOnNoForDeclineRequest = function() {
        console.log('no');
        $scope.showConfirmationDialogForDeclineRequest = false;
    }

    $scope.clickOnYesForDeclineRequest = function() {
        console.log('yes');
        $scope.showConfirmationDialogForDeclineRequest = false;
        // TODO add parameter for displaying specific stuff on private-link page
        console.log('redirect: /private-link');
        $location.path('/private-link');
    }

    // Outgoing Requests
    $scope.clickOnCancelRequest = function() {
        console.log('cancel clicked');
        $scope.showConfirmationDialogForCancelRequest = true;
    }

    $scope.clickOnNoForCancelRequest = function() {
        console.log('no');
        $scope.showConfirmationDialogForCancelRequest = false;
    }

    $scope.clickOnYesForCancelRequest = function() {
        console.log('yes');
        $scope.showConfirmationDialogForCancelRequest = false;
        // TODO add parameter for displaying specific stuff on private-link page
        console.log('redirect: /private-link');
        $location.path('/private-link');
    }

    // for editable text box
    $scope.changeToEditable = function() {
        $('#textboxInRequest').removeAttr('disabled');
        $scope.textAreaContent = $('#textboxInRequest').val();
        $scope.showEditButtons = true;
        $scope.showPencil = false;
    }

    $scope.clickOnCancelWhenEdit = function() {
        // fill text area with previous content
        $('#textboxInRequest').val($scope.textAreaContent).attr('disabled', '');
        $scope.showEditButtons = false;
        $scope.showPencil = true;
    }

    $scope.clickOnFinishedWhenEdit = function() {
        // TODO send a new request
        console.log('send a new request');
        $('#textboxInRequest').attr('disabled', '');
        $scope.showEditButtons = false;
    }

    // Matches

    $scope.clickOnRemoveMatch = function() {
        console.log('remove clicked');
        $scope.showConfirmationDialogForRemoveMatch = true;
    }

    $scope.clickOnNoForRemoveMatch = function() {
        console.log('no');
        $scope.showConfirmationDialogForRemoveMatch = false;
    }

    $scope.clickOnYesForRemoveMatch = function() {
        console.log('yes');
        $scope.showConfirmationDialogForRemoveMatch = false;
        // TODO add parameter for displaying specific stuff on private-link page
        console.log('redirect: /private-link');
        $location.path('/private-link');
    }


    $scope.clickOnRequestConversation = function() {
        console.log('request conversation');
        if ($scope.rateValue > 0) {
            $scope.showMatchControl = true;
            $scope.showWarningForRating = false;
        } else {
            $scope.showWarningForRating = true;
        }
    }

    $scope.clickOnSendRequestMessage = function() {
        console.log('send request message');
        $scope.showMatchControl = false;
        // TODO add parameter for displaying specific stuff on private-link page
        console.log('redirect: /private-link');
        $location.path('/private-link');
    }

    $scope.openFacebook = function() {
        window.open(
            'https://www.facebook.com/sharer/sharer.php?u={{publicLink}}', //+encodeURIComponent(location.href),
            'facebook-share-dialog',
            'width=626,height=436');
        return false;
    }

    $scope.initialize();
})



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