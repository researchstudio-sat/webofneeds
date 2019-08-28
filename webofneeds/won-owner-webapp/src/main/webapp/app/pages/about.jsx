/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import WonModalDialog from "../components/modal-dialog.jsx";
import WonToasts from "../components/toasts.jsx";
import WonMenu from "../components/menu.jsx";
import WonSlideIn from "../components/slide-in.jsx";
import WonFooter from "../components/footer.jsx";
import WonTopnav from "../components/topnav.jsx";
import WonHowTo from "../components/howto.jsx";
import WonFlexGrid from "../components/flexgrid.jsx";
import WonAccordion from "../components/accordion.jsx";
import { get, getIn, toAbsoluteURL } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import { ownerBaseUrl } from "~/config/default.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import { h } from "preact";

import "~/style/_about.scss";

const template = (
  <container>
    <won-preact
      className="modalDialog"
      component="self.WonModalDialog"
      props="{}"
      ng-if="self.showModalDialog"
    />
    <won-preact
      className="topnav"
      component="self.WonTopnav"
      props="{pageTitle: 'About'}"
    />
    <won-preact
      className="menu"
      component="self.WonMenu"
      props="{}"
      ng-if="self.isLoggedIn"
    />
    <won-preact className="toasts" component="self.WonToasts" props="{}" />
    <won-preact
      className="slideIn"
      component="self.WonSlideIn"
      props="{}"
      ng-if="self.showSlideIns"
    />
    <main className="about" id="allSections">
      <section className="about__welcome" ng-if="!self.visibleSection">
        <div className="about__welcome__title">What is the Web of Needs?</div>
        <won-preact
          className="about__welcome__grid flexGrid"
          props="{items: self.peopleGrid, className: 'about__welcome__grid'}"
          component="self.WonFlexGrid"
        />
        <div className="about__welcome__description">
          <span className="about__welcome__description__title">
            What is an &laquo;atom&raquo;?
          </span>
          <span className="about__welcome__description__text">
            An atom helps you find people who can help you - or who share your
            interest.
          </span>
          <span
            className="about__welcome__description__more clickable"
            ng-click="::self.toggleMoreInfo()"
          >
            Read more
          </span>

          <svg
            style="--local-primary:var(--won-primary-color);"
            className="about__welcome__description__arrow clickable"
            ng-click="::self.toggleMoreInfo()"
            ng-show="!self.moreInfo"
          >
            <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
          </svg>

          <svg
            style="--local-primary:var(--won-primary-color);"
            className="about__welcome__description__arrow clickable"
            ng-click="::self.toggleMoreInfo()"
            ng-show="self.moreInfo"
          >
            <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
          </svg>

          <span
            className="about__welcome__description__text"
            ng-show="self.moreInfo"
          >
            An atom is much like an automatic classified ad. You say what you
            are looking for, and other such ads will be matched with yours. You
            could think of it as of a long-lived search query that can itself be
            found by others. Once you found a useful match, you can connect to
            it and start to chat.
          </span>
        </div>
      </section>
      <won-preact
        className="about__howto howTo"
        component="self.WonHowTo"
        props="{className: 'about__howto'}"
        ng-if="!self.visibleSection || self.visibleSection === 'aboutHowTo'"
      />
      <section
        className="about__privacyPolicy"
        ng-if="!self.visibleSection || self.visibleSection === 'aboutPrivacyPolicy'"
      >
        <div className="about__privacyPolicy__title">Privacy Policy</div>
        <div
          className="about__privacyPolicy__text"
          ng-include="self.privacyPolicyTemplate"
        />
      </section>
      <section
        className="about__termsOfService"
        ng-if="!self.visibleSection || self.visibleSection === 'aboutTermsOfService'"
      >
        <div className="about__termsOfService__title">Terms Of Service</div>
        <div
          className="about__termsOfService__text"
          ng-include="self.tosTemplate"
        />
      </section>
      <section
        className="about__imprint"
        ng-if="!self.visibleSection || self.visibleSection === 'aboutImprint'"
      >
        <div className="about__imprint__title">Imprint</div>
        <div
          className="about__imprint__text"
          ng-include="self.imprintTemplate"
        />
      </section>
      <section
        className="about__faq"
        ng-if="!self.visibleSection || self.visibleSection === 'aboutFaq'"
      >
        <div className="about__faq__title">FAQs</div>
        <won-preact
          className="about__faq__questions"
          props="{items: self.questions, className: 'about__faq__questions'}"
          component="self.WonAccordion"
        />
      </section>
    </main>
    <won-preact className="footer" component="self.WonFooter" props="{}" />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$state",
  "$scope" /*'$routeParams' /*injections as strings here*/,
  "$element",
];

