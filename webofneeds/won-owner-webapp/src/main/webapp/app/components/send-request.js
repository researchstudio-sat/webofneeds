;

import angular from 'angular';
import 'ng-redux';
import extendedGalleryModule from '../components/extended-gallery';
import { selectLastUpdateTime } from '../selectors';
import { labels, relativeTime } from '../won-label-utils';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];

function genComponentConf() {
    let template = `
        <div class="sr__caption">
            <div class="sr__caption__title">Send Conversation Request</div>
            <a ui-sref="overviewMatches({connectionUri: null})">
                <img class="sr__caption__icon clickable" src="generated/icon-sprite.svg#ico36_close"/>
            </a>
        </div>
        <div class="sr__header">
            <div class="sr__header__title">
                <div class="sr__header__title__topline">
                    <div class="sr__header__title__topline__title">{{self.theirNeed.get('title')}}</div>
                    <div class="sr__header__title__topline__date">{{self.theirCreationDate}}</div>
                </div>
                <div class="sr__header__title__subtitle">
                    <span class="sr__header__title__subtitle__group" ng-show="self.theirNeed.get('group')">
                        <img src="generated/icon-sprite.svg#ico36_group" class="sr__header__title__subtitle__group__icon">{{self.theirNeed.get('group')}}<span class="sr__header__title__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="sr__header__title__subtitle__type">{{self.labels.type[self.theirNeed.get('basicNeedType')]}}</span>
                </div>
            </div>
        </div>
        <div class="sr__content">
            <div class="sr__content__images" ng-show="self.theirNeed.get('images')">
                <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.theirNeed.get('images')" class="vertical"></won-extended-gallery>
            </div>
            <div class="sr__content__description"
                ng-show="self.theirNeed.get('location') || self.theirNeed.get('deadline') || self.theirNeed.get('description')">
                    <div class="sr__content__description__location" ng-show="self.theirNeed.get('location')">
                        <img class="sr__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                        <span>{{ self.theirNeed.get('location') }}</span>
                    </div>
                    <div class="sr__content__description__datetime" ng-show="self.theirNeed.get('deadline')">
                        <img class="sr__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                        <span>{{ self.theirNeed.get('deadline') }} </span>
                    </div>
                    <div class="sr__content__description__text" ng-show="self.theirNeed.get('description')">
                        <img class="sr__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_description"/>
                        <span>{{ self.theirNeed.get('description') }} </span>
                    </div>
            </div>
        </div>
        <div class="sr__footer">
            <input type="text" ng-model="self.message" placeholder="Reply Message (optional)"/>
            <div class="flexbuttons">
                <button class="won-button--filled black" ui-sref="overviewMatches({connectionUri: null})">Cancel</button>
                <button class="won-button--filled red" ng-click="self.sendRequest(self.message)" ui-sref="overviewMatches({connectionUri: null})">Request Contact</button>
            </div>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.maxThumbnails = 9;
            this.labels = labels;
            this.message = '';
            window.openMatch4dbg = this;

            const selectFromState = (state) => {
                const connectionUri = decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri']));

                const theirNeedUri = state.getIn(['connections', connectionUri, 'hasRemoteNeed']);
                const theirNeed = state.getIn(['needs','theirNeeds', theirNeedUri]);

                return {
                    connectionUri: connectionUri,
                    connection: state.getIn(['connections', connectionUri]),
                    theirNeed,
                    theirCreationDate: theirNeed ?
                        relativeTime(selectLastUpdateTime(state, theirNeed.get('creationDate'))) :
                        undefined,
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        sendRequest(message) {
            this.connections__connect(this.connectionUri, message);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.sendRequest', [
    extendedGalleryModule
])
    .directive('wonSendRequest', genComponentConf)
    .name;

