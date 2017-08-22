/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';
import {
    attach,
    delay,
    dispatchEvent,
} from '../utils';
import { actionCreators }  from '../actions/actions';

import * as srefUtils from '../sref-utils';

function genLoginConf() {
    let template = `<a class="wl__button" ng-click="self.hideLogin()">
                        <span class="wl__button__caption">Sign in</span>
                        <img src="generated/icon-sprite.svg#ico16_arrow_up_hi" class="wl__button__carret">
                    </a>
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
                    <!-- TODO: Implement remember me and forgot password --><!--div class="wl__table">
                        <div class="wlt__left">
                            <input type="checkbox" ng-model="self.rememberMe" id="rememberMe"/><label for="rememberMe">Remember me</label>
                        </div>
                        <div class="wlt__right">
                            <a href="#">Forgot Password?</a>
                        </div>
                    </div>-->
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

            const disconnect = this.$ngRedux.connect(login, actionCreators)(this);
            this.$scope.$on('$destroy',disconnect);

            this.autofillHack();

            this.$scope.$watch(() => this.loginVisible, (newVis, oldVis) => {
                console.log('lic4dbg loginVisible: ', oldVis, newVis);
                //if(newVis && !oldVis) {
                this.autofillHack();
                //}
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
        scope: {open: '='},
        template: template
    }
}

export default angular.module('won.owner.components.login', [])
    .directive('wonLogin', genLoginConf)
    .name;

