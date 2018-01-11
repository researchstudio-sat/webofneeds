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
            <won-labelled-hr label="::' or be more specific '"></won-labelled-hr>
            
            
            <!--
					Changes Here
			-->
			
			<!-- Tab links -->
			<div class="tab">
			  <button id="searchButton" class="tablinks active" ng-click="[self.selectType('is'), self.openTab(event, 'Search', 'searchButton')]">Search</button>
			  <button id="postButton" class="tablinks" ng-click="[self.selectType('seeks'), self.openTab(event, 'Post', 'postButton')]">Post</button>
			</div>

			<!-- Tab content -->
			<div id="Search" class="tabcontent" style="display: block;">
			 	<div class="cp__mandatory-rest ng-if="self.draft.type"">
			        <won-image-dropzone on-image-picked="::self.pickImage(image)">
			        </won-image-dropzone>
					<need-textfield on-draft-change="::self.setDraft(draft)"></need-textfield>
				</div>
				<div class="cp__textfield_instruction" ng-if="self.isValid()">
					<span>Title (1st line) &crarr; Longer description. Supports #tags.</span>
				</div>
			</div>
			
			<div id="Post" class="tabcontent">
    			<div class="cp__mandatory-rest ng-if="self.draft.type"">
			        <won-image-dropzone on-image-picked="::self.pickImage(image)">
			        </won-image-dropzone>
					<need-textfield on-draft-change="::self.setDraft(draft)"></need-textfield>
				</div>
				<div class="cp__textfield_instruction" ng-if="self.isValid()">
					<span>Title (1st line) &crarr; Longer description. Supports #tags.</span>
				</div>
    		</div>

    		
    		<!--
			-->

            <div class="cp__details" ng-repeat="detail in self.details[self.isSeeks] track by $index" ng-if="self.isValid()">
                <div class="cp__tags" ng-if="detail === 'tags'">
                    <div class="cp__header tags" ng-click="self.removeDetail($index)">
                        <span class="nonHover">Tags</span>
                        <span class="hover">Remove Tags</span>
                    </div>
                    <div class="cp__taglist">
                        <span class="cp__taglist__tag" ng-repeat="tag in self.draftObject[self.isSeeks].tags">{{tag}}</span>
                    </div>
                    <input class="cp__tags__input" placeholder="e.g. #couch #free" type="text" ng-model="self.tagsString[self.isSeeks]" ng-keyup="::self.addTags()"/>
                </div>
                
                <div class="cp__location" ng-if="detail === 'location'">
                    <div class="cp__header location" ng-click="self.removeDetail($index)">
                        <span class="nonHover">Location</span>
                        <span class="hover">Remove Location</span>
                    </div>
                    <won-location-picker on-draft-change="::self.setDraft(draft)" location-is-saved="::self.locationIsSaved()"></won-location-picker>
                </div>
            </div>

            <div class="cp__addDetail" ng-if="self.isValid()">
                <div class="cp__header addDetail clickable" ng-click="self.toggleDetail()" ng-class="{'closedDetail': !self.showDetail[self.isSeeks]}">
                    <span class="nonHover">Add more detail</span>
                    <span class="hover" ng-if="!self.showDetail[self.isSeeks]">Open more detail</span>
                    <span class="hover" ng-if="self.showDetail[self.isSeeks]">Close more detail</span>
                </div>
                <div class="cp__detail__items" ng-if="self.showDetail[self.isSeeks]" >
                    <div class="cp__detail__items__item location" 
                        ng-click="!self.isDetailPresent('location') && self.addDetail('location')"
                        ng-class="{'picked' : self.isDetailPresent('location')}">Address or Location</div>
                        
                    <div class="cp__detail__items__item tags"
                        ng-click="!self.isDetailPresent('tags') && self.addDetail('tags')"
                        ng-class="{'picked' : self.isDetailPresent('tags')}">Tags</div>
                        
                    <!-- <div class="cp__detail__items__item image" 
                        ng-click="!self.isDetailPresent('image') && self.addDetail('image')"
                        ng-class="{'picked' : self.isDetailPresent('image')}">Image or Media</div>
                    <div class="cp__detail__items__item description" 
                        ng-click="!self.isDetailPresent('description') && self.addDetail('description')"
                        ng-class="{'picked' : self.isDetailPresent('description')}">Description</div>
                    <div class="cp__detail__items__item timeframe" 
                        ng-click="!self.isDetailPresent('timeframe') && self.addDetail('timeframe')"
                        ng-class="{'picked' : self.isDetailPresent('timeframe')}">Deadline or Timeframe</div> -->
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
            this.draftSeeks = {title: "", type: postTypeTexts[0].type, description: "", tags: undefined, location: undefined, thumbnail: undefined};
            this.draftIs = {title: "", type: postTypeTexts[1].type, description: "", tags: undefined, location: undefined, thumbnail: undefined};
            this.draftObject = {seeks: this.draftSeeks, is: this.draftIs};
            this.isSeeks = 'is';
            
            this.pendingPublishing = false;

            this.showDetail = {seeks: false, is: false};
            this.details = {seeks: [], is: []};
            this.tagsString = {seeks: "", is: ""};
            this.tempTags = {seeks: [], is: []};
            
            //this.selectType(0);
            //this.selectedTab = 'Search';          
            const selectFromState = (state) => {
                return {
                    existingWhatsAroundNeeds: state.get("needs").filter(need => need.get("isWhatsAround")),
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }
        isValid(){
            return this.draftObject[this.isSeeks] && this.draftObject[this.isSeeks].title && this.draftObject[this.isSeeks].title.length < this.characterLimit;
        }

        
       openTab(evt, tabName, button) {
            // Declare all variables
            var i, tabcontent, tablinks;

            // Get all elements with class="tabcontent" and hide them
            tabcontent = document.getElementsByClassName("tabcontent");
            for (i = 0; i < tabcontent.length; i++) {
                tabcontent[i].style.display = "none";
            }

            // Get all elements with class="tablinks" and remove the class "active"
            tablinks = document.getElementsByClassName("tablinks");
            for (i = 0; i < tablinks.length; i++) {
                tablinks[i].className = tablinks[i].className.replace(" active", "");
            }

            // Show the current tab, and add an "active" class to the button that opened the tab
            document.getElementById(tabName).style.display = "block";
            document.getElementById(button).className += " active";
        }
        
        
    
        selectType(type) {
        	this.isSeeks = type;
        }
        unselectType() {
            this.draftObject[this.isSeeks].type = undefined;
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

                //TODO: Check for both and make tags + location independent
                /*
                if(!this.isDetailPresent("tags")){
                    this.draftObject.is.tags = undefined;
                }
                if(!this.isDetailPresent("location")){
                    this.draftObject.is.location = undefined;
                }
                
                if(!this.isDetailPresent("tags")){
                    this.draftObject[this.isSeeks].tags = undefined;
                }
                if(!this.isDetailPresent("location")){
                    this.draftObject[this.isSeeks].location = undefined;
                }*/

                this.needs__create(
                    //this.draft,
                    this.draftObject,
                		this.$ngRedux.getState().getIn(['config', 'defaultNodeUri'])
                );
            }
        }

        setDraft(updatedDraft) {
            if(updatedDraft && updatedDraft.tags && updatedDraft.tags.length > 0 && !this.isDetailPresent("tags")){
                this.addDetail("tags");
            }
            this.tempTags[this.isSeeks] = updatedDraft.tags;
            updatedDraft.tags = this.mergeTags();
            Object.assign(this.draftObject[this.isSeeks], updatedDraft);
        }

        mergeTags() {
            let detailTags = Immutable.Set(this.tagsString[this.isSeeks]? this.tagsString[this.isSeeks].match(/#(\S+)/gi) : []).map(tag => tag.substr(1)).toJS();

            let combinedTags = this.tempTags[this.isSeeks]? detailTags.concat(this.tempTags[this.isSeeks]) : detailTags;

            const immutableTagSet = Immutable.Set(combinedTags);
            return immutableTagSet.toJS();
        }

        addTags() {
            this.draftObject[this.isSeeks].tags = this.mergeTags();
        }

        locationIsSaved() {
            return this.isDetailPresent("location") && this.draftObject[this.isSeek].location && this.draftObject[this.isSeeks].location.name;
        }

        pickImage(image) {
            this.draftObject[this.isSeeks].thumbnail = image;
        }

        toggleDetail(){
            this.showDetail[this.isSeeks] = !this.showDetail[this.isSeeks];
        }

        addDetail(detail) {
            this.details[this.isSeeks].push(detail);
        }

        removeDetail(detailIndex) {
            var tempDetails = [];
            for(var i=0; i < this.details[this.isSeeks].length; i++){
                if(i!=detailIndex) tempDetails.push(this.details[this.isSeeks][i]);
            }
            this.details[this.isSeeks] = tempDetails;
        }

        isDetailPresent(detail) {
            return this.details[this.isSeeks].indexOf(detail) > -1;
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
                                        whatsAround: true
                                    };

                                    this.existingWhatsAroundNeeds
                                    	.filter( need => need.get("state") == "won:Active" )
                                    	.map(need => this.needs__close(need.get("uri")) );

                                    //TODO: Point to same DataSet instead of double it
                                    this.draftObject.is = whatsAround;
                                    this.draftObject.seeks = whatsAround;
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
        template: template
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
