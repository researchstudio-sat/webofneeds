;

import angular from 'angular';
import topNavModule from '../topnav.js';
import compareToModule from '../../directives/compareTo.js';
import accordionModule from '../accordion.js';
import flexGridModule from '../flexgrid.js';
import {
    attach, 
    getIn,
} from '../../utils.js';
import { actionCreators }  from '../../actions/actions.js';

const goodServicesOffer = "matchat.org";
const goodServicesOfferLink = goodServicesOffer.link("https://www.matchat.org");

const howSecure1 = window.location.protocol+"//"+window.location.host+"/owner/";
const howSecure1Link = howSecure1.link(window.location.protocol+"//"+window.location.host+"/owner/");
const howSecure2 = window.location.protocol+"//"+window.location.host+"/won/";
const howSecure2Link = howSecure2.link(window.location.protocol+"//"+window.location.host+"/won/");

const behindScenes  = "here";
const behindScenesLink = behindScenes.link("http://researchstudio-sat.github.io/webofneeds/");

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

const workGrid = [
        {svgSrc: '#ico36_description', text: 'Post your need anonymously', detail: 'Needs can be very personal, so privacy is important. You don\'t have to reveal your identity here.'},
        {svgSrc: '#ico36_match', text: 'Get matches', detail: 'Based on the' + ' information you provide, we will try to connect you with others'},
        {svgSrc: '#ico36_incoming', text: 'Request contact – or be contacted', detail: 'If you\'re interested,' + ' make a contact request - or get one if your counterpart is faster than you' },
        {svgSrc: '#ico36_message', text: 'Interact and exchange', detail: 'You found someone' + ' who has what you need, wants to meet or change something in your common environment? Go chat with them! '}
    ];

const peopleGrid = ({theme}) => ([
        {imageSrc: `skin/${theme}/images/face1.png`, text: '"I have something to offer"'},
        {imageSrc: `skin/${theme}/images/face2.png`, text: '"I want something"'},
        {imageSrc: `skin/${theme}/images/face3.png`, text: '"I want to do something together"'},
        {imageSrc: `skin/${theme}/images/face4.png`, text: '"I want to change something"'}
    ]);

const questions = [
    {title: "Do I need to register?", detail: "Currently, you have to register, but we're working on a feature to" +
    " let you use the site without registering. You can also run your own server if you don't want to register here."},
    {title: "Is there a native mobile app?", detail: "No. The system is open source though. You can make one if" +
    " you like. Maybe one that talks to your own server? Contact us if you would like support in doing that!"},
    {title: "Who can view my posts?", detail: "Anyone can view your posts. (That's the idea!) Post-level access" +
    " control is definitely possible and interesting, but we have not designed that, yet. Contact us if you would" +
    " like to share your thoughts on how to do that."},
    {title: "What about my privacy?", detail: "All the data you provide in a need description is" +
    " public and unencrypted. All the messages in a conversation are unencrypted but only visible to us (operating" +
    " the" +
    " servers) and to you and your conversation partner."},
    {title: "Will the people I talk to see my identity?", detail: "If everything" +
    " works as intended, no. Each need has its own cryptographic identity. They will know that the person who" +
    " wrote the posting is the one sending them messages. As long as you do not provide any" +
    " information that identifies you, others will not easily find out who you really are. But please do not think" +
    " that you are fully anonymous here. Even if you are behind a proxy or an anonymization network, there might be" +
    " ways to de-anonymize you based on the data you provide."},
    {title: "I am offering goods/services on the Internet already. Can people find them here?",
         detail: "Yes! The idea is to have an open, decentralized infrastructure that makes it easy for anyone" +
        " to post what they seek or what they offer, and to find people to interact with. " +
        " You'll have to set up a WoN node or post your offerings to a service (like "+goodServicesOfferLink+") that handles everything for you. We can help you with that.", unsafeHtmlEnabled: true},
    {title: "Is this website production-ready?", detail :"No. Do not use it for anything important to you. It is a" +
    " demonstrator of a research project and running on modest hardware. We're working on it, though."},
    {title: "Is this website secure?", detail :"No. See below for more information."},
    {title: "Can I post whatever I want here?", detail :"No. Make sure it is legal, unoffensive, and that it pleases" +
    " us. We will delete any postings we do not like without prior notice, explanation, and there will be no way to" +
    " restore the data."},
    {title: "Are you selling my data?", detail :"No."},
    {title: "How is this different from the websites I already know?",
        detail: "Two-way matching: The person searching for something will find supply, the person offering something" +
        " will find demands. Genericity: The process (post/match/chat) is quite general and can be applied to a great" +
        " variety of cases. Here, we demonstrate a few, but we ask you to come up with more variations and build" +
        " your own applications on top of them. Openness: " +
        " If you want, you can set up your own website to host your own content using either our software or your" +
        " own implementation of the protocols. " +
        " Someone else can still find your content and interact with it from here because the two websites speak" +
        " the same protocol. You can also set up your own matching service to provide better matches. In both" +
        " cases, We will be glad to help if you want to try!"},
    {title: "How secure is this system?", detail:"tl;dr: Probably not very secure at the moment. As a general rule," +
    " try" +
    " not to post or write anything here you would not be happy to write on a postcard with your name on it. (By the" +
    " way, you should handle e-mail the same way.)" +
    " Here is how we" +
    " implemented it - in the bird's eye view: Your data is stored in clear text on our servers. The communication" +
    " between your browser and your counterpart's browser is relayed over at most four intermediate servers: your" +
    " Owner Application ("+howSecure1Link+"), your WoN node ("+howSecure2Link+"), your" +
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
    " assessing the security of our approach, or if you have feedback for us, please contact us.", unsafeHtmlEnabled: true},
    {title: "Is there a plug-in for [shop, marketplace, social network, forum] software X?", detail: "No." +
    " If you have an idea for an integration, or if you would like to help with one, let us know."},
    {title: "What is behind the scenes?", detail: "You may go "+behindScenesLink+" for an explanation.", unsafeHtmlEnabled: true}
];

class AboutController {
    constructor(/* arguments <- serviceDependencies */){
        attach(this, serviceDependencies, arguments);

        const self = this;

        window.ab4dbg = this;

        const select = (state) => {
            const theme = getIn(state, ['config','theme', 'name']);
            return { 
                theme,
                imprintTemplate: "./skin/" + theme + "/" + getIn(state, ['config', 'theme', 'imprintTemplate']), 
                peopleGrid: peopleGrid({theme}),
            }
        };
        const disconnect = this.$ngRedux.connect(select, actionCreators)(this);


        this.$scope.$on('$destroy',disconnect);

        this.questions = questions;
        this.peopleGrid = [];
        this.workGrid = workGrid;
        this.moreInfo = false;
    }

    toggleMoreInfo(){
        this.moreInfo = !this.moreInfo;
    }
}

export default angular.module('won.owner.components.about', [
    accordionModule,
    topNavModule,
    flexGridModule,
    compareToModule,
])
    .controller('AboutController', [...serviceDependencies, AboutController])
    .name;
