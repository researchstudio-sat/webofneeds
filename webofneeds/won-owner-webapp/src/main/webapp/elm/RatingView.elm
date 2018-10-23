module RatingView exposing (main)

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


type alias Model =
    { rating : Maybe Rating
    , popupState : PopupState
    , mouseOver : Bool
    }


type alias Popup =
    { reviewText : String
    , selectedValue : Maybe Rating
    , hoveredValue : Maybe Rating
    }


type PopupState
    = Closed
    | Open Popup


init : Int -> ( Model, Cmd Msg )
init ratingFlag =
    ( { rating = fromInt ratingFlag
      , popupState = Closed
      , mouseOver = False
      }
    , Cmd.none
    )



---- UPDATE ----


type PopupMsg
    = ReviewChanged String
    | HoveredValue (Maybe Rating)
    | SelectedValue (Maybe Rating)


type Msg
    = Hover Bool
    | TogglePopup
    | PopupMsg PopupMsg


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    ( case msg of
        Hover hovered ->
            { model
                | mouseOver = hovered
            }

        TogglePopup ->
            case model.popupState of
                Closed ->
                    { model
                        | popupState =
                            Open
                                { reviewText = ""
                                , selectedValue = Nothing
                                , hoveredValue = Nothing
                                }
                    }

                Open _ ->
                    { model
                        | popupState = Closed
                        , mouseOver = False
                    }

        PopupMsg popupMsg ->
            { model
                | popupState = updatePopup popupMsg model.popupState
            }
    , Cmd.none
    )


updatePopup : PopupMsg -> PopupState -> PopupState
updatePopup msg popupState =
    case popupState of
        Closed ->
            Closed

        Open state ->
            case msg of
                ReviewChanged review ->
                    Open
                        { state
                            | reviewText = review
                        }

                HoveredValue rating ->
                    Open
                        { state
                            | hoveredValue = rating
                        }

                SelectedValue rating ->
                    Open
                        { state
                            | selectedValue = rating
                        }


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none



---- VIEW ----


viewRating : Skin -> Rating -> Element msg
viewRating skin rating =
    let
        numberOfFilled =
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

                        Open popupState ->
                            popup skin popupState
                ]
              <|
                viewMaybeRating skin model.rating
            , if model.mouseOver && model.popupState == Closed then
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
            [ Background.color skin.primaryColor
            , padding 14
            , width fill
            , Border.rounded 3
            ]
            { label =
                el
                    [ centerX
                    , Font.color Skin.white
                    , Font.size 16
                    ]
                <|
                    text "Submit"
            , onPress = Just TogglePopup
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
            case displayedValue of
                Just One ->
                    1

                Just Two ->
                    2

                Just Three ->
                    3

                Just Four ->
                    4

                Just Five ->
                    5

                Nothing ->
                    0
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
