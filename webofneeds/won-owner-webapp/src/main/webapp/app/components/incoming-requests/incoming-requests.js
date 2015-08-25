/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import mainTabBarModule from '../main-tab-bar';

class IncomingRequestsController {
    constructor() {}

}

IncomingRequestsController.$inject = [];

export default angular.module('won.owner.components.incomingRequests', [
        mainTabBarModule
    ])
    .controller('Incoming-requestsController', IncomingRequestsController)
    .name;
