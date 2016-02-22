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
        this.ownerSelection = 2; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{
            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) {
                return {
                    incomingRequests: Object.keys(state.getIn(['connections', 'connections']).toJS())
                        .map(key=>state.getIn(['connections', 'connections']).toJS()[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestReceived && state.getIn(['events', conn.connection.uri]) !== undefined) {
                                return true
                            }
                        }),
                    incomingRequestsOfNeed: mapToMatches(Object.keys(state.getIn(['connections', 'connections']).toJS())
                        .map(key=>state.getIn(['connections', 'connections']).toJS()[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestReceived) {
                                return true
                            }
                        }))
                };
            }else{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                return {
                    post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                    incomingRequests: Object.keys(state.getIn(['connections', 'connections']).toJS())
                        .map(key=>state.getIn(['connections', 'connections']).toJS()[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestReceived && state.getIn(['events', conn.connection.uri]) !== undefined && conn.ownNeed.uri === postId) {
                                return true
                            }
                        }),
                    incomingRequestsOfNeed: mapToMatches(Object.keys(state.getIn(['connections', 'connections']).toJS())
                        .map(key=>state.getIn(['connections', 'connections']).toJS()[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestReceived && conn.ownNeed.uri === postId) {
                                return true
                            }
                        }))
                };
            }
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
