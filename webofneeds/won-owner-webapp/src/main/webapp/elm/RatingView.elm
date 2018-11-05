port module RatingView exposing (main)

import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Element.Font as Font
import Element.Input as Input
import Html exposing (Html)
import Html.Attributes as HA
import Skin exposing (Skin)


main =
    Skin.skinnedElement
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }



---- MODEL ----


type Rating
    = One
    | Two
    | Three
    | Four
    | Five


fromInt : Int -> Maybe Rating
fromInt rating =
    case rating of
        1 ->
            Just One

        2 ->
            Just Two

        3 ->
            Just Three

        4 ->
            Just Four

        5 ->
            Just Five

        _ ->
            Nothing


toInt : Rating -> Int
toInt rating =
    case rating of
        One ->
            1

        Two ->
            2

        Three ->
            3

        Four ->
            4

        Five ->
            5


type alias Model =
    { rating : Maybe Rating
    , popupState : PopupState
    }


type alias Popup =
    { reviewText : String
    , selectedValue : Maybe Rating
    , hoveredValue : Maybe Rating
    }


type PopupState
    = Closed
    | Hovered
    | Open Popup


init : Int -> ( Model, Cmd Msg )
init ratingFlag =
    ( { rating = fromInt ratingFlag
      , popupState = Closed
      }
    , Cmd.none
    )



---- PORT ----


port reviewSubmitted : { value : Int, message : String } -> Cmd msg



---- UPDATE ----


type PopupMsg
    = ReviewChanged String
    | HoveredValue (Maybe Rating)
    | SelectedValue (Maybe Rating)
    | SubmitReview


type Msg
    = Hover Bool
    | TogglePopup
    | PopupMsg PopupMsg


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        Hover hovered ->
            ( { model
                | popupState = updateHover hovered model.popupState
              }
            , Cmd.none
            )

        TogglePopup ->
            ( { model
                | popupState =
                    case model.popupState of
                        Closed ->
                            Open initialPopup

                        Hovered ->
                            Open initialPopup

                        Open _ ->
                            Closed
              }
            , Cmd.none
            )

        PopupMsg popupMsg ->
            let
                ( newPopupState, popupCmd ) =
                    updatePopup popupMsg model.popupState
            in
            ( { model
                | popupState = newPopupState
              }
            , Cmd.map PopupMsg popupCmd
            )


initialPopup : Popup
initialPopup =
    { reviewText = ""
    , selectedValue = Nothing
    , hoveredValue = Nothing
    }


updateHover : Bool -> PopupState -> PopupState
updateHover hovered state =
    case ( hovered, state ) of
        ( _, Open _ ) ->
            state

        ( True, _ ) ->
            Hovered

        ( False, _ ) ->
            Closed


updatePopup : PopupMsg -> PopupState -> ( PopupState, Cmd PopupMsg )
updatePopup msg popupState =
    case popupState of
        Closed ->
            ( Closed, Cmd.none )

        Hovered ->
            ( Hovered, Cmd.none )

        Open state ->
            case msg of
                ReviewChanged review ->
                    ( Open
                        { state
                            | reviewText = review
                        }
                    , Cmd.none
                    )

                HoveredValue rating ->
                    ( Open
                        { state
                            | hoveredValue = rating
                        }
                    , Cmd.none
                    )

                SelectedValue rating ->
                    ( Open
                        { state
                            | selectedValue = rating
                        }
                    , Cmd.none
                    )

                SubmitReview ->
                    case state.selectedValue of
                        Just value ->
                            -- TODO: actually do something
                            ( Closed
                            , reviewSubmitted
                                { value = toInt value
                                , message = state.reviewText
                                }
                            )

                        Nothing ->
                            ( popupState, Cmd.none )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none



---- VIEW ----


