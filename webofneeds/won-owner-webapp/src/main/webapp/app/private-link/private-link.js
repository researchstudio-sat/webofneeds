/**
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/15/14
 * Time: 8:08 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner').controller('PrivateLinkCtrl', function ($scope, $location, userService, $rootScope) {

    $scope.$on("CreateNeedResponseMessageReceived", onNeedCreated = function(event, msg){
        $scope.need.needURI = msg.receiverNeed+"new";
    })
    $scope.title = 'New Flat, Need Furniture';
    $scope.img_path = '/owner/images/thumbnail_demo.jpg';
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

    // all types of messages will be shown when the page is loaded
    var msgFilterCriteria = [1, 2, 3];

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

    $scope.messageFilter = function(msg) {
        return msgFilterCriteria.indexOf(msg.type) >= 0 ? true : false;
    }

    $scope.clickOnMessageButton = function(buttonId) {
        // configuring message filter
        var critIndex = msgFilterCriteria.indexOf(buttonId);
        if (critIndex == -1) {
            msgFilterCriteria.push(buttonId);
        } else {
            msgFilterCriteria.splice(critIndex, 1);
        }

        var button = $('#' + buttonId);
        if (button.hasClass('btn-success')) {
            button.removeClass('btn-success').addClass('btn-default');
        } else {
            button.removeClass('btn-default').addClass('btn-success');
        }
    }

    $scope.conversationCollapseClick = function() {
        $scope.conversationCollapsed = !$scope.conversationCollapsed;
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

    $scope.prevMessageId = null;
    $scope.chosenMessage = null;
    // helper function to get message according to its id from messages
    function getMessageById(msgId) {
        for(var i = 0; i < $scope.messages.length; i++) {
            if ($scope.messages[i].id == msgId) return $scope.messages[i];
        }
        // should not get here
    }

    $scope.rateValue = 0;
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
            $scope.chosenMessage = $scope.chosenMessage == null ? getMessageById(msgId) : null;
        } else {
            $scope.chosenMessage = getMessageById(msgId);
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
    $scope.showConfirmationDialogForDeclineRequest = false;

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
    $scope.showConfirmationDialogForCancelRequest = false;

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
    $scope.showEditButtons = false;
    $scope.showPencil = true;
    $scope.textAreaContent = '';
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
    $scope.showConfirmationDialogForRemoveMatch = false;

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

    $scope.showWarningForRating = false;
    $scope.showMatchControl = false;
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