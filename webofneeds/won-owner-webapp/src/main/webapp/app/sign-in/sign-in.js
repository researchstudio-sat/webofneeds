/**
 * Created by ksinger on 18.02.2015.
 */

;
angular.module('won.owner').controller('SignInModalInstanceCtrl',
    function ($scope, $route, $window, $location, $http,
              $modalInstance, applicationStateService, userService) {

        $scope.username = '';
        $scope.password = '';
        $scope.error = '';
        $scope.processing = false;

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

        $scope.signin = function(name, pw) {
            $scope.error = '';
            if($scope.signInForm.$valid) {
                /* TODO probably such functions as logInAndSetUpApplicationState()
                (that combine then->then of several
                services) should be kept in application-control-service? */

                $scope.processing = true;
                userService.logInAndSetUpApplicationState({username: name, password: pw})
                    .then(function(response) {
                        $scope.processing = false;
                        if (response.status == "OK") {
                            $modalInstance.close(); //close() could also take params that would be returned
                        } else if (response.status == "ERROR") {
                            $scope.error = response.message;
                        } else {
                            $log.debug(response.messsage);
                        }
                });
            }
        }
});
