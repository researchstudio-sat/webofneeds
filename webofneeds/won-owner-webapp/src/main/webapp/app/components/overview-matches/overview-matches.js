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

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class OverviewMatchesController {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc=this;

        this.selection = 3;
        this.ownerSelection = 1; //ONLY NECESSARY FOR VIEW WITH NEED
        this.labels = labels;

        this.viewType = 0;

        const selectFromState = (state)=>{
            if(state.getIn(['router', 'currentParams', 'myUri']) === undefined){
                return {
                    matches: Object.keys(state.getIn(['connections','connections']).toJS())
                        .map(key=>state.getIn(['connections','connections']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested){
                                return true
                            }
                        }),
                    matchesOfNeed:mapToMatches(Object.keys(state.getIn(['connections','connections']).toJS())
                        .map(key=>state.getIn(['connections','connections']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested){
                                return true
                            }
                        }))
                };
            }else{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                return {
                    post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                    matches: Object.keys(state.getIn(['connections','connections']).toJS())
                        .map(key=>state.getIn(['connections','connections']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested && conn.ownNeed.uri === postId){
                                return true
                            }
                        }),
                    matchesOfNeed:mapToMatches(Object.keys(state.getIn(['connections','connections']).toJS())
                        .map(key=>state.getIn(['connections','connections']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested && conn.ownNeed.uri === postId){
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

