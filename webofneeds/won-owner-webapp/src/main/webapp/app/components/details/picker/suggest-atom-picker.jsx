/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn, sortBy} from "../../../utils.js";
import {actionCreators} from "../../../actions/actions.js";

import * as generalSelectors from "../../../redux/selectors/general-selectors.js";

import "~/style/_suggest-atom-picker.scss";
import Immutable from "immutable";
import WonAtomHeader from "../../atom-header.jsx";
import WonLabelledHr from "../../labelled-hr";

export default class WonSuggestAtomPicker extends React.Component {
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

  componentDidMount() {
    this.initialValue = this.props.initialValue;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.initialValue = nextProps.initialValue;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const suggestedAtomUri = this.initialValue;
    const allActiveAtoms = generalSelectors.getActiveAtoms(state);

    const allSuggestableAtoms =
      allActiveAtoms &&
      allActiveAtoms.filter(atom => this.isSuggestable(atom));

    const allForbiddenAtoms =
      allActiveAtoms &&
      allActiveAtoms.filter(atom => !this.isSuggestable(atom));

    const suggestedAtom = get(allSuggestableAtoms, suggestedAtomUri);
    const sortedActiveAtoms =
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
      !this.hasAtLeastOneAllowedSocket(
        get(allForbiddenAtoms, this.state.uriToFetch)
      );
    const uriToFetchIsExcluded = this.isExcludedAtom(
      get(allForbiddenAtoms, this.state.uriToFetch)
    );

    return {
      suggestedAtomUri,
      uriToFetchLoading,
      uriToFetchFailedToLoad,
      uriToFetchIsExcluded,
      uriToFetchIsNotAllowed,
      allSuggestableAtoms,
      allForbiddenAtoms,
      suggestionsAvailable:
        allSuggestableAtoms && allSuggestableAtoms.size > 0,
      sortedActiveAtoms,
      suggestedAtom,
      noSuggestionsLabel:
        this.noSuggestionsText || "No Atoms available to suggest",
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
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    let suggestions;

    if (this.state.suggestionsAvailable) {
      const suggestionItems = this.state.sortedActiveAtoms.map((atom, atomUri) => {
        return (
          <div
            key={atom.get('uri')}
            onClick={() => this.selectAtom(atom)}
            className={"sap__posts__post clickable " + (this.isSelected(atom) ? "won--selected" : "")}
          >
            <WonAtomHeader atomUri={atom.get('uri')} ngRedux={this.props.ngRedux}/>
          </div>
        );
      });

      suggestions = (
        <div className="sap__posts">{suggestionItems}</div>
      );
    } else {
      suggestions = (
        <div className="sap__noposts">{this.props.noSuggestionsLabel}</div>
      );
    }

    let suggestPostInputIcon;

    if(this.state.uriToFetchLoading) {
      suggestPostInputIcon = (
        <svg className="sap__input__icon hspinner">
          <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim"></use>
        </svg>
      );
    } else if (this.fetchAtomUriFieldHasText()) {
      if (this.state.showFetchButton && !this.state.uriToFetchFailed) {
        suggestPostInputIcon = (
          <svg className="sap__input__icon clickable" onClick={() => this.fetchAtom()}>
            <use xlinkHref="#ico16_checkmark" href="#ico16_checkmark"></use>
          </svg>
        );
      } else if (this.state.showResetButton || this.state.uriToFetchFailed) {
        suggestPostInputIcon = (
          <svg className="sap__input__icon clickable" onClick={() => this.resetAtomUriField()}>
            <use xlinkHref="#ico36_close" href="#ico36_close"></use>
          </svg>
        );
      }
    }

    let suggestPostErrors;
    if(this.fetchAtomUriFieldHasText()) {
      let errorText;

      if(this.state.uriToFetchFailedToLoad) {
        errorText = "Failed to Load Suggestion, might not be a valid uri.";
      } else if(this.state.uriToFetchIsExcluded) {
        errorText = this.props.excludedText;
      } else if(this.state.uriToFetchIsNotAllowed) {
        errorText = this.props.notAllowedSocketText;
      }

      suggestPostErrors = (<div className="sap__error">{errorText}</div>);
    };

    return (
      <won-suggest-atom-picker>
        {suggestions}
        <WonLabelledHr label="Not happy with the options? Add an Atom-URI below"/>
        <div className="sap__input">
          {suggestPostInputIcon}
          <input
            ref={uriInput => this.uriInput = uriInput}
            type="url"
            placeholder={this.props.detail.placeholder}
            className="sap__input__inner"
            onChange={() => this.updateFetchAtomUriField()}/>
        </div>
        {suggestPostErrors}
      </won-suggest-atom-picker>
    );
  }

  hasAtLeastOneAllowedSocket(atom) {
    if (this.props.allowedSockets) {
      const allowedSocketsImm = Immutable.fromJS(this.props.allowedSockets);
      const atomSocketsImm = getIn(atom, ["content", "sockets"]);

      return (
        atomSocketsImm &&
        atomSocketsImm.find(socket => allowedSocketsImm.contains(socket))
      );
    }
    return true;
  }

  isExcludedAtom(atom) {
    if (this.props.excludedUris) {
      const excludedUrisImm = Immutable.fromJS(this.props.excludedUris);

      return excludedUrisImm.contains(get(atom, "uri"));
    }
    return false;
  }

  isSuggestable(atom) {
    return (
      !this.isExcludedAtom(atom) && this.hasAtLeastOneAllowedSocket(atom)
    );
  }

  isSelected(atom) {
    return (
      atom &&
      this.state.suggestedAtom &&
      get(atom, "uri") === get(this.state.suggestedAtom, "uri")
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
    this.setState({uriToFetch: undefined, showResetButton: showResetButton, showFetchButton: showFetchButton});
  }

  fetchAtomUriFieldHasText() {
    console.debug("suggest-atom-picker: ", "fetchAtomUriFieldHasText()");
    const text = this.uriInput && this.uriInput.value;
    return text && text.length > 0;
  }

  resetAtomUriField() {
    console.debug("suggest-atom-picker: ", "resetAtomUriField(", omitUpdate, ")");
    if (this.uriInput) {
      this.uriInput.value = "";
    }
    this.setState({uriToFetch: undefined, showResetButton: false, showFetchButton: false});
  }

  fetchAtom() {
    let uriToFetch = this.uriInput && this.uriInput.value;
    uriToFetch = uriToFetch && uriToFetch.trim();
    console.debug("suggest-atom-picker: ", "fetchAtom()", " uriToFetch: ", uriToFetch);
    if (
      !getIn(this.state.allSuggestableAtoms, uriToFetch) &&
      !get(this.state.allForbiddenAtoms, uriToFetch)
    ) {
      this.props.ngRedux.dispatch(actionCreators.atoms__fetchUnloadedAtom(uriToFetch));
      this.setState({uriToFetch: uriToFetch});
    } else if (get(this.state.allForbiddenAtoms, uriToFetch)) {
      this.setState({uriToFetch: uriToFetch});
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