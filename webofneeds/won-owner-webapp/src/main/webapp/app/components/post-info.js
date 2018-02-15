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
                    ng-show="self.isPart.is.get('description')">
                    Description
                </h2>
                <p class="post-info__details"
                    ng-show="self.isPart.is.get('description')">
                    {{ self.isPart.is.get('description')}}
                </p>

                <h2 class="post-info__heading"
                    ng-show="self.isPart.is.get('tags')">
                    Tags
                </h2>
                <div class="post-info__details post-info__tags"
                    ng-show="self.isPart.is.get('tags')">
                        <span class="post-info__tags__tag" ng-repeat="tag in self.isPart.is.get('tags').toJS()">{{tag}}</span>
                </div>

                <h2 class="post-info__heading"
                    ng-show="self.isPart.location">
                    Location
                </h2>
                <p class="post-info__details clickable"
	                ng-show="self.isPart.address"  ng-click="self.toggleMap(self.is)">
	                {{ self.isPart.address }}
    				<svg class="post-info__carret" ng-show="!self.showMap.is">
			            <use href="#ico16_arrow_down"></use>
			        </svg>
		            <svg class="post-info__carret" ng-show="self.showMap.is">
		                 <use href="#ico16_arrow_up"></use>
		            </svg>
	            </p>                     
                <won-need-map 
                    uri="self.post.get('uri')+'#is'"
                    isSeeks="self.isPart.isString"
                    ng-if="self.isPart.location && self.showMap.is">
                </won-need-map>
                <br/>
                
                <div ng-show="self.seeksPart">
	                
	                <won-labelled-hr label="::'Search'" class="cp__labelledhr"></won-labelled-hr>
	                <!--
	                <h1 class="ntb_title ng-binding"
	                    ng-show="self.seeksPart.seeks.get('title')">
	                     {{ self.seeksPart.seeks.get('title')}}
	                </h1> -->
	                <h2 class="post-info__heading"
	                    ng-show="self.seeksPart.seeks.get('title')">
	                    Title
	                </h2>
	                <p class="post-info__details"
	                    ng-show="self.seeksPart.seeks.get('title')">
	                    {{ self.seeksPart.seeks.get('title')}}
	                </p>
	                <h2 class="post-info__heading"
	                    ng-show="self.seeksPart.seeks.get('description')">
	                    Description
	                </h2>
	                <p class="post-info__details"
	                    ng-show="self.seeksPart.seeks.get('description')">
	                    {{ self.seeksPart.seeks.get('description')}}
	                </p>
	
	                <h2 class="post-info__heading"
	                    ng-show="self.seeksPart.seeks.get('tags')">
	                    Tags
	                </h2>
	                <div class="post-info__details post-info__tags"
	                    ng-show="self.seeksPart.seeks.get('tags')">
	                        <span class="post-info__tags__tag" ng-repeat="tag in self.seeksPart.seeks.get('tags').toJS()">{{tag}}</span>
	                </div>
	
	                <h2 class="post-info__heading"
	                    ng-show="self.seeksPart.location">
	                    Location
	                </h2>
	                <p class="post-info__details clickable"
	                    ng-show="self.seeksPart.address"  ng-click="self.toggleMap(self.seeks)">
	                    {{ self.seeksPart.address }}
    					<svg class="post-info__carret" ng-show="!self.showMap.seeks">
			               <use href="#ico16_arrow_down"></use>
			            </svg>
		                <svg class="post-info__carret" ng-show="self.showMap.seeks">
		                   <use href="#ico16_arrow_up"></use>
		                </svg>
	                </p>                
	                <won-need-map 
	                    uri="self.post.get('uri')+'#seeks'"
	                    isSeeks="self.seeksPart.seeksString"
	                    ng-if="self.seeksPart.location && self.showMap.seeks">
	                </won-need-map>
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
            this.showMap = {is: false, seeks: false};
            
            const selectFromState = (state) => {
                const postUri = selectOpenPostUri(state);
                const post = state.getIn(["needs", postUri]);
                const is = post? post.get('is') : undefined;
                const seeks = post? post.get('seeks') : undefined;
                //const isLocation = is && is.get('location');
                
                return {
                	isPart: is? {
                		is: is,
                		isString: 'is',
                		location: is && is.get('location'),
                		address: is.get('location') && is.get('location').get('address'),
                	}: undefined,
                	seeksPart: seeks? {
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
        
        toggleMap(isSeeks) {
        	this.showMap[isSeeks] = !this.showMap[isSeeks];
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
