;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import openRequestModule from '../open-request';
import connectionsOverviewModule from '../connections-overview';
import { attach, } from '../../utils';
import { actionCreators }  from '../../actions/actions';
import {
    selectNeedByConnectionUri,
    selectAllConnections
} from '../../selectors';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];

class IncomingRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.WON = won.WON;

        this.selection = 2;
        this.ownerSelection = 2; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{
            const connectionUri = decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri']));
            const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
            const connection = need && need.getIn(["connections", connectionUri]);

            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) {
                const connections = selectAllConnections(state);

                return {
                    WON: won.WON,
                    connection,
                    hasRequests: connections.filter(conn => conn.get("state") === won.WON.RequestReceived).size > 0,
                };
            }else{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                const post = state.getIn(["needs", postId]);

                return {
                    WON: won.WON,
                    post,
                    connection,
                };
            }
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    selectedConnection(connectionUri) {
        this.router__stateGo('overviewIncomingRequests', {connectionUri});
    }
}

IncomingRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewIncomingRequests', [
        overviewTitleBarModule,
        openRequestModule,
        connectionsOverviewModule,
    ])
    .controller('OverviewIncomingRequestsController', [...serviceDependencies,IncomingRequestsController])
    .name;
