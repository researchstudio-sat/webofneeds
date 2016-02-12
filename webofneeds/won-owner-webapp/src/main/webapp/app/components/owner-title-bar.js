/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach,mapToMatches } from '../utils';
import won from '../won-es6';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="ntb__inner">
                <div class="ntb__inner__left">
                    <a ng-click="self.back()">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                    </a>
                    <won-square-image title="blabla" src="images/need.jpg"></won-square-image>
                    <div class="ntb__inner__left__titles">
                        <h1 class="ntb__title">New flat, need furniture</h1>
                        <div class="ntb__inner__left__titles__type">I want to have something</div>
                    </div>
                </div>
                <div class="ntb__inner__right">
                    <img class="ntb__icon" src="generated/icon-sprite.svg#ico_settings">
                    <ul class="ntb__tabs">
                        <li ng-class="{'mtb__tabs__selected' : self.selection == 0}"><a ui-sref="postConversations({myUri: 'http://example.org/121337345'})">
                            Messages
                            <span class="ntb__tabs__unread">5</span>
                        </a></li>
                        <li ng-class="{'mtb__tabs__selected' : self.selection == 1}"><a href="#">
                            Matches
                            <span class="ntb__tabs__unread">5</span>
                        </a></li>
                        <li ng-class="{'mtb__tabs__selected' : self.selection == 2}"><a href="#">
                             Requests
                            <span class="ntb__tabs__unread">18</span>
                        </a></li>
                        <li ng-class="{'mtb__tabs__selected' : self.selection == 3}"><a href="#">
                             Sent Requests
                            <span class="ntb__tabs__unread">18</span>
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const selectFromState = (state)=>{

                return {
                    matchesCount: Object.keys(state.getIn(['events','unreadEventUris']).toJS()).filter(event =>{
                        if(event.eventType===won.EVENT.HINT_RECEIVED){
                            return true
                        }
                    }),
                    matchesOfNeed:mapToMatches(state.getIn(['matches','matches']).toJS())
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            //  this.loadMatches();
            this.$scope.$on('$destroy', disconnect);
        }
        back() { window.history.back() }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        template: template,
        scope: {selection: "="}
    }
}

export default angular.module('won.owner.components.needTitleBar', [])
    .directive('wonOwnerTitleBar', genComponentConf)
    .name;
