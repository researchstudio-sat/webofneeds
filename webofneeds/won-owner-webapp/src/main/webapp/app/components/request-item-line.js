;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import { labels } from '../won-label-utils';
import {attach} from '../utils.js';
import { actionCreators }  from '../actions/actions';

import { selectUnreadEvents } from '../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
            <div class="ril clickable" ng-click="self.toggleRequest()">
                <won-square-image 
                    src="self.item.titleImgSrc" 
                    title="self.item[0].ownNeed['won:hasContent']['dc:title']"
                    uri="self.item[0].ownNeed['@id']">
                </won-square-image>
                <div class="ril__description">
                    <div class="ril__description__topline">
                        <div class="ril__description__topline__title">{{self.item[0].ownNeed['won:hasContent']['dc:title']}}</div>
                        <div class="ril__description__topline__messagecount">{{self.item.length}}</div>
                    </div>
                    <div class="ril__description__subtitle">
                        <span class="ril__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="ril__description__subtitle__group__icon">{{self.item.group}}<span class="ril__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="ril__description__subtitle__type">
                            {{
                                self.labels.type[
                                    self.item[0].ownNeed['won:hasBasicNeedType']['@id']
                                ]
                            }}
                        </span>
                    </div>
                </div>
                <div class="ril__carret">
                    <img class="ril__arrow" ng-show="self.open" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="ril__arrow" ng-show="!self.open" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>
            <div class="mil" ng-show="self.open">
                <a class="mil__item clickable"
                    ng-class="request.connection.uri === self.connectionUri? 'selected' : ''"
                    ng-repeat="request in self.item"
                    ui-sref="{{::self.openRequestItemUrl()}}"
                    ng-click="self.openMessage(request)">
                    <won-square-image 
                        src="request.titleImgSrc" 
                        title="request.remoteNeed['won:hasContent']['dc:title']"
                        uri="request.remoteNeed['@id']">
                    </won-square-image>
                    <div class="mil__item__description">
                        <div class="mil__item__description__topline">
                            <div class="mil__item__description__topline__title">{{request.remoteNeed['won:hasContent']['dc:title']}}</div>
                            <div class="mil__item__description__topline__date">{{request.timeStamp}}</div>
                        </div>
                        <div class="mil__item__description__subtitle">
                            <span class="mil__item__description__subtitle__group" ng-show="request.group">
                                <img src="generated/icon-sprite.svg#ico36_group" class="mil__item__description__subtitle__group__icon">{{request.group}}<span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
                            </span>
                            <span class="mil__item__description__subtitle__type">{{self.labels.type[self.item[0].remoteNeed['won:hasBasicNeedType']['@id']]}}</span>
                        </div>
                        <div class="mil__item__description__message">
                            <span class="mil__item__description__message__indicator" ng-show="!self.read(request)"/>{{request.message}}
                        </div>
                    </div>
                </a>
            </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.reqitemline = this;
            const selectFromState = (state)=>{
                return {
                    connectionUri: decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri'])),
                    currentPage: state.getIn(['router','currentState','name']),
                    unreadUris: selectUnreadEvents(state)
                };
            };
            this.labels = labels;

            const disconnect = this.$ngRedux.connect(selectFromState,actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        openRequestItemUrl() {
            return "{connectionUri: request.connection.uri}";
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
            //this.events__read(request.connection.uri)
        }
    

    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                open: "="},
        template: template
    }

}

export default angular.module('won.owner.components.requestItemLine', [])
    .directive('wonRequestItemLine', genComponentConf)
    .name;

