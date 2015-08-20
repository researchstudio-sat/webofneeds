/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="settings-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="astb__inner">
                <div class="astb__inner__left">
                    <img src="generated/icon-sprite.svg#ico36_backarrow" class="astb__icon">
                </div>
                <div class="astb__inner__center">
                    <h1 class="astb__title">Account Settings</h1>
                    <ul class="astb__tabs">
                        <li class="astb__tabs__selected"><a href="#">
                            General Settings
                            <span class="astb__tabs__unread">5</span>
                        </a></li>
                        <li><a href="#">
                            Manage Avatars
                            <span class="astb__tabs__unread">5</span>
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    return {
        restrict: 'E',
        template: template
    }
}
export default angular.module('won.owner.components.settingsTitleBar', [])
    .directive('wonSettingsTitleBar', genComponentConf)
    .name;
