/**
 * Created by ksinger on 05.11.2015.
 */


/*
* This redux wrapper for the old message-service consists of:
*
* * an "agent" that registers with the service, receives messages
* from it and triggers redux actions.
* * an "component" that listens to state changes and triggers
* messages to the server via the service.
 */

import { attach } from '../utils';
//import './message-service'; //TODO still uses es5

const serviceDependencies = ['$ngRedux', /*injections as strings here*/];
class Service {
    static factory (/* arguments <- serviceDependencies */) {
      return new Service(arguments);
    }
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

    }


}
Service.factory.$inject = serviceDependencies;



export default angular.module('won.owner.service.wonserviceredux', [
    ])
    .factory('wonServiceRedux', Service.factory)
    .name
