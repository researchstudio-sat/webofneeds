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

        $scope.signUp = function (name, pw, pwRepeated) {

            if ($scope.signUpForm.$valid &&
                pw.length > 0 &&
                pw === pwRepeated) {

                var credentials = {
                    username : name,
                    password : pw,
                    passwordAgain : pwRepeated
                }

                //TODO spinning wheel
                userService.registerUser(credentials).then(function onRegisterResponse(response) {
                        if (response.status == "OK") {
                            $scope.error = '';

                            //angular.resetForm($scope, "registerForm"); //<-- would force retyping == bad ux
                            userService.logInAndSetUpApplicationState(credentials).then(
                                function success(){
                                    $modalInstance.close(); //close() could also take params that would be returned
                                },
                                function failure(){
                                    $scope.error = response.message;
                                });
                        } else if (response.status == "ERROR") {
                            $scope.error = response.message;
                        } else {
                            $log.debug(response.message);
                        }
                    }
                );
            }
        }
    });
