/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ngAnimate from 'angular-animate';

import 'ng-redux';
import posttypeSelectModule from './posttype-select.js';
import labelledHrModule from './labelled-hr.js';
import needTextfieldModule from './need-textfield.js';
import imageDropzoneModule from './image-dropzone.js';
import locationPickerModule from './location-picker.js';
import createIsseeksModule from './create-isseeks.js';
import {
	postTitleCharacterLimit,
} from '../config.js';
import {
	getIn,
    attach,
    reverseSearchNominatim,
    nominatim2draftLocation,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import won from '../won-es6.js';
import Immutable from 'immutable';
import {
    connect2Redux,
} from '../won-utils.js';

const postTypeTexts = [
    {
        type: won.WON.BasicNeedTypeDemand,
        text: 'I\'m looking for',
        helpText: 'Select this if you are looking for things or services other people offer',
    },
    {
        type: won.WON.BasicNeedTypeSupply,
        text: 'I\'m offering',
        helpText: 'Use this if you are offering an item or a service. You will find people who said' +
        ' that they\'re looking for something.'
    },
    {
        type: won.WON.BasicNeedTypeDotogether,
        text: 'I want to find someone to',
        helpText: 'Select this if you are looking to find other people who share your interest. You will be matched' +
        ' with other people who chose this option as well.'
    },
    {
        type: won.WON.BasicNeedTypeCombined,
        text: 'I\'m offering and looking for',
        helpText: 'Select this if you are looking for things or services other people offer + Use this if you are offering an item or a service. You will find people who said' +
        ' that they\'re looking for something.'
    },
];

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = ['$ngRedux', '$scope'/*'$routeParams' /*injections as strings here*/];

function genComponentConf() {
    const template = `
        <div class="cp__header">
            <a class="cp__header__back clickable show-in-responsive"
                ng-click="self.router__stateGoCurrent({showCreateView: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="cp__header__back__icon">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <span class="cp__header__title" ng-if="self.isPost">Post</span>
            <span class="cp__header__title" ng-if="self.isSearch">Search</span>
        </div>
        <div class="cp__content">
            <won-create-isseeks ng-if="self.isPost" is-or-seeks="::'Description'" on-update="::self.updateDraft(draft, self.is)"></won-create-isseeks>
            <won-create-isseeks ng-if="self.isSearch" is-or-seeks="::'Search'" on-update="::self.updateDraft(draft, self.seeks)"></won-create-isseeks>
            <!-- TODO: decide on whether to re-add stuff like an additional search/description window or something for adding contexts -->
        </div>
        <div class="cp__footer">
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <button type="submit" class="won-button--filled red cp__footer__publish"
                    ng-disabled="!self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
        <!-- TODO: move whatsaround functionality somewhere else. i commented out the following code snippet because the fallback ng-if-clause has turned into an oxymoron
            Excluded due to #1632 https://github.com/researchstudio-sat/webofneeds/issues/1632
        -->
        <!--div ng-if="!self.isPost && !self.isSearch">
            <button type="submit"
                    class="won-button--filled red"
                    ng-click="::self.createWhatsAround()">
                <span ng-show="!self.pendingPublishing">
                    See What's Around
                </span>
                <span ng-show="self.pendingPublishing">
                    Retrieving What's Around&nbsp;&hellip;
                </span>
            </button>
        </div-->
        <!-- Excluded due to #1627 https://github.com/researchstudio-sat/webofneeds/issues/1627
        Be aware that the styling of these elements is not valid anymore
        <won-labelled-hr label="::'add context?'" class="cp__footer__labelledhr" ng-if="self.isValid()"></won-labelled-hr>

        <div class="cp__detail" ng-if="self.isValid()">
            <div class="cp__detail__header context">
                <span>Matching Context(s) <span class="opt">(restricts matching)</span></span><br/>
            </div>
            <div class="cp__taglist">
                <span class="cp__taglist__tag" ng-repeat="context in self.tempMatchingContext">{{context}} </span>
            </div>
            <input class="cp__tags__input" placeholder="{{self.tempMatchingString? self.tempMatchingString : 'e.g. \\'sports fitness\\''}}" type="text" ng-model="self.tempMatchingString" ng-keyup="::self.addMatchingContext()"/>
            <div class="cp__textfield_instruction">
                <span>use whitespace to separate context names</span>
            </div>
        </div>
        -->
    `;
    
    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            this.SEARCH = "search";
            this.POST = "post";

            //TODO debug; deleteme
            window.cnc = this;
           
            this.postTypeTexts = postTypeTexts;
            this.characterLimit = postTitleCharacterLimit;
            this.draftIs = {title: "", type: postTypeTexts[3].type, description: "", tags: undefined, location: undefined, thumbnail: undefined, matchingContext: undefined};
            this.draftSeeks = {title: "", type: postTypeTexts[3].type, description: "", tags: undefined, location: undefined, thumbnail: undefined, matchingContext: undefined};
            
            this.draftObject = {is: this.draftIs, seeks: this.draftSeeks};
            
            this.is = 'is'
            this.seeks = 'seeks';
           
            this.pendingPublishing = false;
            this.details = {is: [], seeks: []};
            this.isNew = true;
            
            this.defaultContext = this.$ngRedux.getState().getIn(['config', 'theme', 'defaultContext']);
            this.tempMatchingContext = this.defaultContext? this.defaultContext.toJS() : [];
            this.tempMatchingString = this.tempMatchingContext? this.tempMatchingContext.join(" ") : "";
            
            const selectFromState = (state) => {
                const showCreateView = getIn(state, ['router', 'currentParams', 'showCreateView']);
                const isSearch = showCreateView === this.SEARCH;
                const isPost = showCreateView && !isSearch;

            	return {
                    existingWhatsAroundNeeds: state.get("needs").filter(need => need.get("isWhatsAround")),
                    showCreateView,
                    isSearch,
                    isPost
                }
            };
            
            
            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }
        
        isValid(){
            return (this.draftObject[this.is] || this.draftObject[this.seeks]) 
            		&& (this.draftObject[this.is].title || this.draftObject[this.seeks].title)
            		&& ((this.draftObject[this.is].title.length < this.characterLimit) || (this.draftObject[this.seeks].title.length < this.characterLimit));
        }
       
        updateDraft(updatedDraft, isSeeks) {
        	if(this.isNew){
        		this.isNew = false;
        		if(!this.defaultContext) {
	        		this.defaultContext = this.$ngRedux.getState().getIn(['config', 'theme', 'defaultContext']);
	                this.tempMatchingContext = this.defaultContext? this.defaultContext.toJS() : [];
	                this.tempMatchingString = this.tempMatchingContext? this.tempMatchingContext.join(" ") : "";
        		}
        	}
        	
        	this.draftObject[isSeeks] = updatedDraft;
        }
        
        mergeMatchingContext() {
        	var list = this.tempMatchingString? this.tempMatchingString.match(/(\S+)/gi) : [];
        	var uniq = list.reduce(function(a,b){
        	    if (a.indexOf(b) < 0 ) a.push(b);
        	    return a;
        	  },[]);

        	return list.reduce(function(a,b){if(a.indexOf(b)<0)a.push(b);return a;},[]);          
        }
        
        addMatchingContext() {
            this.tempMatchingContext = this.mergeMatchingContext();
        }

        
        publish() {
        	// Post both needs
            if (!this.pendingPublishing) {
                this.pendingPublishing = true;

                var tmpList = [this.is, this.seeks];
                var newObject = {is: this.draftObject.is, seeks: this.draftObject.seeks}; 
                
                for(i = 0; i < 2; i ++){
                	var tmp = tmpList[i];
                	                   
                    if(this.tempMatchingContext.length > 0){
                    	newObject[tmp].matchingContext = this.tempMatchingContext;
                    }
                    
                    if(newObject[tmp].title === "") {
                    	delete newObject[tmp];
                    }
                }
                
               this.needs__create(
                    newObject,
                		this.$ngRedux.getState().getIn(['config', 'defaultNodeUri'])
                );
            }
        }
      
        createWhatsAround(){
            if (!this.pendingPublishing) {
                this.pendingPublishing = true;

                if ("geolocation" in navigator) {
                    navigator.geolocation.getCurrentPosition(
                        currentLocation => {
                            const lat = currentLocation.coords.latitude;
                            const lng = currentLocation.coords.longitude;
                            const zoom = 13; // TODO use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

                            const degreeConstant = 1.0;

                            // center map around current location

                            reverseSearchNominatim(lat, lng, zoom)
                                .then(searchResult => {
                                    const location = nominatim2draftLocation(searchResult);

                                    let whatsAround = {
                                        title: "What's Around?",
                                        type: "http://purl.org/webofneeds/model#DoTogether",
                                        description: "Automatically created post to see what's happening in your area",
                                        tags: undefined,
                                        location: location,
                                        thumbnail: undefined,
                                        whatsAround: true,
                                        matchingContext: this.tempMatchingContext
                                    };

                                    this.existingWhatsAroundNeeds
                                    	.filter( need => need.get("state") == "won:Active" )
                                    	.map(need => this.needs__close(need.get("uri")) );

                                    //TODO: Point to same DataSet instead of double it
                                    this.draftObject.is = whatsAround;
                                    this.draftObject.seeks = this.draftObject.is;
                                    this.needs__create(
                                        this.draftObject,
                                        this.$ngRedux.getState().getIn(['config', 'defaultNodeUri'])
                                    );
                                });
                        },
                        error => { //error handler
                            console.error("Could not retrieve geolocation due to error: ", error.code, "fullerror:", error);
                            this.geoLocationDenied();
                            this.pendingPublishing = false;
                        },
                        { //options
                            enableHighAccuracy: true,
                            timeout: 5000,
                            maximumAge: 0
                        }
                    )
                }
            }
        }
    }
    Controller.$inject = serviceDependencies;

    return {
    	restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {/*scope-isolation*/},
        template: template,
    }
}

export default angular.module('won.owner.components.createPost', [
        posttypeSelectModule,
        labelledHrModule,
        imageDropzoneModule,
        needTextfieldModule,
        locationPickerModule,
        createIsseeksModule,
        ngAnimate,
    ])
    .directive('wonCreatePost', genComponentConf)
    //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
