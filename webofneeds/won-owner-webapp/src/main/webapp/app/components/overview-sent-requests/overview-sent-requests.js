/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import openRequestModule from '../open-request';
import { attach,mapToMatches } from '../../utils';
import { actionCreators }  from '../../actions/actions';
import { selectAllByConnections } from '../../selectors';

import connectionsOverviewModule from  '../connections-overview';


const serviceDependencies = ['$ngRedux', '$scope'];
class SentRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        window.oireq = this;

        this.selection = 2;
        this.ownerSelection = 3; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{

            const connectionsDeprecated = selectAllByConnections(state).toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
            const connectionUri = decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri']));

            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) {
                return {
                    connection: state.getIn(['connections', connectionUri]),
                    sentRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent && state.getIn(['events', conn.connection.uri]) !== undefined) {
                                return true
                            }
                        }),
                    /*
                    sentRequestsOfNeed: mapToMatches(Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent) {
                                return true
                            }
                        }))
                        */
                };
            }else{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                return {
                    post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                    connection: state.getIn(['connections', connectionUri]),
                    sentRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent && state.getIn(['events', conn.connection.uri]) !== undefined && conn.ownNeed['@id'] === postId) {
                                return true
                            }
                        }),
                    /*
                    sentRequestsOfNeed: mapToMatches(Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent && conn.ownNeed['@id'] === postId) {
                                return true
                            }
                        }))
                        */
                };
            }
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);
    }
    selectedConnection(connectionUri) {
        this.router__stateGo('overviewSentRequests', {connectionUri});
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
