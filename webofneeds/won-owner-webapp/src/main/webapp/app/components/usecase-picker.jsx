import React from "react";
import PropTypes from "prop-types";
import { getIn } from "../utils.js";
import { connect } from "react-redux";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-picker.scss";
import { Link, withRouter } from "react-router-dom";

const mapStateToProps = state => {
  const visibleUseCasesByConfig = getIn(state, [
    "config",
    "theme",
    "visibleUseCases",
  ]);

  const showGroupsThreshold = 1;
  const customUseCase = useCaseUtils.getCustomUseCase();

  const visibleUseCaseGroups =
    visibleUseCasesByConfig &&
    useCaseUtils.getVisibleUseCaseGroups(
      showGroupsThreshold,
      visibleUseCasesByConfig
    );
  const ungroupedUseCases =
    visibleUseCasesByConfig &&
    useCaseUtils.getUnGroupedUseCases(
      showGroupsThreshold,
      visibleUseCasesByConfig
    );

  return {
    customUseCase,
    visibleUseCaseGroups,
    ungroupedUseCases,
    visibleUseCasesByConfig,
  };
};

class WonUsecasePicker extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      searchText: "",
      searchResults: [],
    };

    this.updateSearch = this.updateSearch.bind(this);
  }

  render() {
    return (
      <won-usecase-picker>
        {/*}<!-- HEADER -->*/}
        <div className="ucp__header">
          <span className="ucp__header__title">Pick one!</span>
        </div>

        <div className="ucp__main">
          {/*<!-- SEARCH FIELD -->*/}
          <div className="ucp__main__search">
            <WonTitlePicker
              onUpdate={this.updateSearch.bind(this)}
              initialValue={this.state.searchText}
              detail={{ placeholder: "Search for Usecases" }}
            />
          </div>
          {/*<!-- SEARCH RESULTS -->*/}
          {this.state.searchText &&
          this.state.searchText.trim &&
          this.state.searchText.trim().length > 1 ? (
            this.state.searchResults.length > 0 ? (
              this.state.searchResults.map((useCase, index) => (
                <Link
                  key={useCase.identifier + "-" + index}
                  className="ucp__main__searchresult clickable"
                  to={location => this.startFromRoute(location, useCase)}
                >
                  {!!useCase.icon && (
                    <svg className="ucp__main__searchresult__icon">
                      <use xlinkHref={useCase.icon} href={useCase.icon} />
                    </svg>
                  )}
                  {!!useCase.label && (
                    <div className="ucp__main__searchresult__label">
                      {useCase.label}
                    </div>
                  )}
                </Link>
              ))
            ) : (
              <React.Fragment>
                <div className="ucp__main__noresults">
                  {"No Results found for '" + this.state.searchText + "'."}
                </div>
                {!!this.props.customUseCase && (
                  <Link
                    className="ucp__main__newcustom clickable"
                    to={location =>
                      this.startFromRoute(location, this.props.customUseCase)
                    }
                  >
                    {!!this.props.customUseCase.icon && (
                      <svg className="ucp__main__newcustom__icon">
                        <use
                          xlinkHref={this.props.customUseCase.icon}
                          href={this.props.customUseCase.icon}
                        />
                      </svg>
                    )}
                    {!!this.props.customUseCase.label && (
                      <div className="ucp__main__newcustom__label">
                        {this.props.customUseCase.label}
                      </div>
                    )}
                  </Link>
                )}
              </React.Fragment>
            )
          ) : (
            <React.Fragment>
              {/*<!-- USE CASE GROUPS -->*/}
              {!!this.props.visibleUseCaseGroups &&
                Object.values(this.props.visibleUseCaseGroups).map(
                  (ucg, index) => (
                    <Link
                      key={ucg.identifier + "-" + index}
                      className="ucp__main__usecase-group clickable"
                      to={location => this.viewUseCaseGroupRoute(location, ucg)}
                    >
                      {!!ucg.icon && (
                        <svg className="ucp__main__usecase-group__icon">
                          <use xlinkHref={ucg.icon} href={ucg.icon} />
                        </svg>
                      )}
                      {!!ucg.label && (
                        <div className="ucp__main__usecase-group__label">
                          {ucg.label}
                        </div>
                      )}
                    </Link>
                  )
                )}
              {/*<!-- USE CASES WITHOUT GROUPS -->*/}
              {!!this.props.ungroupedUseCases &&
                Object.values(this.props.ungroupedUseCases).map(
                  (useCase, index) => (
                    <Link
                      key={useCase.identifier + "-" + index}
                      className="ucp__main__usecase-group clickable"
                      to={location => this.startFromRoute(location, useCase)}
                    >
                      {!!useCase.icon && (
                        <svg className="ucp__main__usecase-group__icon">
                          <use xlinkHref={useCase.icon} href={useCase.icon} />
                        </svg>
                      )}
                      {!!useCase.label && (
                        <div className="ucp__main__usecase-group__label">
                          {useCase.label}
                        </div>
                      )}
                    </Link>
                  )
                )}
            </React.Fragment>
          )}
        </div>
      </won-usecase-picker>
    );
  }

  startFromRoute(location, selectedUseCase) {
    const selectedUseCaseIdentifier =
      selectedUseCase && selectedUseCase.identifier;

    if (selectedUseCaseIdentifier) {
      return `${location.pathname}?useCase=${encodeURIComponent(
        selectedUseCaseIdentifier
      )}`;
    } else {
      console.warn("No identifier found for given usecase, ", selectedUseCase);
    }
  }

  viewUseCaseGroupRoute(location, selectedUseCaseGroup) {
    const selectedGroupIdentifier =
      selectedUseCaseGroup && selectedUseCaseGroup.identifier;

    if (selectedGroupIdentifier) {
      return `${location.pathname}?useCaseGroup=${encodeURIComponent(
        selectedGroupIdentifier
      )}`;
    } else {
      console.warn(
        "No identifier found for given usecase group, ",
        selectedUseCaseGroup
      );
    }
  }

  updateSearch({ value }) {
    this.setState(
      {
        searchText: value,
      },
      () => {
        if (value && value.trim().length > 1) {
          const searchResults = useCaseUtils.filterUseCasesBySearchQuery(
            value,
            this.props.visibleUseCasesByConfig
          );

          const sortByLabelAsc = (a, b) => {
            const bValue = b && b.label && b.label.toLowerCase();
            const aValue = a && a.label && a.label.toLowerCase();

            if (aValue < bValue) return -1;
            if (aValue > bValue) return 1;
            return 0;
          };

          this.setState({
            searchResults: searchResults
              ? searchResults.sort(sortByLabelAsc)
              : [],
          });
        } else {
          this.setState({
            searchResults: [],
          });
        }
      }
    );
  }
}
WonUsecasePicker.propTypes = {
  location: PropTypes.object,
  customUseCase: PropTypes.object,
  visibleUseCaseGroups: PropTypes.objectOf(PropTypes.object),
  visibleUseCasesByConfig: PropTypes.objectOf(PropTypes.object),
  ungroupedUseCases: PropTypes.objectOf(PropTypes.object),
};

export default withRouter(connect(mapStateToProps)(WonUsecasePicker));
