/**
 * created by ms on 04.11.2019
 */
import React from "react";
import { connect } from "react-redux";
import SwipeableViews from "react-swipeable-views";
import "~/style/_atom-context-layout.scss";

import WonAtomHeader from "./atom-header.jsx";
import PropTypes from "prop-types";

import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";
import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";

const mapStateToProps = (state, ownProps) => {
  return {
    atomUri: ownProps.atomUri,
    actionButtons: ownProps.actionButtons ? ownProps.actionButtons : undefined,
    className: ownProps.className,
    enableMouseEvents: false,
    hideTimestamp: ownProps.hideTimestamp ? ownProps.hideTimestamp : false,
  };
};

/*const mapDispatchToProps = dispatch => {
  return {};
};*/

class WonAtomContextSwipeableView extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      show: false,
    };
    this.handleClick = this.handleClick.bind(this);
  }

  handleClick(show) {
    this.setState({
      show: !show,
    });
  }

  render() {
    const headerElement = (
      <WonAtomHeader
        atomUri={this.props.atomUri}
        hideTimestamp={this.props.hideTimestamp}
        toLink={this.props.toLink}
      />
    );

    let buttons = this.props.actionButtons;

    if (this.props.actionButtons) {
      const show = this.state.show;
      buttons = (
        <div onClick={() => this.handleClick(show)}>
          {this.props.actionButtons}
        </div>
      );

      let triggerIcon = (
        <React.Fragment>
          <svg
            className="cl__trigger cl__trigger--waiting clickable"
            onClick={() => this.handleClick(show)}
          >
            <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
          </svg>
          <svg
            className="cl__trigger cl__trigger--add clickable"
            onClick={() => this.handleClick(show)}
          >
            <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
          </svg>
          <svg
            className="cl__trigger cl__trigger--default clickable"
            onClick={() => this.handleClick(show)}
          >
            <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
          </svg>
        </React.Fragment>
      );

      return (
        <won-atom-context-layout class={this.props.className}>
          <div className="cl__main ">
            <SwipeableViews
              index={show ? 1 : 0}
              enableMouseEvents={this.props.enableMouseEvents}
            >
              {headerElement}
              {buttons}
            </SwipeableViews>
          </div>
          {triggerIcon}
        </won-atom-context-layout>
      );
    } else {
      return headerElement;
    }
  }
}

WonAtomContextSwipeableView.propTypes = {
  atomUri: PropTypes.string,
  toLink: PropTypes.string,
  actionButtons: PropTypes.object,
  className: PropTypes.string,
  enableMouseEvents: PropTypes.bool,
  hideTimestamp: PropTypes.bool,
};

export default connect(mapStateToProps)(WonAtomContextSwipeableView);
