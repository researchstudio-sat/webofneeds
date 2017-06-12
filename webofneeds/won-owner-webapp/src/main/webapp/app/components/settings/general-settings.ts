;

import angular from 'angular';
import accountSettingsModule from '../settings/account-settings';
import addressSettingsModule from '../settings/address-settings';
import notificationSettingsModule from '../settings/notification-settings';

function genComponentConf() {
    let template = `
        <won-account-settings></won-account-settings>
        <div class="flexrow">
            <won-address-settings items="self.addresses"></won-address-settings>
            <won-notification-settings items="self.notifications"></won-notification-settings>
        </div>
            `;

    class Controller {
        constructor() {
            this.addresses = [{name: "address1"}, {name: "address2"}];
            this.notifications =  [{name: "notification1"}, {name: "notification2"}];
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}
export default angular.module('won.owner.components.generalSettings', [
        accountSettingsModule,
        addressSettingsModule,
        notificationSettingsModule,
    ])
    .directive('wonGeneralSettings', genComponentConf)
    .name;
