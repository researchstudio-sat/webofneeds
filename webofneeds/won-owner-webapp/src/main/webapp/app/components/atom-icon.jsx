/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils";
import PropTypes from "prop-types";
import { connect } from "react-redux";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isPersona = atomUtils.isPersona(atom);
  const image = isPersona ? atomUtils.getDefaultPersonaImage(atom) : undefined;

  const useCaseIcon = !isPersona
    ? atomUtils.getMatchedUseCaseIcon(atom)
    : undefined;
  const useCaseIconBackground = !isPersona
    ? atomUtils.getBackground(atom)
    : undefined;

  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;

  // Icons/Images of the AtomHolder
  const personaUri = atomUtils.getHeldByUri(atom);
  const persona = getIn(state, ["atoms", personaUri]);
  const holderImage = atomUtils.getDefaultPersonaImage(persona);
  const holderIdenticonSvg = atomUtils.getIdenticonSvg(persona);
  const showHolderIdenticon = !holderImage && holderIdenticonSvg;
  const showHolderImage = holderImage;

  const process = get(state, "process");
  return {
    className: ownProps.className,
    onClick: ownProps.onClick,
    isPersona,
    atomImage: isPersona ? atomUtils.getDefaultPersonaImage(atom) : undefined,
    atomInactive: atomUtils.isInactive(atom),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, ownProps.atomUri),
    useCaseIcon,
    useCaseIconBackground,
    showIdenticon: !image && !!identiconSvg,
    showImage: !!image,
    identiconSvg,
    image,
    showHolderIdenticon,
    showHolderImage,
    holderImage,
    holderIdenticonSvg,
  };
};

class WonAtomIcon extends React.Component {
  render() {
    let holderIcon = undefined;
    let atomIcon = undefined;

    if (this.props.showHolderIdenticon) {
      holderIcon = (
        <img
          className="holderIcon"
          alt="Auto-generated title image for persona that holds the atom"
          src={"data:image/svg+xml;base64," + this.props.holderIdenticonSvg}
        />
      );
    } else if (this.props.showHolderImage) {
      holderIcon = (
        <img
          className="holderIcon"
          alt={this.props.holderImage.get("name")}
          src={
            "data:" +
            this.props.holderImage.get("type") +
            ";base64," +
            this.props.holderImage.get("data")
          }
        />
      );
    }

    if (this.props.showIdenticon) {
      atomIcon = (
        <img
          className="image"
          alt="Auto-generated title icon"
          src={"data:image/svg+xml;base64," + this.props.identiconSvg}
        />
      );
    } else if (this.props.showImage) {
      atomIcon = (
        <img
          className="image"
          alt={this.props.image.get("name")}
          src={
            "data:" +
            this.props.image.get("type") +
            ";base64," +
            this.props.image.get("data")
          }
        />
      );
    } else if (this.props.useCaseIcon) {
      const style = {
        backgroundColor: this.props.useCaseIconBackground,
      };

      atomIcon = (
        <div className="image usecaseimage" style={style}>
          <svg className="si__usecaseicon">
            <use
              xlinkHref={this.props.useCaseIcon}
              href={this.props.useCaseIcon}
            />
          </svg>
        </div>
      );
    }

    return (
      <won-atom-icon
        class={
          (this.props.className ? this.props.className : "") +
          " " +
          (this.props.isPersona ? " won-is-persona " : "") +
          (this.props.atomFailedToLoad ? " won-failed-to-load " : "") +
          (this.props.atomInactive ? " inactive " : "") +
          (this.props.onClick ? " clickable " : "")
        }
        onClick={this.props.onClick}
      >
        {atomIcon}
        {holderIcon}
      </won-atom-icon>
    );
  }
}
WonAtomIcon.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  onClick: PropTypes.func,
  isPersona: PropTypes.bool,
  atomImage: PropTypes.object,
  atomInactive: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  useCaseIcon: PropTypes.string,
  useCaseIconBackground: PropTypes.string,
  showIdenticon: PropTypes.bool,
  showImage: PropTypes.bool,
  identiconSvg: PropTypes.object,
  image: PropTypes.object,
  showHolderIdenticon: PropTypes.bool,
  showHolderImage: PropTypes.bool,
  holderImage: PropTypes.object,
  holderIdenticonSvg: PropTypes.object,
};

export default connect(mapStateToProps)(WonAtomIcon);
