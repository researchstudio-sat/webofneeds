import React from "react";

import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get, getIn, toAbsoluteURL } from "../utils.js";
import { ownerBaseUrl } from "~/config/default.js";
import * as wonUtils from "../won-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import "~/style/_atom-share-link.scss";
import ico16_copy_to_clipboard from "~/images/won-icons/ico16_copy_to_clipboard.svg";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";

const mapStateToProps = (state, ownProps) => {
  const atom = ownProps.atomUri && getIn(state, ["atoms", ownProps.atomUri]);

  let linkToPost;
  if (ownerBaseUrl && atom) {
    const path = "#!post/" + `?postUri=${encodeURI(ownProps.atomUri)}`;

    linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
  }
  let svgQrCodeToPost = wonUtils.generateSvgQrCode(linkToPost);

  return {
    atomUri: ownProps.atomUri,
    className: ownProps.className,
    hasConnections: get(atom, "connections")
      ? get(atom, "connections").size > 0
      : false,
    isOwned: generalSelectors.isAtomOwned(state, ownProps.atomUri),
    linkToPost,
    svgQrCodeToPost,
  };
};

class WonAtomShareLink extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showLink: true,
      copied: false,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const labelElement = ((this.props.hasConnections && this.props.isOwned) ||
      !this.props.isOwned) && (
      <p className="asl__text">
        Know someone who might also be interested in this atom? Consider sharing
        the link below in social media.
      </p>
    );

    const linkElement = this.state.showLink ? (
      <div className="asl__link">
        <div className="asl__link__copyfield">
          <input
            ref={linkInput => (this.linkInput = linkInput)}
            className="asl__link__copyfield__input"
            value={this.props.linkToPost}
            readOnly
            type="text"
            onFocus={() => this.selectLink()}
            onBlur={() => this.clearSelection()}
          />
          <button
            className="red won-button--filled asl__link__copyfield__copy-button"
            onClick={() => this.copyLink()}
          >
            <svg className="asl__link__copyfield__copy-button__icon">
              {this.state.copied ? (
                <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
              ) : (
                <use
                  xlinkHref={ico16_copy_to_clipboard}
                  href={ico16_copy_to_clipboard}
                />
              )}
            </svg>
          </button>
        </div>
      </div>
    ) : (
      <div className="asl__qrcode">
        <img
          className="asl__qrcode__code"
          src={"data:image/svg+xml;utf8," + this.props.svgQrCodeToPost}
        />
      </div>
    );

    return (
      <won-atom-share-link
        class={this.props.className ? this.props.className : ""}
      >
        <div className="asl__content">
          {labelElement}
          <div className="asl__tabs">
            <div
              className={
                "asl__tabs__tab clickable " +
                (this.state.showLink ? " asl__tabs__tab--selected " : "")
              }
              onClick={() => this.setState({ showLink: true })}
            >
              Link
            </div>
            <div
              className={
                "asl__tabs__tab clickable " +
                (!this.state.showLink ? " asl__tabs__tab--selected " : "")
              }
              onClick={() => this.setState({ showLink: false })}
            >
              QR-Code
            </div>
          </div>
          {linkElement}
        </div>
      </won-atom-share-link>
    );
  }

  copyLink() {
    const linkEl = this.linkInput;
    if (linkEl) {
      linkEl.focus();
      linkEl.setSelectionRange(0, linkEl.value.length);
      if (!document.execCommand("copy")) {
        window.prompt(
          "Something went wrong while automatically copying the link. Copy to clipboard: Ctrl+C",
          linkEl.value
        );
      } else {
        linkEl.setSelectionRange(0, 0);
        linkEl.blur();

        //Temprorarily show checkmark
        this.setState({ copied: true });
        setTimeout(() => {
          this.setState({ copied: false });
        }, 1000);
      }
    }
  }

  selectLink() {
    const linkEl = this.linkInput;
    if (linkEl) {
      linkEl.setSelectionRange(0, linkEl.value.length);
    }
  }

  clearSelection() {
    const linkEl = this.linkInput;
    if (linkEl) {
      linkEl.setSelectionRange(0, 0);
    }
  }
}
WonAtomShareLink.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  hasConnections: PropTypes.bool,
  isOwned: PropTypes.bool,
  linkToPost: PropTypes.string,
  svgQrCodeToPost: PropTypes.string,
};

export default connect(mapStateToProps)(WonAtomShareLink);
