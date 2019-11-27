/**
 * Created by fsuda on 03.10.2018.
 */
import { generateIdString } from "../../app/utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonReviewViewer from "../../app/components/details/viewer/review-viewer.jsx";
import WonReviewPicker from "../../app/components/details/picker/review-picker.jsx";

/*Detail based on https://schema.org/Review*/
export const review = {
  identifier: "review",
  label: "Review",
  icon: "#ico36_detail_title" /*TODO: REVIEW/RATING ICON*/,
  component: WonReviewPicker,
  viewerComponent: WonReviewViewer,
  placeholder: "Review Text (optional)",
  rating: [
    { value: undefined, label: "Select a Rating", default: true },
    { value: 1, label: "1 Star" },
    { value: 2, label: "2 Stars" },
    { value: 3, label: "3 Stars" },
    { value: 4, label: "4 Stars" },
    { value: 5, label: "5 Stars" },
  ],
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value || !value.rating) {
      return { "s:review": undefined };
    }

    const randomString = generateIdString(10);

    return {
      "s:review": {
        "@type": "s:Review",
        "@id":
          contentUri && identifier
            ? contentUri + "#" + identifier + "-" + randomString
            : undefined,
        "s:about":
          "TODO: here should be the identityUri that the review refers to", //TODO: use identity uri
        "s:author":
          "TODO:  here should be the identityUri of the review-author", //TODO: use identity uri
        "s:reviewRating": {
          "@type": "s:Rating",
          "@id":
            contentUri && identifier
              ? contentUri +
                "#" +
                identifier +
                "-" +
                randomString +
                "-" +
                generateIdString(10)
              : undefined,
          "s:bestRating": { "@value": 5, "@type": "xsd:int" }, //not necessary but possible
          "s:ratingValue": { "@value": value.rating, "@type": "xsd:int" },
          "s:worstRating": { "@value": 1, "@type": "xsd:int" }, //not necessary but possible
        },
        "s:description": value.text,
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const text = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:review", "s:description"],
      "xsd:string"
    );

    const rating = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:review", "s:reviewRating", "s:ratingValue"],
      "xsd:int"
    );

    //TODO: EXTRACT AUTHOR AND ABOUT URI

    if (!rating) {
      return undefined;
    } else {
      return { text: text, rating: rating };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      //TODO: EXTRACT AUTHOR AND ABOUT URI
      const text = value.text;

      let ratingLabel = undefined;

      this.rating &&
        this.rating.forEach(rating => {
          if (rating.value === value.rating) {
            ratingLabel = rating.label;
          }
        });
      ratingLabel = ratingLabel || value.rating;

      return (
        (includeLabel ? this.label + ": " + ratingLabel : ratingLabel) +
        (text ? " - " + text : "")
      );
    }
    return undefined;
  },
};
