/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';
import loginComponent from 'app/components/login';
import logoutComponent from 'app/components/logout';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';

function genTopnavConf() {
    let template = `
        <nav class="topnav">
            <div class="topnav__inner">
                <div class="topnav__inner__left">
                    <a  ui-sref="{{ self.loggedIn ? 'feed' : 'landingpage' }}" class="topnav__button">
                        <img src="generated/icon-sprite.svg#WON_ico_header" class="topnav__button__icon">
                        <span class="topnav__page-title topnav__button__caption">Web of Needs &ndash; Beta</span>
                    </a>
                </div>
                <div class="topnav__inner__center" ng-show="self.loggedIn">
                    <a ui-sref="createNeed" class="topnav__button">
                        <img src="generated/icon-sprite.svg#ico36_plus" class="topnav__button__icon">
                        <span class="topnav__button__caption">New Need</span>
                    </a>
                </div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
                        <li ng-show="!self.loggedIn">
                            <button ui-sref="landingpage({focusSignup: true})" class="topnav__button won-button--filled lighterblue" ng-show="!self.open">Sign up</button>
                        </li>
                        <li ng-show="!self.loggedIn">
                            <a class="topnav__button" ng-click="self.open = !self.open" ng-class="{'open' : self.open}">
                                <span class="topnav__button__caption__always">Sign in</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down" ng-show="!self.open" class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico16_arrow_up_hi" ng-show="self.open" class="topnav__carret">
                            </a>
                        </li>
                        <li ng-show="self.loggedIn" ng-click="self.open = !self.open">
                            <a class="topnav__button">
                                <span class="topnav__button__caption">{{self.email}}</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down" class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico36_person" class="topnav__button__icon">
                            </a>
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
    `;

    const serviceDependencies = ['$q', '$ngRedux', '$scope', /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);
            window.tnc4dbg = this;

            const selectFromState = (state) => ({
                loggedIn: state.getIn(['user', 'loggedIn']),
                email: state.getIn(['user','email'])
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

