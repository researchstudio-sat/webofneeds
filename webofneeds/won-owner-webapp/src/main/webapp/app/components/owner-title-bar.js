/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="ntb__inner">
                <div class="ntb__inner__left">
                    <a ng-click="self.back()">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                    </a>
                    <won-square-image title="blabla" src="images/someNeedTitlePic.png"></won-square-image>
                    <div class="ntb__inner__left__titles">
                        <h1 class="ntb__title">New flat, need furniture</h1>
                        <div class="ntb__inner__left__titles__type">I want to have something</div>
                    </div>
                </div>
                <div class="ntb__inner__right">
                    <img class="ntb__icon" src="generated/icon-sprite.svg#ico_settings">
                    <ul class="ntb__tabs">
                        <li><a href="#">
                            Messages
                            <span class="ntb__tabs__unread">5</span>
                        </a></li>
                        <li class="ntb__tabs__selected"><a href="#">
                            Matches
                            <span class="ntb__tabs__unread">5</span>
                        </a></li>
                        <li><a href="#">
                             Requests
                            <span class="ntb__tabs__unread">18</span>
                        </a></li>
                        <li><a href="#">
                             Sent Requests
                            <span class="ntb__tabs__unread">18</span>
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    const serviceDependencies = ['$q', '$ngRedux', '$scope'];
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
                    matchesOfNeed:this.mapToMatches(state.getIn(['matches','matches']).toJS())
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            //  this.loadMatches();
            this.$scope.$on('$destroy', disconnect);
        }
        back() { window.history.back() }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        template: template
    }
}

export default angular.module('won.owner.components.needTitleBar', [])
    .directive('wonOwnerTitleBar', genComponentConf)
    .name;
