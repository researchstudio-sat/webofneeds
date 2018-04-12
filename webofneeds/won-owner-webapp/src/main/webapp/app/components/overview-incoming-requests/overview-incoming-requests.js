;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar.js';
import openRequestModule from '../open-request.js';
import connectionsOverviewModule from '../connections-overview.js';
import {
    attach,
    getIn,
} from '../../utils.js';
import { actionCreators }  from '../../actions/actions.js';
import {
    selectNeedByConnectionUri,
    selectAllConnections
} from '../../selectors.js';
import {
    resetParams,
} from '../../configRouting.js';


const serviceDependencies = ['$ngRedux', '$scope'];

class IncomingRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.WON = won.WON;
        this.resetParams = resetParams;
        this.open = {};

        this.selection = 2;
        this.ownerSelection = 2; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{
            const connectionUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'connectionUri']));
            const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
            const connection = need && need.getIn(["connections", connectionUri]);
            const connectionType = need && connectionUri && need.getIn(["connections", connectionUri, 'state']);

            const connections = selectAllConnections(state);

            return {
                WON: won.WON,
                connection,
                connectionType,
                //hasRequests: connections.filter(conn => conn.get("state") === won.WON.RequestReceived).size > 0,
                //hasOpenConnections: connections.filter(conn => conn.get("state") !== won.WON.Closed).size > 0,
                hasConnections: connections.size > 0,
                open,
            };
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    selectedNeed(needUri) {
        this.router__stateGoCurrent({connectionUri: undefined, postUri: needUri})
    }

    selectedConnection(connectionUri) {
        this.markAsRead(connectionUri);
        this.router__stateGoCurrent({connectionUri});
    }

    markAsRead(connectionUri){
        const need = selectNeedByConnectionUri(this.$ngRedux.getState(), connectionUri);
        const connections = need && need.get("connections");
        const connection = connections && connections.get(connectionUri);

        if(connection && connection.get("unread") && connection.get("state") !== won.WON.Connected) {
            const payload = {
                connectionUri: connectionUri,
                needUri: need.get("uri"),
            };

            this.connections__markAsRead(payload);
        }
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
