/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach, mapToMatches, decodeUriComponentProperly } from '../utils';
import won from '../won-es6';
import { labels } from '../won-label-utils';
import { selectOpenPost } from '../selectors';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <a ng-click="self.back()">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="vtb__icon">
                    </a>
                    <won-square-image title="self.post.get('title')" src="self.post.get('titleImgSrc')"></won-square-image>
                    <div class="vtb__inner__left__titles">
                        <h1 class="vtb__title">{{self.post.get('title')}}</h1>
                        <div class="vtb__inner__left__titles__type">{{self.labels.type[self.post.get('type')]}}, {{self.post.get('creationDate')}} </div>
                    </div>
                </div>
                <div class="vtb__inner__right" ng-show="self.hasConnectionWithOwnPost">
                    <button class="won-button--filled red">Quit Contact</button>
                    <ul class="vtb__tabs">
                        <li ng-class="self.selection == 0? 'vtb__tabs__selected' : ''" ng-click="self.selection = 0">
                        <a ui-sref="postVisitorMsgs({myUri: 'http://example.org/121337345'})">
                            Messages
                            <span class="vtb__tabs__unread">{{self.post.get('messages').length}}</span>
                        </a></li>
                        <li ng-class="self.selection == 1? 'vtb__tabs__selected' : ''" ng-click="self.selection = 1">
                        <a ui-sref="postVisitor({myUri: 'http://example.org/121337345'})">
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
            const selectFromState = state => ({
                post: selectOpenPost(state)
                hasConnectionWithOwnPost: false,
            });
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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
