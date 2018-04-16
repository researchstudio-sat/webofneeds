/**
 * Created by ksinger on 10.05.2016.
 */


;

import angular from 'angular';
import postSeeksInfoModule from './post-seeks-info.js';
import postIsInfoModule from './post-is-info.js';
import postHeaderModule from './post-header.js';

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
            <a class="clickable"
               ng-click="self.router__stateGoCurrent({postUri : null})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__icon clickable">
                    <use href="#ico36_close"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.post.get('uri')"
                timestamp="self.createdTimestamp"
                hide-image="::false">
            </won-post-header>
            <!-- TODO: Implement a menu with all the necessary buttons -->
            <svg class="post-info__header__icon__small clickable"
                style="--local-primary:#var(--won-secondary-color);"
                ng-show="!self.contextMenuOpen"
                ng-click="self.contextMenuOpen = true">
                    <use href="#ico16_arrow_down"></use>
            </svg>
            <div class="post-info__header__contextmenu contextmenu" ng-show="self.contextMenuOpen">
                <div class="content">
                    <div class="topline">
                        <svg class="post-info__header__icon__small__contextmenu clickable"
                            style="--local-primary:black;"
                            ng-click="self.contextMenuOpen = false">
                            <use href="#ico16_arrow_up"></use>
                        </svg>
                    </div>
                    <a class="rdflink withlabel clickable"
                        target="_blank"
                        href="{{self.post.get('uri')}}">
                        <svg class="rdflink__small">
                            <use href="#rdf_logo_1"></use>
                        </svg>
                        <span class="rdflink__text">Show RDF</span>
                    </a>
                </div>
            </div>
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
            <!-- IS Part -->
            <div ng-show="self.isPart">
                <won-post-is-info is-part="::self.isPart"></won-post-is-info>
            </div>
            </br>
            <!-- SEEKS Part -->
            <div ng-show="self.seeksPart">
                <won-post-seeks-info seeks-part="::self.seeksPart"></won-post-seeks-info>
            </div>
            </br>
        </div>
        <div class="post-info__footer">
            <div class="post-info__footer__link" ng-if="self.post.get('state') !== self.WON.InactiveCompacted">
                <p class="post-info__footer__link__text" ng-if="self.post.get('connections').size == 0 && self.post.get('ownNeed')">
                    Your posting has no connections yet. Consider sharing the link below in social media, or wait for matchers to connect you with others.
                </p>
                <p class="post-info__footer__link__text" ng-if="(self.post.get('connections').size != 0 && self.post.get('ownNeed')) || !self.post.get('ownNeed')">
                    Know someone who might also be interested in this posting? Consider sharing the link below in social media.
                </p>
                <input class="post-info__footer__link__input" value="{{self.linkToPost}}" disabled type="text">
            </div>

            <button class="post-info__footer__button won-button--filled red"
                    ng-if="self.post.get('ownNeed') && self.post.get('state') === self.WON.InactiveCompacted"
                    ng-click="self.reOpenPost()">
                    Reopen Post
            </button>
            <button class="post-info__footer__button won-button--filled red"
                    ng-if="self.post.get('ownNeed') && self.post.get('state') === self.WON.ActiveCompacted"
                    ng-click="self.closePost()">
                    Close Post
            </button>
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
                    linkToPost: post && new URL("/owner/#!/post/?postUri="+encodeURI(post.get('uri')), window.location.href).href,
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.includeHeader'], this);
        }

        closePost() {
            if(this.post.get("ownNeed")){
                console.log("CLOSING THE POST: "+this.post.get('uri'));
                this.needs__close(this.post.get('uri'));
            }
        }

        reOpenPost() {
            if(this.post.get("ownNeed")){
                console.log("RE-OPENING THE POST: "+this.post.get('uri'));
                this.needs__reopen(this.post.get('uri'));
            }
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
        postHeaderModule
	])
    .directive('wonPostInfo', genComponentConf)
    .name;
