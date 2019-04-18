port module Application exposing (Style, element, logError)

import Browser
import Html exposing (Html)
import Json.Decode as Decode exposing (Decoder, Value)
import Json.Decode.Extra as Decode
import Json.Decode.Pipeline as DP
import Result.Extra as Result



---- PORTS ----


port inPort :
    (Value -> msg)
    -> Sub msg


type alias ExternalData props =
    { style : Maybe Style
    , props : Maybe props
    , unmount : Bool
    }


externalUpdateDecoder : Decoder props -> Decoder (ExternalData props)
externalUpdateDecoder propsDecoder =
    let
        optional field dec =
            DP.optional field (Decode.map Just dec) Nothing
    in
    Decode.succeed ExternalData
        |> optional "newStyle" styleDecoder
        |> optional "newProps" propsDecoder
        |> DP.custom
            (Decode.maybe (Decode.field "unmount" Decode.bool)
                |> Decode.map (Maybe.withDefault False)
            )


port errorPort : String -> Cmd msg


logError : String -> Cmd msg
logError =
    errorPort



---- STYLE ----


type alias Style =
    {}


styleDecoder : Decoder Style
styleDecoder =
    Decode.succeed {}



---- MODEL ----


type Model props subModel
    = ParsingFailed String
    | Model
        { subModel : subModel
        , props : props
        , style : Style
        }
    | Unmounted


type Msg props subMsg
    = SubMsg subMsg
    | ParsingError String
    | ExternalUpdate (ExternalData props)
    | BeingUnmounted



---- APPLICATION ----


element :
    { view :
        { style : Style
        , props : props
        , model : subModel
        }
        -> Html subMsg
    , update :
        subMsg
        ->
            { model : subModel
            , props : props
            }
        -> ( subModel, Cmd subMsg )
    , init : props -> ( subModel, Cmd subMsg )
    , subscriptions : subModel -> Sub subMsg
    , propDecoder : Decoder props
    }
    ->
        Program
            { props : Value
            , style : Value
            }
            (Model props subModel)
            (Msg props subMsg)
element options =
    let
        -- VIEW
        view model =
            case model of
                ParsingFailed _ ->
                    Html.text "Parsing the arguments failed, please look at the log for errors"

                Model { props, style, subModel } ->
                    Html.map SubMsg <|
                        options.view
                            { style = style
                            , model = subModel
                            , props = props
                            }

                Unmounted ->
                    Html.span [] []

        -- UPDATE
        update msg modelWrapper =
            case modelWrapper of
                ParsingFailed _ ->
                    ( modelWrapper, Cmd.none )

                Model model ->
                    case msg of
                        SubMsg subMsg ->
                            let
                                ( newModel, cmd ) =
                                    options.update subMsg
                                        { model = model.subModel
                                        , props = model.props
                                        }
                            in
                            ( Model
                                { model
                                    | subModel = newModel
                                }
                            , Cmd.map SubMsg cmd
                            )

                        ParsingError message ->
                            ( ParsingFailed message
                            , logError <| "Error on update:\n" ++ message
                            )

                        ExternalUpdate { props, style, unmount } ->
                            if unmount then
                                ( Unmounted, Cmd.none )

                            else
                                ( Model
                                    { model
                                        | style =
                                            style
                                                |> Maybe.withDefault model.style
                                        , props =
                                            props
                                                |> Maybe.withDefault model.props
                                    }
                                , Cmd.none
                                )

                        BeingUnmounted ->
                            ( Unmounted, Cmd.none )

                Unmounted ->
                    ( modelWrapper, Cmd.none )

        -- SUBSCRIPTIONS
        subscriptions model =
            case model of
                ParsingFailed _ ->
                    Sub.none

                Model { subModel } ->
                    Sub.batch
                        [ options.subscriptions subModel
                            |> Sub.map SubMsg
                        , inPort
                            (\val ->
                                Decode.decodeValue
                                    (externalUpdateDecoder options.propDecoder)
                                    val
                                    |> Result.map ExternalUpdate
                                    |> Result.extract
                                        (Decode.errorToString
                                            >> ParsingError
                                        )
                            )
                        ]

                Unmounted ->
                    Sub.none

        -- INIT
        init { props, style } =
            Result.map2
                (\sty pr ->
                    let
                        ( subModel, subCmd ) =
                            options.init pr
                    in
                    ( Model
                        { style = sty
                        , subModel = subModel
                        , props = pr
                        }
                    , Cmd.map SubMsg subCmd
                    )
                )
                (Decode.decodeValue styleDecoder style)
                (Decode.decodeValue options.propDecoder props)
                |> Result.extract
                    (\e ->
                        let
                            message =
                                "Error on init:\n"
                                    ++ Decode.errorToString e
                        in
                        ( ParsingFailed message
                        , errorPort message
                        )
                    )
    in
    Browser.element
        { view = view
        , update = update
        , subscriptions = subscriptions
        , init = init
        }
