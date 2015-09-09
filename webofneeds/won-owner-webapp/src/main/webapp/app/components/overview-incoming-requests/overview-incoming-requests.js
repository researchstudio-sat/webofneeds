/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';

class IncomingRequestsController {
    constructor() {
        this.selection = 2;
    }
}

IncomingRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewIncomingRequests', [
        overviewTitleBarModule
    ])
    .controller('OverviewIncomingRequestsController', IncomingRequestsController)
    .name;
