import React from "react";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";

import "~/style/_accordion.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import ico16_arrow_up from "~/images/won-icons/ico16_arrow_up.svg";

export default class WonAccordion extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selectedIdx: undefined,
    };
  }

  render() {
    return (
      <won-accordion
        className={this.props.className ? this.props.className : ""}
      >
        {this.props.items.map((item, index) => (
          <div
            key={index}
            className="accordion__element clickable"
            onClick={() => this.openElement(index)}
          >
            <div className="header clickable">{item.title}</div>
            {index !== this.state.selectedIdx ? (
              <svg className="arrow clickable">
                <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
              </svg>
            ) : (
              <React.Fragment>
                <svg className="arrow clickable">
                  <use xlinkHref={ico16_arrow_up} href={ico16_arrow_up} />
                </svg>
                <ReactMarkdown
                  className="detail markdown"
                  source={item.detail}
                />
              </React.Fragment>
            )}
          </div>
        ))}
      </won-accordion>
    );
  }

  openElement(index) {
    this.setState({
      selectedIdx: index === this.state.selectedIdx ? undefined : index,
    });
  }
}
WonAccordion.propTypes = {
  items: PropTypes.arrayOf(PropTypes.object).isRequired,
  className: PropTypes.string,
};
