/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach, } from '../utils.js';
import { labels } from '../won-label-utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { selectOpenPostUri } from '../selectors.js';
import { actionCreators }  from '../actions/actions.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <a ng-click="self.back()" class="clickable">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="vtb__icon">
                    </a>
                    <won-square-image 
                        title="self.theirPost.get('title')"
                        src="self.theirPost.get('titleImgSrc')"
                        uri="self.theirPost.get('@id')">
                    </won-square-image>
                    <hgroup>
                        <h1 class="vtb__title">{{ self.theirPost.get('title') }}</h1>
                        <div class="vtb__titles__type">{{self.labels.type[self.theirPost.get("type")]}}</div>
                    </hgroup>
                </div>
                <div class="vtb__inner__right" ng-show="self.hasConnectionWithOwnPost">
                    <button class="won-button--filled red">Quit Contact</button>
                    <ul class="vtb__tabs">
                        <li class="clickable"
                            ng-class="self.selection == 0? 'vtb__tabs__selected' : ''"
                            ng-click="self.selection = 0">
                        <a ng-click="self.router__stateGoAbs('post', {ERROR: 'Messages tab not implemented yet'})">
                            Messages
                            <span class="vtb__tabs__unread">{{self.theirPost.get('messages').length}}</span>
                        </a></li>
                        <li class="clickable" ng-class="self.selection == 1? 'vtb__tabs__selected' : ''" ng-click="self.selection = 1">
                        <a ng-click="self.router__stateGoAbs('postVisitor', {myUri: 'http://example.org/121337345'})">
                            Post Info
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            window.vtb4dbg = this;
            const selectFromState = state => {
                const postUri = selectOpenPostUri(state);
                const theirPost = state.getIn(["needs", postUri]);
                return {
                    theirPost,
                    labels,
                    hasConnectionWithOwnPost: false,
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);
        }
        back() { window.history.back() }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {selection: "=",
                item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.visitorTitleBar', [])
    .directive('wonVisitorTitleBar', genComponentConf)
    .name;
