;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import matchesFlowItemModule from '../matches-flow-item';
import matchesGridItemModule from '../matches-grid-item';
import matchesListItemModule from '../matches-list-item';
import sendRequestModule from '../send-request';

import { attach } from '../../utils';
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
                matches: state.getIn(['matches','matches']).toJS(),
                matchesOfNeed:this.mapToMatches(state.getIn(['matches','matches']).toJS())
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
    mapToMatches(connections){
        let needMap = {}
        if(connections){

            Object.keys(connections).forEach(function(key){

                if(!needMap[connections[key].ownNeed.uri]){
                    let connectionsArr = [connections[key]]
                    needMap[connections[key].ownNeed.uri]=connectionsArr
                }else{
                    needMap[connections[key].ownNeed.uri].push(connections[key])
                }
            }.bind(this))
        }
        return needMap;

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

