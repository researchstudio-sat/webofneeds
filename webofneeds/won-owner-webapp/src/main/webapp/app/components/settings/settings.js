/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';

class SettingsController {
    constructor() {}

}

SettingsController.$inject = [];

export default angular.module('won.owner.components.settings', [])
    .controller('SettingsController', SettingsController)
    .name;
