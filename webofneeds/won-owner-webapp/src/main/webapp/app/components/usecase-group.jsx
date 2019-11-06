import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import * as useCaseUtils from "../usecase-utils.js";
import { getUseCaseGroupFromRoute } from "../redux/selectors/general-selectors.js";

import "~/style/_usecase-group.scss";
import { getIn } from "../utils";

const mapStateToProps = state => {
  const visibleUseCasesByConfig = getIn(state, [
    "config",
    "theme",
    "visibleUseCases",
  ]);

  const selectedGroup = getUseCaseGroupFromRoute(state);
  return {
    visibleUseCasesByConfig,
    useCaseGroup: useCaseUtils.getUseCaseGroupByIdentifier(selectedGroup),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
    },
  };
};

class WonUsecasePicker extends React.Component {
  render() {
    return (
      <won-usecase-group>
        {this.props.useCaseGroup ? (
          <React.Fragment>
            <div className="ucg__header">
              {this.props.useCaseGroup.icon && (
                <svg className="ucg__header__icon">
                  <use
                    xlinkHref={this.props.useCaseGroup.icon}
                    href={this.props.useCaseGroup.icon}
                  />
                </svg>
              )}
              {this.props.useCaseGroup.label && (
                <div className="ucg__header__title">
                  {this.props.useCaseGroup.label}
                </div>
              )}
            </div>
            <div className="ucg__main">
              {!!this.props.useCaseGroup.subItems &&
                Object.values(this.props.useCaseGroup.subItems).map(
                  (subItem, index) => {
                    if (useCaseUtils.isDisplayableItem(subItem)) {
                      return (
                        <div
                          key={subItem.identifier + "-" + index}
                          className="ucg__main__usecase clickable"
                          onClick={() => this.startFrom(subItem)}
                        >
                          {!!subItem.icon && (
                            <svg className="ucg__main__usecase__icon">
                              <use
                                xlinkHref={subItem.icon}
                                href={subItem.icon}
                              />
                            </svg>
                          )}
                          {!!subItem.label && (
                            <div className="ucg__main__usecase__label">
                              {subItem.label}
                            </div>
                          )}
                        </div>
                      );
                    }
                  }
                )}
            </div>
          </React.Fragment>
        ) : (
          <div className="ucg__header">
            <div className="ucg__header__title">Group not found</div>
          </div>
        )}
      </won-usecase-group>
    );
  }

  startFrom(subItem) {
    const subItemIdentifier = subItem && subItem.identifier;

    if (subItemIdentifier) {
      if (useCaseUtils.isUseCaseGroup(subItem)) {
        this.props.routerGoCurrent({
          useCaseGroup: encodeURIComponent(subItemIdentifier),
        });
      } else {
        this.props.routerGoCurrent({
          useCase: encodeURIComponent(subItemIdentifier),
        });
      }
    } else {
      console.warn("No identifier found for given usecase, ", subItem);
    }
  }
}
WonUsecasePicker.propTypes = {
  visibleUseCasesByConfig: PropTypes.arrayOf(PropTypes.string),
  useCaseGroup: PropTypes.object,
  routerGoCurrent: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonUsecasePicker);
