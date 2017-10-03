/**
 * Created by ksinger on 10.05.2016.
 */


;

import angular from 'angular';
import needMapModule from './need-map.js';

import { attach, } from '../utils.js';
import won from '../won-es6.js';
import {
    relativeTime,
} from '../won-label-utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import {
    selectOpenPostUri,
    selectLastUpdateTime,
} from '../selectors.js';
import { actionCreators }  from '../actions/actions.js';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];
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
                    ng-show="self.post.get('description')">
                    Description
                </h2>
                <p class="post-info__details"
                    ng-show="self.post.get('description')">
                    {{ self.post.get('description')}}
                </p>

                <h2 class="post-info__heading"
                    ng-show="self.post.get('tags')">
                    Tags
                </h2>
                <div class="post-info__details post-info__tags"
                    ng-show="self.post.get('tags')">
                        <span class="post-info__tags__tag" ng-repeat="tag in self.post.get('tags').toJS()">{{tag}}</span>
                </div>

                <h2 class="post-info__heading"
                    ng-show="self.location">
                    Location
                </h2>
                <p class="post-info__details"
                    ng-show="self.address">
                    {{ self.address }}
                </p>                
                <won-need-map 
                    uri="self.post.get('uri')"
                    ng-show="self.location">
                </won-need-map>
                <p ng-show="self.debugmode">
                    <a class="debuglink" target="_blank" href="{{self.post.get('uri')}}">[DATA]</a>
                </p>
            </div>
        </div>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.pi4dbg = this;

            const selectFromState = (state) => {
                const postUri = selectOpenPostUri(state);
                const post = state.getIn(["needs", postUri]);
                const location = post && post.get('location');
                return {
                    post,
                    location: location,
                    address: location && location.get('address'),
                    friendlyTimestamp: post && relativeTime(
                        selectLastUpdateTime(state),
                        post.get('creationDate')
                    ),
                    debugmode: won.debugmode
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);
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

export default angular.module('won.owner.components.postInfo', [ needMapModule ])
    .directive('wonPostInfo', genComponentConf)
    .name;
