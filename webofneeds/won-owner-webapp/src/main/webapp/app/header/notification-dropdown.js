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
                openNeedDetailView: '&',
                getTypePicURI: '&'
             },

            templateUrl: "app/header/notification-dropdown.html",
            link: function notifDropDownLink(scope) {
                console.log("notif dropdown link ----------");
                console.log(scope.unreadEventsByTypeByNeed);
            },
            controller: function ($scope, applicationStateService) {
                $scope.clickOnNotification = function (unreadEventType) {

                    applicationStateService.setEventsAsReadForType(unreadEventType);

                }
                $scope.getHintCount = function(){
                    return $scope.unreadEventsByTypeByNeed.hint.count;
                }
            }
        }
        return dtv;
    }
);


