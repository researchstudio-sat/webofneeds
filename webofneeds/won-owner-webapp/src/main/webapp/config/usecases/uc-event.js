/**
 * Created by fsuda on 18.09.2018.
 */
import {
  defaultReactions,
  details,
  mergeInEmptyDraft,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_detail_datetime from "../../images/won-icons/ico36_detail_datetime.svg";

export const event = {
  identifier: "event",
  label: "Event",
  icon: ico36_detail_datetime, //TODO: Find better Icon
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Event"],
        sockets: {
          "#sEventInverseSocket": vocab.WXSCHEMA.EventInverseSocketCompacted,
          "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
          "#sAttendeeSocket": vocab.WXSCHEMA.AttendeeSocketCompacted,
          "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
          "#groupSocket": vocab.GROUP.GroupSocketCompacted,
          "#sAssociatedArticleSocket":
            vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
        },
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXSCHEMA.AttendeeSocketCompacted]: {
      [vocab.WXSCHEMA.AttendeeInverseSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
        labels: {
          owned: {
            default: "Persona",
            addNew: "New Persona",
            picker: "Pick a Persona to invite",
          },
          nonOwned: {
            default: "Persona",
            addNew: "New Persona",
            picker: "Pick a Persona to invite",
          },
        },
      },
    },
    [vocab.WXSCHEMA.EventInverseSocketCompacted]: {
      [vocab.WXSCHEMA.EventSocketCompacted]: {
        useCaseIdentifiers: ["organization", "persona", "project"],
      },
    },
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: {
      [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: {
        useCaseIdentifiers: ["newsarticle"],
        labels: {
          owned: {
            default: "Article",
            addNew: "New Article",
            picker: "Pick an Article",
          },
          nonOwned: {
            default: "Article",
            addNew: "New Article",
            picker: "Pick an Article",
          },
        },
      },
    },
    [vocab.GROUP.GroupSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
  },
  details: {
    title: { ...details.title, mandatory: true },
    eventObjectAboutUris: { ...details.eventObjectAboutUris, mandatory: true },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
    fromDatetime: { ...details.fromDatetime, mandatory: true },
    throughDatetime: { ...details.throughDatetime, mandatory: true },
  },
  seeksDetails: undefined,
};
