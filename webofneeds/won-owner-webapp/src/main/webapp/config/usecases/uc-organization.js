/**
 * Created by fsuda on 18.09.2018.
 */
import {
  defaultReactions,
  details,
  mergeInEmptyDraft,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico16_uc_organization from "../../images/won-icons/ico16_uc_organization.svg";

export const organization = {
  identifier: "organization",
  label: "Organization",
  icon: ico16_uc_organization,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Organization"],
        sockets: {
          // TODO: Currently not in use in favour of more generic member -> Role -> member relation
          // "#worksForInverseSocket":
          //   vocab.WXSCHEMA.WorksForInverseSocketCompacted,
          "#memberSocket": vocab.WXSCHEMA.MemberSocketCompacted,
          "#associatedArticleSocket":
            vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
          "#parentOrgSocket": vocab.WXSCHEMA.ParentOrganizationSocketCompacted,
          "#projectSocket": vocab.PROJECT.ProjectSocketCompacted,
          "#subOrgSocket": vocab.WXSCHEMA.SubOrganizationSocketCompacted,
          "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
          "#sEventSocket": vocab.WXSCHEMA.EventSocketCompacted,
        },
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    // TODO: Currently not in use in favour of more generic member -> Role -> member relation
    // [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: {
    //   [vocab.WXSCHEMA.WorksForSocketCompacted]: {
    //     useCaseIdentifiers: ["persona"],
    //   },
    // },
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
            default: "Join Organization",
            addNew: "Add New Member",
            picker: "Pick a Persona to invite",
          },
        },
      },
      // TODO: INCLUDE ROLES AGAIN ONCE THE ACL'S ARE EDITABLE
      // [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: {
      //   useCaseIdentifiers: ["role"],
      //   refuseNonOwned: true,
      //   labels: {
      //     owned: {
      //       default: "Role",
      //       addNew: "Add New Role",
      //       picker: "Add Role",
      //     },
      //     nonOwned: {
      //       default: "Role",
      //       addNew: "Add New Role",
      //       picker: "Add Role",
      //     },
      //   },
      // },
    },
    [vocab.PROJECT.ProjectSocketCompacted]: {
      [vocab.WXSCHEMA.ProjectOfSocketCompacted]: {
        useCaseIdentifiers: ["project"],
        labels: {
          owned: {
            default: "Project",
            addNew: "Add New Project",
            picker: "Pick a Project to add",
          },
          nonOwned: {
            default: "Project",
            addNew: "Add New Project",
            picker: "Pick a Project to add",
          },
        },
      },
    },
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: {
      [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: {
        useCaseIdentifiers: ["newsarticle"],
      },
    },
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: {
      [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
      },
    },
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: {
      [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
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
