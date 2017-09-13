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

        this.selection = 2;
        this.ownerSelection = 2; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{
            const connectionUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'connectionUri']));
            const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
            const connection = need && need.getIn(["connections", connectionUri]);

            if(getIn(state, ['router', 'currentParams', 'myUri']) === undefined) {
                const connections = selectAllConnections(state);

                return {
                    WON: won.WON,
                    connection,
                    hasRequests: connections.filter(conn => conn.get("state") === won.WON.RequestReceived).size > 0,
                };
            }else{
                const postId = decodeURIComponent(getIn(state, ['router', 'currentParams', 'myUri']));
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
        this.router__stateGoCurrent({connectionUri});
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
