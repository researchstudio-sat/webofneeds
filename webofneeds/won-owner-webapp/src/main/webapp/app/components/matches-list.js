;

import Immutable from 'immutable'
import angular from 'angular';
import won from '../won-es6';
import squareImageModule from './square-image';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import matchesListItemModule from './matches-list-item';
import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';
import {
    selectOwnNeeds,
    displayingOverview,
    selectUnreadCountsByNeedAndType,
    selectLastUpdateTime,
    selectLastUpdatedPerConnection,
    selectAllByConnections,
    selectMatchesUrisByNeed,
    selectOpenPostUri,
    selectOpenPost,
} from '../selectors'
//import won from '../won-es6';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="mli clickable"
            ng-repeat="ownNeed in self.ownNeeds.toArray()"
            ng-click="self.toggleMatches(ownNeed.get('@id'))"
            ng-show="self.hasMatches(ownNeed.get('@id'))">

            <div class="mli__ownneed" ng-show="self.isOverview">
                <won-square-image
                    src="self.seeksOrIs(ownNeed).get('imageTODO')"
                    title="self.seeksOrIs(ownNeed).get('dc:title')"
                    uri="ownNeed.get('@id')">
                </won-square-image>
                <div class="mli__description">
                    <div class="mli__description__topline">
                        <div class="mli__description__topline__title">
                            {{ self.seeksOrIs(ownNeed).get('dc:title') }}
                        </div>
                        <div class="mli__description__topline__matchcount">
                            {{
                                self.unreadCounts.getIn([
                                    ownNeed.get('@id'),
                                    self.WONMSG.hintMessage
                                ])
                            }}
                        </div>
                    </div>
                    <div class="mli__description__subtitle">
                        <!--
                        <span class="mli__description__subtitle__group" ng-show="self.item.group">
                            <img
                                src="generated/icon-sprite.svg#ico36_group"
                                class="mli__description__subtitle__group__icon">
                            {{self.item.group}}
                            <span class="mli__description__subtitle__group__dash">
                                &ndash;
                            </span>
                        </span>
                        -->
                        <span class="mli__description__subtitle__type">
                            {{
                                self.labels.type[
                                    self.inferLegacyNeedType(ownNeed)
                                ]
                            }}
                        </span>
                    </div>
                </div>
                <div class="mli__carret">
                    <img class="mli__arrow" ng-show="self.isOpen(ownNeed.get('@id'))"
                        src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="mli__arrow" ng-show="!self.isOpen(ownNeed.get('@id'))"
                        src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>


            <div class="smli" ng-show="self.isOpen(ownNeed.get('@id'))">
                <won-matches-list-item
                    class="smli__item clickable"
                    ui-sref="{connectionUri: matchUri}"
                    ng-repeat="matchUri in self.matchesUrisByNeed.get(ownNeed.get('@id')).toArray()"
                    connection-uri="matchUri">
                    <!--
                    ui-sref="{connectionUri: '{{matchUri}}'}"
                    -->
                </won-matches-list-item>
            </div>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.ml4dbg = this;
            this.seeksOrIs = seeksOrIs;
            this.inferLegacyNeedType = inferLegacyNeedType;
            this.open = [];
            this.WONMSG = won.WONMSG;

            this.maxThumbnails = 4;
            this.labels = labels;
            this.relativeTime = relativeTime;

            const selectFromState = (state) => {
                //const ownNeed = ownNeeds && ownNeeds.get(self.ownNeedUri);
                const unreadCounts = selectUnreadCountsByNeedAndType(state);
                const openPost = selectOpenPost(state);

                const ownNeeds = openPost?
                    Immutable.Map([[openPost.get('@id'), openPost]]) : // only show one post (the one which detail page it is)
                    selectOwnNeeds(state);  // show all posts


                return {
                    isOverview: !openPost,
                    ownNeeds,
                    ownNeedUris: ownNeeds && ownNeeds.keySeq().toArray(),
                    unreadCounts,
                    matchesUrisByNeed: selectMatchesUrisByNeed(state),
                };
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        hasMatches(ownNeedUri) {
            const matchUris = this.matchesUrisByNeed.get(ownNeedUri);
            return matchUris && matchUris.size > 0
        }

        toggleMatches(ownNeedUri) {
            this.open[ownNeedUri] = !this.open[ownNeedUri];
        }
        isOpen (ownNeedUri) {
            return !this.isOverview || //if only one post is shown, always display all matches
                this.open[ownNeedUri];
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { },
        template: template
    }
}

export default angular.module('won.owner.components.matchesList', [
    squareImageModule,
    matchesListItemModule,
])
    .directive('wonMatchesList', genComponentConf)
    .name;

