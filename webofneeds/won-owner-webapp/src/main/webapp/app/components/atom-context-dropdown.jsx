import React from "react";

import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { get, getIn, toAbsoluteURL } from "../utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils";
import { ownerBaseUrl } from "~/config/default.js";
import * as wonUtils from "../won-utils.js";
import pdfMake from "pdfmake/build/pdfmake";
import pdfFonts from "pdfmake/build/vfs_fonts";

import "~/style/_context-dropdown.scss";

export default class WonAtomContextDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    //TODO: REACT ON CLICK OUTSIDE OF COMPONENT AND CLOSE THE DIALOG (maybe with hooks)
  }

  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  componentWillUnmount() {
    this.disconnect();
  }

  selectFromState(state) {
    const atom = this.atomUri && getIn(state, ["atoms", this.atomUri]);

    let linkToAtom;
    if (ownerBaseUrl && atom) {
      const path = "#!post/" + `?postUri=${encodeURI(get(atom, "uri"))}`;

      linkToAtom = toAbsoluteURL(ownerBaseUrl).toString() + path;
    }

    const process = get(state, "process");

    return {
      adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
      isOwnAtom: generalSelectors.isAtomOwned(state, this.atomUri),
      isActive: atomUtils.isActive(atom),
      isInactive: atomUtils.isInactive(atom),
      isUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
        state,
        this.atomUri
      ),
      isEditable: generalSelectors.isAtomEditable(state, this.atomUri),
      atom,
      atomLoading:
        !atom || processUtils.isAtomLoading(process, get(atom, "uri")),
      atomFailedToLoad:
        atom && processUtils.hasAtomFailedToLoad(process, get(atom, "uri")),
      linkToAtom,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const iconElement =
      this.state.atomLoading || this.state.atomFailedToLoad ? (
        <svg className="cdd__icon__small">
          <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
        </svg>
      ) : (
        <svg
          className="cdd__icon__small clickable"
          onClick={() => this.setState({ contextMenuOpen: true })}
        >
          <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
        </svg>
      );

    let dropdownElement;

    if (this.state.contextMenuOpen) {
      const buttons = []; //TODO

      buttons.push(
        <button
          className="won-button--outlined thin red"
          onClick={() => this.exportPdf()}
        >
          Export as PDF
        </button>
      );
      this.state.isUsableAsTemplate &&
        buttons.push(
          <button
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.ngRedux.dispatch(
                actionCreators.router__stateGoAbs("create", {
                  fromAtomUri: this.atomUri,
                  mode: "DUPLICATE",
                })
              )
            }
          >
            Post this too!
          </button>
        );
      this.state.isEditable &&
        buttons.push(
          <button
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.ngRedux.dispatch(
                actionCreators.router__stateGoAbs("create", {
                  fromAtomUri: this.atomUri,
                  mode: "EDIT",
                })
              )
            }
          >
            Edit
          </button>
        );
      this.state.adminEmail &&
        buttons.push(
          <a
            className="won-button--outlined thin red"
            href={
              "mailto:" +
              this.state.adminEmail +
              "?" +
              this.generateReportAtomMailParams()
            }
          >
            Report
          </a>
        );
      this.state.isOwnAtom &&
        this.state.isInactive &&
        buttons.push(
          <button
            className="won-button--filled red"
            onClick={() => this.reOpenAtom()}
          >
            Reopen
          </button>
        );
      this.state.isOwnAtom &&
        this.state.isInactive &&
        buttons.push(
          <button
            className="won-button--filled red"
            onClick={() => this.deleteAtom()}
          >
            Delete
          </button>
        );
      this.state.isOwnAtom &&
        this.state.isActive &&
        buttons.push(
          <button
            className="won-button--filled red"
            onClick={() => this.closeAtom()}
          >
            Remove
          </button>
        );

      dropdownElement = (
        <div className="cdd__contextmenu">
          <div
            className="cdd__contextmenu__content"
            onClick={() => this.setState({ contextMenuOpen: false })}
          >
            <div className="topline">
              <svg className="cdd__icon__small__contextmenu clickable">
                <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
              </svg>
            </div>
            {/* Buttons when connection is available -->*/}
            {buttons}
          </div>
        </div>
      );
    }

    return (
      <won-atom-context-dropdown
        class={this.props.className ? this.props.className : undefined}
      >
        {iconElement}
        {dropdownElement}
      </won-atom-context-dropdown>
    );
  }

  generateReportAtomMailParams() {
    //todo
    const subject = `[Report Atom] - ${this.atomUri}`;
    const body = `Link to Atom: ${this.linkToAtom}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

    return `subject=${subject}&body=${body}`;
  }

  exportPdf() {
    if (!this.state.atom) return;
    const docDefinition = wonUtils.createDocumentDefinitionFromPost(
      this.state.atom
    );

    if (docDefinition) {
      pdfMake.vfs = pdfFonts.pdfMake.vfs;
      pdfMake.createPdf(docDefinition).download();
    }
  }

  reOpenAtom() {
    if (this.state.isOwnAtom) {
      this.props.ngRedux.dispatch(actionCreators.atoms__reopen(this.atomUri));
    }
  }

  deleteAtom() {
    if (this.state.isOwnAtom) {
      const payload = {
        caption: "Attention!",
        text: "Deleting the Atom is irreversible, do you want to proceed?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              this.props.ngRedux.dispatch(
                actionCreators.atoms__delete(this.atomUri)
              );
              this.props.ngRedux.dispatch(
                actionCreators.router__stateGoAbs("inventory")
              );
              this.props.ngRedux.dispatch(
                actionCreators.view__hideModalDialog()
              );
            },
          },
          {
            caption: "No",
            callback: () => {
              this.props.ngRedux.dispatch(
                actionCreators.view__hideModalDialog()
              );
            },
          },
        ],
      };
      this.props.ngRedux.dispatch(
        actionCreators.view__showModalDialog(payload)
      );
    }
  }

  closeAtom() {
    if (this.state.isOwnAtom) {
      const payload = {
        caption: "Attention!",
        text:
          "Deleting or archiving the Atom will close all connections, do you want to proceed?",
        buttons: [
          {
            caption: "Delete",
            callback: () => {
              this.deleteAtom();
            },
          },
          {
            caption: "Archive",
            callback: () => {
              this.props.ngRedux.dispatch(
                actionCreators.atoms__close(this.atomUri)
              );
              this.props.ngRedux.dispatch(
                actionCreators.view__hideModalDialog()
              );
            },
          },
          {
            caption: "Cancel",
            callback: () => {
              this.props.ngRedux.dispatch(
                actionCreators.view__hideModalDialog()
              );
            },
          },
        ],
      };
      this.props.ngRedux.dispatch(
        actionCreators.view__showModalDialog(payload)
      );
    }
  }
}
WonAtomContextDropdown.propTypes = {
  atomUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
  className: PropTypes.string,
};