const peopleGrid = ({ themeName }) => [
  {
    imageSrc: `skin/${themeName}/images/face1.png`,
    text: '"I have something to offer"',
  },
  {
    imageSrc: `skin/${themeName}/images/face2.png`,
    text: '"I want something"',
  },
  {
    imageSrc: `skin/${themeName}/images/face3.png`,
    text: '"I want to do something together"',
  },
  {
    imageSrc: `skin/${themeName}/images/face4.png`,
    text: '"I want to change something"',
  },
];

const questions = [
  {
    title: "Who can view my posts?",
    detail:
      "Anyone can view your posts. (That's the idea!) Post-level access" +
      " control is definitely possible and interesting, but we have not designed that, yet. Contact us if you would" +
      " like to share your thoughts on how to do that.",
  },
  {
    title: "What about my privacy?",
    detail:
      "All the data you provide in an atom description is" +
      " public and unencrypted. All the messages in a conversation are unencrypted but only visible to us (operating" +
      " the" +
      " servers) and to you and your conversation partner.",
  },
  {
    title: "Will the people I talk to see my identity?",
    detail:
      "If everything" +
      " works as intended, no. Each atom has its own cryptographic identity. They will know that the person who" +
      " wrote the posting is the one sending them messages. As long as you do not provide any" +
      " information that identifies you, others will not easily find out who you really are. But please do not think" +
      " that you are fully anonymous here. Even if you are behind a proxy or an anonymization network, there might be" +
      " ways to de-anonymize you based on the data you provide.",
  },
  {
    title:
      "I am offering goods/services on the Internet already. Can people find them here?",
    detail:
      "Yes! The idea is to have an open, decentralized infrastructure that makes it easy for anyone" +
      " to post what they seek or what they offer, and to find people to interact with. " +
      " You'll have to set up a WoN node or post your offerings to a service (like " +
      "[matchat.org](https://www.matchat.org)" +
      ") that handles everything for you. We can help you with that.",
  },
  {
    title: "Is this website production-ready?",
    detail:
      "No. Do not use it for anything important to you. It is a" +
      " demonstrator of a research project and running on modest hardware. We're working on it, though.",
  },
  {
    title: "Is this website secure?",
    detail: "No. See below for more information.",
  },
  {
    title: "Can I post whatever I want here?",
    detail:
      "No. Make sure it is legal, unoffensive, and that it pleases" +
      " us. We will delete any postings we do not like without prior notice, explanation, and there will be no way to" +
      " restore the data.",
  },
  { title: "Are you selling my data?", detail: "No." },
  {
    title: "How is this different from the websites I already know?",
    detail:
      "Two-way matching: The person searching for something will find supply, the person offering something" +
      " will find demands. Genericity: The process (post/match/chat) is quite general and can be applied to a great" +
      " variety of cases. Here, we demonstrate a few, but we ask you to come up with more variations and build" +
      " your own applications on top of them. Openness: " +
      " If you want, you can set up your own website to host your own content using either our software or your" +
      " own implementation of the protocols. " +
      " Someone else can still find your content and interact with it from here because the two websites speak" +
      " the same protocol. You can also set up your own matching service to provide better matches. In both" +
      " cases, We will be glad to help if you want to try!",
  },
  {
    title: "How secure is this system?",
    detail:
      "tl;dr: Probably not very secure at the moment. As a general rule," +
      " try" +
      " not to post or write anything here you would not be happy to write on a postcard with your name on it. (By the" +
      " way, you should handle e-mail the same way.)" +
      " Here is how we" +
      " implemented it - in the bird's eye view: Your data is stored in clear text on our servers. The communication" +
      " between your browser and your counterpart's browser is relayed over at most four intermediate servers: your" +
      " Owner Application (" +
      "[" +
      toAbsoluteURL(ownerBaseUrl) +
      "](" +
      ownerBaseUrl +
      ")" +
      "), your WoN node (" +
      "[" +
      toAbsoluteURL("/won/") +
      "](/won/)" +
      "), your" +
      " counterpart's WoN node and your counterpart's Owner Application. Each one of" +
      " these communication channels are secured with TLS, so the data is encrypted as it is transmitted. Once your" +
      " messages reach your Owner Application, they are signed using asymmetric cryptography so your counterpart can " +
      "verify that the message was written by the same person who created the atom in the first place. As messages are" +
      " forwarded to the next processing node, signatures are added by " +
      "the receiving node. Messages sent later contain the signatures of earlier messages, so it becomes impossible " +
      "to change the content of messages or pretend they were never sent or received." +
      " The Owner Application creates a new key pair for every atom you" +
      " create, so others should not be able to find out which atoms are yours and which atoms are from other" +
      " people. Note that this system has not had an independent security audit. If you are interested in" +
      " assessing the security of our approach, or if you have feedback for us, please contact us.",
  },
  {
    title:
      "Is there a plug-in for [shop, marketplace, social network, forum] software X?",
    detail:
      "No." +
      " If you have an idea for an integration, or if you would like to help with one, let us know.",
  },
  {
    title: "What is going on behind the scenes?",
    detail:
      "You may go [here](http://researchstudio-sat.github.io/webofneeds/) for an explanation.",
  },
  {
    title: "Is there a native mobile app?",
    detail:
      "No. The system is open source though. You can make one if" +
      " you like. Maybe one that talks to your own server? Contact us if you would like support in doing that!",
  },
];

