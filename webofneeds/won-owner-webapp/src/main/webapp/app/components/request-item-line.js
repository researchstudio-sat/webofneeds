;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import {attach,getType} from '../utils.js';
import { actionCreators }  from '../actions/actions';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
            <div class="ril clickable" ng-click="self.toggleRequest()">
                <won-square-image src="self.item.titleImgSrc" title="self.item.ownNeed.title"></won-square-image>
                <div class="ril__description">
                    <div class="ril__description__topline">
                        <div class="ril__description__topline__title">{{self.item[0].ownNeed.title}}</div>
                        <div class="ril__description__topline__messagecount">{{self.item.length}}</div>
                    </div>
                    <div class="ril__description__subtitle">
                        <span class="ril__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="ril__description__subtitle__group__icon">{{self.item.group}}<span class="ril__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="ril__description__subtitle__type">{{self.getType(self.item[0].ownNeed.basicNeedType)}}</span>
                    </div>
                </div>
                <div class="ril__carret">
                    <img class="ril__arrow" ng-show="self.open" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="ril__arrow" ng-show="!self.open" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>
            <div class="mil" ng-show="self.open">
                <div class="mil__item clickable" ng-class="self.openRequest === request? 'selected' : ''" ng-repeat="request in self.item" ng-click="self.openMessage(request)">
                    <won-square-image src="request.titleImgSrc" title="request.title"></won-square-image>
                    <div class="mil__item__description">
                        <div class="mil__item__description__topline">
                            <div class="mil__item__description__topline__title">{{self.item[0].remoteNeed.title}}</div>
                            <div class="mil__item__description__topline__date">{{request.timeStamp}}</div>
                        </div>
                        <div class="mil__item__description__subtitle">
                            <span class="mil__item__description__subtitle__group" ng-show="request.group">
                                <img src="generated/icon-sprite.svg#ico36_group" class="mil__item__description__subtitle__group__icon">{{request.group}}<span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
                            </span>
                            <span class="mil__item__description__subtitle__type">{{self.getType(self.item[0].remoteNeed.basicNeedType)}}</span>
                        </div>
                        <div class="mil__item__description__message">
                            <span class="mil__item__description__message__indicator" ng-show="!self.read(request)"/>{{request.message}}
                        </div>
                    </div>
                </div>
            </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            console.log(this.item)

            const selectFromState = (state)=>{

                return {
                    unreadUris: state.getIn(['events','unreadEventUris'])
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState,actionCreators)(this);
            //  this.loadMatches();
            this.$scope.$on('$destroy', disconnect);
        }

        read(request){
            if(!this.unreadUris.has(request.connection.uri)){
                return true
            }
            return false;
        }
        toggleRequest() {
            this.open = !this.open;
        }

        openMessage(request) {
            this.events__read(request.connection.uri)
            this.openRequest = request;
        }

    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                open: "=",
                openRequest: "="},
        template: template
    }

}

export default angular.module('won.owner.components.requestItemLine', [])
    .directive('wonRequestItemLine', genComponentConf)
    .name;

