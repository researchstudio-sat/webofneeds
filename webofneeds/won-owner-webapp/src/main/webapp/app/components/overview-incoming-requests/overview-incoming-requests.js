;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import requestItemLineModule from '../request-item-line';
import openRequestModule from '../open-request';
import { attach,mapToMatches } from '../../utils';
import { actionCreators }  from '../../actions/actions';
import { selectAllByConnections } from '../../selectors';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];

class IncomingRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        window.oireq = this;

        this.selection = 2;
        this.ownerSelection = 2; //ONLY NECESSARY FOR VIEW WITH NEED

        const selectFromState = (state)=>{
            const connectionsDeprecated = selectAllByConnections(state).toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
            const connectionUri = decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri']));

            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) {
                return {
                    connection: state.getIn(['connections', connectionUri]),
                    incomingRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>
                            conn.connection.hasConnectionState === won.WON.RequestReceived &&
                            state.getIn(['events', conn.connection.uri]) !== undefined
                        ),
                    incomingRequestsOfNeed: mapToMatches(Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>
                            conn.connection.hasConnectionState === won.WON.RequestReceived
                        )
                    )
                };
            }else{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                return {
                    post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                    connection: state.getIn(['connections', connectionUri]),
                    incomingRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>
                            conn.connection.hasConnectionState === won.WON.RequestReceived &&
                            state.getIn(['events', conn.connection.uri]) !== undefined && conn.ownNeed['@id'] === postId
                        ),
                    incomingRequestsOfNeed: mapToMatches(Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>
                            conn.connection.hasConnectionState === won.WON.RequestReceived && conn.ownNeed['@id'] === postId
                        )
                    )
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
