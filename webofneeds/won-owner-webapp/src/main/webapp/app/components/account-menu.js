/**
 * Created by ksinger on 01.09.2017.
 */
;
import angular from 'angular';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    connect2Redux,
} from '../won-utils.js';

import dropdownModule from './covering-dropdown.js';
import loginFormModule from './login-form.js';
import loggedInMenuModule from './logged-in-menu.js';

import * as srefUtils from '../sref-utils.js';

function genLogoutConf() {
    let template = `
        <won-dropdown class="dd-right-aligned-header">
            <won-dd-header class="topnav__button">


                <span class="topnav__button__caption hide-in-responsive">
                    {{self.loggedIn? self.email : "Sign In"}}
                </span>
                <img
                    src="generated/icon-sprite.svg#ico16_arrow_down"
                    class="topnav__carret">
                <img
                    class="topnav__button__icon"
                    src="generated/icon-sprite.svg#ico36_person">


            </won-dd-header>
            <won-dd-menu>


                <won-logged-in-menu
                    class="am__menu--loggedin"
                    ng-show="self.loggedIn">
                </won-logged-in-menu>


                <won-login-form
                    class="am__menu--loggedout"
                    ng-show="!self.loggedIn">
                </won-login-form>


            </won-dd-menu>
        </won-dropdown>
    `;

    const serviceDependencies = ['$state', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

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

export default angular.module('won.owner.components.accountMenu', [
    dropdownModule,
    loginFormModule,
    loggedInMenuModule,
])
    .directive('wonAccountMenu', genLogoutConf)
    .name;
