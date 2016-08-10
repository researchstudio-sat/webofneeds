/**
 * Created by ksinger on 10.05.2016.
 */


;

import angular from 'angular';
import { attach, } from '../utils';
import won from '../won-es6';
import {
    relativeTime,
} from '../won-label-utils';
import {
    selectOpenPost,
    selectLastUpdateTime,
} from '../selectors';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="post-info__inner">
            <won-gallery
                class="post-info__inner__left"
                ng-show="self.post.get('hasImages')">
            </won-gallery>

            <div class="post-info__inner__right">
                <h2 class="post-info__heading" ng-show="self.friendlyTimestamp">
                    Created
                </h2>
                <p class="post-info__details" ng-show="self.friendlyTimestamp">
                    {{ self.friendlyTimestamp }}
                </p>

                <h2 class="post-info__heading"
                    ng-show="self.post.getIn(['won:hasContent','won:hasTextDescription'])">
                    Description
                </h2>
                <p class="post-info__details"
                    ng-show="self.post.getIn(['won:hasContent','won:hasTextDescription'])">
                    {{ self.post.getIn(['won:hasContent','won:hasTextDescription'])}}
                </p>

                <!-- TODO tags -->

                <h2 class="post-info__heading"
                    ng-show="self.post.get('location')">
                    Location
                </h2>
                <p class="post-info__details"
                    ng-show="self.post.get('location')">
                    {{ self.post.get('location') }}
                </p>
            </div>
        </div>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const selectFromState = (state) => {
                const post = selectOpenPost(state);
                return {
                    post: post,
                    friendlyTimestamp: relativeTime(
                        selectLastUpdateTime(state),
                        post && post.get('dct:created')
                    ),
                }
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
}
Controller.$inject = serviceDependencies;
return {
    restrict: 'E',
    controller: Controller,
    controllerAs: 'self',
    bindToController: true, //scope-bindings -> ctrl
    template: template,
    scope: { }
}
}

export default angular.module('won.owner.components.postInfo', [])
    .directive('wonPostInfo', genComponentConf)
    .name;
