import React from "react";
import PropTypes from "prop-types";
import WonLabelledHr from "./labelled-hr.jsx";

import "~/style/_flexgrid.scss";

export default class WonFlexGrid extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selectedIdx: undefined,
    };
  }

  render() {
    return (
      <won-flex-grid
        class={this.props.className ? this.props.className : undefined}
      >
        {this.props.items.map((item, index) => (
          <div className="flexgrid__item" key={index}>
            <div
              className={
                "fgi__block " + (item.detail !== undefined ? " clickable " : "")
              }
              onClick={() => this.openElement(index)}
            >
              {item.imageSrc !== undefined && (
                <img className="fgi__image" src={item.imageSrc} />
              )}
              {item.svgSrc !== undefined && (
                <svg className="fgi__image">
                  <use href={item.svgSrc} />
                </svg>
              )}
              {item.text2 === undefined &&
                item.separatorText === undefined && (
                  <span className="fgi__text">{item.text}</span>
                )}
              {item.text2 !== undefined &&
                item.separatorText !== undefined && (
                  <span className="fgi__text">
                    {item.text}
                    <WonLabelledHr label={item.separatorText} />
                    {item.text2}
                  </span>
                )}
              {item.detail !== undefined &&
                index === this.state.selectedIdx && (
                  <svg className="fgi__arrow">
                    <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                  </svg>
                )}
              {item.detail !== undefined &&
                index !== this.state.selectedIdx && (
                  <svg className="fgi__arrow">
                    <use
                      xlinkHref="#ico16_arrow_down"
                      href="#ico16_arrow_down"
                    />
                  </svg>
                )}
            </div>
            {item.detail !== undefined &&
              index === this.state.selectedIdx && (
                <span className="fgi__additionaltext">{item.detail}</span>
              )}
          </div>
        ))}
      </won-flex-grid>
    );
  }

  openElement(index) {
    this.setState({
      selectedIdx: index === this.state.selectedIdx ? undefined : index,
    });
  }
}
WonFlexGrid.propTypes = {
  items: PropTypes.arrayOf(PropTypes.object).isRequired,
  className: PropTypes.string,
};
