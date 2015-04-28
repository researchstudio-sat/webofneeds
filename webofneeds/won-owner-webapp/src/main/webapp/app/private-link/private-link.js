/**
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/15/14
 * Time: 8:08 AM
 * To change this template use File | Settings | File Templates.
 */

angular.module('won.owner')
    .controller('PrivateLinkCtrl', function ($scope
        , $location
        , userService
        , $rootScope
        , $log
        , $routeParams
        , applicationStateService
        , applicationControlService
        , linkedDataService
        , wonService) {

    // all types of messages will be shown when the page is loaded
     var msgFilterCriteria = [1, 2, 3];
        //TODO: refactor this.
    $scope.$watch('lastEventOfEachConnectionOfCurrentNeed', function(newValue, oldValue){
        var newCnt = newValue != null ? newValue.length : "null";
        var oldCnt = oldValue != null ? oldValue.length : "null";
        $log.debug("events changed! now have " + newCnt + " events!" + " had: " + oldCnt +"...");
        //if chosen message not null then use connection uri to find and update
        updateChosenMessageIfAny();
    });

    var updateChosenMessageIfAny = function(){
        if ($scope.chosenMessage != null) {
            $scope.chosenMessage = getEventByConnectionUri($scope.chosenMessage.connection.uri);
            $scope.addConnectionLastTextMessages($scope.chosenMessage);
            $scope.prevMessageId = $scope.chosenMessage.event.uri;
        }
    }

    $scope.sortedField = 'event.hasTimestamp';
    $scope.reversedSort = true;

    $scope.setSortParams = function(fieldName) {
        if ($scope.sortedField == fieldName) {
            $scope.reversedSort = !$scope.reversedSort;
        } else {
            $scope.reversedSort = false;
            $scope.sortedField = fieldName;
        }
    }

    $scope.currentEventType = [
     won.WONMSG.connectionMessage,
     won.WONMSG.connectMessage,
     won.WONMSG.openMessage,
     won.WONMSG.hintMessage,
     won.WONMSG.closeMessage];

    $rootScope.postClosed=false;
    //$scope.title = 'New Flat, Need Furniture';
        $scope.need = applicationStateService.getCurrentNeed().then(function(need){
            $rootScope.postClosed =  applicationStateService.checkIfNeedIsInactive(need);
        });

    $scope.img_path = '/owner/images/thumbnail_demo.jpg';




    $rootScope.postShouldBeClosed = false;
    $rootScope.postShouldBeReopened = false;


    //settings

    if ($routeParams.id != null) {

        userService.setUpRegistrationForUserWithPrivateLink($routeParams.id).then(
            function success() {
                // calling replace() removes it from the browser history when clicking back button,
                // i.e. if I enter a private link A and then change the session (time-out, log-in with
                // other user account or create a new private link B) clicking 'back' in the browser won't
                // display my private link A. This is a big plus, but still needs some work: at least in
                // Chrome, I can still see my private link in the browser history page.
                // TODO bug: sometimes, when entering the private link page (try 4-10 times in a raw)
                // connections are not loaded...
                $location.url('/private-link').replace();
            }
            //TODO error
        );
        return;
    }

    if (userService.isPrivateUser()) {
        $scope.privateLink = applicationStateService.getPrivateLink(userService.getUserName());
    }

    $scope.publicLink = applicationStateService.getPublicLink(applicationStateService.getCurrentNeedURI());

        // default settings for all users
    $scope.notificationEmail = '';
    $scope.emailChange = false;
    $scope.notificationChecks = {
        val1: false,
        val2: false,
        val3: false,
        changed: false
    };
    // ask back-end for current need of the user settings
        //TODO or load need settings in main when loading current need?
    userService.getSettingsForNeed(applicationStateService.getCurrentNeedURI()).then(
        function success(settingsData) {
            $scope.notificationEmail = settingsData.email;
            if ($scope.notificationEmail == null) {
                $scope.emailChange = true;
            } else {
                $scope.emailChange = false;
            }
            $scope.notificationChecks.val1 = settingsData.notifyConversations;
            $scope.notificationChecks.val2 = settingsData.notifyRequests;
            $scope.notificationChecks.val3 = settingsData.notifyMatches;
            $scope.notificationChecks.changed = false;
        }
        //TODO error
    );

    $scope.prevMessageId = null;

    $scope.rateValue = 0;
    $scope.showConfirmationDialogForDeclineRequest = false;
    $scope.showConfirmationDialogForCancelRequest = false;
    $scope.showEditButtons = false;
    $scope.showPencil = true;
    $scope.textAreaContent = '';
    $scope.showConfirmationDialogForRemoveMatch = false;

    $scope.showWarningForRating = false;
    $scope.showMatchControl = false;



    $scope.showPublic = function() {
        return userService.isAuth();
    };

    $scope.showPrivateUser = function() {
        return userService.isPrivateUser();
    };

    $scope.settingsCollapseClick= function() {
        $scope.settingsCollapsed = !$scope.settingsCollapsed;
    };

    $scope.notifyMe = function() {
        $scope.emailChange = false;
        $scope.notificationChecks.changed = false;

        //send to backend:

        var setting = {
            email: $scope.notificationEmail,
            notifyConversations: $scope.notificationChecks.val1,
            notifyRequests: $scope.notificationChecks.val2,
            notifyMatches : $scope.notificationChecks.val3,
            needUri: applicationStateService.getCurrentNeedURI(),
            username: userService.getUnescapeUserName()
        };

        userService.setSettingsForNeed(setting).then(
            function success(settingsData) {
                // this is expected, do nothing
            },
            function error(settingsData) {
                $scope.emailChange = true;
                $scope.notificationChecks.changed = true;
                //TODO error notification
            }
        );

    };
    $scope.changeEmailAddress = function() {
        $scope.emailChange = true;
    };

    $scope.notificationChecksChanged = function() {
        $scope.notificationChecks.changed = true;
    }

    //Post Options
    $scope.newMessage = '';
    $scope.mesagesCollapsed = false;
    $scope.messageTypeColapsed = -1;
    $scope.settingsCollapsed = true;
    $scope.postOptionsCollapsed = true;
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

    $scope.getIconClass = function (typeText) {
        if (typeText=='Conversation') return 'fa fa-comment-o fa-lg';
        else if (typeText=='Incoming Request') return 'fa fa-reply fa-lg';
        else if (typeText=='Outgoing Request') return 'fa fa-share fa-lg';
        else if (typeText=='Incoming Closed') return 'fa fa-times fa-lg';
        else if (typeText=='Outgoing Closed') return 'fa fa-trash-o fa-lg';
        else return 'fa fa-puzzle-piece fa-lg';
    };

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
                // if the chosen message is of the type that is being removed
                if ($scope.chosenMessage != null && $scope.chosenMessage.event.hasMessageType == elem) {
                    // then set the chosen message to null and reload the page
                    $scope.chosenMessage = null;
                    $location.url('/private-link');
                }
                list.splice(pos,1);
            }
        }
    }

    $scope.clickOnMessageButton = function(buttonId) {
        switch (buttonId) {
            case 1:
                toggleEventTypes([won.WONMSG.connectionMessage, won.WONMSG.openMessage]);
                break;
            case 2:
                toggleEventTypes([won.WONMSG.connectMessage]);
                break;
            case 3:
                toggleEventTypes([won.WONMSG.hintMessage]);
                break;
            default:
                $log.debug("not handling message button id " + buttonId)  ;
        }
        $scope.changeEventTypeButtonsDisplay(buttonId);
    }

    $scope.conversationCollapseClick = function() {
        $scope.conversationCollapsed = !$scope.conversationCollapsed;
    };


    $scope.messagesCollapseClick = function() {
        $scope.mesagesCollapsed = !$scope.mesagesCollapsed;
    };



    // helper function to get message according to its id from messages
    function getEventById(msgId) {
        for(var i = 0; i < $scope.lastEventOfEachConnectionOfCurrentNeed.length; i++) {
            if ($scope.lastEventOfEachConnectionOfCurrentNeed[i].event.uri == msgId) return $scope.lastEventOfEachConnectionOfCurrentNeed[i];
        }
        return null; //should not get here
    }
    // helper function to get message according to its connection id from messages
    function getEventByConnectionUri(connUri) {
        for(var i = 0; i < $scope.lastEventOfEachConnectionOfCurrentNeed.length; i++) {
            if ($scope.lastEventOfEachConnectionOfCurrentNeed[i].connection.uri == connUri) {
                return $scope.lastEventOfEachConnectionOfCurrentNeed[i];
            }
        }
        return null; //should not get here
    }

    $scope.initRater = function() {
        $("#rater").rating({
            starCaptions: function(val) {
                $scope.rateValue = val;
                return val;
            }
        });
    }

        /**
         * Updates the chosenMessage:
         * if there was no message previously chosen, or there was a messageChosen
         * different from the provided message, then chosenMessage is set to the
         * provided message. Otherwise the chosenMessage is set to null (toggled)
         * @param matchEvent the match event on which the user has clicked
         */
        $scope.clickOnMessage = function(msgEvent) {
            // msgId can't be null here
            if ($scope.prevMessageId == msgEvent.event.uri) {
                $scope.chosenMessage = $scope.chosenMessage == null ? msgEvent : null;
                applicationStateService.setCurrentConnectionURI(null);
            } else {
                $scope.chosenMessage = msgEvent;
                applicationStateService.setCurrentConnectionURI(msgEvent.connection.uri)
            }
            $scope.prevMessageId = msgEvent.event.uri;
            // store the text of this message connection previous event, if any
            // (this is temp functionality here as it probably should be loaded elsewhere - when the event itself is loaded)
            if ($scope.chosenMessage != null &&
                ($scope.chosenMessage.typeText == 'Conversation'
                    || $scope.chosenMessage.typeText == "Incoming Closed"
                    || $scope.chosenMessage.typeText == "Outgoing Closed")) {
                $scope.addConnectionLastTextMessages($scope.chosenMessage);
            }
            applicationStateService.removeEvent(applicationStateService.getUnreadEventTypeFromHasMessageType(msgEvent.event.hasMessageType), msgEvent.connection.uri)
        }

        $scope.addConnectionLastTextMessages = function(currentMessage){
            linkedDataService.getConnectionTextMessages(currentMessage.connection.uri)
                .then(function(messages){
                    currentMessage.lastMessages = won.appendStrippingDuplicates(currentMessage.lastMessages, messages);
                });
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
        $log.debug('decline clicked');
        $scope.showConfirmationDialogForDeclineRequest = true;
    }

    $scope.clickOnAcceptForInRequest = function() {
        $log.debug('accept clicked');
        // TODO add parameter for displaying specific stuff on private-link page
        wonService.open($scope.chosenMessage, $scope.newMessage);
        //$scope.prevMessageId = null;
        //$scope.chosenMessage = null;
        //clean textarea
        $scope.newMessage = "";
        $log.debug('redirect: /private-link');
        $location.url('/private-link');
    }

    $scope.clickOnNoForDeclineRequest = function() {
        $log.debug('no');
        $scope.showConfirmationDialogForDeclineRequest = false;
    }

    $scope.clickOnYesForDeclineRequest = function() {
        $log.debug('yes');
        $scope.showConfirmationDialogForDeclineRequest = false;
        // TODO add parameter for displaying specific stuff on private-link page
        wonService.closeConnection($scope.chosenMessage, $scope.newMessage);
        //$scope.prevMessageId = null;
        //$scope.chosenMessage = null;
        // clean textarea
        $scope.newMessage = "";
        $log.debug('redirect: /private-link');
        $location.url('/private-link');
    }

    // Outgoing Requests
    $scope.clickOnCancelRequest = function() {
        $log.debug('cancel clicked');
        $scope.showConfirmationDialogForCancelRequest = true;
    }

    $scope.clickOnNoForCancelRequest = function() {
        $log.debug('no');
        $scope.showConfirmationDialogForCancelRequest = false;
    }

    $scope.clickOnYesForCancelRequest = function() {
        $log.debug('yes');
        $scope.showConfirmationDialogForCancelRequest = false;
        // TODO add parameter for displaying specific stuff on private-link page
        wonService.closeConnection($scope.chosenMessage);
        $scope.prevMessageId = null;
        $scope.chosenMessage = null;
        $log.debug('redirect: /private-link');
        $location.url('/private-link');
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
        $log.debug('send a new request');
        $('#textboxInRequest').attr('disabled', '');
        $scope.showEditButtons = false;
    }

    // Matches

    $scope.clickOnRemoveMatch = function() {
        $log.debug('remove clicked');
        $scope.showConfirmationDialogForRemoveMatch = true;

    }

    $scope.clickOnNoForRemoveMatch = function() {
        $log.debug('no');
        $scope.showConfirmationDialogForRemoveMatch = false;
    }

    $scope.clickOnYesForRemoveMatch = function() {
        $log.debug('yes');
        $scope.showConfirmationDialogForRemoveMatch = false;
        wonService.closeConnection($scope.chosenMessage);
        $scope.prevMessageId = null;
        $scope.chosenMessage = null;
        // TODO add parameter for displaying specific stuff on private-link page
        $log.debug('redirect: /private-link');
        $location.url('/private-link');
    }


    $scope.clickOnRequestConversation = function() {
        $log.debug('request conversation');
        if ($scope.rateValue > 0) {
            $scope.showMatchControl = true;
            $scope.showWarningForRating = false;
        } else {
            $scope.showWarningForRating = true;
        }
    }

    $scope.clickOnSendRequestMessage = function() {
        $log.debug('send request message');
        //wonService.open($scope.chosenMessage.connection.uri);
        wonService.openSuggestedConnection($scope.chosenMessage.connection.uri, $scope.textboxInMatchModel);
        $scope.showMatchControl = false;
        // clean textarea
        $scope.textboxInMatchModel = "";
        // reset rating
        $('#rater').rating('reset');
        //$scope.chosenMessage = null;
        //$scope.prevMessageId = null;
        // TODO add parameter for displaying specific stuff on private-link page
        $log.debug('redirect: /private-link');
        $location.url('/private-link');
    }

    $scope.openFacebook = function() {
        window.open(
            'https://www.facebook.com/sharer/sharer.php?u={{publicLink}}', //+encodeURIComponent(location.href),
            'facebook-share-dialog',
            'width=626,height=436');
        return false;
    }

    $scope.clickOnPostDetail = function(needUri) {
        applicationControlService.goToNeedDetailView(needUri);
    }

    $scope.$on(won.EVENT.CONNECTION_MESSAGE_RECEIVED, function(ngEvent, eventData) {
            $scope.addConnectionLastTextMessages($scope.chosenMessage);
    });
    $scope.$on(won.EVENT.CONNECTION_MESSAGE_SENT, function(ngEvent, eventData) {
            $scope.addConnectionLastTextMessages($scope.chosenMessage);
    });

})

