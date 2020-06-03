import React from "react";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../actions/actions.js";
// import { get } from "../utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
// import * as generalSelectors from "../redux/selectors/general-selectors.js";

import "~/style/_footer.scss";
import { Link } from "react-router-dom";

export default function WonFooter({ className }) {
  const dispatch = useDispatch();
  // const theme = useSelector(generalSelectors.getTheme);
  // const themeName = get(theme, "name");
  // const appTitle = get(theme, "title");
  const shouldShowRdf = useSelector(viewSelectors.showRdf);
  const debugMode = useSelector(viewSelectors.isDebugModeEnabled);

  function getDebugModeLabel() {
    return debugMode ? "Turn Off Debugmode" : "Turn On Debugmode";
  }

  return (
    <won-footer class={className ? className : ""}>
      <div className="footer">
        {/*<!-- TODO: find or create logos that are stylable -->
        <!--<img src="skin/{{themeName}}/images/logo.svg" class="footer__logo">
        <div class="footer__appTitle">
            {{ appTitle }}
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
            onClick={() => dispatch(actionCreators.view__toggleDebugMode())}
          >
            {getDebugModeLabel()}
          </span>
          <span className="footer__linksdesktop__divider">|</span>
          <span
            className="footer__linksdesktop__link"
            onClick={() => dispatch(actionCreators.view__toggleRdf())}
          >
            {shouldShowRdf ? "Hide raw RDF data" : "Show raw RDF data"}
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
            onClick={() => dispatch(actionCreators.view__toggleDebugMode())}
          >
            {getDebugModeLabel()}
          </span>
          <span
            className="footer__linksmobile__link"
            onClick={() => dispatch(actionCreators.view__toggleRdf())}
          >
            {shouldShowRdf ? "Hide raw RDF data" : "Show raw RDF data"}
          </span>
        </div>
      </div>
    </won-footer>
  );
}
WonFooter.propTypes = {
  className: PropTypes.string,
};
