/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get, getIn, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_persona-card.scss";
import { withRouter, Link } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const identiconSvg = atomUtils.getIdenticonSvg(atom);
  const atomImage = atomUtils.getDefaultPersonaImage(atom);

  return {
    atomUri: ownProps.atomUri,
    isInactive: atomUtils.isInactive(atom),
    atom,
    personaName: get(atom, "humanReadable"),
    atomImage,
    showDefaultIcon: !atomImage,
    identiconSvg,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    selectAtomTab: (atomUri, selectTab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({
            atomUri: atomUri,
            selectTab: selectTab,
          })
        )
      );
    },
  };
};

class WonPersonaCard extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const personaIdenticon =
      this.props.showDefaultIcon && this.props.identiconSvg ? (
        <img
          className="identicon"
          alt="Auto-generated title image"
          src={"data:image/svg+xml;base64," + this.props.identiconSvg}
        />
      ) : (
        undefined
      );

    const personaImage = this.props.atomImage ? (
      <img
        className="image"
        alt={this.props.atomImage.get("name")}
        src={
          "data:" +
          this.props.atomImage.get("encodingFormat") +
          ";base64," +
          this.props.atomImage.get("encoding")
        }
      />
    ) : (
      undefined
    );

    return (
      <Link
        className="won-persona-card"
        to={generateLink(
          this.props.history.location,
          { postUri: this.props.atomUri, tab: "DETAIL" },
          "/post"
        )}
      >
        <div
          className={
            "card__icon clickable " + (this.props.isInactive ? "inactive" : "")
          }
        >
          {personaIdenticon}
          {personaImage}
        </div>
        <div className="card__main clickable">
          <div className="card__main__name">{this.props.personaName}</div>
        </div>
      </Link>
    );
  }
}
WonPersonaCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  selectAtomTab: PropTypes.func,
  atom: PropTypes.object,
  isInactive: PropTypes.bool,
  personaName: PropTypes.string,
  atomImage: PropTypes.string,
  showDefaultIcon: PropTypes.bool,
  identiconSvg: PropTypes.string,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonPersonaCard)
);
