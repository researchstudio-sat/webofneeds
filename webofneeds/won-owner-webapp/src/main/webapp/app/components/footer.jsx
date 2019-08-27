import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn } from "../utils.js";

import "~/style/_footer.scss";
import won from "../won-es6";
import Immutable from "immutable";

const mapStateToProps = state => {
  return {
    themeName: getIn(state, ["config", "theme", "name"]),
    appTitle: getIn(state, ["config", "theme", "title"]),
    shouldShowRdf: state.getIn(["view", "showRdf"]),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    toastPush: toastImm => {
      dispatch(actionCreators.toasts__push(toastImm));
    },
    toggleRdf: () => {
      dispatch(actionCreators.view__toggleRdf());
    },
  };
};

class WonFooter extends React.Component {
  render() {
    return (
      <won-footer>
        <div className="footer">
          {/*<!-- TODO: find or create logos that are stylable -->
        <!--<img src="skin/{{self.themeName}}/images/logo.svg" class="footer__logo">
        <div class="footer__appTitle">
            {{ self.appTitle }}
        </div>
        <div class="footer__tagLine">Web of Needs</div>-->*/}
          <div className="footer__linksdesktop hide-in-responsive">
            <a
              className="footer__linksdesktop__link"
              onClick={() =>
                this.props.routerGo("about", { aboutSection: undefined })
              }
            >
              About
            </a>
            <span className="footer__linksdesktop__divider">|</span>
            <a
              className="footer__linksdesktop__link"
              onClick={() =>
                this.props.routerGo("about", {
                  aboutSection: "aboutPrivacyPolicy",
                })
              }
            >
              Privacy
            </a>
            <span className="footer__linksdesktop__divider">|</span>
            <a
              className="footer__linksdesktop__link"
              onClick={() =>
                this.props.routerGo("about", { aboutSection: "aboutFaq" })
              }
            >
              FAQ
            </a>
            <span className="footer__linksdesktop__divider">|</span>
            <a
              className="footer__linksdesktop__link"
              onClick={() =>
                this.props.routerGo("about", {
                  aboutSection: "aboutTermsOfService",
                })
              }
            >
              Terms Of Service
            </a>
            <span className="footer__linksdesktop__divider">|</span>
            <span
              className="footer__linksdesktop__link"
              onClick={() => this.toggleDebugMode()}
            >
              {this.getDebugModeLabel()}
            </span>
            <span className="footer__linksdesktop__divider">|</span>
            <span
              className="footer__linksdesktop__link"
              onClick={() => this.props.toggleRdf()}
            >
              {this.props.shouldShowRdf
                ? "Hide raw RDF data"
                : "Show raw RDF data"}
            </span>
          </div>
          <div className="footer__linksmobile show-in-responsive">
            <a
              className="footer__linksmobile__link"
              onClick={() =>
                this.props.routerGo("about", { aboutSection: undefined })
              }
            >
              About
            </a>
            <a
              className="footer__linksmobile__link"
              onClick={() =>
                this.props.routerGo("about", {
                  aboutSection: "aboutPrivacyPolicy",
                })
              }
            >
              Privacy
            </a>
            <a
              className="footer__linksmobile__link"
              onClick={() =>
                this.props.routerGo("about", { aboutSection: "aboutFaq" })
              }
            >
              FAQ
            </a>
            <a
              className="footer__linksmobile__link"
              onClick={() =>
                this.props.routerGo("about", {
                  aboutSection: "aboutTermsOfService",
                })
              }
            >
              Terms Of Service
            </a>
            <span
              className="footer__linksmobile__link"
              onClick={() => this.toggleDebugMode()}
            >
              {this.getDebugModeLabel()}
            </span>
            <span
              className="footer__linksmobile__link"
              onClick={() => this.props.toggleRdf()}
            >
              {this.props.shouldShowRdf
                ? "Hide raw RDF data"
                : "Show raw RDF data"}
            </span>
          </div>
        </div>
      </won-footer>
    );
  }

  toggleDebugMode() {
    won.debugmode = !won.debugmode;
    const text = won.debugmode ? "Debugmode On" : "Debugmode Off";
    this.props.toastPush(Immutable.fromJS({ text }));
  }

  getDebugModeLabel() {
    return won.debugmode ? "Turn Off Debugmode" : "Turn On Debugmode";
  }
}
WonFooter.propTypes = {
  themeName: PropTypes.string,
  appTitle: PropTypes.string,
  shouldShowRdf: PropTypes.bool,
  routerGo: PropTypes.func,
  toastPush: PropTypes.func,
  toggleRdf: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonFooter);