angular.module('won.owner').controller('CloseAndReopenPostCtrl', function ($scope,$route,$window,$location,userService,applicationStateService, $rootScope,wonService) {

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

    $scope.$on(won.EVENT.CLOSE_NEED_SENT, function(ngEvent, eventData) {
        linkedDataService.getNeed(eventData.hasSenderNeed).then(function(need){
            $rootScope.postClosed = applicationStateService.checkIfNeedIsInactive(need);
        });
    });
    $scope.$on(won.EVENT.ACTIVATE_NEED_SENT, function(ngEvent, eventData) {
        linkedDataService.getNeed(eventData.hasSenderNeed).then(function(need){
            $rootScope.postClosed = applicationStateService.checkIfNeedIsInactive(need);
        });
    });

    $scope.onClickYes = function () {
        $scope.error = '';

        //userService.logIn($scope.user).then(onLoginResponse);
        if($rootScope.postShouldBeClosed) {
            //TODO logic
            $scope.close = true;
            $rootScope.postShouldBeClosed = false;
            wonService.closeNeed($scope.currentNeed.uri);
        }

        //TODO logic
        if($rootScope.postShouldBeReopened){
            $scope.reopen = true;
            $rootScope.postShouldBeReopened = false;
            wonService.activateNeed($scope.currentNeed.uri);
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