port module Application exposing
    ( Id
    , Need(..)
    , NeedData
    , NeedStorage(..)
    , Persona
    , State
    , element
    , ownedNeeds
    , urlDecoder
    )

import Browser
import Dict exposing (Dict)
import Element.Styled as Element exposing (Element, Style)
import Html exposing (Html)
import Json.Decode as Decode exposing (Decoder, Value)
import Json.Decode.Extra as Decode
import Json.Decode.Pipeline as DP
import Json.Encode as Encode
import NonEmpty exposing (NonEmpty)
import Palette
import Result.Extra as Result
import Set exposing (Set)
import Time
import Url exposing (Url)


port outPort : Value -> Cmd msg


port inPort :
    (Value -> msg)
    -> Sub msg


type alias Id =
    String


port errorPort : String -> Cmd msg


type NeedStorage
    = ToLoad
    | Loading
    | Loaded NeedData
    | FailedToLoad


type alias NeedData =
    { need : Need
    , created : Time.Posix
    }


type Need
    = PersonaNeed Persona
    | Other
        { holder : Maybe Id
        }


type alias State =
    { needs : Dict Id NeedStorage
    , owned : Set Id
    }


ownedNeeds : State -> Dict Id NeedStorage
ownedNeeds state =
    let
        selectNeed url =
            Dict.get url state.needs
                |> Maybe.map
                    (\needStorage ->
                        ( url, needStorage )
                    )
    in
    Set.toList state.owned
        |> List.filterMap selectNeed
        |> Dict.fromList


type UpdateType attributes
    = StyleUpdate Style
    | StateUpdate State
    | AttrUpdate attributes
    | StyleState Style State
    | StyleAttr Style attributes
    | AttrState attributes State
    | StyleAttrState Style attributes State
    | ParsingError String


type Msg attributes subMsg
    = SubMsg subMsg
    | ExternalUpdate (UpdateType attributes)


type Model attributes subModel
    = ParsingFailed String
    | Model
        { state : State
        , subModel : subModel
        , attributes : attributes
        , style : Style
        }


jsonldSet : Decoder comparable -> Decoder (Set comparable)
jsonldSet decoder =
    Decode.oneOf
        [ Decode.map Set.singleton decoder
        , Decode.set decoder
        ]


types : Decoder (Set String)
types =
    Decode.field "@type" <| jsonldSet Decode.string


type alias Persona =
    { name : NonEmpty String
    , description : Maybe (NonEmpty String)
    , url : Maybe (NonEmpty String)
    }


personaDecoder : Decoder Persona
personaDecoder =
    Decode.succeed Persona
        |> DP.required "s:name" NonEmpty.stringDecoder
        |> DP.optional "s:description" (Decode.map Just NonEmpty.stringDecoder) Nothing
        |> DP.optional "s:url" (Decode.map Just NonEmpty.stringDecoder) Nothing


dateDecoder : Decoder Time.Posix
dateDecoder =
    Decode.value
        |> Decode.andThen
            (Encode.encode 0
                >> Decode.decodeString Decode.datetime
                >> Result.mapError Decode.errorToString
                >> Decode.fromResult
            )


urlDecoder : Decoder Id
urlDecoder =
    Decode.string
        |> Decode.andThen
            (Url.fromString
                >> Maybe.map Url.toString
                >> Result.fromMaybe "Not a valid Url"
                >> Decode.fromResult
            )


otherDecoder :
    Decoder
        { holder : Maybe Id
        }
otherDecoder =
    Decode.succeed
        (\holder ->
            { holder = holder
            }
        )
        |> DP.optionalAt [ "won:heldBy", "@id" ] (Decode.map Just urlDecoder) Nothing


needDecoder : Decoder NeedData
needDecoder =
    Decode.succeed NeedData
        |> DP.required "jsonld"
            (Decode.oneOf
                [ Decode.when types (Set.member "won:Persona") personaDecoder
                    |> Decode.map PersonaNeed
                , otherDecoder
                    |> Decode.map Other
                ]
            )
        |> DP.required "creationDate" dateDecoder


