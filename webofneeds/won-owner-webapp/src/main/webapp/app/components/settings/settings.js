/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import settingsTitleBarModule from '../settings-title-bar';

class SettingsController {
    constructor() {}

}

SettingsController.$inject = [];

export default angular.module('won.owner.components.settings', [
        settingsTitleBarModule
    ])
    .controller('SettingsController', SettingsController)
    .name;
