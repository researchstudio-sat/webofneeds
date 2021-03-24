/**
 * Created by fsuda on 18.09.2018.
 */
import {
  defaultReactions,
  details,
  mergeInEmptyDraft,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_project from "../../images/won-icons/ico36_uc_project.svg";

export const project = {
  identifier: "project",
  label: "Project",
  icon: ico36_uc_project,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Project"],
        sockets: {
          "#memberSocket": vocab.WXSCHEMA.MemberSocketCompacted,
          "#associatedArticleSocket":
            vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
          "#projectOfSocket": vocab.PROJECT.ProjectOfSocketCompacted,
          "#relatedProjectSocket": vocab.PROJECT.RelatedProjectSocketCompacted,
          "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
          "#sEventSocket": vocab.WXSCHEMA.EventSocketCompacted,
        },
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXSCHEMA.MemberSocketCompacted]: {
      [vocab.WXSCHEMA.MemberOfSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
        labels: {
          owned: {
            default: "Member",
            addNew: "Add New Member",
            picker: "Pick a Persona to invite",
          },
          nonOwned: {
            default: "Join Project",
            addNew: "Add New Member",
            picker: "Pick a Persona to invite",
          },
        },
      },
    },
    [vocab.PROJECT.ProjectOfSocketCompacted]: {
      [vocab.PROJECT.ProjectSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
        labels: {
          owned: {
            default: "Organization",
            addNew: "Add To New Organization",
            picker: "Pick an Organization to connect with",
          },
          nonOwned: {
            default: "Organization",
            addNew: "Add To New Organization",
            picker: "Pick an Organization to connect with",
          },
        },
      },
    },
    [vocab.PROJECT.RelatedProjectSocketCompacted]: {
      [vocab.PROJECT.RelatedProjectSocketCompacted]: {
        useCaseIdentifiers: ["project"],
        labels: {
          owned: {
            default: "Project",
            addNew: "Add New Related Project",
            picker: "Pick a Related Project to connect with",
          },
          nonOwned: {
            default: "Organization",
            addNew: "Add New Related Project",
            picker: "Pick a Related Project to connect with",
          },
        },
      },
    },
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: {
      [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: {
        useCaseIdentifiers: ["newsarticle"],
      },
    },
  },
  details: {
    title: { ...details.title, mandatory: true },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
    website: { ...details.website },
  },
  seeksDetails: undefined,
};
