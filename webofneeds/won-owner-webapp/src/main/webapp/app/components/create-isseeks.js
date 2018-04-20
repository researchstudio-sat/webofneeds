/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';

import 'ng-redux';
import won from '../won-es6.js';
import {
	postTitleCharacterLimit,
} from '../config.js';
import needTextfieldModule from './need-textfield.js';
import imageDropzoneModule from './image-dropzone.js';
import locationPickerModule from './location-picker.js';
import {
	getIn,
    attach,
    deepFreeze,
    clone,
    dispatchEvent,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import Immutable from 'immutable';
import {
    connect2Redux,
} from '../won-utils.js';


const emptyDraft = deepFreeze({
	title: "", 
	type: won.WON.BasicNeedTypeCombined, 
	description: "",
	tags: undefined, 
	location: undefined, 
	thumbnail: undefined, 
	matchingContext: undefined
});

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = ['$ngRedux', '$scope', '$element'/*, '$routeParams' /*injections as strings here*/];

function genComponentConf() {
    const template = `
		<div class="cp__header addDetail clickable" 
		    ng-click="self.toggleDropDown()" 
		    ng-class="{'closedDetail': !self.checkDropDown()}">
		
		    <svg class="cp__circleicon" ng-show="!self.checkDropDown()">
		        <use href="#ico36_plus_circle"></use>
		    </svg>
		    <svg class="cp__circleicon" ng-show="self.checkDropDown()">
		        <use href="#ico36_close_circle"></use>
		    </svg>
		    <span ng-show="!self.checkDropDown()">
		        Add 
		    </span>
		    <span class="hover" ng-show="self.checkDropDown()">
		        Remove
		    </span>
		    {{::self.isOrSeeks}}
		    <span class="opt">(allows others to find your post)</span>
		</div>
		<div class="cp__detail__items" ng-if="self.checkDropDown()" >
		    <div class="cp__mandatory-rest ng-if="self.checkDropDown()">
		        <won-image-dropzone on-image-picked="::self.pickImage(image)">
		        </won-image-dropzone>
				<need-textfield on-draft-change="::self.setDraft(draft)"></need-textfield>
			</div>
			<div class="cp__textfield_instruction" ng-if="self.checkDropDown()">
				<span>Title (1st line) &crarr; Longer description. Supports #tags.</span>
			</div>
			
			<!-- DETAILS -->
			<div class="cp__details" ng-repeat="detail in self.getArrayFromSet(self.details) track by $index" ng-if="self.isValid()">
				<div class="cp__location"  ng-if="detail === 'location'">
		        	<div class="cp__header location" ng-click="self.details.delete('location') && self.updateDraft()">
		                <svg class="cp__circleicon">
		                    <use href="#ico36_location_circle"></use>
		                </svg>
		                <span class="nonHover">Location</span>
		                <span class="hover">Remove Location</span>
					</div>
		        	<won-location-picker id="seeksPicker" 
		        		on-draft-change="::self.setDraft(draft)"
		        		location-is-saved="::self.locationIsSaved()">
		        	</won-location-picker>
		    	</div>
		    	<!-- TAGS -->
		    	 <div class="cp__tags" ng-if="detail === 'tags'">
		            <div class="cp__header tags" ng-click="self.resetTags() && self.updateDraft()">
		                <svg class="cp__circleicon">
		                    <use href="#ico36_tags_circle"></use>
		                </svg>
		                <span class="nonHover">Tags</span>
		                <span class="hover">Remove Tags</span>
		            </div>
		            <div class="cp__taglist">
		                <span class="cp__taglist__tag" ng-repeat="tag in self.draftObject.tags">{{tag}}</span>
		            </div>
		            <input class="cp__tags__input" 
			            placeholder="e.g. #couch #free" type="text" 
			            ng-model="self.tagsString"
			            ng-keyup="::self.addTags()"
			         />
		        </div>
		    </div>
			<!-- /DETAILS -->
			<!-- DETAILS Picker -->
			<div class="cp__addDetail" ng-if="self.isValid()">
		        <div class="cp__header detailPicker clickable" 
		            ng-click="self.toggleDetail()" 
		            ng-class="{'closedDetailPicker': !self.showDetail}">
		                <span class="nonHover">Add more detail</span>
		                <span class="hover" ng-if="!self.showDetail">Open more detail</span>
		                <span class="hover" ng-if="self.showDetail">Close more detail</span>
		                <svg class="cp__carret" ng-show="!self.showDetail">
		                    <use href="#ico16_arrow_down"></use>
		                </svg>
		                <svg class="cp__carret" ng-show="self.showDetail">
		                    <use href="#ico16_arrow_up"></use>
		                </svg>
		        </div>
		        <div class="cp__detail__items" ng-if="self.showDetail" >
		            <div class="cp__detail__items__item location" 
		                ng-click="!self.details.has('location') && self.details.add('location')"
		                ng-class="{'picked' : self.details.has('location')}">
		                    <svg class="cp__circleicon" ng-show="!self.details.has('location')">
		                        <use href="#ico36_location_circle"></use>
		                    </svg>
		                    <svg class="cp__circleicon" ng-show="self.details.has('location')">
		                        <use href="#ico36_added_circle"></use>
		                    </svg>
		                    Address or Location
		                </div>   
		            <div class="cp__detail__items__item tags"
		                ng-click="!self.details.has('tags') && self.details.add('tags')"
		                ng-class="{'picked' : self.details.has('tags')}">
		                    <svg class="cp__circleicon" ng-show="!self.details.has('tags')">
		                        <use href="#ico36_tags_circle"></use>
		                    </svg>
		                    <svg class="cp__circleicon" ng-show="self.details.has('tags')">
		                        <use href="#ico36_added_circle"></use>
		                    </svg>
		                    Tags
		            </div>
		        </div>
		    </div>
		    <!-- /DETAIL Picker/ -->
		</div>
`;
    
    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            //TODO debug; deleteme
            window.cis4dbg = this;

            this.characterLimit = postTitleCharacterLimit;
           
            this.isOpen = false;
            this.showDetail = false;
            this.details = new Set();
            
            this.resetObject();
            
            const selectFromState = (state) => {
 
            	return {
                   
                }
            };
            
            
            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }
    
        isValid(){
    		return (this.draftObject) 
	        		&& (this.draftObject.title)
	        		&& (this.draftObject.title.length < this.characterLimit);  
        }
        checkDropDown(){
        	return this.isOpen;
        }       
        toggleDropDown(){
        	if(this.isOpen){
        		this.resetObject();
        		this.updateDraft();         	
        	}
        	this.isOpen = !this.isOpen;
        	
        }
        resetObject(){
        	this.showDetail = false;
        	this.details = new Set();
        	this.tagsString = "";
        	this.tempTags = [];
            this.draftObject = clone(emptyDraft);
        }
        
        resetTags(){
        	this.tagsString = "";
        	this.tempTags = [];
        	this.details.delete('tags');
        	this.draftObject.tags = undefined;
        }
        
        updateDraft(){
        	if(!this.details.has("tags")){
        		this.draftObject.tags = undefined;
            }
            if(!this.details.has("location")){
            	this.draftObject.location = undefined;
            }
        	
        	this.onUpdate({draft: this.draftObject});
            dispatchEvent(this.$element[0], 'update', {draft: this.draftObject});
        }
        
        setDraft(updatedDraft) {       	
            if(updatedDraft && updatedDraft.tags && updatedDraft.tags.length > 0 && !this.details.has("tags")){
                this.details.add("tags");
            }
            this.tempTags = updatedDraft.tags;
            updatedDraft.tags = this.mergeTags();
            Object.assign(this.draftObject, updatedDraft);
            this.updateDraft();
        }
        
       


        mergeTags() {
            let detailTags = Immutable.Set(this.tagsString? this.tagsString.match(/#(\S+)/gi) : []).map(tag => tag.substr(1)).toJS();

            let combinedTags = this.tempTags? detailTags.concat(this.tempTags) : detailTags;

            const immutableTagSet = Immutable.Set(combinedTags);
            return immutableTagSet.toJS();
        }

        addTags() {
            this.draftObject.tags = this.mergeTags();
        }
        
        locationIsSaved() {
            return this.details.has("location") && this.draftObject.location && this.draftObject.location.name;
        }

        pickImage(image) {
            this.draftObject.thumbnail = image;
        }

        toggleDetail(){
        	this.showDetail = !this.showDetail;
        }
        
        getArrayFromSet(set){
        	return Array.from(set);
        }
    }
    
    
    
    Controller.$inject = serviceDependencies;

    return {
    	restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
        	isOrSeeks: '=',
        	 /*
             * Usage:
             *  on-update="::myCallback(draft)"
             */
        	onUpdate: '&',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.createIsseek', [
        /*createNeedTitleBarModule,
        posttypeSelectModule,
        labelledHrModule,
        imageDropzoneModule,
        needTextfieldModule,
        locationPickerModule,
        ngAnimate,*/
    ])
    .directive('wonCreateIsseeks', genComponentConf)
    //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
