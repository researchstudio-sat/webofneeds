import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { ownerBaseUrl } from "~/config/default.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, getIn, toAbsoluteURL } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonFooter from "../../components/footer.jsx";
import WonAccordion from "../../components/accordion.jsx";
import WonHowTo from "../../components/howto.jsx";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFlexGrid from "../../components/flexgrid.jsx";

import "~/style/_about.scss";

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

const mapStateToProps = state => {
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
    showModalDialog: getIn(state, ["view", "showModalDialog"]),
    showSlideIns:
      viewSelectors.hasSlideIns(state) &&
      viewSelectors.isSlideInsVisible(state),
  };
};

class PageAbout extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      moreInfo: false,
    };

    this.toggleMoreInfo = this.toggleMoreInfo.bind(this);
  }

  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        <WonTopnav pageTitle="About" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        <main className="about" id="allSections">
          {!this.props.visibleSection && (
            <section className="about__welcome">
              <div className="about__welcome__title">
                What is the Web of Needs?
              </div>
              <WonFlexGrid
                items={this.props.peopleGrid}
                className="about__welcome__grid"
              />
              <div className="about__welcome__description">
                <span className="about__welcome__description__title">
                  What is an &laquo;atom&raquo;?
                </span>
                <span className="about__welcome__description__text">
                  An atom helps you find people who can help you - or who share
                  your interest.
                </span>
                <span
                  className="about__welcome__description__more clickable"
                  onClick={this.toggleMoreInfo}
                >
                  Read more
                </span>
                <svg
                  className="about__welcome__description__arrow clickable"
                  onClick={this.toggleMoreInfo}
                >
                  {this.state.moreInfo ? (
                    <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                  ) : (
                    <use
                      xlinkHref="#ico16_arrow_down"
                      href="#ico16_arrow_down"
                    />
                  )}
                </svg>

                {this.state.moreInfo && (
                  <span className="about__welcome__description__text">
                    An atom is much like an automatic classified ad. You say
                    what you are looking for, and other such ads will be matched
                    with yours. You could think of it as of a long-lived search
                    query that can itself be found by others. Once you found a
                    useful match, you can connect to it and start to chat.
                  </span>
                )}
              </div>
            </section>
          )}
          {(!this.props.visibleSection ||
            this.props.visibleSection === "aboutHowTo") && (
            <WonHowTo className="about__howto" />
          )}
          {(!this.props.visibleSection ||
            this.props.visibleSection === "aboutPrivacyPolicy") && (
            <section className="about__privacyPolicy">
              <div className="about__privacyPolicy__title">Privacy Policy</div>
              <div
                className="about__privacyPolicy__text"
                dangerouslySetInnerHTML={{
                  __html: this.props.privacyPolicyTemplate,
                }}
              />
            </section>
          )}
          {(!this.props.visibleSection ||
            this.props.visibleSection === "aboutTermsOfService") && (
            <section className="about__termsOfService">
              <div className="about__termsOfService__title">
                Terms Of Service
              </div>
              <div
                className="about__termsOfService__text"
                dangerouslySetInnerHTML={{ __html: this.props.tosTemplate }}
              />
            </section>
          )}
          {(!this.props.visibleSection ||
            this.props.visibleSection === "aboutImprint") && (
            <section className="about__imprint">
              <div className="about__imprint__title">Imprint</div>
              <div
                className="about__imprint__text"
                dangerouslySetInnerHTML={{ __html: this.props.imprintTemplate }}
              />
            </section>
          )}
          {(!this.props.visibleSection ||
            this.props.visibleSection === "aboutFaq") && (
            <section className="about__faq">
              <div className="about__faq__title">FAQs</div>
              <WonAccordion
                items={questions}
                className="about__faq__questions"
              />
            </section>
          )}
        </main>
        <WonFooter />
      </section>
    );
  }

  toggleMoreInfo() {
    this.setState({
      moreInfo: !this.state.moreInfo,
    });
  }
}
PageAbout.propTypes = {
  showModalDialog: PropTypes.bool,
  isLoggedIn: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  visibleSection: PropTypes.string,
  privacyPolicyTemplate: PropTypes.string,
  tosTemplate: PropTypes.string,
  imprintTemplate: PropTypes.string,
  peopleGrid: PropTypes.arrayOf(PropTypes.object),
};

export default connect(mapStateToProps)(PageAbout);
