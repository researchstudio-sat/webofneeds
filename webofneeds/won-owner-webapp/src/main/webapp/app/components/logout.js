/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import {
    connect2Redux,
} from '../won-utils';

import * as srefUtils from '../sref-utils';

function genLogoutConf() {
    let template = `<a class="wl__button clickable" ng-click="self.hideLogin()">
                        <span class="wl__button__caption">{{self.email}}</span>
                        <img src="generated/icon-sprite.svg#ico16_arrow_up" class="wl__button__carret">
                        <img src="generated/icon-sprite.svg#ico36_person" class="wl__button__icon">
                    </a>
                    <!--<a class="wl__button" ui-sref="{{ self.absSRef('about') }}">About</a>-->
                    <button class="won-button--filled lighterblue" ng-click="::self.logout()">Sign out</button>`;
    //let template = `<a class="menu-root-btn clickable" ng-click="self.hideLogin()">
    //                    <span class="mrb__caption">{{self.email}}</span>
    //                    <img src="generated/icon-sprite.svg#ico16_arrow_up" class="mrb__carret">
    //                    <img src="generated/icon-sprite.svg#ico36_person" class="mrb__icon">
    //                </a>
    //                <!--<ul class="menu-entries">-->
    //                    <!--<a ui-sref="{{ self.absSRef('about') }}">About</a>-->
    //                <!--</ul>-->
    //                <div style="width: 100%;" class="wl__button">
    //                    <a class="wl__button"
    //                        style="margin-left: auto; margin-right: auto; color: #f04646;">
    //                            About
    //                    </a>
    //                </div>
    //                <button class="won-button--filled lighterblue" ng-click="::self.logout()">Sign out</button>`;

    const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);
            Object.assign(this, srefUtils);

            window.lopu4dbg = this;

            this.email = "";
            this.password = "";

            const logout = (state) => ({
                loggedIn: state.getIn(['user','loggedIn']),
                email: state.getIn(['user','email'])
            });

            connect2Redux(logout, actionCreators, [], this);
        }

        hideLogin() {
            this.open = false;
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

export default angular.module('won.owner.components.logout', [])
    .directive('wonLogout', genLogoutConf)
    .name;

