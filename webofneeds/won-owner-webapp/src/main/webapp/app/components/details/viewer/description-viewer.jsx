import React from "react";

import "~/style/_description-viewer.scss";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";

export default class WonDescriptionViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="dv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="dv__header__label">{this.props.detail.label}</span>
    );

    return (
      <won-description-viewer class={this.props.className}>
        <div className="dv__header">
          {icon}
          {label}
        </div>
        <ReactMarkdown
          className="markdown"
          source={this.props.content}
          linkTarget="_blank"
        />
      </won-description-viewer>
    );
  }
}
WonDescriptionViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
