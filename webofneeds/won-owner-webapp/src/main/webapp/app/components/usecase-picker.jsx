import React from "react";
import PropTypes from "prop-types";
import { getIn } from "../utils.js";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-picker.scss";

const mapStateToProps = state => {
  const visibleUseCasesByConfig = getIn(state, [
    "config",
    "theme",
    "visibleUseCases",
  ]);

  const showGroupsThreshold = 1;
  const customUseCase = useCaseUtils.getCustomUseCase();
  const useCaseGroups = useCaseUtils.getUseCaseGroups();
  const ungroupedUseCases = useCaseUtils.getUnGroupedUseCases(
    showGroupsThreshold,
    visibleUseCasesByConfig
  );

  return {
    customUseCase,
    showGroupsThreshold,
    useCaseGroups,
    ungroupedUseCases,
    visibleUseCasesByConfig,
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
                <div
                  key={useCase.identifier + "-" + index}
                  className="ucp__main__searchresult clickable"
                  onClick={() => this.startFrom(useCase)}
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
                </div>
              ))
            ) : (
              <React.Fragment>
                <div className="ucp__main__noresults">
                  {"No Results found for '" + this.state.searchText + "'."}
                </div>
                {!!this.props.customUseCase && (
                  <div
                    className="ucp__main__newcustom clickable"
                    onClick={() => this.startFrom(this.props.customUseCase)}
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
                  </div>
                )}
              </React.Fragment>
            )
          ) : (
            <React.Fragment>
              {/*<!-- USE CASE GROUPS -->*/}
              {!!this.props.useCaseGroups &&
                Object.values(this.props.useCaseGroups).map((ucg, index) => {
                  if (
                    useCaseUtils.isDisplayableUseCaseGroup(ucg) &&
                    useCaseUtils.countDisplayableItemsInGroup(ucg) >
                      this.props.showGroupsThreshold
                  ) {
                    return (
                      <div
                        key={ucg.identifier + "-" + index}
                        className="ucp__main__usecase-group clickable"
                        onClick={() => this.viewUseCaseGroup(ucg)}
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
                      </div>
                    );
                  }
                })}
              {/*<!-- USE CASES WITHOUT GROUPS -->*/}
              {!!this.props.ungroupedUseCases &&
                Object.values(this.props.ungroupedUseCases).map(
                  (useCase, index) => {
                    if (useCaseUtils.isDisplayableUseCase(useCase)) {
                      return (
                        <div
                          key={useCase.identifier + "-" + index}
                          className="ucp__main__usecase-group clickable"
                          onClick={() => this.startFrom(useCase)}
                        >
                          {!!useCase.icon && (
                            <svg className="ucp__main__usecase-group__icon">
                              <use
                                xlinkHref={useCase.icon}
                                href={useCase.icon}
                              />
                            </svg>
                          )}
                          {!!useCase.label && (
                            <div className="ucp__main__usecase-group__label">
                              {useCase.label}
                            </div>
                          )}
                        </div>
                      );
                    }
                  }
                )}
            </React.Fragment>
          )}
        </div>
      </won-usecase-picker>
    );
  }

  startFrom(selectedUseCase) {
    const selectedUseCaseIdentifier =
      selectedUseCase && selectedUseCase.identifier;

    if (selectedUseCaseIdentifier) {
      this.props.routerGoCurrent({
        useCase: encodeURIComponent(selectedUseCaseIdentifier),
      });
    } else {
      console.warn(
        "No usecase identifier found for given usecase, ",
        selectedUseCase
      );
    }
  }

  viewUseCaseGroup(selectedUseCaseGroup) {
    const selectedGroupIdentifier =
      selectedUseCaseGroup && selectedUseCaseGroup.identifier;

    if (selectedGroupIdentifier) {
      this.props.routerGoCurrent({
        useCaseGroup: encodeURIComponent(selectedGroupIdentifier),
      });
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
          const searchResults = useCaseUtils.filterUseCasesBySearchQuery(value);

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
  routerGoCurrent: PropTypes.func,
  customUseCase: PropTypes.object,
  showGroupsThreshold: PropTypes.number,
  useCaseGroups: PropTypes.arrayOf(PropTypes.object),
  ungroupedUseCases: PropTypes.arrayOf(PropTypes.object),
  visibleUseCasesByConfig: PropTypes.arrayOf(PropTypes.string),
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonUsecasePicker);
