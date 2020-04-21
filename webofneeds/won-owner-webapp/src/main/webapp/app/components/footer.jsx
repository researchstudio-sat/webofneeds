import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn } from "../utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";

import "~/style/_footer.scss";
import { Link } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  return {
    className: ownProps.className,
    themeName: getIn(state, ["config", "theme", "name"]),
    appTitle: getIn(state, ["config", "theme", "title"]),
    shouldShowRdf: viewSelectors.showRdf(state),
    debugMode: viewSelectors.isDebugModeEnabled(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    toastPush: toastImm => {
      dispatch(actionCreators.toasts__push(toastImm));
    },
    toggleRdf: () => {
      dispatch(actionCreators.view__toggleRdf());
    },
    toggleDebugMode: () => {
      dispatch(actionCreators.view__toggleDebugMode());
    },
  };
};

class WonFooter extends React.Component {
  render() {
    return (
      <won-footer class={this.props.className ? this.props.className : ""}>
        <div className="footer">
          {/*<!-- TODO: find or create logos that are stylable -->
        <!--<img src="skin/{{this.props.themeName}}/images/logo.svg" class="footer__logo">
        <div class="footer__appTitle">
            {{ this.props.appTitle }}
        </div>
        <div class="footer__tagLine">Web of Needs</div>-->*/}
          <div className="footer__linksdesktop hide-in-responsive">
            <Link className="footer__linksdesktop__link" to="/about">
              About
            </Link>
            <span className="footer__linksdesktop__divider">|</span>
            <Link
              className="footer__linksdesktop__link"
              to="/about?aboutSection=aboutPrivacyPolicy"
            >
              Privacy
            </Link>
            <span className="footer__linksdesktop__divider">|</span>
            <Link
              className="footer__linksdesktop__link"
              to="/about?aboutSection=aboutFaq"
            >
              FAQ
            </Link>
            <span className="footer__linksdesktop__divider">|</span>
            <Link
              className="footer__linksdesktop__link"
              to="/about?aboutSection=aboutTermsOfService"
            >
              Terms Of Service
            </Link>
            <span className="footer__linksdesktop__divider">|</span>
            <span
              className="footer__linksdesktop__link"
              onClick={() => this.props.toggleDebugMode()}
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
            <Link className="footer__linksmobile__link" to="/about">
              About
            </Link>
            <Link
              className="footer__linksmobile__link"
              to="/about?aboutSection=aboutPrivacyPolicy"
            >
              Privacy
            </Link>
            <Link
              className="footer__linksmobile__link"
              to="/about?aboutSection=aboutFaq"
            >
              FAQ
            </Link>
            <Link
              className="footer__linksmobile__link"
              to="/about?aboutSection=aboutTermsOfService"
            >
              Terms Of Service
            </Link>
            <span
              className="footer__linksmobile__link"
              onClick={() => this.props.toggleDebugMode()}
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

  getDebugModeLabel() {
    return this.props.debugMode ? "Turn Off Debugmode" : "Turn On Debugmode";
  }
}
WonFooter.propTypes = {
  className: PropTypes.string,
  themeName: PropTypes.string,
  appTitle: PropTypes.string,
  shouldShowRdf: PropTypes.bool,
  debugMode: PropTypes.bool,
  toastPush: PropTypes.func,
  toggleRdf: PropTypes.func,
  toggleDebugMode: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonFooter);
