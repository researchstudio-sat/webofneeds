module RatingView exposing (main)

import Application
import Element exposing (..)
import Html exposing (Html)
import Html.Attributes as HA
import Html.Events as HE
import Json.Decode as Decode exposing (Decoder)
import Palette
import Persona


main =
    Application.element
        { init = init
        , update = update
        , subscriptions = always Sub.none
        , view = view
        , propDecoder = propDecoder
        }



---- PROPS ----


type alias Props =
    { rating : Maybe Rating
    , connectionUri : Maybe String
    }


ratingDecoder : Decoder Rating
ratingDecoder =
    Decode.int
        |> Decode.andThen
            (fromInt
                >> Maybe.map Decode.succeed
                >> Maybe.withDefault (Decode.fail "Not a valid rating")
            )


propDecoder : Decoder Props
propDecoder =
    Decode.map2 Props
        (Decode.field "rating" ratingDecoder
            |> Decode.maybe
        )
        (Decode.field "connectionUri" Decode.string
            |> Decode.maybe
        )



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


type alias Popup =
    { reviewText : String
    , selectedValue : Maybe Rating
    , hoveredValue : Maybe Rating
    }


type Model
    = CannotRate
    | Closed
    | Hovered
    | Open Popup


init : Props -> ( Model, Cmd Msg )
init { connectionUri } =
    ( case connectionUri of
        Just _ ->
            Closed

        Nothing ->
            CannotRate
    , Cmd.none
    )



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


update :
    Msg
    ->
        { model : Model
        , props : Props
        }
    -> ( Model, Cmd Msg )
update msg { model, props } =
    case msg of
        Hover hovered ->
            ( updateHover hovered model
            , Cmd.none
            )

        TogglePopup ->
            ( case model of
                CannotRate ->
                    CannotRate

                Closed ->
                    Open initialPopup

                Hovered ->
                    Open initialPopup

                Open _ ->
                    Closed
            , Cmd.none
            )

        PopupMsg popupMsg ->
            case props.connectionUri of
                Just connectionUri ->
                    let
                        ( newPopupState, popupCmd ) =
                            updatePopup connectionUri popupMsg model
                    in
                    ( newPopupState
                    , Cmd.map PopupMsg popupCmd
                    )

                Nothing ->
                    ( CannotRate, Cmd.none )


initialPopup : Popup
initialPopup =
    { reviewText = ""
    , selectedValue = Nothing
    , hoveredValue = Nothing
    }


updateHover : Bool -> Model -> Model
updateHover hovered state =
    case ( hovered, state ) of
        ( _, CannotRate ) ->
            state

        ( _, Open _ ) ->
            state

        ( True, _ ) ->
            Hovered

        ( False, _ ) ->
            Closed


updatePopup : String -> PopupMsg -> Model -> ( Model, Cmd PopupMsg )
updatePopup connectionUri msg popupState =
    case popupState of
        CannotRate ->
            ( CannotRate, Cmd.none )

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
                            ( Closed
                            , Persona.review
                                { connection = connectionUri
                                , review =
                                    { value = toInt value
                                    , message = state.reviewText
                                    }
                                }
                            )

                        Nothing ->
                            ( popupState, Cmd.none )



---- VIEW ----


viewRating : Rating -> String
viewRating rating =
    let
        numberOfFilled =
            toInt rating
    in
    List.repeat numberOfFilled "★"
        ++ List.repeat (5 - numberOfFilled) "☆"
        |> String.concat


viewMaybeRating : Maybe Rating -> Html Msg
viewMaybeRating maybeRating =
    Html.span
        [ HA.class "rating-display"
        , HE.onClick TogglePopup
        ]
        [ Html.text <|
            case maybeRating of
                Just rating ->
                    viewRating rating

                Nothing ->
                    "☆☆☆☆☆"
        ]


view :
    { props : Props
    , model : Model
    }
    -> Html Msg
view { model, props } =
    Html.span
        [ HE.onMouseEnter <| Hover True
        , HE.onMouseLeave <| Hover False
        , HA.class "won-rating-view"
        ]
    <|
        List.filterMap identity
            [ Just <| viewMaybeRating props.rating
            , if model == Hovered then
                Just <|
                    Html.span
                        [ HA.class "rating-display" ]
                        [ Html.text "+" ]

              else
                Nothing
            , case model of
                CannotRate ->
                    Nothing

                Closed ->
                    Nothing

                Hovered ->
                    Nothing

                Open popupState ->
                    Just <| popup popupState
            ]


popup : Popup -> Html Msg
popup state =
    let
        canSubmit =
            case state.selectedValue of
                Just _ ->
                    True

                Nothing ->
                    False
    in
    Html.div [ HA.class "rating-popup" ]
        [ Html.label []
            [ Html.text "Set a rating:"
            , starSelector state
            ]
        , Html.label [ HA.class "review-text" ]
            [ Html.text "Write a review:"
            , Html.textarea
                [ HA.placeholder "..."
                , HE.onInput (ReviewChanged >> PopupMsg)
                ]
                [ Html.text state.reviewText ]
            ]
        , Palette.wonButton
            [ HE.onClick <| PopupMsg SubmitReview
            , HA.disabled <| not canSubmit
            , HA.class "submit-button"
            ]
            [ Html.text "Submit" ]
        ]


starSelector :
    { a
        | selectedValue : Maybe Rating
        , hoveredValue : Maybe Rating
    }
    -> Html Msg
starSelector { selectedValue, hoveredValue } =
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
    Html.div
        [ HE.onMouseLeave (PopupMsg <| HoveredValue Nothing)
        , HA.class "star-selector"
        ]
        (List.range 1 5
            |> List.map
                (\id ->
                    let
                        currentRating =
                            fromInt id
                    in
                    Html.span
                        [ HE.onMouseEnter
                            (PopupMsg <| HoveredValue currentRating)
                        , HE.onClick
                            (PopupMsg <| SelectedValue currentRating)
                        , HA.class "star"
                        ]
                        [ if id <= numberOfFilled then
                            Html.text "★"

                          else
                            Html.text "☆"
                        ]
                )
        )
