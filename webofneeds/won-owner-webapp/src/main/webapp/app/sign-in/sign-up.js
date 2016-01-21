/**
 * Created by ksinger on 18.02.2015.
 */

;
angular.module('won.owner').controller('SignUpModalInstanceCtrl',
    function ($scope, $route, $window, $location, $http,
              $modalInstance, applicationStateService, userService) {
        // function ($scope, $route, $window, $location, userService) { <-- only these + modalInst (?)
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

        $scope.username = '';
        $scope.password = '';
        $scope.passwordAgain = '';
        $scope.error = '';
        $scope.processing = false;

        $scope.signUp = function (name, pw, pwRepeated) {
            if ($scope.signUpForm.$valid &&
                pw.length > 0 &&
                pw === pwRepeated) {

                var credentials = {
                    username : name,
                    password : pw
                }

                $scope.processing = true;
                userService.registerUser(credentials).then(function (response) {
                    if (response.status === "OK") {
                        $scope.error = '';
                        return userService.logInAndSetUpApplicationState(credentials)
                    } else {
                        throw new Error(response.message);
                    }
                }).then( function(){
                    $modalInstance.close(); //close() could also take params that would be returned
                }).catch (function(response) {
                    $scope.error = response.message;
                }).finally(function(){
                    $scope.processing = false;
                });
            }
        }
    });
