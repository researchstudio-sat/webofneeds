/**
 * Currently only handles the matches dropdown. Needs to
 * be generalized to work for the other notification types as well.
 * (problems with data binding prevented this before)
 * even better: abstract to take list of (icon,text,nr,link)
 * Created by moru on 23/10/14.
 */

//TODO use MainCtrls scope instead of rootScope to prevent js library name-clashes
app.directive(('notifDropdown'), function notifDropdownFct() { //TODO $rootScope is very hacky for a directive (reduces reusability)
        console.log("registering directive: notifDropdown");
        //console.log($rootScope.unreadEventsByNeedByType);
        var dtv = {
            restrict: 'A',
            scope: {
                unreadEventsByTypeByNeed: '=',
                unreadEventsByNeedByType: '=',
                getTypePicURI: '&',
                onClick: '&'
             },

            templateUrl: "app/header/notification-dropdown.html",
            link: function notifDropDownLink(scope,elem, attr) {
                scope.eventType = attr.eventType;
                scope.setData();
                console.log("notif dropdown link ----------");
                console.log(scope.unreadEventsByTypeByNeed);
            },
            controller: function ($scope, applicationStateService, applicationControlService) {
                $scope.setData = function(){
                    $scope.data = {};
                    switch ($scope.eventType){
                        case won.UNREAD.TYPE.HINT:
                            $scope.data.buttonImageClass = "fa fa-puzzle-piece fa-lg";
                            $scope.data.typeText =  "matches";
                            break;
                        case won.UNREAD.TYPE.MESSAGE:
                            $scope.data.buttonImageClass = "fa fa-comment-o fa-lg";
                            $scope.data.typeText =  "messages";
                            break;
                        case won.UNREAD.TYPE.CONNECT:
                            $scope.data.buttonImageClass = "fa fa-male fa-lg";
                            $scope.data.typeText =  "connects";
                            break;
                    }
                }

                $scope.setEventType= function(type){
                    $scope.eventType = type;
                }
                $scope.clickOnNotification = function (unreadEventType) {
                    applicationStateService.setEventsAsReadForType(unreadEventType);
                }
                $scope.getAllUnreadEventCountsByNeed = function(){
                    var count = 0;
                    for(var eventByNeed in $scope.unreadEventsByNeedByType) {
                        count += $scope.unreadEventsByNeedByType[eventByNeed][$scope.eventType].count;
                    }
                    return count;
                }
                $scope.getCount = function(){
                    switch ($scope.eventType) {
                        case won.UNREAD.TYPE.HINT:
                            return $scope.unreadEventsByTypeByNeed.hint.count;
                        case won.UNREAD.TYPE.MESSAGE:
                            return $scope.unreadEventsByTypeByNeed.message.count;
                        case won.UNREAD.TYPE.CONNECT:
                            return $scope.unreadEventsByTypeByNeed.connect.count;
                    }
                }
                $scope.getTypeText= function(){
                    return $scope.data.typeText;
                }
                $scope.getButtonImageClass = function(){
                    return $scope.data.buttonImageClass;
                }

            }
        }
        return dtv;
    }
);
app.directive(('notifLink'), function notifLinkFct(){
    var dtv = {
        restrict: 'E',
        transclude: true,
        templateUrl: "app/header/notification-link.html",
        link: function(scope, elem, attr){
          scope.need = scope.getUnreadEventsOfNeed();
        },
        controller: function($scope){
            $scope.getUnreadEventsOfNeed = function(){
                switch($scope.eventType){
                    case won.UNREAD.TYPE.HINT: return $scope.entry.hint;
                    case won.UNREAD.TYPE.CONNECT: return $scope.entry.connect;
                    case won.UNREAD.TYPE.MESSAGE: return $scope.entry.message;
                }
            }
            $scope.showPublic = function(){
                if($scope.need!=undefined && $scope.need.count>0){
                    return true
                }else{
                    return false;
                }
            }
            $scope.$watch('need.count',function(newVal,oldVal){
                $scope.need = $scope.getUnreadEventsOfNeed();
            })
        }
    }
    return dtv;
})


