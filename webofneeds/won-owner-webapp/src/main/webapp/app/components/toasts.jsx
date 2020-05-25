import React from "react";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import { get } from "../utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import "~/style/_responsiveness-utils.scss";
import "~/style/_toasts.scss";
import "~/style/_won-markdown.scss";
import ico27_close from "~/images/won-icons/ico27_close.svg";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico16_indicator_info from "~/images/won-icons/ico16_indicator_info.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";

import vocab from "../service/vocab.js";
import ReactMarkdown from "react-markdown";

export default function WonToasts() {
  const dispatch = useDispatch();
  const theme = useSelector(generalSelectors.getTheme);

  const adminEmail = get(theme, "adminEmail");
  const toasts = useSelector(state => get(state, "toasts"));
  const toastsArray = toasts ? toasts.toArray() : [];

  return (
    <won-toasts>
      <div className="topnav__toasts">
        {toastsArray.map((toast, index) => {
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
                    onClick={() =>
                      dispatch(actionCreators.toasts__delete(toast))
                    }
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
                    onClick={() =>
                      dispatch(actionCreators.toasts__delete(toast))
                    }
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
                      <a href={"mailto:" + adminEmail}>{adminEmail}</a>
                    </p>
                  </div>

                  <svg
                    className="topnav__toasts__element__close clickable"
                    onClick={() =>
                      dispatch(actionCreators.toasts__delete(toast))
                    }
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