viewRating : Skin -> Rating -> Element msg
viewRating skin rating =
    let
        numberOfFilled =
            toInt rating
    in
    row
        [ spacing 2
        ]
    <|
        List.repeat numberOfFilled (text "★")
            ++ List.repeat (5 - numberOfFilled) (text "☆")


viewMaybeRating : Skin -> Maybe Rating -> Element Msg
viewMaybeRating skin maybeRating =
    el
        [ Font.color skin.primaryColor
        , Font.size 12
        , Font.bold
        , Events.onClick <| TogglePopup
        ]
    <|
        case maybeRating of
            Just rating ->
                viewRating skin rating

            Nothing ->
                text "☆☆☆☆☆"


view : Skin -> Model -> Html Msg
view skin model =
    layout
        [ htmlAttribute <| HA.style "display" "inline-block"
        , paddingEach
            { left = 5
            , right = 0
            , top = 0
            , bottom = 0
            }
        , width shrink
        ]
    <|
        row
            [ spacing 5
            , Events.onMouseEnter <| Hover True
            , Events.onMouseLeave <| Hover False
            , htmlAttribute <| HA.style "user-select" "none"
            ]
            [ el
                [ below <|
                    case model.popupState of
                        Closed ->
                            none

                        Hovered ->
                            none

                        Open popupState ->
                            popup skin popupState
                ]
              <|
                viewMaybeRating skin model.rating
            , if model.popupState == Hovered then
                el
                    [ Font.size 14
                    , Font.color skin.primaryColor
                    , Events.onClick <| TogglePopup
                    ]
                <|
                    text "+"

              else
                none
            ]


popup : Skin -> Popup -> Element Msg
popup skin state =
    let
        canSubmit =
            case state.selectedValue of
                Just _ ->
                    True

                Nothing ->
                    False
    in
    column
        [ Background.color Skin.white
        , moveDown 3
        , padding 5
        , Border.color skin.lineGray
        , Border.width 1
        , Font.size 14
        , spacing 5
        , width <| minimum 200 shrink
        ]
        [ text "Set a rating:"
        , starSelector skin state
        , Input.multiline []
            { onChange = ReviewChanged >> PopupMsg
            , text = state.reviewText
            , label = Input.labelAbove [] <| text "Write a review:"
            , placeholder = Just <| Input.placeholder [] <| text "..."
            , spellcheck = False
            }
        , Input.button
            ((if canSubmit then
                [ Background.color skin.primaryColor
                ]

              else
                [ Background.color skin.lightGray
                , htmlAttribute <| HA.style "cursor" "auto"
                , focused []
                ]
             )
                ++ [ padding 14
                   , width fill
                   , Border.rounded 3
                   ]
            )
            { label =
                el
                    [ centerX
                    , Font.color Skin.white
                    , Font.size 16
                    ]
                <|
                    text "Submit"
            , onPress =
                if canSubmit then
                    Just (PopupMsg SubmitReview)

                else
                    Nothing
            }
        ]


starSelector :
    Skin
    ->
        { a
            | selectedValue : Maybe Rating
            , hoveredValue : Maybe Rating
        }
    -> Element Msg
starSelector skin { selectedValue, hoveredValue } =
    let
        orElse left right =
            Maybe.map Just left
                |> Maybe.withDefault right

        displayedValue =
            orElse hoveredValue selectedValue

        numberOfFilled =
            Maybe.map toInt displayedValue
                |> Maybe.withDefault 0
    in
    row
        [ Font.size 18
        , Font.color skin.primaryColor
        , Events.onMouseLeave (PopupMsg <| HoveredValue Nothing)
        ]
        (List.range 1 5
            |> List.map
                (\id ->
                    let
                        currentRating =
                            fromInt id
                    in
                    el
                        [ Events.onMouseEnter
                            (PopupMsg <| HoveredValue currentRating)
                        , Events.onClick
                            (PopupMsg <| SelectedValue currentRating)
                        ]
                    <|
                        if id <= numberOfFilled then
                            text "★"

                        else
                            text "☆"
                )
        )
