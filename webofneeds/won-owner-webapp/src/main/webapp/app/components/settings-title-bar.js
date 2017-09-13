/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import {
    attach,
    getIn,
} from '../utils.js';
import { actionCreators } from '../actions/actions.js';
import {
    connect2Redux,
} from '../won-utils.js';

function genComponentConf() {
    let template = `
        <nav class="settings-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="astb__inner">
                <a class="astb__inner__left" ng-click="self.back()">
                    <img src="generated/icon-sprite.svg#ico36_backarrow" class="astb__icon">
                </a>
                <div class="astb__inner__center">
                    <h1 class="astb__title">Account Settings</h1>
                    <ul class="astb__tabs">
                        <li ng-class="{'astb__tabs__selected' : self.routerState === 'settings.general'}"><a ui-sref="settings.general">
                            General Settings
                            <!--<span class="astb__tabs__unread">5</span>-->
                        </a></li>
                        <li ng-class="{'astb__tabs__selected' : self.routerState === 'settings.avatars'}"><a ui-sref="settings.avatars">
                            Manage Avatars
                            <span class="astb__tabs__unread">8</span>
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    const serviceDependencies = ['$ngRedux', '$scope'/*injections as strings here*/];
    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const selectFromState = (state) => ({
                routerState: getIn(state, ['router','currentState','name']),
            });
            connect2Redux(selectFromState, actionCreators, [], this);

        }
        back() { window.history.back() }

        select(selectedTab) {
            console.log("selection: "+selectedTab);
            this.selection = selectedTab;
            console.log("selection: "+this.selection);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {selection: "=",
                avatarcount: "="},
        template: template
    }
}
export default angular.module('won.owner.components.settingsTitleBar', [

    ])
    .directive('wonSettingsTitleBar', genComponentConf)
    .name;
