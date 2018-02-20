/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ngAnimate from 'angular-animate';

import 'ng-redux';
import createNeedTitleBarModule from './create-need-title-bar.js';
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
        <div class="cp__inner">
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
            
            <won-labelled-hr label="::' or create a specific '" style="margin-top: 2rem; margin-bottom: 2rem;" ></won-labelled-hr>
           
           <div class="cp__title">Post</div>
            <div class="cp__addDetail">

    			<won-create-isseeks is-or-seeks="::'Description'" on-update="::self.updateDraft(draft, self.is)"></won-create-isseeks>
    			
    			<won-labelled-hr label="::' + '" class="cp__labelledhr" ng-if="self.isValid()"></won-labelled-hr> 
    			
    			<won-create-isseeks is-or-seeks="::'Search'" on-update="::self.updateDraft(draft, self.seeks)"></won-create-isseeks>
    			
    		</div>
    		
	       	<won-labelled-hr label="::'add context?'" class="cp__labelledhr" ng-if="self.isValid()"></won-labelled-hr>
	       	
	       	<div class="cp__detail" ng-if="self.isValid()">
		       	<div class="cp__header context">
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
	       	
	       	<won-labelled-hr label="::'done?'" class="cp__labelledhr" ng-if="self.isValid()"></won-labelled-hr>
	       	
	       	<button type="submit" class="won-button--filled red cp__publish"
                    ng-if="self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
    `;
    
    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

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
 
            	return {
                    existingWhatsAroundNeeds: state.get("needs").filter(need => need.get("isWhatsAround")) 
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
        createNeedTitleBarModule,
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
