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
import { selectAllByConnections } from '../../selectors';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];

class SentRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        window.oireq = this;

        this.selection = 2;
        this.ownerSelection = 3; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{

            const connectionsDeprecated = selectAllByConnections(state).toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.

            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) {
                return {
                    sentRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent && state.getIn(['events', conn.connection.uri]) !== undefined) {
                                return true
                            }
                        }),
                    sentRequestsOfNeed: mapToMatches(Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent) {
                                return true
                            }
                        }))
                };
            }else{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                return {
                    post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                    sentRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent && state.getIn(['events', conn.connection.uri]) !== undefined && conn.ownNeed.uri === postId) {
                                return true
                            }
                        }),
                    sentRequestsOfNeed: mapToMatches(Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=> {
                            if (conn.connection.hasConnectionState === won.WON.RequestSent && conn.ownNeed.uri === postId) {
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

SentRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewSentRequests', [
        overviewTitleBarModule,
        requestItemLineModule,
        openRequestModule
    ])
    .controller('OverviewSentRequestsController', [...serviceDependencies,SentRequestsController])
    .name;
