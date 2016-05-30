;

import angular from 'angular';
import topNavModule from '../topnav';
import overviewTitleBarModule from '../visitor-title-bar';
import compareToModule from '../../directives/compareTo';
import accordionModule from '../accordion';
import flexGridModule from '../flexgrid';
import { attach, scrollTo } from '../../utils';
import { actionCreators }  from '../../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

const workGrid = [{imageSrc: 'generated/icon-sprite.svg#ico36_description', text: 'Post your need anonymously', detail: 'Needs can be very personal, so privacy is important. You don\'t have to reveal your identity here.'},
    {imageSrc: 'generated/icon-sprite.svg#ico36_match', text: 'Get matches', detail: 'Based on the' +
    ' information you provide, we will try to connect you with others'},
    {imageSrc: 'generated/icon-sprite.svg#ico36_incoming', text: 'Request contact', detail: 'If you\'re interested,' +
    ' make a contact request - or get one if your counterpart is faster than you', separatorText: 'or', text2: 'Be' +
    ' contacted'},
    {imageSrc: 'generated/icon-sprite.svg#ico36_message', text: 'Interact and exchange', detail: 'You found someone' +
    ' who has what you need, wants to meet or change something in your common environment? Go chat with them! '}];

const peopleGrid = [{imageSrc: 'images/face1.png', text: '"I have something to offer"'},
    {imageSrc: 'images/face2.png', text: '"I want to have something"'},
    {imageSrc: 'images/face3.png', text: '"I want to do something together"'},
    {imageSrc: 'images/face4.png', text: '"I want to change something"'}];

const questions = [{title: "What about my personal data?", detail: "All the data you provide in a need description is" +
" public and unencrypted. All the mesages in a conversation are unencrypted but only visible to us (operating the" +
" servers) and to you and your conversation partner."},
    {title: "Who can view my posts?", detail: "Anyone can view your posts."},
    {title: "Do I need to register?", detail: "You can use the site without creating an account, by accessing your" +
    " need via a secret link that only you have. However, you can only manage one need at a time with that link"}
];

class LandingpageController {
    constructor(/* arguments <- serviceDependencies */){
        attach(this, serviceDependencies, arguments);
        const self = this;

        const signup = (state) => ({
            focusSignup: state.getIn(['router', 'currentParams', 'focusSignup']) === "true",
            loggedIn: state.getIn(['user','loggedIn']),
            registerError: state.getIn(['user','registerError'])
        });

        const disconnect = this.$ngRedux.connect(signup, actionCreators)(this);

        this.$scope.$on('$destroy',disconnect);
        this.$scope.$on('$viewContentLoaded', function(){
            const focusSignup = self.$ngRedux.getState().getIn(['router', 'currentParams', 'focusSignup']) === "true";
            if(focusSignup){
                scrollTo("signup");
                angular.element('input#registerEmail').trigger('focus');
            }
        });

        this.questions = questions;
        this.peopleGrid = peopleGrid;
        this.workGrid = workGrid;
        this.moreInfo = false;
    }

    toggleMoreInfo(){
        this.moreInfo = !this.moreInfo;
    }
}

export default angular.module('won.owner.components.landingpage', [
    overviewTitleBarModule,
    accordionModule,
    topNavModule,
    flexGridModule,
    compareToModule
])
    .controller('LandingpageController', [...serviceDependencies, LandingpageController])
    .name;

