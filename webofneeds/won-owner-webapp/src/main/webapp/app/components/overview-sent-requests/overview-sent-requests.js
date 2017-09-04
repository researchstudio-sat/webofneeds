/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import openRequestModule from '../open-request';
import {
    attach,
    getIn,
} from '../../utils';
import { actionCreators }  from '../../actions/actions';

import connectionsOverviewModule from  '../connections-overview';


const serviceDependencies = ['$ngRedux', '$scope'];
class SentRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);

        this.selection = 2;
        this.ownerSelection = 3; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{
            const connectionUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'connectionUri']));
            const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
            const connection = need && need.getIn(["connections", connectionUri]);

            if(getIn(state, ['router', 'currentParams', 'myUri']) === undefined) {
                return {
                    connection,
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

SentRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewSentRequests', [
        overviewTitleBarModule,
        openRequestModule,
        connectionsOverviewModule,
    ])
    .controller('OverviewSentRequestsController', [...serviceDependencies,SentRequestsController])
    .name;
