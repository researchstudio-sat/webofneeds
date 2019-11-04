/**
 * created by ms on 04.11.2019
 */
import React from "react";
import { connect } from "react-redux";
import SwipeableViews from "react-swipeable-views";
import "~/style/_atom-context-layout.scss";

import WonAtomHeader from "./atom-header.jsx";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  return {
    atomUri: ownProps.atomUri,
    onClick: ownProps.onClick,
    actionButtons: ownProps.actionButtons,
    className: ownProps.className,
    enableMouseEvents: false, //ownProps.enableMouseEvents,
    hideTimestamp: ownProps.hideTimestamp,
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
    if (this.props.actionButtons) {
      const show = this.state.show;
      let triggerIcon = (
        <svg className="cl__trigger" onClick={() => this.handleClick(show)}>
          <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
        </svg>
      );
      return (
        <won-atom-context-layout>
          <div className="cl__main">
            <SwipeableViews
              index={show ? 1 : 0}
              enableMouseEvents={this.props.enableMouseEvents}
            >
              <WonAtomHeader
                className={this.props.className}
                atomUri={this.props.atomUri}
                hideTimestamp={this.props.hideTimestamp}
                onClick={this.props.onClick}
              />
              {this.props.actionButtons}
            </SwipeableViews>
          </div>
          {triggerIcon}
        </won-atom-context-layout>
      );
    } else {
      return (
        <WonAtomHeader
          className={this.props.className}
          atomUri={this.props.atomUri}
          hideTimestamp={this.props.hideTimestamp}
          onClick={this.props.onClick}
        />
      );
    }
  }
}

WonAtomContextSwipeableView.propTypes = {
  atomUri: PropTypes.string,
  onClick: PropTypes.func,
  actionButtons: PropTypes.object,
  className: PropTypes.string,
  enableMouseEvents: PropTypes.bool,
  hideTimestamp: PropTypes.bool,
};

export default connect(mapStateToProps)(WonAtomContextSwipeableView);
