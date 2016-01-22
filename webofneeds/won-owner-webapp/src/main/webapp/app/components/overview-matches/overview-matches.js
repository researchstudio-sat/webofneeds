;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import matchesFlowItemModule from '../matches-flow-item';
import matchesGridItemModule from '../matches-grid-item';
import matchesListItemModule from '../matches-list-item';
import sendRequestModule from '../send-request';

import { attach,mapToMatches,getType } from '../../utils';
import { actionCreators }  from '../../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class OverviewMatchesController {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc=this;

        this.selection = 3;

        this.viewType = 0;

        const selectFromState = (state)=>{

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
        }
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
      //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);
    }

    loadMatches(){
        this.matches__load(
            this.$ngRedux.getState().getIn(['needs','needs']).toJS()
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

