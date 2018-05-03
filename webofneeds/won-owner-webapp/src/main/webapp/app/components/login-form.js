/**
 * Created by ksinger on 01.09.2017.
 */
;
import angular from 'angular';
import {
    attach,
    delay,
    dispatchEvent,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    connect2Redux,
} from '../won-utils.js';

import * as srefUtils from '../sref-utils.js';

function genLoginConf() {
    let template = `
        <form ng-submit="::self.login({email: self.email, password: self.password, rememberMe: self.rememberMe}, {redirectToFeed: false})"
            id="loginForm"
            class="loginForm"
        >
            <input
                id="loginEmail"
                placeholder="Email address"
                ng-model="self.email"
                type="email"
                required
                autofocus
                ng-keyup="self.formKeyUp($event)"/>
            <span class="wl__errormsg">
                {{self.loginError}}
            </span>
            <input
                id="loginPassword"
                placeholder="Password"
                ng-model="self.password"
                type="password"
                required
                ng-keyup="self.formKeyUp($event)"/>

            <!-- <input type="submit" value="LOGIN"/>-->
            <button
                class="won-button--filled lighterblue"
                ng-disabled="loginForm.$invalid">
                    Sign In
            </button>
            <input
                id="remember-me"
                ng-model="self.rememberMe"
                type="checkbox"/> Remember me
        </form>
        <div class="wl__register">
            No account yet?
            <a href="{{ self.absHRef(self.$state, 'signup') }}" ng-click="self.hideMainMenuDisplay()">
                Sign up
            </a>
        </div>`;

    const serviceDependencies = ['$ngRedux', '$scope', '$element', '$state' /*'$routeParams' /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);
            Object.assign(this, srefUtils); // bind srefUtils to scope

            window.lic4dbg = this;

            this.email = "";
            this.password = "";
            this.rememberMe = false;

            const login = (state) => ({
                loggedIn: state.getIn(['user','loggedIn']),
                loginError: state.getIn(['user','loginError'])
            });

            connect2Redux(login, actionCreators, [], this);
        }

        formKeyUp(event) {
            this.typedAtLoginCredentials();
            if(event.keyCode == 13) {
                this.login({
                    email: this.email,
                    password: this.password,
                    rememberMe: this.rememberMe,
                }, {
                    redirectToFeed: false
                });
            }
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.loginForm', [])
    .directive('wonLoginForm', genLoginConf)
    .name;
