/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn, sortBy } from "../../../utils.js";
import { actionCreators } from "../../../actions/actions.js";
import { connect } from "react-redux";

import * as generalSelectors from "../../../redux/selectors/general-selectors.js";

import "~/style/_suggest-atom-picker.scss";
import Immutable from "immutable";
import WonAtomHeader from "../../atom-header.jsx";
import WonLabelledHr from "../../labelled-hr.jsx";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  const hasAtLeastOneAllowedSocket = (atom, allowedSockets) => {
    if (allowedSockets) {
      const allowedSocketsImm = Immutable.fromJS(allowedSockets);
      const atomSocketsImm = getIn(atom, ["content", "sockets"]);

      return (
        atomSocketsImm &&
        atomSocketsImm.find(socket => allowedSocketsImm.contains(socket))
      );
    }
    return true;
  };

  const isExcludedAtom = (atom, excludedUris) => {
    if (excludedUris) {
      const excludedUrisImm = Immutable.fromJS(excludedUris);

      return excludedUrisImm.contains(get(atom, "uri"));
    }
    return false;
  };

  const suggestedAtomUri = ownProps.initialValue;
  const allActiveAtoms = generalSelectors.getActiveAtoms(state);

  const allSuggestableAtoms =
    allActiveAtoms &&
    allActiveAtoms.filter(
      atom =>
        !isExcludedAtom(atom, ownProps.excludedUris) &&
        hasAtLeastOneAllowedSocket(atom, ownProps.allowedSockets)
    );

  const allForbiddenAtoms =
    allActiveAtoms &&
    allActiveAtoms.filter(
      atom =>
        !(
          !isExcludedAtom(atom, ownProps.excludedUris) &&
          hasAtLeastOneAllowedSocket(atom, ownProps.allowedSockets)
        )
    );

  const suggestedAtom = get(allSuggestableAtoms, suggestedAtomUri);
  const sortedActiveAtomsArray =
    allSuggestableAtoms &&
    sortBy(allSuggestableAtoms, elem =>
      (elem.get("humanReadable") || "").toLowerCase()
    );

  const uriToFetchProcess = getIn(state, [
    "process",
    "atoms",
    this.state.uriToFetch,
  ]);
  const uriToFetchLoading = !!get(uriToFetchProcess, "loading");
  const uriToFetchFailedToLoad = !!get(uriToFetchProcess, "failedToLoad");
  const uriToFetchIsNotAllowed =
    !!get(allForbiddenAtoms, this.state.uriToFetch) &&
    !hasAtLeastOneAllowedSocket(
      get(allForbiddenAtoms, this.state.uriToFetch),
      ownProps.allowedSockets
    );
  const uriToFetchIsExcluded = isExcludedAtom(
    get(allForbiddenAtoms, this.state.uriToFetch),
    ownProps.excludedUris
  );

  return {
    initialValue: ownProps.initialValue,
    detail: ownProps.detail,
    excludedText: ownProps.excludedText,
    notAllowedSocketText: ownProps.notAllowedSocketText,
    onUpdate: ownProps.onUpdate,
    uriToFetchLoading,
    uriToFetchFailedToLoad,
    uriToFetchIsExcluded,
    uriToFetchIsNotAllowed,
    allSuggestableAtoms,
    allForbiddenAtoms,
    suggestionsAvailable: allSuggestableAtoms && allSuggestableAtoms.size > 0,
    sortedActiveAtomsArray,
    suggestedAtom,
    noSuggestionsLabel:
      ownProps.noSuggestionsText || "No Atoms available to suggest",
    uriToFetchSuccess:
      this.state.uriToFetch &&
      !uriToFetchLoading &&
      !uriToFetchFailedToLoad &&
      get(allSuggestableAtoms, this.state.uriToFetch),
    uriToFetchFailed:
      this.state.uriToFetch &&
      !uriToFetchLoading &&
      (uriToFetchFailedToLoad ||
        uriToFetchIsExcluded ||
        uriToFetchIsNotAllowed),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: uri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(uri));
    },
  };
};

class WonSuggestAtomPicker extends React.Component {
  //TODO: CHANGE AND UPDATE LISTENERS DONT WORK
  //TODO: IMPLEMENT ->
  /*
    this.$scope.$watch(
        () => this.state.uriToFetchSuccess,
        () =>
          delay(0).then(() => {
            if (this.state.uriToFetchSuccess) {
              this.update(this.uriToFetch);
              this.resetAtomUriField();
            }
          })
      );
   */
  constructor(props) {
    super(props);
    this.state = {
      showFetchButton: false,
      showResetButton: false,
      uriToFetch: undefined,
    };
  }

