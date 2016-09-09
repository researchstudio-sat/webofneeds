;

import angular from 'angular';
import topNavModule from '../topnav';
import overviewTitleBarModule from '../visitor-title-bar';
import compareToModule from '../../directives/compareTo';
import accordionModule from '../accordion';
import flexGridModule from '../flexgrid';
import {
    attach,
    scrollTo,
    delay,
} from '../../utils';
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

const questions = [
    {title: "What are your terms of service?", detail: "Simple: don't use our software. It will probably get you in" +
    " trouble. Go away. You have been warned. Don't blame us!"},
    {title: "Is this website production-ready?", detail :"No. Do not use it for anything important to you."},
    {title: "Is this website secure?", detail :"No. See below for more information."},
    {title: "Can I post whatever I want here?", detail :"No. Make sure it is legal, and it pleases us. However, we" +
    " will probably delete it anyway."},
    {title: "How do I know who I am talking to?", detail: "If everything works as intended, you don't. Each need has" +
    " its own cryptographic identity. It can be linked to other identities but we aren't doing that currently."},
    {title: "So how do you prevent man-in-the-middle attacks?", detail: "We don't."},
    {title: "Are you selling my data?", detail :"We'd love to, but nobody's buying. If you're interested, contact" +
    " us. If you don't want us to sell your data tweet a rant to @webofneeds #wtfwonissellingmydata. And then set up" +
    " your own server."},
    {title: "Do I need to register?", detail: "Currently, you have to register, but we're working on a feature to" +
    " let you use the site without registering. Or, you can run your own server."},
    {title: "Is there a native mobile app?", detail: "No. The system is open source though. You can make one if" +
    " you like. Maybe one that talks to your own server?"},
    {title: "Who can view my posts?", detail: "Anyone can view your posts. (That's the idea!) Post-level access" +
    " control is definitely possible and interesting, but not anything we can do in the near future."},
    {title: "I am offering goods/services on the Internet already. Can people find them here?",
        detail: "Yes! The idea is to have an open, decentralized infrastructure that makes it easy for anyone" +
        " to post what they seek or what they offer, and to find people to interact with. " +
        " You'll have to set up a WoN node or post your offerings to a service (like" +
        " matchat.org) that handles everything for you. We can help you with that."
    },
    {title: "What about my personal data?", detail: "All the data you provide in a need description is" +
" public and unencrypted. All the mesages in a conversation are unencrypted but only visible to us (operating the" +
" servers) and to you and your conversation partner."},
    {title: "How is this different from the websites I already know?",
        detail: "Two-way matching: The person searching for someting will find supply, the person offering something" +
        " will find demands. Genericity: The process (post/match/chat) is very general and can be applied to a great" +
        " variety of cases. Here, we demonstrate a few, but we ask you to come up with more variations and build" +
        " your own applications on top of them. Openness: " +
        " If you want, you can set up your own website to host your own content using either our software or your" +
        " own implementation of the protocols. " +
        " Someone else can still find your content and interact with it from here because the two websites speak" +
        " the same protocol. You can also set up your own matching service to provide better matches. In both" +
        " cases, We will be glad to help if you want to try!"},
    {title: "How secure is this?", detail:"tl;dr: Probably not very secure at the moment. As a general rule, try" +
    " not to post or write anything here you would not be happy to write on a postcard with your name on it." +
    " Here is how we" +
    " implemented it - in the bird's eye view: Your data is stored in clear text on our servers. The communication" +
    " between your browser and your counterpart's browser is relayed over at most four intermediate servers: your" +
    " Owner Application (https://matchat.org/owner/), your WoN node (https://matchat.org/won/), your" +
    " counterpart's WoN node and your counterpart's Owner Application. Each one of" +
    " these communication channels are secured with TLS, so the data is encrypted as it is transmitted. Once your" +
    " messages reach your Owner Application, they are signed using asymmetric cryptography so your counterpart can " +
    "verify that the message was written by the same person who created the need in the first place. As messages are" +
    " forwarded to the next processing node, signatures are added by " +
    "the receiving node. Messages sent later contain the signatures of earlier messages, so it becomes impossible " +
    "to change the content of messages or pretend they were never sent or received." +
    " The Owner Application creates a new key pair for every need you" +
    " create, so others should not be able to find out which needs are yours and which needs are from other" +
    " people. Note that this system has not had an independent security audit. If you are interested in" +
    " assessing the security of our approach, please contact us."},
    {title: "Protocols?", detail:"A protocol is a set of rules that allow systems to communicate."+
    " Unfortunately, we do not have a formal protocol specification handy - they are still being experimented" +
    " with." +
    " Contact us if you wish to collaborate!"}

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

        this.$scope.$watch('self.focusSignup', (oldval, newval) => {
            if (!oldval && newval){
               delay(0).then(() => self.scrollToSignup());
            }
        })

        this.$scope.$on('$viewContentLoaded', function(){
            const focusSignup = self.$ngRedux.getState().getIn(['router', 'currentParams', 'focusSignup']) === "true";
            if(focusSignup){
                delay(0).then(() => self.scrollToSignup());
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

    scrollToSignup(){
        const topPos = document.getElementById('signupSection').offsetTop;
        document.getElementById('allSections').scrollTop = topPos;
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

