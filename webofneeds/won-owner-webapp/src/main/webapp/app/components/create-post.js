/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular'
import ngAnimate from 'angular-animate';

import 'ng-redux';
import createNeedTitleBarModule from './create-need-title-bar.js';
import posttypeSelectModule from './posttype-select.js';
import labelledHrModule from './labelled-hr.js';
import needTextfieldModule from './need-textfield.js';
import imageDropzoneModule from './image-dropzone.js';
import locationPickerModule from './location-picker.js';
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

	            <!-- IS PART -->
                <div class="cp__header addDetail clickable" 
                    ng-click="self.toggleDropDown(self.is)" 
                    ng-class="{'closedDetail': !self.checkDropDown(self.is)}">

                    <svg class="cp__circleicon" ng-show="!self.checkDropDown(self.is)">
                        <use href="#ico36_plus_circle"></use>
                    </svg>
                    <svg class="cp__circleicon" ng-show="self.checkDropDown(self.is)">
                        <use href="#ico36_close_circle"></use>
                    </svg>
                    <span ng-show="!self.checkDropDown(self.is)">
                        Add 
                    </span>
                    <span class="hover" ng-show="self.checkDropDown(self.is)">
                        Remove
                    </span>
                    Description
                    <span class="opt">(allows others to find your post)</span>
    			</div>
	            <div class="cp__detail__items" ng-if="self.checkDropDown(self.is)" >
		            <div class="cp__mandatory-rest ng-if="self.checkDropDown(self.is)">
				        <won-image-dropzone on-image-picked="::self.pickImage(image, self.is)">
				        </won-image-dropzone>
						<need-textfield on-draft-change="::self.setDraft(draft, self.is)"></need-textfield>
					</div>
					<div class="cp__textfield_instruction" ng-if="self.checkDropDown(self.is)">
						<span>Title (1st line) &crarr; Longer description. Supports #tags.</span>
					</div>
					
					<!-- DETAILS -->
					<div class="cp__details" ng-repeat="detail in self.details[self.is] track by $index" ng-if="self.isValid(self.is)">
						<!-- LOCATION ng-if="self.isDetailPresent('location', self.is)"-->
						<div class="cp__location"  ng-if="detail === 'location'">
	                    	<div class="cp__header location" ng-click="self.removeDetail('location', self.is)">
                                <svg class="cp__circleicon">
                                    <use href="#ico36_location_circle"></use>
                                </svg>
		                        <span class="nonHover">Location</span>
		                        <span class="hover">Remove Location</span>
	    					</div>
	                    	<won-location-picker id="seeksPicker" on-draft-change="::self.setDraft(draft, self.is)" location-is-saved="::self.locationIsSaved(self.is)"></won-location-picker>
	                	</div>
	                	<!-- TAGS -->
	                	 <div class="cp__tags" ng-if="detail === 'tags'">
		                    <div class="cp__header tags" ng-click="self.removeDetail('tags', self.is)">
                                <svg class="cp__circleicon">
                                    <use href="#ico36_tags_circle"></use>
                                </svg>
		                        <span class="nonHover">Tags</span>
		                        <span class="hover">Remove Tags</span>
		                    </div>
		                    <div class="cp__taglist">
		                        <span class="cp__taglist__tag" ng-repeat="tag in self.draftObject[self.is].tags">{{tag}}</span>
		                    </div>
		                    <input class="cp__tags__input" placeholder="e.g. #couch #free" type="text" ng-model="self.tagsString[self.is]" ng-keyup="::self.addTags(self.is)"/>
		                </div>
		            </div>
                	<!-- /DETAILS -->
    				<!-- DETAILS Picker -->
	    			<div class="cp__addDetail" ng-if="self.isValid(self.is)">
                        <div class="cp__header detailPicker clickable" 
                            ng-click="self.toggleDetail(self.is)" 
                            ng-class="{'closedDetailPicker': !self.showDetail[self.is]}">
                                <span class="nonHover">Add more detail</span>
                                <span class="hover" ng-if="!self.showDetail[self.is]">Open more detail</span>
                                <span class="hover" ng-if="self.showDetail[self.is]">Close more detail</span>
                                <svg class="cp__carret" ng-show="!self.showDetail[self.is]">
                                    <use href="#ico16_arrow_down"></use>
                                </svg>
                                <svg class="cp__carret" ng-show="self.showDetail[self.is]">
                                    <use href="#ico16_arrow_up"></use>
                                </svg>
		                </div>
			            <div class="cp__detail__items" ng-if="self.showDetail[self.is]" >
		                    <div class="cp__detail__items__item location" 
		                        ng-click="!self.isDetailPresent('location', self.is) && self.addDetail('location', self.is)"
                                ng-class="{'picked' : self.isDetailPresent('location', self.is)}">
                                    <svg class="cp__circleicon" ng-show="!self.isDetailPresent('location', self.is)">
                                        <use href="#ico36_location_circle"></use>
                                    </svg>
                                    <svg class="cp__circleicon" ng-show="self.isDetailPresent('location', self.is)">
                                        <use href="#ico36_added_circle"></use>
                                    </svg>
                                    Address or Location
                                </div>   
		                    <div class="cp__detail__items__item tags"
		                        ng-click="!self.isDetailPresent('tags', self.is) && self.addDetail('tags', self.is)"
                                ng-class="{'picked' : self.isDetailPresent('tags', self.is)}">
                                    <svg class="cp__circleicon" ng-show="!self.isDetailPresent('tags', self.is)">
                                        <use href="#ico36_tags_circle"></use>
                                    </svg>
                                    <svg class="cp__circleicon" ng-show="self.isDetailPresent('tags', self.is)">
                                        <use href="#ico36_added_circle"></use>
                                    </svg>
                                    Tags
                            </div>
		                </div>
		            </div>
		            <!-- /DETAIL Picker/ -->
                </div>
                <!-- /IS PART/ -->
                    
                <won-labelled-hr label="::' + '" class="cp__labelledhr" ng-if="(self.checkDropDown(self.seeks) || self.checkDropDown(self.is))"></won-labelled-hr> 
                
                <!-- SEEKS PART -->   	
                <div class="cp__header addDetail clickable" 
                    ng-click="self.toggleDropDown(self.seeks)" 
                    ng-class="{'closedDetail': !self.checkDropDown(self.seeks)}">

                    <svg class="cp__circleicon" ng-show="!self.checkDropDown(self.seeks)">
                        <use href="#ico36_plus_circle"></use>
                    </svg>
                    <svg class="cp__circleicon" ng-show="self.checkDropDown(self.seeks)">
                        <use href="#ico36_close_circle"></use>
                    </svg>
                    <span ng-show="!self.checkDropDown(self.seeks)">
                        Add 
                    </span>
                    <span class="hover" ng-show="self.checkDropDown(self.seeks)">
                        Remove
                    </span>
                    Search
                    <span class="opt">(search in other posts)</span>
    			</div>
	            <div class="cp__detail__items" ng-if="self.checkDropDown(self.seeks)" >
		            <div class="cp__mandatory-rest ng-if="self.checkDropDown(self.seeks)">
				        <won-image-dropzone on-image-picked="::self.pickImage(image, self.seeks)">
				        </won-image-dropzone>
						<need-textfield on-draft-change="::self.setDraft(draft, self.seeks)"></need-textfield>
					</div>
					<div class="cp__textfield_instruction" ng-if="self.checkDropDown(self.seeks)">
						<span>Title (1st line) &crarr; Longer description. Supports #tags.</span>
					</div>
					
					<!-- DETAILS -->
					<div class="cp__details" ng-repeat="detail in self.details[self.seeks] track by $index" ng-if="self.isValid(self.seeks)">
						<!-- LOCATION ng-if="self.isDetailPresent('location', self.seeks)"-->
						<div class="cp__location"  ng-if="detail === 'location'">
	                    	<div class="cp__header location" ng-click="self.removeDetail('location', self.seeks)">
                                <svg class="cp__circleicon">
                                    <use href="#ico36_location_circle"></use>
                                </svg>
		                        <span class="nonHover">Location</span>
		                        <span class="hover">Remove Location</span>
	    					</div>
	                    	<won-location-picker id="seeksPicker" on-draft-change="::self.setDraft(draft, self.seeks)" location-is-saved="::self.locationIsSaved(self.seeks)"></won-location-picker>
	                	</div>
	                	<!-- TAGS -->
	                	 <div class="cp__tags" ng-if="detail === 'tags'">
		                    <div class="cp__header tags" ng-click="self.removeDetail('tags', self.seeks)">
                                <svg class="cp__circleicon">
                                    <use href="#ico36_tags_circle"></use>
                                </svg>
		                        <span class="nonHover">Tags</span>
		                        <span class="hover">Remove Tags</span>
		                    </div>
		                    <div class="cp__taglist">
		                        <span class="cp__taglist__tag" ng-repeat="tag in self.draftObject[self.seeks].tags">{{tag}}</span>
		                    </div>
		                    <input class="cp__tags__input" placeholder="e.g. #couch #free" type="text" ng-model="self.tagsString[self.seeks]" ng-keyup="::self.addTags(self.seeks)"/>
		                </div>
		            </div>
                	<!-- /DETAILS -->
    				<!-- DETAILS Picker -->
	    			<div class="cp__addDetail" ng-if="self.isValid(self.seeks)">
                        <div class="cp__header detailPicker clickable" 
                            ng-click="self.toggleDetail(self.seeks)" 
                            ng-class="{'closedDetailPicker': !self.showDetail[self.seeks]}">
                                <span class="nonHover">Add more detail</span>
                                <span class="hover" ng-if="!self.showDetail[self.seeks]">Open more detail</span>
                                <span class="hover" ng-if="self.showDetail[self.seeks]">Close more detail</span>
                                <svg class="cp__carret" ng-show="!self.showDetail[self.seeks]">
                                    <use href="#ico16_arrow_down"></use>
                                </svg>
                                <svg class="cp__carret" ng-show="self.showDetail[self.seeks]">
                                    <use href="#ico16_arrow_up"></use>
                                </svg>
		                </div>
			            <div class="cp__detail__items" ng-if="self.showDetail[self.seeks]" >
		                    <div class="cp__detail__items__item location" 
		                        ng-click="!self.isDetailPresent('location', self.seeks) && self.addDetail('location', self.seeks)"
                                ng-class="{'picked' : self.isDetailPresent('location', self.seeks)}">
                                    <svg class="cp__circleicon" ng-show="!self.isDetailPresent('location', self.seeks)">
                                        <use href="#ico36_location_circle"></use>
                                    </svg>
                                    <svg class="cp__circleicon" ng-show="self.isDetailPresent('location', self.seeks)">
                                        <use href="#ico36_added_circle"></use>
                                    </svg>
                                    Address or Location
                                </div>   
		                    <div class="cp__detail__items__item tags"
		                        ng-click="!self.isDetailPresent('tags', self.seeks) && self.addDetail('tags', self.seeks)"
                                ng-class="{'picked' : self.isDetailPresent('tags', self.seeks)}">
                                    <svg class="cp__circleicon" ng-show="!self.isDetailPresent('tags', self.seeks)">
                                        <use href="#ico36_tags_circle"></use>
                                    </svg>
                                    <svg class="cp__circleicon" ng-show="self.isDetailPresent('tags', self.seeks)">
                                        <use href="#ico36_added_circle"></use>
                                    </svg>
                                    Tags
                            </div>
		                </div>
		            </div>
		            <!-- /DETAIL Picker/ -->
                </div>
	            <!-- /SEEKS PART/ -->
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
            this.characterLimit = 140; //TODO move to conf
            this.draftIs = {title: "", type: postTypeTexts[3].type, description: "", tags: undefined, location: undefined, thumbnail: undefined, matchingContext: undefined};
            this.draftSeeks = {title: "", type: postTypeTexts[3].type, description: "", tags: undefined, location: undefined, thumbnail: undefined, matchingContext: undefined};
            this.draftObject = {is: this.draftIs, seeks: this.draftSeeks};
            
            this.isOpen = {is: false, seeks: false};
            this.is = 'is'
            this.seeks = 'seeks';
           
            this.select = ['isPart', 'seeksPart'];
            
            this.pendingPublishing = false;

            this.showDetail = {is: false, seeks: false};
            this.details = {is: [], seeks: []};
            this.tagsString = {is: "", seeks: ""};
            this.tempTags = {is: [], seeks: []};
            
            this.isNew = false;
            
            this.tempMContext = this.$ngRedux.getState().getIn(['config', 'theme', 'defaultContext']);
            this.tempMatchingContext = this.tempMContext? this.tempMContext.toJS() : [];
            this.tempMatchingString = this.tempMatchingContext? this.tempMatchingContext.join(" ") : "";
            
            const selectFromState = (state) => {
 
            	return {
                    existingWhatsAroundNeeds: state.get("needs").filter(need => need.get("isWhatsAround"))
                }
            };
            
            
            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }
        isValid(isSeeks){
        	if(isSeeks){
        		return (this.draftObject[isSeeks]) 
		        		&& (this.draftObject[isSeeks].title)
		        		&& (this.draftObject[isSeeks].title.length < this.characterLimit);
        	}
        	
            return (this.draftObject[this.is] || this.draftObject[this.seeks]) 
            		&& (this.draftObject[this.is].title || this.draftObject[this.seeks].title)
            		&& ((this.draftObject[this.is].title.length < this.characterLimit) || (this.draftObject[this.seeks].title.length < this.characterLimit));
        }
        checkDropDown(isSeeks){
        	return this.isOpen[isSeeks];
        }       
        toggleDropDown(isSeeks){
        	if(this.isOpen[isSeeks]){
        		this.draftObject[isSeeks] = this.resetObject(isSeeks);
         	}
        	this.isOpen[isSeeks] = !this.isOpen[isSeeks];
        	
        }
        resetObject(isSeeks){
        	this.showDetail[isSeeks] = false;
        	this.details[isSeeks] = [];
        	this.tagsString[isSeeks] = "";
            this.tempTags[isSeeks] = [];
        	return {title: "", type: postTypeTexts[3].type, description: "", tags: undefined, location: undefined, thumbnail: undefined, matchingContext: undefined};
        }
        
        titlePicZoneNg() {
            return angular.element(this.titlePicZone())
        }
        titlePicZone() {
            if(!this._titlePicZone) {
                this._titlePicZone = this.$element[0].querySelector('#titlePic');
            }
            return this._titlePicZone;
        }
        publish() {
        	// Post both needs
            if (!this.pendingPublishing) {
                this.pendingPublishing = true;

                var tmpList = [this.is, this.seeks];
                var newObject = {is: this.draftObject.is, seeks: this.draftObject.seeks}; 
                
                for(i = 0; i < 2; i ++){
                	var tmp = tmpList[i];
                	if(!this.isDetailPresent("tags", tmp)){
                		newObject[tmp].tags = undefined;
                    }
                    if(!this.isDetailPresent("location", tmp)){
                    	newObject[tmp].location = undefined;
                    }
                    
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

        setDraft(updatedDraft, isSeeks) {
            if(updatedDraft && updatedDraft.tags && updatedDraft.tags.length > 0 && !this.isDetailPresent("tags", isSeeks)){
                this.addDetail("tags", isSeeks);
            }
            this.tempTags[isSeeks] = updatedDraft.tags;
            updatedDraft.tags = this.mergeTags();
            Object.assign(this.draftObject[isSeeks], updatedDraft);
        }

        mergeTags(isSeeks) {
            let detailTags = Immutable.Set(this.tagsString[isSeeks]? this.tagsString[isSeeks].match(/#(\S+)/gi) : []).map(tag => tag.substr(1)).toJS();

            let combinedTags = this.tempTags[isSeeks]? detailTags.concat(this.tempTags[isSeeks]) : detailTags;

            const immutableTagSet = Immutable.Set(combinedTags);
            return immutableTagSet.toJS();
        }

        addTags(isSeeks) {
            this.draftObject[isSeeks].tags = this.mergeTags(isSeeks);
        }
        
        mergeMatchingContext() {
        	
        	//return (this.matchingContextString? this.matchingContextString.match(/(\S+)/gi) : []);
        	
        	//var names = ["Mike","Matt","Nancy","Adam","Jenny","Nancy","Carl"];

        	var list = this.tempMatchingString? this.tempMatchingString.match(/(\S+)/gi) : [];
        	var uniq = list.reduce(function(a,b){
        	    if (a.indexOf(b) < 0 ) a.push(b);
        	    return a;
        	  },[]);

        	//console.log(uniq, names) // [ 'Mike', 'Matt', 'Nancy', 'Adam', 'Jenny', 'Carl' ]

        	// one liner
        	return list.reduce(function(a,b){if(a.indexOf(b)<0)a.push(b);return a;},[]);
        	
        	/*
            //let detailContext = Immutable.Set(this.matchingContextString? this.matchingContextString.match(/(\S+)/gi) : []).map(context => context.substr(1)).toJS();
        	let detailContext = Immutable.Set(this.matchingContextString? this.matchingContextString.match(/#(\S+)/gi) : []).map(context => context.substr(1)).toJS();
        	
            let combinedContext = this.tempMatchingContext? detailContext.concat(this.tempMatchingContext) : detailContext;

            const immutableContextSet = Immutable.Set(combinedContext);
            return immutableContextSet.toJS();
            */
            
        }
        
        addMatchingContext() {
            this.tempMatchingContext = this.mergeMatchingContext();
        }

        locationIsSaved(isSeeks) {
            return this.isDetailPresent("location", isSeeks) && this.draftObject[isSeeks].location && this.draftObject[isSeeks].location.name;
        }

        pickImage(image, isSeeks) {
            this.draftObject[isSeeks].thumbnail = image;
        }

        toggleDetail(isSeeks){
        	//TODO decide if clear detail list or not
        	this.showDetail[isSeeks] = !this.showDetail[isSeeks];
        }

        addDetail(detail, isSeeks) {
            this.details[isSeeks].push(detail);
        }

        removeDetail(detail, isSeeks) {
        	 var tempDetails = [];
             for(var i=0; i < this.details[isSeeks].length; i++){
                 if(this.details[isSeeks][i] != detail) tempDetails.push(this.details[isSeeks][i]);
             }
             this.details[isSeeks] = tempDetails;
        }

        isDetailPresent(detail, isSeeks) {
            return this.details[isSeeks].indexOf(detail) > -1;
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
        ngAnimate,
    ])
    .directive('wonCreatePost', genComponentConf)
    //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