guard : String -> Decoder a -> Decoder a
guard field decoder =
    Decode.when (Decode.field field Decode.bool) identity decoder


needStorageDecoder : Decoder (Maybe NeedStorage)
needStorageDecoder =
    Decode.oneOf
        [ Decode.succeed (Just <| Loading)
            |> guard "isLoading"
        , Decode.succeed Nothing
            |> guard "isBeingCreated"
        , needDecoder
            |> Decode.map (Just << Loaded)
        , Decode.succeed (Just FailedToLoad)
        ]


keyValueDecoder :
    (String -> Decoder key)
    -> Decoder value
    -> Decoder (List ( key, value ))
keyValueDecoder keyDecoder valDecoder =
    let
        decodePair ( maybeKey, val ) =
            keyDecoder maybeKey
                |> Decode.map (\key -> ( key, val ))
    in
    Decode.keyValuePairs valDecoder
        |> Decode.andThen
            (List.map decodePair
                >> Decode.combine
            )


needsDecoder : Decoder (Dict Id NeedStorage)
needsDecoder =
    let
        needFilter ( key, maybeVal ) =
            Maybe.map (\val -> ( key, val )) maybeVal
    in
    keyValueDecoder
        (\url ->
            Url.fromString url
                |> Maybe.map Url.toString
                |> Result.fromMaybe (url ++ " is not a valid url")
                |> Decode.fromResult
        )
        needStorageDecoder
        |> Decode.map (List.filterMap needFilter)
        |> Decode.map Dict.fromList


ownedDecoder : Decoder (Set Id)
ownedDecoder =
    let
        decodeUrl url =
            Url.fromString url
                |> Maybe.map Url.toString
                |> Result.fromMaybe (url ++ " is not a valid url")
                |> Decode.fromResult

        decodeIsOwned =
            Decode.field "isOwned" Decode.bool
                |> Decode.maybe
                |> Decode.map
                    (\isOwned ->
                        case isOwned of
                            Just True ->
                                Just ()

                            _ ->
                                Nothing
                    )

        convertToUrls ( url, isOwned ) =
            case isOwned of
                Just () ->
                    Just url

                Nothing ->
                    Nothing
    in
    keyValueDecoder
        decodeUrl
        decodeIsOwned
        |> Decode.map
            (\list ->
                List.filterMap convertToUrls list
                    |> Set.fromList
            )


stateDecoder : Decoder State
stateDecoder =
    Decode.succeed State
        |> DP.required "needs" needsDecoder
        |> DP.required "needs" ownedDecoder


updateStyle : Style -> { a | style : Style } -> { a | style : Style }
updateStyle style model =
    { model
        | style = style
    }


updateAttrs : attributes -> { a | attributes : attributes } -> { a | attributes : attributes }
updateAttrs attrs model =
    { model
        | attributes = attrs
    }


updateState : State -> { a | state : State } -> { a | state : State }
updateState state model =
    { model
        | state = state
    }


updateExternal :
    UpdateType attributes
    -> Model attributes subModel
    -> Model attributes subModel
updateExternal external model =
    case model of
        ParsingFailed _ ->
            model

        Model realModel ->
            case external of
                StateUpdate state ->
                    updateState state
                        realModel
                        |> Model

                AttrUpdate attrs ->
                    realModel
                        |> updateAttrs attrs
                        |> Model

                StyleUpdate style ->
                    realModel
                        |> updateStyle style
                        |> Model

                StyleState style state ->
                    realModel
                        |> updateStyle style
                        |> updateState state
                        |> Model

                StyleAttr style attrs ->
                    realModel
                        |> updateStyle style
                        |> updateAttrs attrs
                        |> Model

                AttrState attrs state ->
                    realModel
                        |> updateAttrs attrs
                        |> updateState state
                        |> Model

                StyleAttrState style attrs state ->
                    realModel
                        |> updateStyle style
                        |> updateAttrs attrs
                        |> updateState state
                        |> Model

                ParsingError message ->
                    ParsingFailed message


