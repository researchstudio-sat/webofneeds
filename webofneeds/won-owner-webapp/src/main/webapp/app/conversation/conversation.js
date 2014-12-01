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
 * Created by LEIH-NB on 24.10.2014.
 */

angular.module('won.owner')
    .directive('textMessage', function factory(){
        return{
            restrict: 'AE',
            scope : {
                chosenMessage: '=',
                message: '=',
                id: '@'
            },
            link: function(scope, element, attrs) {
                scope.getContentUrl = function () {
                    if (scope.message.senderNeed == scope.chosenMessage.connection.belongsToNeed) {
                        return "/owner/app/conversation/sent-text-message.html";
                    } else return "/owner/app/conversation/received-text-message.html";
                }
            },
            template: '<div ng-attr-id="id" class="row col-lg-12" ng-include="getContentUrl()"></div>',
            controller : function($scope){

                $scope.getImageForMessage = function(message){
                    if(message.senderNeed == $scope.chosenMessage.connection.belongsToNeed){
                        return "/owner/images/house.gif";
                    }else return "/owner/images/User-blue-icon.png";
                }

            }

        }
    })
angular.module('won.owner')
    .directive('conversation', function factory($log){
        return{
            restrict: 'AE',
            templateUrl : 'app/conversation/conversation.html',
            scope : {
                chosenMessage: '='
            },

            controller : function($scope, $location, $log,$anchorScroll, $rootScope, wonService){
                $scope.newMessage = '';
                $scope.showConversations = function() {
                    if($scope.chosenMessage != null){
                        $log.debug("checking show conversations") ;
                        if($scope.chosenMessage.typeText == 'Conversation'
                            || ($scope.chosenMessage.typeText == "Incoming Closed" && $scope.showPublic())
                            || $scope.chosenMessage.typeText == "Outgoing Closed" && $scope.showPublic()) return true;
                    }else return false;
                }
                $scope.messageIndex ='';
                $scope.showPublic = function(){
                    if($scope.chosenMessage != null && $scope.chosenMessage.lastMessages != null && $scope.chosenMessage.lastMessages.length>0){
                        return true;
                    }else{
                        return false;
                    }
                }
                $scope.showSendMessageForm = function() {
                    if($scope.chosenMessage != null) {
                        if ($scope.chosenMessage.typeText == "Incoming Closed"
                            || $scope.chosenMessage.typeText == "Outgoing Closed") {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
                $scope.sendMessage = function() {
                    //TODO logic
                    wonService.textMessage($scope.newMessage, $scope.chosenMessage.connection.uri);
                    // clean textarea
                    $scope.newMessage = "";
                };
                $scope.goToLastMessage = function(id){
                    $location.hash(id);
                    $anchorScroll();
                };
                $scope.clickOnLeaveConversation = function() {
                    $log.debug('leave conversation clicked');
                    $scope.showConfirmationDialogForLeaveConversation = true;
                };
                $scope.clickOnNoForLeaveConversation = function() {
                    $log.debug('no');
                    $scope.showConfirmationDialogForLeaveConversation = false;
                }

                $scope.clickOnYesForLeaveConversation = function() {
                    $log.debug('yes');
                    $scope.showConfirmationDialogForLeaveConversation = false;
                    wonService.closeConnection($scope.chosenMessage, $scope.newMessage);
                    // clean textarea
                    $scope.newMessage = "";
                    //$location.path('/private-link');
                }

            },
            link: function(scope, element, attrs){
               $log.debug("conversation container");
            }

        }
    })
angular.module('won.owner')
    .directive('notifyRenderFinished', function factory($timeout,$log) {
        return{
            restrict: 'A',
            controller: function($scope, $rootScope,$anchorScroll,$location){

                $scope.$on('RenderFinishedEvent', function(scope, element, attrs){
                    $timeout(function(){
                        var old = $location.hash();
                        $location.hash($scope.$index);
                        $anchorScroll();
                        $location.hash(old);
                    },200)
                });

            },
            link: function(scope, element, attrs){

                $log.debug('notify render finished directive') ;
                if (scope.$last){
                    $timeout(function(){
                            scope.$emit('RenderFinishedEvent', element, attrs);

                    }, 100)


                 }
            }
        };
    });