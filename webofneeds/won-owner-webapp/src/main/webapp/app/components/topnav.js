/**
 * Created by ksinger on 20.08.2015.
 */
;
import won from '../won-es6.js';
import angular from 'angular';
//import loginComponent from './login.js';
//import logoutComponent from './logout.js';
import dropdownModule from './covering-dropdown.js';
import accountMenuModule from './account-menu.js';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import config from '../config.js';
import {
    connect2Redux,
} from '../won-utils.js';

import * as srefUtils from '../sref-utils.js';

function genTopnavConf() {
    let template = `
        <!-- <div class="slide-in" ng-show="self.connectionHasBeenLost">-->
        <div class="slide-in" ng-class="{'visible': self.connectionHasBeenLost}">
            <img class="si__icon"
                src="generated/icon-sprite.svg#ico16_indicator_warning_white"/>
            <span class="si__text">
                Lost connection &ndash; make sure your internet-connection
                is working, then click &ldquo;reconnect&rdquo;.
            </span>
            <button
                ng-show="self.connectionHasBeenLost && !self.reconnecting"
                ng-click="self.reconnect()"
                class="si__button">
                    Reconnect
            </button>

            <img src="images/spinner/on_red.gif"
                alt="Reconnecting&hellip;"
                ng-show="self.reconnecting"
                class="hspinner"/>
        </div>


        <nav class="topnav">

            <div class="topnav__inner">
                <div class="topnav__inner__left">
                    <a  ui-sref="{{ self.resetParamsSRef(self.loggedIn ? 'feed' : 'landingpage') }}"
                        class="topnav__button">
                            <img src="generated/icon-sprite.svg#WON_ico_header"
                                class="topnav__button__icon">
                            <span class="topnav__page-title topnav__button__caption">
                                Web of Needs &ndash; Beta
                            </span>
                    </a>
                </div>
                <div class="topnav__inner__center">
                    <a ui-sref="{{ self.resetParamsSRef('createNeed') }}"
                       class="topnav__button"
                       ng-show="self.loggedIn"> <!-- need creation possible via landingpage while not logged in -->
                        <img src="generated/icon-sprite.svg#ico36_plus"
                            class="topnav__button__icon logo">
                        <span class="topnav__button__caption">New Post</span>
                    </a>
                </div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">

                        <li ng-show="!self.loggedIn">
                            <a  ui-sref="{{ self.absSRef('signup') }}"
                                class="topnav__signupbtn"
                                ng-show="!self.open">
                                    Sign up
                            </a>
                        </li>

                        <li>
                            <won-account-menu>
                            </won-account-menu>
                        </li>

                    </ul>
                </div>
            </div>
        </nav>


        <nav class="loginOverlay" ng-show="self.open && !self.loggedIn">
            <div class="lo__inner">
                <div class="lo__inner__right">
                    <won-login open="self.open"></won-login>
                </div>
            </div>
        </nav>


        <nav class="loginOverlay" ng-show="self.open && self.loggedIn">
            <div class="lo__inner">
                <div class="lo__inner__right">
                    <won-logout open="self.open"></won-logout>
                </div>
            </div>
        </nav>


        <div class="topnav__toasts">
            <div class="topnav__toasts__element" 
            ng-class="{ 'info' : toast.get('type') === self.WON.infoToast,
                        'warn' : toast.get('type') === self.WON.warnToast,
                        'error' : toast.get('type') === self.WON.errorToast
                      }"
            ng-repeat="toast in self.toastsArray">
                <img
                    class="topnav__toasts__element__close clickable"
                    ng-click="self.toasts__delete(toast)"
                    src="generated/icon-sprite.svg#ico27_close"/>
                <div class="topnav__toasts__element__text">
                    <p ng-show="!toast.get('htmlEnabled')">
                        {{toast.get('msg')}}
                    </p>
                    <p ng-show="toast.get('htmlEnabled')"
                        ng-bind-html="toast.get('msg')">
                    </p>
                    <p ng-show="toast.get('type') === self.WON.errorToast">
                        If the problem persists please contact
                        <a href="mailto:{{::self.config.adminEmail}}">
                            {{::self.config.adminEmail}}
                        </a>
                    </p>
                </div>

            </div>
        </div>
    `;

    const serviceDependencies = ['$ngRedux', '$scope', '$sanitize', /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);
            Object.assign(this, srefUtils); // bind srefUtils to scope
            this.config = config;

            window.tnc4dbg = this;

            const selectFromState = (state) => ({
                WON: won.WON,
                loginVisible: state.get('loginVisible'),
                open: state.get('loginVisible'), // TODO interim while transition to redux-state based solution (i.e. "loginVisible")
                loggedIn: state.getIn(['user', 'loggedIn']),
                email: state.getIn(['user','email']),
                toastsArray: state.getIn(['toasts']).toArray(),
                connectionHasBeenLost: state.getIn(['messages', 'lostConnection']), // name chosen to avoid name-clash with the action-creator
                reconnecting: state.getIn(['messages', 'reconnecting']),
            });

            connect2Redux(selectFromState, actionCreators, [], this);
        }

        showLogin() {
            this.open = true;
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        scope: {},//isolate scope to allow usage within other controllers/components
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template
    }
}

export default angular.module('won.owner.components.topnav', [
        'ngSanitize',
        //loginComponent,
        //logoutComponent,
        dropdownModule,
        accountMenuModule,
    ])
    .directive('wonTopnav', genTopnavConf)
    .name;

