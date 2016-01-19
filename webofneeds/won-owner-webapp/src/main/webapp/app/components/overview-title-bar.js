/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav ng-cloak ng-show="{{true}}" class="main-tab-bar">
            <div class="mtb__inner">
                <ul class="mtb__inner__center mtb__tabs">
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 0}"><a ui-sref="feed">Feed</a></li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 1}"><a ui-sref="overviewPosts">Posts
                        <span class="mtb__tabs__unread">5</span>
                    </a></li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 2}"><a ui-sref="overviewIncomingRequests">Incoming Requests
                        <span class="mtb__tabs__unread">5</span>
                    </a></li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 3}"><a ui-sref="overviewMatches">Matches
                        <span class="mtb__tabs__unread">{{self.matchesCount.length}}</span>
                    </a></li>
                </ul>
                <div class="mtb__inner__right">
                    <a href="#" class="mtb__searchbtn">
                        <img src="generated/icon-sprite.svg#ico36_search_nomargin" class="mtb__icon">
                    </a>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.otb = this;

            const selectFromState = (state)=>{

                return {
                    matchesCount: Object.keys(state.getIn(['events','unreadEventUris']).toJS())
                        .map(key=>state.getIn(['events','unreadEventUris'])
                            .toJS()[key])
                        .filter(event =>{
                            if(event.eventType===won.EVENT.HINT_RECEIVED){
                                return true
                            }
                         }),
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            //  this.loadMatches();
            this.$scope.$on('$destroy', disconnect);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {selection: "="},
        template: template
    }
}

export default angular.module('won.owner.components.overviewTitleBar', [])
    .directive('wonOverviewTitleBar', genComponentConf)
    .name;
