/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';

class IncomingRequestsController {
    constructor() {}

}

IncomingRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewIncomingRequests', [
        overviewTitleBarModule
    ])
    .controller('Overview-incoming-requestsController', IncomingRequestsController)
    .name;
