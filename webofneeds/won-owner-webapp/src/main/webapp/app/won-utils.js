/**
 * Created by ksinger on 11.08.2016.
 */

import { generateIdString, toAbsoluteURL, getUri } from "./utils.js";

import { ownerBaseUrl } from "~/config/default.js";
import qr from "qr-image";

import Immutable from "immutable";
import * as useCaseUtils from "./usecase-utils.js";
import { getTitle } from "./redux/utils/atom-utils.js";

import won from "./won-es6.js";

/**
 * Generates a privateId of `[usernameFragment]-[password]`
 * @returns {string}
 */
export function generatePrivateId() {
  return generateIdString(8) + "-" + generateIdString(8); //<usernameFragment>-<password>
}

/**
 * Parses a given privateId into a fake email address and a password.
 * @param privateId
 * @returns {{email: string, password: *}}
 */
export function privateId2Credentials(privateId) {
  const [usernameFragment, password] = privateId.split("-");
  const email = usernameFragment + "@matchat.org";
  return {
    email,
    password,
    privateId,
  };
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {email, password}
 */
export function parseCredentials(credentials) {
  return credentials.privateId
    ? privateId2Credentials(credentials.privateId)
    : credentials;
}

export function getRandomWonId() {
  // needs to start with a letter, so N3 doesn't run into
  // problems when serializing, see
  // https://github.com/RubenVerborgh/N3.js/issues/121
  return (
    getRandomString(1, "abcdefghijklmnopqrstuvwxyz") +
    getRandomString(11, "abcdefghijklmnopqrstuvwxyz0123456789")
  );
}

/**
 * generates a string of random characters
 *
 * @param {*} length the length of the string to be generated. e.g. in the example below: 5
 * @param {*} chars the allowed characters, e.g. "abc123" to generate strings like "a3cba"
 */
function getRandomString(
  length,
  chars = "abcdefghijklmnopqrstuvwxyz0123456789"
) {
  const randomChar = () => chars[Math.floor(Math.random() * chars.length)];
  return Array.from(
    {
      length: length,
    },
    randomChar
  ).join("");
}

export function createDocumentDefinitionFromPost(post, externalDataState) {
  if (!post) return;

  let title = { text: getTitle(post, externalDataState), style: "title" };
  let contentHeader = { text: "Description", style: "branchHeader" };
  let seeksHeader = { text: "Looking For", style: "branchHeader" };

  let content = [];
  content.push(title);

  const allDetails = useCaseUtils.getAllDetails();

  const postContent = post.get("content");
  if (postContent) {
    content.push(contentHeader);
    postContent.map((detailValue, detailKey) => {
      const detailJS =
        detailValue && Immutable.Iterable.isIterable(detailValue)
          ? detailValue.toJS()
          : detailValue;

      const detailDefinition = allDetails[detailKey];
      if (detailDefinition && detailJS) {
        content.push({ text: detailDefinition.label, style: "detailHeader" });
        content.push({
          text: detailDefinition.generateHumanReadable({
            value: detailJS,
            includeLabel: false,
          }),
          style: "detailText",
        });
      }
    });
  }

  const seeksBranch = post.get("seeks");
  if (seeksBranch) {
    content.push(seeksHeader);
    seeksBranch.map((detailValue, detailKey) => {
      const detailJS =
        detailValue && Immutable.Iterable.isIterable(detailValue)
          ? detailValue.toJS()
          : detailValue;

      const detailDefinition = allDetails[detailKey];
      if (detailDefinition && detailJS) {
        content.push({ text: detailDefinition.label, style: "detailHeader" });
        content.push({
          text: detailDefinition.generateHumanReadable({
            value: detailJS,
            includeLabel: false,
          }),
          style: "detailText",
        });
      }
    });
  }

  if (ownerBaseUrl && post) {
    const path = "#!/post" + `?postUri=${encodeURI(getUri(post))}`;
    const linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;

    if (linkToPost) {
      content.push({ text: linkToPost, style: "postLink" });
      const base64PngQrCode = generateBase64PngQrCode(linkToPost);
      if (base64PngQrCode) {
        content.push({
          image: "data:image/png;base64," + base64PngQrCode,
          width: 200,
          height: 200,
        });
      }
    }
  }

  let styles = {
    title: {
      fontSize: 20,
      bold: true,
    },
    branchHeader: {
      fontSize: 18,
      bold: true,
    },
    detailHeader: {
      fontSize: 12,
      bold: true,
    },
    detailText: {
      fontSize: 12,
    },
    postLink: {
      fontSize: 10,
    },
  };
  return {
    content: content /*[title, 'This is an sample PDF printed with pdfMake '+this.linkToPost]*/,
    styles: styles,
  };
}

export function generateSvgQrCode(link) {
  return link && qr.imageSync(link, { type: "svg" });
}

function generatePngQrCode(link) {
  return link && qr.imageSync(link, { type: "png" });
}

function generateBase64PngQrCode(link) {
  const pngQrCode = generatePngQrCode(link);
  return pngQrCode && btoa(String.fromCharCode.apply(null, pngQrCode));
}

export function parseRestErrorMessage(error) {
  if (error && error.get("code") === won.RESPONSECODE.PRIVATEID_NOT_FOUND) {
    return "Sorry, we couldn't find the private ID (the one in your url-bar). If you copied this address make sure you **copied everything** and try **reloading the page**. If this doesn't work you can try [removing it](#) to start fresh.";
  } else if (
    error &&
    error.get("code") === won.RESPONSECODE.USER_NOT_VERIFIED
  ) {
    return "You haven't verified your email addres yet. Please do so now by clicking the link in the email we sent you.";
  } else if (error) {
    //return the message FIXME: once the localization is implemented use the correct localization
    return error.get("message");
  }

  return error;
}

export function genDetailBaseUri(baseUri) {
  if (!baseUri) {
    return undefined;
  }
  const randomId = generateIdString(10);
  return baseUri + "#" + randomId;
}
