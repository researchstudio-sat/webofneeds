import React from "react";

import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { get, getIn, toAbsoluteURL } from "../utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils";
import { ownerBaseUrl } from "~/config/default.js";
import * as wonUtils from "../won-utils.js";
import pdfMake from "pdfmake/build/pdfmake";
import pdfFonts from "pdfmake/build/vfs_fonts";

import "~/style/_context-dropdown.scss";
import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";

const mapStateToProps = (state, ownProps) => {
  const atom = ownProps.atomUri && getIn(state, ["atoms", ownProps.atomUri]);

  let linkToAtom;
  if (ownerBaseUrl && atom) {
    const path = "#!post/" + `?postUri=${encodeURI(get(atom, "uri"))}`;

    linkToAtom = toAbsoluteURL(ownerBaseUrl).toString() + path;
  }

  const process = get(state, "process");

  return {
    atomUri: ownProps.atomUri,
    className: ownProps.className,
    adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
    isOwnAtom: generalSelectors.isAtomOwned(state, ownProps.atomUri),
    isActive: atomUtils.isActive(atom),
    isInactive: atomUtils.isInactive(atom),
    isUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
      state,
      ownProps.atomUri
    ),
    isEditable: generalSelectors.isAtomEditable(state, ownProps.atomUri),
    atom,
    atomLoading: !atom || processUtils.isAtomLoading(process, get(atom, "uri")),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, get(atom, "uri")),
    linkToAtom,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGoAbs: (path, props) => {
      dispatch(actionCreators.router__stateGoAbs(path, props));
    },
    atomReOpen: atomUri => {
      dispatch(actionCreators.atoms__reopen(atomUri));
    },
    atomClose: atomUri => {
      dispatch(actionCreators.atoms__close(atomUri));
    },
    atomDelete: atomUri => {
      dispatch(actionCreators.atoms__delete(atomUri));
    },
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showModalDialog: payload => {
      dispatch(actionCreators.view__showModalDialog(payload));
    },
  };
};

class WonAtomContextDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    this.handleClick = this.handleClick.bind(this);
  }

  render() {
    const iconElement =
      this.props.atomLoading || this.props.atomFailedToLoad ? (
        <svg className="cdd__icon__small">
          <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
        </svg>
      ) : (
        <svg
          className="cdd__icon__small clickable"
          onClick={() => this.setState({ contextMenuOpen: true })}
        >
          <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
        </svg>
      );

    let dropdownElement;

    if (this.state.contextMenuOpen) {
      const buttons = [];

      buttons.push(
        <button
          key="export"
          className="won-button--outlined thin red"
          onClick={() => this.exportPdf()}
        >
          Export as PDF
        </button>
      );
      this.props.isUsableAsTemplate &&
        buttons.push(
          <button
            key="duplicate"
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.routerGoAbs("create", {
                fromAtomUri: this.props.atomUri,
                mode: "DUPLICATE",
              })
            }
          >
            Post this too!
          </button>
        );
      this.props.isEditable &&
        buttons.push(
          <button
            key="edit"
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.routerGoAbs("create", {
                fromAtomUri: this.props.atomUri,
                mode: "EDIT",
              })
            }
          >
            Edit
          </button>
        );
      this.props.adminEmail &&
        buttons.push(
          <a
            key="report"
            className="won-button--outlined thin red"
            href={
              "mailto:" +
              this.props.adminEmail +
              "?" +
              this.generateReportAtomMailParams()
            }
          >
            Report
          </a>
        );
      this.props.isOwnAtom &&
        this.props.isInactive &&
        buttons.push(
          <button
            key="reopen"
            className="won-button--filled red"
            onClick={() => this.reOpenAtom()}
          >
            Reopen
          </button>
        );
      this.props.isOwnAtom &&
        this.props.isInactive &&
        buttons.push(
          <button
            key="delete"
            className="won-button--filled red"
            onClick={() => this.deleteAtom()}
          >
            Delete
          </button>
        );
      this.props.isOwnAtom &&
        this.props.isActive &&
        buttons.push(
          <button
            key="close"
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
                <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
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
        class={this.props.className ? this.props.className : ""}
        ref={node => (this.node = node)}
      >
        {iconElement}
        {dropdownElement}
      </won-atom-context-dropdown>
    );
  }

  generateReportAtomMailParams() {
    //todo
    const subject = `[Report Atom] - ${this.props.atomUri}`;
    const body = `Link to Atom: ${this.linkToAtom}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

    return `subject=${subject}&body=${body}`;
  }

  exportPdf() {
    if (!this.props.atom) return;
    const docDefinition = wonUtils.createDocumentDefinitionFromPost(
      this.props.atom
    );

    if (docDefinition) {
      pdfMake.vfs = pdfFonts.pdfMake.vfs;
      pdfMake.createPdf(docDefinition).download();
    }
  }

  reOpenAtom() {
    if (this.props.isOwnAtom) {
      this.props.atomReOpen(this.props.atomUri);
    }
  }

  deleteAtom() {
    if (this.props.isOwnAtom) {
      const payload = {
        caption: "Attention!",
        text: "Deleting the Atom is irreversible, do you want to proceed?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              this.props.atomDelete(this.props.atomUri);
              this.props.routerGoAbs("inventory");
              this.props.hideModalDialog();
            },
          },
          {
            caption: "No",
            callback: () => {
              this.props.hideModalDialog();
            },
          },
        ],
      };
      this.props.showModalDialog(payload);
    }
  }

  closeAtom() {
    if (this.props.isOwnAtom) {
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
              this.props.atomClose(this.props.atomUri);
              this.props.hideModalDialog();
            },
          },
          {
            caption: "Cancel",
            callback: () => {
              this.props.hideModalDialog();
            },
          },
        ],
      };
      this.props.showModalDialog(payload);
    }
  }

  componentWillMount() {
    document.addEventListener("mousedown", this.handleClick, false);
  }
  componentWillUnmount() {
    document.removeEventListener("mousedown", this.handleClick, false);
  }

  handleClick(e) {
    if (!this.node.contains(e.target) && this.state.contextMenuOpen) {
      this.setState({ contextMenuOpen: false });

      return;
    }
  }
}
WonAtomContextDropdown.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  adminEmail: PropTypes.string,
  isOwnAtom: PropTypes.bool,
  isActive: PropTypes.bool,
  isInactive: PropTypes.bool,
  isUsableAsTemplate: PropTypes.bool,
  isEditable: PropTypes.bool,
  atom: PropTypes.object,
  atomLoading: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  linkToAtom: PropTypes.string,
  routerGoAbs: PropTypes.func,
  atomReOpen: PropTypes.func,
  atomClose: PropTypes.func,
  atomDelete: PropTypes.func,
  hideModalDialog: PropTypes.func,
  showModalDialog: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomContextDropdown);
