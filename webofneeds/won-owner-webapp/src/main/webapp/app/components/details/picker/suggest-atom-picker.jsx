/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../../../actions/actions.js";
import { connect } from "react-redux";
import Immutable from "immutable";
import PropTypes from "prop-types";

import { get, getIn, getUri } from "../../../utils.js";
import * as atomUtils from "../../../redux/utils/atom-utils.js";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import WonAtomHeader from "../../atom-header.jsx";
import WonLabelledHr from "../../labelled-hr.jsx";

import "~/style/_suggest-atom-picker.scss";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";

const mapStateToProps = (state, ownProps) => {
  const externalDataState = generalSelectors.getExternalDataState(state);
  const hasAtLeastOneAllowedSocket = (atom, allowedSockets) => {
    if (allowedSockets) {
      const allowedSocketsImm = Immutable.fromJS(allowedSockets);
      const atomSocketsImm = atomUtils.getSockets(atom);

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

      return excludedUrisImm.contains(getUri(atom));
    }
    return false;
  };

  const suggestedAtomUri = ownProps.initialValue;
  const allActiveAtoms = generalSelectors.getActiveAtoms(state);

  const sortedSuggestableAtoms =
    allActiveAtoms &&
    allActiveAtoms
      .filter(
        atom =>
          !isExcludedAtom(atom, ownProps.excludedUris) &&
          hasAtLeastOneAllowedSocket(atom, ownProps.allowedSockets)
      )
      .toOrderedMap()
      .sortBy(atom => {
        const title = atomUtils.getTitle(atom, externalDataState);
        return title ? title.toLowerCase() : undefined;
      });

  const allForbiddenAtoms =
    allActiveAtoms &&
    allActiveAtoms.filter(
      atom =>
        !(
          !isExcludedAtom(atom, ownProps.excludedUris) &&
          hasAtLeastOneAllowedSocket(atom, ownProps.allowedSockets)
        )
    );

  const suggestedAtom = get(sortedSuggestableAtoms, suggestedAtomUri);
  return {
    initialValue: ownProps.initialValue,
    detail: ownProps.detail,
    excludedText: ownProps.excludedText,
    notAllowedSocketText: ownProps.notAllowedSocketText,
    onUpdate: ownProps.onUpdate,
    processState: get(state, "process"),
    allForbiddenAtoms,
    allowedSockets: ownProps.allowedSockets,
    excludedUris: ownProps.excludedUris,
    sortedSuggestableAtoms,
    suggestedAtom,
    noSuggestionsLabel:
      ownProps.noSuggestionsText || "No Atoms available to suggest",
    hasAtLeastOneAllowedSocket,
    isExcludedAtom,
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
  constructor(props) {
    super(props);
    this.state = {
      uriToFetchLoading: false,
      uriToFetchFailedToLoad: false,
      uriToFetchFailed: false,
      uriToFetchIsNotAllowed: false,
      uriToFetchIsExcluded: false,
      showFetchButton: false,
      showResetButton: false,
      uriToFetch: "",
    };
  }

  static getDerivedStateFromProps(props, state) {
    const uriToFetchProcess = getIn(props.processState, [
      "atoms",
      state.uriToFetch,
    ]);
    const uriToFetchLoading = !!get(uriToFetchProcess, "loading");
    const uriToFetchFailedToLoad = !!get(uriToFetchProcess, "failedToLoad");
    const uriToFetchIsNotAllowed =
      !!get(props.allForbiddenAtoms, state.uriToFetch) &&
      !props.hasAtLeastOneAllowedSocket(
        get(props.allForbiddenAtoms, state.uriToFetch),
        props.allowedSockets
      );
    const uriToFetchIsExcluded = props.isExcludedAtom(
      get(props.allForbiddenAtoms, state.uriToFetch),
      props.excludedUris
    );
    const uriToFetchSuccess =
      state.uriToFetch &&
      !uriToFetchLoading &&
      !uriToFetchFailedToLoad &&
      get(props.sortedSuggestableAtoms, state.uriToFetch);
    const uriToFetchFailed =
      state.uriToFetch &&
      !uriToFetchLoading &&
      (uriToFetchFailedToLoad ||
        uriToFetchIsExcluded ||
        uriToFetchIsNotAllowed);

    if (uriToFetchSuccess && state.fetching) {
      if (state.uriToFetch && state.uriToFetch.trim().length > 0) {
        props.onUpdate({ value: state.uriToFetch });
      } else {
        props.onUpdate({ value: undefined });
      }

      return {
        uriToFetchLoading: uriToFetchLoading,
        uriToFetchFailedToLoad: uriToFetchFailedToLoad,
        uriToFetchFailed: uriToFetchFailed,
        uriToFetchIsNotAllowed: uriToFetchIsNotAllowed,
        uriToFetchIsExcluded: uriToFetchIsExcluded,
        uriToFetch: "",
        fetching: false,
        showResetButton: false,
        showFetchButton: false,
      };
    } else {
      return {
        uriToFetchLoading: uriToFetchLoading,
        uriToFetchFailedToLoad: uriToFetchFailedToLoad,
        uriToFetchFailed: uriToFetchFailed,
        uriToFetchIsNotAllowed: uriToFetchIsNotAllowed,
        uriToFetchIsExcluded: uriToFetchIsExcluded,
        uriToFetch: state.uriToFetch,
        showResetButton: state.showResetButton,
        showFetchButton: state.showFetchButton,
      };
    }
  }

  render() {
    let suggestions;

    if (
      this.props.sortedSuggestableAtoms &&
      this.props.sortedSuggestableAtoms.size > 0
    ) {
      const suggestionElements = [];

      this.props.sortedSuggestableAtoms.map((atom, atomUri) =>
        suggestionElements.push(
          <div
            key={atomUri}
            onClick={() => this.selectAtom(atom)}
            className={
              "sap__posts__post clickable " +
              (this.isSelected(atom) ? "won--selected" : "")
            }
          >
            <WonAtomHeader atom={atom} hideTimestamp={true} />
          </div>
        )
      );
      suggestions = <div className="sap__posts">{suggestionElements}</div>;
    } else {
      suggestions = (
        <div className="sap__noposts">{this.props.noSuggestionsLabel}</div>
      );
    }

    let suggestPostInputIcon;

    if (this.state.uriToFetchLoading) {
      suggestPostInputIcon = (
        <svg className="sap__input__icon hspinner">
          <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
        </svg>
      );
    } else if (this.fetchAtomUriFieldHasText()) {
      if (this.state.showFetchButton && !this.state.uriToFetchFailed) {
        suggestPostInputIcon = (
          <svg
            className="sap__input__icon clickable"
            onClick={this.fetchAtom.bind(this)}
          >
            <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
          </svg>
        );
      } else if (this.state.showResetButton || this.state.uriToFetchFailed) {
        suggestPostInputIcon = (
          <svg
            className="sap__input__icon clickable"
            onClick={this.resetAtomUriField.bind(this)}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        );
      }
    }

    let suggestPostErrors;
    if (this.fetchAtomUriFieldHasText()) {
      let errorText;

      if (this.state.uriToFetchFailedToLoad) {
        errorText = "Failed to Load Suggestion, might not be a valid uri.";
      } else if (this.state.uriToFetchIsExcluded) {
        errorText = this.props.excludedText;
      } else if (this.state.uriToFetchIsNotAllowed) {
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
            type="url"
            placeholder={this.props.detail.placeholder}
            className="sap__input__inner"
            value={this.state.uriToFetch}
            onChange={this.updateFetchAtomUriField.bind(this)}
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
      getUri(atom) === getUri(this.props.suggestedAtom)
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

  updateFetchAtomUriField(event) {
    const text = event.target.value;

    let showFetchButton;
    let showResetButton;
    if (text && text.trim().length > 0) {
      if (event.target.checkValidity()) {
        showResetButton = false;
        showFetchButton = true;
      } else {
        showResetButton = true;
        showFetchButton = false;
      }
    }
    this.setState({
      uriToFetch: text.trim(),
      showResetButton: showResetButton,
      showFetchButton: showFetchButton,
    });
  }

  fetchAtomUriFieldHasText() {
    return this.state.uriToFetch && this.state.uriToFetch.length > 0;
  }

  resetAtomUriField() {
    this.setState({
      uriToFetch: "",
      showResetButton: false,
      showFetchButton: false,
    });
  }

  fetchAtom() {
    console.debug(
      "suggest-atom-picker: ",
      "fetchAtom()",
      " uriToFetch: ",
      this.state.uriToFetch
    );
    if (
      !getIn(this.props.sortedSuggestableAtoms, this.state.uriToFetch) &&
      !get(this.props.allForbiddenAtoms, this.state.uriToFetch)
    ) {
      this.setState({ fetching: true }, () =>
        this.props.fetchAtom(this.state.uriToFetch)
      );
    } else {
      this.update(this.state.uriToFetch);
    }
  }

  selectAtom(atom) {
    console.debug("suggest-atom-picker: ", "selectAtom(", atom, ")");
    const atomUri = getUri(atom);

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
  sortedSuggestableAtoms: PropTypes.object,
  allForbiddenAtoms: PropTypes.object,
  suggestedAtom: PropTypes.object,
  noSuggestionsLabel: PropTypes.string,
  fetchAtom: PropTypes.func,
  processState: PropTypes.object,
  excludedUris: PropTypes.arrayOf(PropTypes.string),
  allowedSockets: PropTypes.arrayOf(PropTypes.string),
  isExcludedAtom: PropTypes.func,
  hasAtLeastOneAllowedSocket: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonSuggestAtomPicker);
