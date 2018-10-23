module RatingView exposing (main)

import Element exposing (..)
import Element.Font as Font
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
    }


init : Int -> ( Model, Cmd Msg )
init ratingFlag =
    ( { rating = fromInt ratingFlag }, Cmd.none )



---- UPDATE ----


type alias Msg =
    Never


update : Msg -> Model -> ( Model, Cmd Msg )
update absurd model =
    never absurd


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
        [ Font.color skin.primaryColor
        , spacing 2
        ]
    <|
        List.repeat numberOfFilled (text "★")
            ++ List.repeat (5 - numberOfFilled) (text "☆")


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
        , Font.size 12
        , Font.bold
        ]
    <|
        case model.rating of
            Just rating ->
                viewRating skin rating

            Nothing ->
                el [ Font.color skin.primaryColor ] <|
                    text "☆☆☆☆☆"
