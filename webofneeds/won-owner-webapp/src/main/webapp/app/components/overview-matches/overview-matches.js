;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import matchesFlowItemModule from '../matches-flow-item';
import matchesGridItemModule from '../matches-grid-item';
import matchesListItemModule from '../matches-list-item';
import sendRequestModule from '../send-request';

import { attach,mapToMatches} from '../../utils';
import { labels } from '../../won-label-utils';
import { actionCreators }  from '../../actions/actions';
import { selectAllByConnections } from '../../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class OverviewMatchesController {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc=this;

        this.labels = labels;

        const selectFromState = (state) => {
            const allMatchesByConnections = selectAllByConnections(state)
                    .filter(conn => conn.getIn(['connection', 'hasConnectionState']) === won.WON.Suggested);

            const connectionUri = decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri']));
            const encodedPostUri =
                state.getIn(['router', 'currentParams', 'postUri']) ||
                state.getIn(['router', 'currentParams', 'myUri']); //deprecated parameter
            const postUri = decodeURIComponent(encodedPostUri)

            // either of 'tiles', 'grid', 'list'
            let layout = state.getIn(['router','currentParams','layout']);
            if(!layout) {
                layout = 'tiles';
            }

            let matchesByConnectionUri;
            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) { //overview
                matchesByConnectionUri = allMatchesByConnections.toList();
            } else { // post-owner view
                matchesByConnectionUri = allMatchesByConnections
                    .filter(conn => conn.getIn(['ownNeed', 'uri']) === postUri)
                    .toList();
            }

            return {
                layout,
                connection: state.getIn(['connections', connectionUri]),
                matches: matchesByConnectionUri.toArray(),
                matchesOfNeed: mapToMatches(matchesByConnectionUri.toJS()),//TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
                post: state.getIn(['needs','ownNeeds', postUri]),
            };
        };
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
      //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);
    }

    loadMatches(){
        this.matches__load(
            this.$ngRedux.getState().getIn(['needs','ownNeeds']).toJS()
        )
    }

}


export default angular.module('won.owner.components.overviewMatches', [
    overviewTitleBarModule,
    matchesFlowItemModule,
    matchesGridItemModule,
    matchesListItemModule,
    sendRequestModule
])
    .controller('OverviewMatchesController', [...serviceDependencies,OverviewMatchesController])
    .name;

