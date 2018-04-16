;

import angular from 'angular';
import sendRequestModule from '../send-request.js';
import postMessagesModule from '../post-messages.js';
import postInfoModule from '../post-info.js';
import connectionsOverviewModule from '../connections-overview.js';
import {
    attach,
    getIn,
} from '../../utils.js';
import { actionCreators }  from '../../actions/actions.js';
import {
    selectNeedByConnectionUri,
    selectAllOwnNeeds,
    selectAllConnections
} from '../../selectors.js';
import {
    resetParams,
} from '../../configRouting.js';


const serviceDependencies = ['$ngRedux', '$scope'];

class ConnectionsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.WON = won.WON;
        this.resetParams = resetParams;
        this.open = {};

        const selectFromState = (state)=>{
            const selectedPostUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'postUri']));
            const selectedPost = selectedPostUri && state.getIn(["needs", selectedPostUri]);
            const connectionUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'connectionUri']));
            const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
            const connection = need && need.getIn(["connections", connectionUri]);
            const connectionType = need && connectionUri && need.getIn(["connections", connectionUri, 'state']);

            const connections = selectAllConnections(state);
            const ownNeeds = selectAllOwnNeeds(state);

            return {
                WON: won.WON,
                selectedPost,
                connection,
                connectionType,
                hasConnections: connections && connections.size > 0,
                hasOwnNeeds: ownNeeds && ownNeeds.size > 0,
                open,
            };
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    selectedNeed(needUri) {
        this.router__stateGoCurrent({connectionUri: undefined, postUri: needUri}); //TODO: Maybe leave the connectionUri in the parameters to go back when closing a selected need
    }

    selectedConnection(connectionUri) {
        this.markAsRead(connectionUri);
        this.router__stateGoCurrent({connectionUri, postUri: undefined});
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

ConnectionsController.$inject = [];

export default angular.module('won.owner.components.connections', [
        sendRequestModule,
        postMessagesModule,
        postInfoModule,
        connectionsOverviewModule,
    ])
    .controller('ConnectionsController', [...serviceDependencies, ConnectionsController])
    .name;
