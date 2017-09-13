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
        <form ng-submit="::self.login({email: self.email, password: self.password}, {redirectToFeed: true})"
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
                <!--ng-click="::self.login(self.email, self.password)">-->
                    Sign in
            </button>
        </form>
        <div class="wl__register">
            No Account yet?
            <a ui-sref="{{ self.absSRef('signup') }}">
                Sign up
            </a>
        </div>`;

    const serviceDependencies = ['$ngRedux', '$scope', '$element' /*'$routeParams' /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);
            Object.assign(this, srefUtils); // bind srefUtils to scope

            window.lic4dbg = this;

            this.email = "";
            this.password = "";

            const login = (state) => ({
                loginVisible: state.get('loginVisible'),
                loggedIn: state.getIn(['user','loggedIn']),
                loginError: state.getIn(['user','loginError'])
            });

            connect2Redux(login, actionCreators, [], this);

            this.autofillHack();

            this.$scope.$watch(() => this.loginVisible, (newVis, oldVis) => {
                this.autofillHack();
            });
        }

        formKeyUp(event) {
            this.loginReset();
            if(event.keyCode == 13) {
                this.login({
                    email: this.email,
                    password: this.password
                }, {
                    redirectToFeed: true
                });
            }
        }

        /**
         * auto-fill hack. firefox doesn't fire an input event when auto-filling,
         * so we do this here manually to make sure the ng-model updates itself.
         */
        autofillHack() {
            const triggerInputEvents = () =>
                ['#loginEmail', '#loginPassword']
                    .map(id => this.$element[0].querySelector(id)) // select #<id>
                    .forEach(el => dispatchEvent(el, 'input' ));

            delay(0).then(triggerInputEvents);

            /*
             * @2s delay: to catch cases where the login is opened before the
             * page finishes loading (for FF+Keefox auto-fill triggers only then)
             * There's no guarantee that 2s are enough tough. However, as not even
             * the DOM has the correct information (e.g. via `.innerHTML`) there's
             * no better way of dealing this except for continuous event spamming.
             */
            delay(500).then(triggerInputEvents);
            delay(1000).then(triggerInputEvents);
            delay(2000).then(triggerInputEvents);

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
