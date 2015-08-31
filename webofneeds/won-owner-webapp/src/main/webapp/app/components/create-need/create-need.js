/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import createNeedTitleBarModule from '../create-need-title-bar';
import posttypeSelectModule from '../posttype-select';

class CreateNeedController {
    constructor() {}

}

CreateNeedController.$inject = [];

export default angular.module('won.owner.components.createNeed', [
        createNeedTitleBarModule,
        posttypeSelectModule
    ])
    .controller('Create-needController', CreateNeedController)
    .name;
