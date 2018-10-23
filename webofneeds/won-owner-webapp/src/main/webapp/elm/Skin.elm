port module Skin exposing
    ( Skin
    , black
    , cssColor
    , decoder
    , default
    , setAlpha
    , skinnedElement
    , white
    )

import Browser
import Element exposing (..)
import Html exposing (Html)
import Json.Decode as Decode exposing (Decoder, Error, Value)


type alias Skin =
    { primaryColor : Color
    , lightGray : Color
    , lineGray : Color
    , subtitleGray : Color
    }


black : Color
black =
    rgb255 0 0 0


white : Color
white =
    rgb255 255 255 255


default : Skin
default =
    { primaryColor = rgb255 240 70 70
    , lightGray = rgb255 240 242 244
    , lineGray = rgb255 203 210 209
    , subtitleGray = rgb255 128 128 128
    }


cssColor : Color -> String
cssColor color =
    let
        { red, green, blue, alpha } =
            toRgb color

        to255 col =
            String.fromInt <| round (col * 255)

        colors =
            ([ red
             , green
             , blue
             ]
                |> List.map to255
            )
                ++ [ String.fromFloat alpha ]
    in
    "rgba("
        ++ String.join "," colors
        ++ ")"


setAlpha : Float -> Color -> Color
setAlpha alpha color =
    let
        oldRgb =
            toRgb color
    in
    fromRgb
        { oldRgb
            | alpha = alpha
        }


colorDecoder : Decoder Color
colorDecoder =
    Decode.list Decode.int
        |> Decode.andThen
            (\channels ->
                case channels of
                    [ r, g, b ] ->
                        Decode.succeed <| rgb255 r g b

                    _ ->
                        Decode.fail "Expected [r, g, b]"
            )


decoder : Decoder Skin
decoder =
    Decode.map4 Skin
        (Decode.field "primaryColor" colorDecoder)
        (Decode.field "lightGray" colorDecoder)
        (Decode.field "lineGray" colorDecoder)
        (Decode.field "subtitleGray" colorDecoder)


port skin : (SkinFlags -> msg) -> Sub msg


type alias Rgb =
    { r : Int, g : Int, b : Int }


type alias Model model =
    { skin : Skin
    , model : model
    }


type Msg msg
    = MsgReceived msg
    | SkinReceived Skin
    | NoOp


type alias SkinFlags =
    { primaryColor : Rgb
    , lightGray : Rgb
    , lineGray : Rgb
    , subtitleGray : Rgb
    }


fromFlags : SkinFlags -> Skin
fromFlags flags =
    let
        frgb { r, g, b } =
            fromRgb255 { red = r, green = g, blue = b, alpha = 1 }
    in
    { primaryColor = frgb flags.primaryColor
    , lightGray = frgb flags.lightGray
    , lineGray = frgb flags.lineGray
    , subtitleGray = frgb flags.subtitleGray
    }


skinnedElement :
    { init : flags -> ( model, Cmd msg )
    , update : msg -> model -> ( model, Cmd msg )
    , subscriptions : model -> Sub msg
    , view : Skin -> model -> Html msg
    }
    -> Program { skin : SkinFlags, flags : flags } (Model model) (Msg msg)
skinnedElement { init, update, subscriptions, view } =
    Browser.element
        { init =
            \flags ->
                let
                    ( model, cmds ) =
                        init flags.flags
                in
                ( { skin = fromFlags flags.skin
                  , model = model
                  }
                , Cmd.map MsgReceived cmds
                )
        , update =
            \msg model ->
                case msg of
                    MsgReceived subMsg ->
                        let
                            ( subModel, cmds ) =
                                update subMsg model.model
                        in
                        ( { model
                            | model = subModel
                          }
                        , Cmd.map MsgReceived cmds
                        )

                    SkinReceived skin_ ->
                        ( { model
                            | skin = skin_
                          }
                        , Cmd.none
                        )

                    NoOp ->
                        ( model, Cmd.none )
        , view =
            \model ->
                view model.skin model.model
                    |> Html.map MsgReceived
        , subscriptions =
            \model ->
                Sub.batch
                    [ skin (fromFlags >> SkinReceived)
                    , Sub.map MsgReceived (subscriptions model.model)
                    ]
        }
