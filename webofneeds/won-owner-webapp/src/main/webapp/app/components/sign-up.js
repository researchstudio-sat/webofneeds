/**
 * Created by ksinger on 16.08.2017.
 */


import angular from 'angular';
import 'ng-redux';
import won from '../won-es6';

import {
    attach,
} from '../utils';
import {
    actionCreators
} from '../actions/actions';



const serviceDependencies = ['$ngRedux', '$scope'/*'$routeParams' /*injections as strings here*/];

function genComponentConf() {
    const template = `
        <div class="trygrid">
            <div class="tg__signup">
                <div class="block" ng-form name="registerForm">
                    <div class="header">
                        Sign up here.
                    </div>

                    <input id="registerEmail" name="email" placeholder="Email address"
                           ng-model="self.email" ng-class="{'ng-invalid': self.registerError}" required type="email"
                           ng-keyup="self.registerReset() || ($event.keyCode == 13 && self.passwordAgain === self.password && self.register({email: self.email, password: self.password}))"/>

                    <span class="tg__errormsg" ng-show="registerForm.email.$error.email">
                        Not a valid E-Mail address
                    </span>
                    <span class="tg__errormsg" ng-show="self.registerError">
                        {{self.registerError}}
                    </span>

                    <input name="password" placeholder="Password" ng-minlength="6"
                           ng-model="self.password" required type="password"
                           ng-keyup="self.registerReset() || ($event.keyCode == 13 && self.passwordAgain === self.password && self.register({email: self.email, password: self.password}))"/>

                    <span class="tg__errormsg" ng-show="registerForm.password.$error.minlength">
                        Password too short, must be at least 6 Characters
                    </span>

                    <input name="password_repeat" placeholder="Repeat Password" ng-minlength="6"
                           ng-model="self.passwordAgain" required type="password" compare-to="self.password"
                           ng-keyup="self.registerReset() || ($event.keyCode == 13 && self.passwordAgain === self.password && self.register({email: self.email, password: self.password}))"/>

                    <span class="tg__errormsg" ng-show="registerForm.password_repeat.$error.compareTo">
                        Password is not equal
                    </span>
                </div>
                <button id="signup" class="won-button--filled darkgray"
                        ng-disabled="registerForm.$invalid" ng-click="::self.register({email: self.email, password: self.password})">
                    That’s all we need. Let’s go!
                </button>
            </div>
        </div>
    `;

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            const select = (state) => ({
                //focusSignup: state.getIn(['router', 'currentParams', 'focusSignup']) === "true",
                loggedIn: state.getIn(['user','loggedIn']),
                registerError: state.getIn(['user','registerError'])
            });

            const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
            this.$scope.$on('$destroy',disconnect);
        }
    }

    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {/*scope-isolation*/},
        template: template
    }
}

export default angular.module('won.owner.components.signUp', [
    ])
    .directive('wonSignUp', genComponentConf)
    .name;
