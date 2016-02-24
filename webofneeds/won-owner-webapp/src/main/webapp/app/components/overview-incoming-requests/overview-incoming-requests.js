/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import requestItemLineModule from '../request-item-line';
import openRequestModule from '../open-request';
import { attach,mapToMatches } from '../../utils';
import { actionCreators }  from '../../actions/actions';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];

class IncomingRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        window.oireq = this;
        this.selection = 2;

        const selectFromState = (state)=>{

            return {
                incomingRequests: Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                    .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.RequestReceived && state.getIn(['events',conn.connection.uri]) !== undefined){
                            return true
                        }
                    }),
                incomingRequestsOfNeed:mapToMatches(Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                    .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.RequestReceived){
                            return true
                        }
                    }))
            };
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);
    }
}

IncomingRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewIncomingRequests', [
        overviewTitleBarModule,
        requestItemLineModule,
        openRequestModule
    ])
    .controller('OverviewIncomingRequestsController', [...serviceDependencies,IncomingRequestsController])
    .name;
