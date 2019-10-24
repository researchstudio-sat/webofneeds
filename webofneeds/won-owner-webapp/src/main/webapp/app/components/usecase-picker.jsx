import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-picker.scss";

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
    this.showGroupsThreshold = 1;
    this.customUseCase = useCaseUtils.getCustomUseCase();
    this.useCaseGroups = useCaseUtils.getUseCaseGroups();
    this.ungroupedUseCases = useCaseUtils.getUnGroupedUseCases(
      this.showGroupsThreshold
    );
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
                {!!this.customUseCase && (
                  <div
                    className="ucp__main__newcustom clickable"
                    onClick={() => this.startFrom(this.customUseCase)}
                  >
                    {!!this.customUseCase.icon && (
                      <svg className="ucp__main__newcustom__icon">
                        <use
                          xlinkHref={this.customUseCase.icon}
                          href={this.customUseCase.icon}
                        />
                      </svg>
                    )}
                    {!!this.customUseCase.label && (
                      <div className="ucp__main__newcustom__label">
                        {this.customUseCase.label}
                      </div>
                    )}
                  </div>
                )}
              </React.Fragment>
            )
          ) : (
            <React.Fragment>
              {/*<!-- USE CASE GROUPS -->*/}
              {!!this.useCaseGroups &&
                Object.values(this.useCaseGroups).map((ucg, index) => {
                  if (
                    useCaseUtils.isDisplayableUseCaseGroup(ucg) &&
                    useCaseUtils.countDisplayableItemsInGroup(ucg) >
                      this.showGroupsThreshold
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
              {!!this.ungroupedUseCases &&
                Object.values(this.ungroupedUseCases).map((useCase, index) => {
                  if (useCaseUtils.isDisplayableUseCase(useCase)) {
                    return (
                      <div
                        key={useCase.identifier + "-" + index}
                        className="ucp__main__usecase-group clickable"
                        onClick={() => this.startFrom(useCase)}
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
                      </div>
                    );
                  }
                })}
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
};

export default connect(
  undefined,
  mapDispatchToProps
)(WonUsecasePicker);
