/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import createNeedTitleBarModule from '../create-need-title-bar';

class CreateNeedController {
    constructor() {}

}

CreateNeedController.$inject = [];

export default angular.module('won.owner.components.createNeed', [
        createNeedTitleBarModule
    ])
    .controller('CreateNeedController', CreateNeedController)
    .name;
