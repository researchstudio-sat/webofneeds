/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import settingsTitleBarModule from '../settings-title-bar';
import accountSettingsModule from '../settings/account-settings';
import addressSettingsModule from '../settings/address-settings';
import notificationSettingsModule from '../settings/notification-settings';
import avatarSettingsModule from '../settings/avatar-settings';

class SettingsController {
    constructor() {
        this.selectedTab = "0";

        this.avatars = [{open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
            {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"}];

        this.addresses = [{name: "address1"}, {name: "address2"}];
        this.notifications =  [{name: "notification1"}, {name: "notification2"}];
    }
}

SettingsController.$inject = [];

export default angular.module('won.owner.components.settings', [
        settingsTitleBarModule,
        accountSettingsModule,
        addressSettingsModule,
        notificationSettingsModule,
        avatarSettingsModule
    ])
    .controller('SettingsController', SettingsController)
    .name;
