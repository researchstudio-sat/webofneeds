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

        this.selection = 3;
        this.ownerSelection = 1; //ONLY NECESSARY FOR VIEW WITH NEED
        this.labels = labels;

        this.viewType = 0;

        const selectFromState = (state) => {
            const allMatchesByConnections = selectAllByConnections(state)
                    .filter(conn => conn.getIn(['connection', 'hasConnectionState']) === won.WON.Suggested);

            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined) {
                //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
                const matchesByConnectionUri = allMatchesByConnections.toList().toJS();
                return {
                    matches: matchesByConnectionUri,
                    matchesOfNeed: mapToMatches(matchesByConnectionUri),
                };
            } else {
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                const matchesByConnectionUri = allMatchesByConnections
                    .filter(conn => conn.getIn(['ownNeed', 'uri']) === postId)
                    .toList().toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
                return {
                    //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
                    post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                    matches: matchesByConnectionUri,
                    matchesOfNeed:mapToMatches(matchesByConnectionUri)
                };
            }
        }
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

