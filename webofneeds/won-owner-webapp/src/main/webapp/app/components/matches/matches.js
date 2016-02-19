/**
 * Created by ksinger on 24.08.2015.
 */
;

import 'ng-redux';
import { attach } from '../../utils';
import { actionCreators }  from '../../actions/actions';

import angular from 'angular';
import overviewTitleBarModule from '../owner-title-bar';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class MatchesController {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.matchController=this;

        const selectFromState = (state)=>{


        }
        this.matches = loadMatches();
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);

    }
    loadMatches(){
        this.matches__load(
            this.$ngRedux.getState().getIn(['needs','ownNeeds']).toJS()
        )
    }

}

MatchesController.$inject = [];

export default angular.module('won.owner.components.matches', [
        overviewTitleBarModule
    ])
    .controller('MatchesController',[...serviceDependencies,MatchesController] )
    .name;
