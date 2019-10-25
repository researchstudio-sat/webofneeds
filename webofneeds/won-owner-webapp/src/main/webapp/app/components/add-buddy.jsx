import React from "react";

import PropTypes from "prop-types";

import "~/style/_add-buddy.scss";

// TODO: Action Label in desktopView
// TODO: Change Icon maybe PersonIcon with a Plus
// TODO: Immediate Action if only one Persona is owned by the User -> Open Modal Dialog to ask
// TODO: Display Possible Personas and Personas that are already Buddies or have pending requests
export default class WonAddBuddy extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    this.handleClick = this.handleClick.bind(this);
  }

  render() {
    const dropdownElement = this.state.contextMenuOpen && (
      <div className="add-buddy__addbuddymenu">
        <div className="add-buddy__addbuddymenu__content">
          <div className="topline">
            <div
              className="add-buddy__addbuddymenu__header clickable"
              onClick={() => this.setState({ contextMenuOpen: false })}
            >
              <svg className="add-buddy__addbuddymenu__header__icon">
                <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
              </svg>
              <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
                Add as Buddy
              </span>
            </div>
          </div>
          <div className="add-buddy__addbuddymenu__content__selection">
            TODO Buddy Selection
          </div>
        </div>
      </div>
    );

    return (
      <won-add-buddy
        class={this.props.className ? this.props.className : ""}
        ref={node => (this.node = node)}
      >
        <div
          className="add-buddy__addbuddymenu__header clickable"
          onClick={() => this.setState({ contextMenuOpen: true })}
        >
          <svg className="add-buddy__addbuddymenu__header__icon">
            <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
          </svg>
          <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
            Add as Buddy
          </span>
        </div>
        {dropdownElement}
      </won-add-buddy>
    );
  }

  componentWillMount() {
    document.addEventListener("mousedown", this.handleClick, false);
  }
  componentWillUnmount() {
    document.removeEventListener("mousedown", this.handleClick, false);
  }

  handleClick(e) {
    if (!this.node.contains(e.target) && this.state.contextMenuOpen) {
      this.setState({ contextMenuOpen: false });

      return;
    }
  }
}
WonAddBuddy.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
};
