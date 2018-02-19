/**
 * Created by ksinger on 10.05.2016.
 */


;

import angular from 'angular';
import postSeeksInfoModule from './post-seeks-info.js';
import postIsInfoModule from './post-is-info.js';

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
            	<!-- GENERAL Part -->
                <h2 class="post-info__heading" ng-show="self.friendlyTimestamp">
                    Created
                </h2>
                <p class="post-info__details" ng-show="self.friendlyTimestamp">
                    {{ self.friendlyTimestamp }}
                </p>
                <!-- IS Part -->
    			<div ng-show="self.isPart">
    				<won-post-is-info is-part="::self.isPart"></won-post-is-info>
                </div>
                <!-- SEEKS Part -->
                <div ng-show="self.seeksPart"> 
	                <won-post-seeks-info seeks-part="::self.seeksPart"></won-post-seeks-info>
    			</div>
                
                </br>
                <hr>
                <p class="post-info__details">
                 <a href="{{self.post.get('uri')}}"
                    target="_blank">
                    <svg class="rdflink__big clickable">
                        <use href="#rdf_logo_1"></use>
                    </svg>
                  </a>
                </p>
                <button class="won-button--filled red" 
                        ng-click="self.router__stateGoCurrent({sendAdHocRequest: true})"
                        ng-show="self.showRequestButton">
                        Chat
                </button>
            </div>
        </div>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.pi4dbg = this;
            
            this.is = 'is';
            this.seeks = 'seeks';
           
            const selectFromState = (state) => {
                const postUri = selectOpenPostUri(state);
                const post = state.getIn(["needs", postUri]);
                const is = post? post.get('is') : undefined;
                
                //TODO it will be possible to have more than one seeks
                const seeks = post? post.get('seeks') : undefined;
                
                return {
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

export default angular.module('won.owner.components.postInfo', [ 
		postIsInfoModule,
		postSeeksInfoModule,
	])
    .directive('wonPostInfo', genComponentConf)
    .name;
