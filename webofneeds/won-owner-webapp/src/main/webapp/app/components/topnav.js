/**
 * Created by ksinger on 20.08.2015.
 */
;
import won from '../won-es6';
import angular from 'angular';
import loginComponent from 'app/components/login';
import logoutComponent from 'app/components/logout';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import config from '../config';

function genTopnavConf() {
    let template = `
        <!-- <div class="slide-in" ng-show="self.connectionHasBeenLost">-->
        <div class="slide-in" ng-class="{'visible': self.connectionHasBeenLost}">
            <img class="si__icon" src="generated/icon-sprite.svg#ico16_indicator_warning_white"/>
            <span class="si__text">Lost connection &ndash; make sure your internet-connection
            is working, then click &ldquo;reconnect&rdquo;.</span>
            <button
                ng-show="self.connectionHasBeenLost && !self.reconnecting" ng-click="self.reconnect()"
                class="si__button won-button outline white">
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
                    <a  ng-click="self.router__stateGoResetParams(self.loggedIn ? 'feed' : 'landingpage')" class="topnav__button">
                        <img src="generated/icon-sprite.svg#WON_ico_header" class="topnav__button__icon">
                        <span class="topnav__page-title topnav__button__caption">
                            Web of Needs &ndash; Beta
                        </span>
                    </a>
                </div>
                <div class="topnav__inner__center">
                    <a ng-click="self.router__stateGoResetParams('createNeed')" class="topnav__button">
                        <img src="generated/icon-sprite.svg#ico36_plus" class="topnav__button__icon logo">
                        <span class="topnav__button__caption">New Need</span>
                    </a>
                </div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
                        <li ng-show="!self.loggedIn">
                            <button
                                ng-click="self.router__stateGoAbs('landingpage', {focusSignup: true})"
                                class="topnav__button won-button--filled lighterblue"
                                ng-show="!self.open">
                                    Sign up
                            </button>
                        </li>
                        <li ng-show="!self.loggedIn">
                            <a class="topnav__button"
                                ng-click="self.showLogin()"
                                ng-class="{'open' : !self.focusSignup && self.open}">
                                    <span class="topnav__button__caption__always">Sign in</span>
                                    <img src="generated/icon-sprite.svg#ico16_arrow_down"
                                        ng-show="!self.open" class="topnav__carret">
                                    <img src="generated/icon-sprite.svg#ico16_arrow_up_hi"
                                        ng-show="self.open" class="topnav__carret">
                            </a>
                        </li>
                        <li ng-show="self.loggedIn" class="clickable" ng-click="self.showLogin()">
                            <a class="topnav__button">
                                <span class="topnav__button__caption">{{self.email}}</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down"
                                    class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico36_person"
                                    class="topnav__button__icon">
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>


        <nav class="loginOverlay" ng-show="!self.focusSignup && self.open && !self.loggedIn">
            <div class="lo__inner">
                <div class="lo__inner__right">
                    <won-login open="self.open"></won-login>
                </div>
            </div>
        </nav>


        <nav class="loginOverlay" ng-show="!self.focusSignup && self.open && self.loggedIn">
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
                    <p>{{toast.get('msg')}}</p>
                    <p ng-show="toast.get('type') === self.WON.errorToast">
                        If the problem persists please contact
                        <a href="mailto:{{::self.config.adminEmail}}">{{::self.config.adminEmail}}</a>
                    </p>
                </div>

            </div>
        </div>
    `;

    const serviceDependencies = ['$ngRedux', '$scope', /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);
            this.config = config;

            window.tnc4dbg = this;

            const selectFromState = (state) => ({
                WON: won.WON,
                loginVisible: state.get('loginVisible'),
                open: state.get('loginVisible'), // TODO interim while transition to redux-state based solution (i.e. "loginVisible")
                loggedIn: state.getIn(['user', 'loggedIn']),
                focusSignup: state.getIn(['router', 'currentParams', 'focusSignup']) === "true",
                email: state.getIn(['user','email']),
                toastsArray: state.getIn(['toasts']).toArray(),
                connectionHasBeenLost: state.getIn(['messages', 'lostConnection']), // name chosen to avoid name-clash with the action-creator
                reconnecting: state.getIn(['messages', 'reconnecting']),
            });

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy',disconnect);
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
        loginComponent,
        logoutComponent
    ])
    .directive('wonTopnav', genTopnavConf)
    .name;

