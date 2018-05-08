/**
 * Created by ksinger on 10.05.2016.
 */


;

import angular from 'angular';
import postSeeksInfoModule from './post-seeks-info.js';
import postIsInfoModule from './post-is-info.js';
import postHeaderModule from './post-header.js';
import postShareLinkModule from './post-share-link.js';
import labelledHrModule from './labelled-hr.js';
import postContextDropdownModule from './post-context-dropdown.js';

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
        <div class="post-info__header" ng-if="self.includeHeader">
            <a class="post-info__header__back clickable show-in-responsive"
               ng-click="self.router__stateGoCurrent({postUri : null})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.post.get('uri')"
                timestamp="self.createdTimestamp"
                hide-image="::false">
            </won-post-header>
            <won-post-context-dropdown ng-if="self.post.get('ownNeed')"></won-post-context-dropdown>
        </div>
        <div class="post-info__content">
            <won-gallery ng-show="self.post.get('hasImages')">
            </won-gallery>

            <!-- GENERAL Part -->
            <h2 class="post-info__heading" ng-show="self.friendlyTimestamp">
                Created
            </h2>
            <p class="post-info__details" ng-show="self.friendlyTimestamp">
                {{ self.friendlyTimestamp }}
            </p>
            <won-post-is-info is-part="::self.isPart" ng-if="self.isPart"></won-post-is-info>
            <won-labelled-hr label="::'Search'" class="cp__labelledhr" ng-show="self.isPart && self.seeksPart"></won-labelled-hr>
            <won-post-seeks-info seeks-part="::self.seeksPart" ng-if="self.seeksPart"></won-post-seeks-info>
            <a class="rdflink clickable"
               ng-if="self.shouldShowRdf"
               target="_blank"
               href="{{self.post.get('uri')}}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Post</span>
            </a>
        </div>
        <div class="post-info__footer">
            <won-post-share-link
                ng-if="self.post.get('state') !== self.WON.InactiveCompacted"
                post-uri="self.post.get('uri')">
            </won-post-share-link>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            this.is = 'is';
            this.seeks = 'seeks';

            const selectFromState = (state) => {
                const postUri = selectOpenPostUri(state);
                const post = state.getIn(["needs", postUri]);
                const is = post? post.get('is') : undefined;
                
                //TODO it will be possible to have more than one seeks
                const seeks = post? post.get('seeks') : undefined;
                
                return {
                    WON: won.WON,
                	isPart: is? {
                		postUri: postUri,
                		is: is,
                		isString: 'is',
                		location: is && is.get('location'),
                		address: is.get('location') && is.get('location').get('address'),
                	}: undefined,
                	seeksPart: seeks? {
                		postUri: postUri,
                		seeks: seeks,
                		seeksString: 'seeks',
                		location: seeks && seeks.get('location'),
                		address: seeks.get('location') && seeks.get('location').get('address'),
                	}: undefined,
                    post,
                    showRequestButton: post && !post.get('ownNeed') && !post.get('isWhatsAround'),
                    friendlyTimestamp: post && relativeTime(
                        selectLastUpdateTime(state),
                        post.get('creationDate')
                    ),
                    createdTimestamp: post && post.get('creationDate'),
                    shouldShowRdf: state.get('showRdf'),
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.includeHeader'], this);
        }
    }

    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template,
        scope: {
            includeHeader: "="
        }
    }
}

export default angular.module('won.owner.components.postInfo', [ 
		postIsInfoModule,
		postSeeksInfoModule,
        postHeaderModule,
        postShareLinkModule,
        labelledHrModule,
        postContextDropdownModule,
	])
    .directive('wonPostInfo', genComponentConf)
    .name;
