/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="settings-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="astb__inner">
                <a class="astb__inner__left" href="javascript:void(0)" ng-click="self.back()">
                    <img src="generated/icon-sprite.svg#ico36_backarrow" class="astb__icon">
                </a>
                <div class="astb__inner__center">
                    <h1 class="astb__title">Account Settings</h1>
                    <ul class="astb__tabs">
                        <li ng-class="self.selection == 0? 'astb__tabs__selected' : ''" ng-click="self.select(0)"><a href="settings">
                            General Settings
                            <span class="astb__tabs__unread">5</span>
                        </a></li>
                        <li ng-class="self.selection == 1? 'astb__tabs__selected' : ''" ng-click="self.select(1)"><a href="settings">
                            Manage Avatars
                            <span class="astb__tabs__unread">{{self.avatarcount}}</span>
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {}
        back() { window.history.back() }

        select(selectedTab) {
            console.log("selection: "+selectedTab);
            this.selection = selectedTab;
            console.log("selection: "+this.selection);
        }
    }

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
export default angular.module('won.owner.components.settingsTitleBar', [])
    .directive('wonSettingsTitleBar', genComponentConf)
    .name;