class AboutController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.WonModalDialog = WonModalDialog;
    this.WonToasts = WonToasts;
    this.WonMenu = WonMenu;
    this.WonSlideIn = WonSlideIn;
    this.WonFooter = WonFooter;
    this.WonTopnav = WonTopnav;
    this.WonHowTo = WonHowTo;
    this.WonFlexGrid = WonFlexGrid;
    this.WonAccordion = WonAccordion;

    window.ab4dbg = this;

    const select = state => {
      const visibleSection = generalSelectors.getAboutSectionFromRoute(state);
      const themeName = getIn(state, ["config", "theme", "name"]);
      return {
        isLoggedIn: accountUtils.isLoggedIn(get(state, "account")),
        themeName,
        visibleSection,
        tosTemplate:
          "./skin/" +
          themeName +
          "/" +
          getIn(state, ["config", "theme", "tosTemplate"]),
        imprintTemplate:
          "./skin/" +
          themeName +
          "/" +
          getIn(state, ["config", "theme", "imprintTemplate"]),
        privacyPolicyTemplate:
          "./skin/" +
          themeName +
          "/" +
          getIn(state, ["config", "theme", "privacyPolicyTemplate"]),
        peopleGrid: peopleGrid({ themeName }),
        showModalDialog: state.getIn(["view", "showModalDialog"]),
        showSlideIns:
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);
    this.$scope.$on("$destroy", disconnect);

    this.questions = questions;
    this.peopleGrid = [];
    this.moreInfo = false;
  }

  toggleMoreInfo() {
    this.moreInfo = !this.moreInfo;
  }
}

export default {
  module: angular
    .module("won.owner.components.about", [ngAnimate])
    .controller("AboutController", [...serviceDependencies, AboutController])
    .name,
  controller: "AboutController",
  template: template,
};
