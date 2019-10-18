port module Old.Persona exposing
    ( Persona
    , PersonaData
    , SaveState(..)
    , data
    , getNewPersonas
    , savePersona
    , saved
    , subscription
    , url
    )

import Dict exposing (Dict)
import Json.Decode as Decode exposing (..)
import Json.Decode.Extra as Decode
import Json.Encode as Encode
import NonEmpty exposing (NonEmpty)
import Time
import Url exposing (Url)
import Debug exposing (log)


type SaveState
    = Saved Time.Posix
    | Unsaved


type Persona
    = Persona
        { url : Url
        , data : PersonaData
        , saved : SaveState
        }

data : Persona -> PersonaData
data (Persona persona) =
    persona.data


url : Persona -> Url
url (Persona persona) =
    persona.url


saved : Persona -> SaveState
saved (Persona persona) =
    persona.saved


type alias PersonaData =
    { displayName : NonEmpty String
    , aboutMe : Maybe (NonEmpty String)
    , website : Maybe (NonEmpty String)
    }



---- PORTS ----


subscription : (Dict String Persona -> msg) -> (Decode.Error -> msg) -> Sub msg
subscription tag errorTag =
    personaIn
        (\value ->
            log "in personaIn: " <|
            case decodeValue listDecoder value of
                Ok list ->
                    tag list

                Err error ->
                    errorTag error
        )


savePersona : PersonaData -> Cmd msg
savePersona data_ =
    personaOut <| encodeData data_


getNewPersonas : Cmd msg
getNewPersonas =
    updatePersonas ()


port personaIn : (Value -> msg) -> Sub msg


port personaOut : Value -> Cmd msg


port updatePersonas : () -> Cmd msg



---- DECODERS ----


listDecoder : Decoder (Dict String Persona)
listDecoder =
    list personaDecoder
        |> map
            (\personaList ->
                personaList
                    |> List.map
                        (\((Persona persona) as originalPersona) ->
                            ( Url.toString persona.url, originalPersona )
                        )
                    |> Dict.fromList
            )


personaDecoder : Decoder Persona
personaDecoder =
    map3
        (\url_ data_ saved_ ->
            Persona
                { url = url_
                , data = data_
                , saved = saved_
                }
        )
        (field "url" urlDecoder)
        dataDecoder
        savedDecoder


dataDecoder : Decoder PersonaData
dataDecoder =
    map3 PersonaData
        (field "displayName" NonEmpty.stringDecoder)
        (maybe <| field "aboutMe" NonEmpty.stringDecoder)
        (maybe <| field "website" NonEmpty.stringDecoder)


urlDecoder : Decoder Url
urlDecoder =
    string
        |> andThen
            (\string ->
                case Url.fromString string of
                    Just url_ ->
                        succeed url_

                    Nothing ->
                        fail "Not a valid url"
            )


savedDecoder : Decoder SaveState
savedDecoder =
    field "saved" bool
        |> andThen
            (\saved_ ->
                if saved_ then
                    field "timestamp" Decode.datetime
                        |> map Saved

                else
                    succeed Unsaved
            )



---- ENCODERS ----


encodeData : PersonaData -> Value
encodeData data_ =
    let
        optionalFields =
            List.filterMap
                (\( key, maybeValue ) ->
                    Maybe.map (\value -> ( key, value )) maybeValue
                )

        nonEmptyString =
            NonEmpty.get >> Encode.string
    in
    Encode.object <|
        optionalFields
            [ ( "displayName"
              , data_.displayName
                    |> nonEmptyString
                    |> Just
              )
            , ( "website", Maybe.map nonEmptyString data_.website )
            , ( "aboutMe", Maybe.map nonEmptyString data_.aboutMe )
            ]
