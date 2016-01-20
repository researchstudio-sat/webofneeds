;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import matchesFlowItemModule from '../matches-flow-item';
import matchesGridItemModule from '../matches-grid-item';
import matchesListItemModule from '../matches-list-item';
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
                matchesOfNeed:this.mapToMatches(this.matches)
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
        if(this.matches){

            Object.keys(this.matches).forEach(function(key){

                if(!needMap[this.matches[key].ownNeed.uri]){
                    let connections = [this.matches[key]]
                    needMap[this.matches[key].ownNeed.uri]=connections
                }else{
                    needMap[this.matches[key].ownNeed.uri].push(this.matches[key])
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
    matchesListItemModule
])
    .controller('OverviewMatchesController', [...serviceDependencies,OverviewMatchesController])
    .name;