  render() {
    let suggestions;

    if (this.props.suggestionsAvailable) {
      const suggestionItems = this.props.sortedActiveAtomsArray.map(atom => {
        return (
          <div
            key={get(atom, "uri")}
            onClick={() => this.selectAtom(atom)}
            className={
              "sap__posts__post clickable " +
              (this.isSelected(atom) ? "won--selected" : "")
            }
          >
            <WonAtomHeader atomUri={get(atom, "uri")} />
          </div>
        );
      });

      suggestions = <div className="sap__posts">{suggestionItems}</div>;
    } else {
      suggestions = (
        <div className="sap__noposts">{this.props.noSuggestionsLabel}</div>
      );
    }

    let suggestPostInputIcon;

    if (this.props.uriToFetchLoading) {
      suggestPostInputIcon = (
        <svg className="sap__input__icon hspinner">
          <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
        </svg>
      );
    } else if (this.fetchAtomUriFieldHasText()) {
      if (this.state.showFetchButton && !this.props.uriToFetchFailed) {
        suggestPostInputIcon = (
          <svg
            className="sap__input__icon clickable"
            onClick={() => this.fetchAtom()}
          >
            <use xlinkHref="#ico16_checkmark" href="#ico16_checkmark" />
          </svg>
        );
      } else if (this.state.showResetButton || this.props.uriToFetchFailed) {
        suggestPostInputIcon = (
          <svg
            className="sap__input__icon clickable"
            onClick={() => this.resetAtomUriField()}
          >
            <use xlinkHref="#ico36_close" href="#ico36_close" />
          </svg>
        );
      }
    }

    let suggestPostErrors;
    if (this.fetchAtomUriFieldHasText()) {
      let errorText;

      if (this.props.uriToFetchFailedToLoad) {
        errorText = "Failed to Load Suggestion, might not be a valid uri.";
      } else if (this.props.uriToFetchIsExcluded) {
        errorText = this.props.excludedText;
      } else if (this.props.uriToFetchIsNotAllowed) {
        errorText = this.props.notAllowedSocketText;
      }

      suggestPostErrors = <div className="sap__error">{errorText}</div>;
    }

    return (
      <won-suggest-atom-picker>
        {suggestions}
        <WonLabelledHr label="Not happy with the options? Add an Atom-URI below" />
        <div className="sap__input">
          {suggestPostInputIcon}
          <input
            ref={uriInput => (this.uriInput = uriInput)}
            type="url"
            placeholder={this.props.detail.placeholder}
            className="sap__input__inner"
            onChange={() => this.updateFetchAtomUriField()}
          />
        </div>
        {suggestPostErrors}
      </won-suggest-atom-picker>
    );
  }

  isSelected(atom) {
    return (
      atom &&
      this.props.suggestedAtom &&
      get(atom, "uri") === get(this.props.suggestedAtom, "uri")
    );
  }

  /**
   * Checks validity and uses callback method
   */
  update(title) {
    console.debug("suggest-atom-picker: ", "update(", title, ")");
    if (title && title.trim().length > 0) {
      this.props.onUpdate({ value: title });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  updateFetchAtomUriField() {
    console.debug("suggest-atom-picker: ", "updateFetchAtomUriField()");
    const text = this.uriInput && this.uriInput.value;

    let showFetchButton;
    let showResetButton;
    if (text && text.trim().length > 0) {
      if (this.uriInput.checkValidity()) {
        showResetButton = false;
        showFetchButton = true;
      } else {
        showResetButton = true;
        showFetchButton = false;
      }
    }
    this.setState({
      uriToFetch: undefined,
      showResetButton: showResetButton,
      showFetchButton: showFetchButton,
    });
  }

  fetchAtomUriFieldHasText() {
    console.debug("suggest-atom-picker: ", "fetchAtomUriFieldHasText()");
    const text = this.uriInput && this.uriInput.value;
    return text && text.length > 0;
  }

  resetAtomUriField() {
    if (this.uriInput) {
      this.uriInput.value = "";
    }
    this.setState({
      uriToFetch: undefined,
      showResetButton: false,
      showFetchButton: false,
    });
  }

  fetchAtom() {
    let uriToFetch = this.uriInput && this.uriInput.value;
    uriToFetch = uriToFetch && uriToFetch.trim();
    console.debug(
      "suggest-atom-picker: ",
      "fetchAtom()",
      " uriToFetch: ",
      uriToFetch
    );
    if (
      !getIn(this.props.allSuggestableAtoms, uriToFetch) &&
      !get(this.props.allForbiddenAtoms, uriToFetch)
    ) {
      this.props.fetchAtom(uriToFetch);
      this.setState({ uriToFetch: uriToFetch });
    } else if (get(this.props.allForbiddenAtoms, uriToFetch)) {
      this.setState({ uriToFetch: uriToFetch });
    } else {
      this.update(uriToFetch);
    }
  }

  selectAtom(atom) {
    console.debug("suggest-atom-picker: ", "selectAtom(", atom, ")");
    const atomUri = get(atom, "uri");

    if (atomUri) {
      this.update(atomUri);
    }
  }
}
WonSuggestAtomPicker.propTypes = {
  initialValue: PropTypes.any,
  detail: PropTypes.any,
  excludedText: PropTypes.string,
  notAllowedSocketText: PropTypes.string,
  noSuggestionsText: PropTypes.string,
  onUpdate: PropTypes.func.isRequired,
  uriToFetchLoading: PropTypes.bool,
  uriToFetchFailedToLoad: PropTypes.bool,
  uriToFetchIsExcluded: PropTypes.bool,
  uriToFetchIsNotAllowed: PropTypes.bool,
  allSuggestableAtoms: PropTypes.object,
  allForbiddenAtoms: PropTypes.object,
  suggestionsAvailable: PropTypes.bool,
  sortedActiveAtomsArray: PropTypes.arrayOf(PropTypes.object),
  suggestedAtom: PropTypes.object,
  noSuggestionsLabel: PropTypes.string,
  uriToFetchSuccess: PropTypes.bool,
  uriToFetchFailed: PropTypes.bool,
  fetchAtom: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonSuggestAtomPicker);