element :
    { view : attributes -> State -> subModel -> Element subMsg
    , update :
        attributes
        -> State
        -> subMsg
        -> subModel
        -> ( subModel, Cmd subMsg )
    , init : attributes -> State -> ( subModel, Cmd subMsg )
    , subscriptions : attributes -> State -> subModel -> Sub subMsg
    , attributeParser : Decoder attributes
    }
    ->
        Program
            { attributes : Value
            , style : Value
            , state : Value
            }
            (Model attributes subModel)
            (Msg attributes subMsg)
element options =
    let
        -- VIEW
        view model =
            case model of
                ParsingFailed _ ->
                    Html.text "Parsing the arguments failed, please look at the log for errors"

                Model { attributes, state, style, subModel } ->
                    Html.map SubMsg <|
                        Element.layoutWith
                            { options = [ Element.noStaticStyleSheet ] }
                            style
                            []
                            (options.view attributes state subModel)

        -- UPDATE
        update msg model =
            case model of
                ParsingFailed _ ->
                    ( model, Cmd.none )

                Model ({ attributes, state, subModel, style } as realModel) ->
                    case msg of
                        SubMsg subMsg ->
                            let
                                ( newModel, cmd ) =
                                    options.update attributes state subMsg subModel
                            in
                            ( Model
                                { realModel
                                    | subModel = newModel
                                }
                            , Cmd.map SubMsg cmd
                            )

                        ExternalUpdate extUpdate ->
                            ( updateExternal extUpdate model
                            , case extUpdate of
                                ParsingError message ->
                                    errorPort <| "Error on update:\n" ++ message

                                _ ->
                                    Cmd.none
                            )

        -- SUBSCRIPTIONS
        subscriptions model =
            case model of
                ParsingFailed _ ->
                    Sub.none

                Model { attributes, state, subModel } ->
                    Sub.batch
                        [ options.subscriptions attributes state subModel
                            |> Sub.map SubMsg
                        , inPort
                            (\val ->
                                let
                                    resultHandler newState newStyle newAttributes =
                                        case
                                            ( newState
                                            , newStyle
                                            , newAttributes
                                            )
                                        of
                                            ( Just st, Nothing, Nothing ) ->
                                                StateUpdate st

                                            ( Nothing, Just sty, Nothing ) ->
                                                StyleUpdate sty

                                            ( Nothing, Nothing, Just attr ) ->
                                                AttrUpdate attr

                                            ( Just st, Just sty, Nothing ) ->
                                                StyleState sty st

                                            ( Just st, Nothing, Just attr ) ->
                                                AttrState attr st

                                            ( Nothing, Just sty, Just attr ) ->
                                                StyleAttr sty attr

                                            ( Just st, Just sty, Just attr ) ->
                                                StyleAttrState sty attr st

                                            ( Nothing, Nothing, Nothing ) ->
                                                ParsingError "No valid input found"

                                    optional field dec =
                                        DP.optional field (Decode.map Just dec) Nothing

                                    decoder =
                                        Decode.succeed resultHandler
                                            |> optional "newState" stateDecoder
                                            |> optional "newStyle" Element.styleDecoder
                                            |> optional "newAttributes" options.attributeParser
                                in
                                Decode.decodeValue decoder val
                                    |> Result.extract
                                        (Decode.errorToString
                                            >> ParsingError
                                        )
                            )
                            |> Sub.map ExternalUpdate
                        ]

        -- INIT
        init { attributes, style, state } =
            Result.map3
                (\sty attr st ->
                    let
                        ( subModel, subCmd ) =
                            options.init attr st
                    in
                    ( Model
                        { state = st
                        , attributes = attr
                        , style = sty
                        , subModel = subModel
                        }
                    , Cmd.map SubMsg subCmd
                    )
                )
                (Decode.decodeValue Element.styleDecoder style)
                (Decode.decodeValue options.attributeParser attributes)
                (Decode.decodeValue stateDecoder state)
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
