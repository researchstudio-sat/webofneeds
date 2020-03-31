import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-group.scss";
import { getIn, getQueryParams } from "../utils";
import { Link, withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const visibleUseCasesByConfig = getIn(state, [
    "config",
    "theme",
    "visibleUseCases",
  ]);

  const { useCaseGroup } = getQueryParams(ownProps.location);
  return {
    visibleUseCasesByConfig,
    useCaseGroup:
      visibleUseCasesByConfig &&
      useCaseUtils.getUseCaseGroupByIdentifier(useCaseGroup),
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
                    if (
                      useCaseUtils.isDisplayableItem(
                        subItem,
                        this.props.visibleUseCasesByConfig
                      )
                    ) {
                      return (
                        <Link
                          key={subItem.identifier + "-" + index}
                          className="ucg__main__usecase clickable"
                          to={location =>
                            this.startFromRoute(location, subItem)
                          }
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
                        </Link>
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

  startFromRoute(location, subItem) {
    const subItemIdentifier = subItem && subItem.identifier;

    if (subItemIdentifier) {
      if (useCaseUtils.isUseCaseGroup(subItem)) {
        return `${location.pathname}?useCaseGroup=${encodeURIComponent(
          subItemIdentifier
        )}`;
      } else {
        return `${location.pathname}?useCase=${encodeURIComponent(
          subItemIdentifier
        )}`;
      }
    } else {
      console.warn("No identifier found for given usecase, ", subItem);
    }
  }
}
WonUsecasePicker.propTypes = {
  location: PropTypes.object,
  visibleUseCasesByConfig: PropTypes.objectOf(PropTypes.string),
  useCaseGroup: PropTypes.object,
};

export default withRouter(connect(mapStateToProps)(WonUsecasePicker));
