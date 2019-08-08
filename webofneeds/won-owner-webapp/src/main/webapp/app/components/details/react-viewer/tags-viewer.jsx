import React from "react";

import "~/style/_tags-viewer.scss";
import PropTypes from "prop-types";

export default class WonTagsViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="tv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="tv__header__label">{this.props.detail.label}</span>
    );

    const tagsArray = this.props.content && this.props.content.toArray();

    const tags =
      tagsArray &&
      tagsArray.map((tag, index) => {
        return (
          <div key={index} className="tv__content__tag">
            {tag}
          </div>
        );
      });

    return (
      <won-tags-viewer>
        <div className="tv__header">
          {icon}
          {label}
        </div>
        <div className="tv__content">{tags}</div>
      </won-tags-viewer>
    );
  }
}
WonTagsViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
