import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";

import "~/style/_responsiveness-utils.scss";
import "~/style/_toasts.scss";
import "~/style/_won-markdown.scss";
import ico27_close from "~/images/won-icons/ico27_close.svg";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico16_indicator_info from "~/images/won-icons/ico16_indicator_info.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";

import vocab from "../service/vocab.js";
import ReactMarkdown from "react-markdown";

const mapStateToProps = state => {
  return {
    adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
    toastsArray: getIn(state, ["toasts"])
      ? getIn(state, ["toasts"]).toArray()
      : [],
  };
};

const mapDispatchToState = dispatch => {
  return {
    toastDelete: toast => {
      dispatch(actionCreators.toasts__delete(toast));
    },
  };
};

class WonToasts extends React.Component {
  render() {
    return (
      <won-toasts>
        <div className="topnav__toasts">
          {this.props.toastsArray.map((toast, index) => {
            switch (get(toast, "type")) {
              case vocab.WON.warnToast:
                return (
                  <div className="topnav__toasts__element warn" key={index}>
                    <svg className="topnav__toasts__element__icon">
                      <use
                        xlinkHref={ico16_indicator_warning}
                        href={ico16_indicator_warning}
                      />
                    </svg>

                    <div className="topnav__toasts__element__text">
                      <ReactMarkdown
                        className="markdown"
                        source={get(toast, "msg")}
                      />
                    </div>

                    <svg
                      className="topnav__toasts__element__close clickable"
                      onClick={() => this.props.toastDelete(toast)}
                    >
                      <use xlinkHref={ico27_close} href={ico27_close} />
                    </svg>
                  </div>
                );

              case vocab.WON.infoToast:
                return (
                  <div className="topnav__toasts__element info" key={index}>
                    <svg className="topnav__toasts__element__icon">
                      <use
                        xlinkHref={ico16_indicator_info}
                        href={ico16_indicator_info}
                      />
                    </svg>

                    <div className="topnav__toasts__element__text">
                      <ReactMarkdown
                        className="markdown"
                        source={get(toast, "msg")}
                      />
                    </div>

                    <svg
                      className="topnav__toasts__element__close clickable"
                      onClick={() => this.props.toastDelete(toast)}
                    >
                      <use xlinkHref={ico27_close} href={ico27_close} />
                    </svg>
                  </div>
                );

              case vocab.WON.errorToast:
              default:
                return (
                  <div className="topnav__toasts__element error" key={index}>
                    <svg className="topnav__toasts__element__icon">
                      <use
                        xlinkHref={ico16_indicator_error}
                        href={ico16_indicator_error}
                      />
                    </svg>

                    <div className="topnav__toasts__element__text">
                      <ReactMarkdown
                        className="markdown"
                        source={get(toast, "msg")}
                      />
                      <p>
                        If the problem persists please contact
                        <a href={"mailto:" + this.props.adminEmail}>
                          {this.props.adminEmail}
                        </a>
                      </p>
                    </div>

                    <svg
                      className="topnav__toasts__element__close clickable"
                      onClick={() => this.props.toastDelete(toast)}
                    >
                      <use xlinkHref={ico27_close} href={ico27_close} />
                    </svg>
                  </div>
                );
            }
          })}
        </div>
      </won-toasts>
    );
  }
}
WonToasts.propTypes = {
  adminEmail: PropTypes.string,
  toastsArray: PropTypes.arrayOf(PropTypes.object),
  toastDelete: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToState
)(WonToasts);
