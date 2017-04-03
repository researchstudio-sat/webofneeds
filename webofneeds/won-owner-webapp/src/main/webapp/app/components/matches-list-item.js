/**
 * Created by ksinger on 03.04.2017.
 */
;

import angular from 'angular';
import squareImageModule from './square-image';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';
import {
    selectLastUpdateTime,
    selectLastUpdatedPerConnection,
    selectAllByConnections,
} from '../selectors'
//import won from '../won-es6';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="smli__item__header">
            <won-square-image
                src="self.theirNeedContent.get('TODO')"
                title="self.theirNeedContent.get('dc:title')"
                uri="self.theirNeed.get('@id')">
            </won-square-image>
            <div class="smli__item__header__text">
                <div class="smli__item__header__text__topline">
                    <div class="smli__item__header__text__topline__title">
                        {{ self.theirNeedContent.get('dc:title') }}
                    </div>
                    <div class="smli__item__header__text__topline__date">
                        {{ self.theirNeedCreatedOn }}
                    </div>
                </div>
                <div class="smli__item__header__text__subtitle">
                    <!--
                    <span class="smli__item__header__text__subtitle__group" ng-show="request.group">
                        <img src="generated/icon-sprite.svg#ico36_group"
                             class="smli__item__header__text__subtitle__group__icon">
                        {{match.group}}
                        <span class="smli__item__header__text__subtitle__group__dash">
                            &ndash;
                        </span>
                    </span>
                    -->
                    <span class="smli__item__header__text__subtitle__type">
                        {{ self.labels.type[ self.theirNeedType ] }}
                    </span>
                </div>
            </div>
        </div>
        <!--
        <div class="smli__item__content">
            <div class="smli__item__content__location">
                <img class="smli__item__content__indicator"
                    src="generated/icon-sprite.svg#ico16_indicator_location"/>
                <span>Vienna area</span>
            </div>
            <div class="smli__item__content__datetime">
                <img class="smli__item__content__indicator"
                    src="generated/icon-sprite.svg#ico16_indicator_time"/>
                <span>Available until 5th May</span>
            </div>
            <div class="smli__item__content__text">
                <img class="smli__item__content__indicator"
                    src="generated/icon-sprite.svg#ico16_indicator_description"/>
                <span>{{match.message}}</span>
            </div>
        </div>
        -->
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.mli4dbg = this;

            this.maxThumbnails = 4;
            this.labels = labels;
            this.relativeTime = relativeTime;

            const selectFromState = (state) => {
                const connectionData = selectAllByConnections(state).get(this.connectionUri);
                const ownNeed = connectionData && connectionData.get('ownNeed');
                const theirNeed = connectionData && connectionData.get('remoteNeed');

                return {
                    connectionData,

                    ownNeed,
                    ownNeedType: ownNeed && inferLegacyNeedType(ownNeed),
                    ownNeedContent: ownNeed && seeksOrIs(ownNeed),

                    theirNeed,
                    theirNeedType: theirNeed && inferLegacyNeedType(theirNeed),
                    theirNeedContent: theirNeed && seeksOrIs(theirNeed),
                    theirNeedCreatedOn: theirNeed && relativeTime(
                        selectLastUpdateTime(state),
                        theirNeed.get('dct:created')
                    ),
                };
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        toggleMatches() {
            this.open = !this.open;
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            connectionUri: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.matchesListItem', [
    squareImageModule
])
    .directive('wonMatchesListItem', genComponentConf)
    .name;
