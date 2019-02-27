module EditNeed exposing (main)

import Actions
import Application
    exposing
        ( Id
        , Need(..)
        , NeedData
        , NeedStorage(..)
        , Persona
        , State
        , ownedNeeds
        )
import Browser
import Dict exposing (Dict)
import Dict.Extra as Dict
import Element.Input.Styled as Input
import Element.Styled as Element exposing (Element, fill, none)
import Json.Decode as JD exposing (Value)
import NonEmpty
import Palette exposing (..)
import Url exposing (Url)


type alias ActiveUrl =
    Id


type alias Model =
    { dropdownOpen : Bool
    }


type Msg
    = ToggleDropdown
    | SelectPersona Id
    | RemovePersona


getNeed : State -> Id -> Maybe Need
getNeed state url =
    case Dict.get url state.needs of
        Just (Loaded needData) ->
            Just needData.need

        _ ->
            Nothing


getHolder : State -> ActiveUrl -> Maybe Id
getHolder state activeNeed =
    getNeed state activeNeed
        |> Maybe.map
            (\need ->
                case need of
                    Other { holder } ->
                        case holder of
                            Just id ->
                                Just id

                            Nothing ->
                                Nothing

                    _ ->
                        Nothing
            )
        |> Maybe.withDefault Nothing


hasPersona : State -> ActiveUrl -> Bool
hasPersona state activeNeed =
    case getHolder state activeNeed of
        Just _ ->
            True

        Nothing ->
            False


getOwnedPersonas : State -> Dict Id Persona
getOwnedPersonas state =
    let
        filter url needStorage =
            case needStorage of
                Loaded { need } ->
                    case need of
                        PersonaNeed persona ->
                            Just persona

                        _ ->
                            Nothing

                _ ->
                    Nothing
    in
    ownedNeeds state
        |> Dict.filterMap filter


init : ActiveUrl -> State -> ( Model, Cmd Msg )
init attrs state =
    ( { dropdownOpen = False
      }
    , Cmd.none
    )


update : ActiveUrl -> State -> Msg -> Model -> ( Model, Cmd Msg )
update activeUrl state msg model =
    let
        dbg =
            Debug.log "state" state
    in
    case msg of
        ToggleDropdown ->
            ( { model
                | dropdownOpen = not model.dropdownOpen
              }
            , Cmd.none
            )

        SelectPersona personaUrl ->
            ( { model
                | dropdownOpen = False
              }
            , Actions.connectPersona
                { personaUrl = personaUrl
                , needUrl = activeUrl
                }
            )

        RemovePersona ->
            case getHolder state activeUrl of
                Just personaUrl ->
                    ( model
                    , Actions.disconnectPersona
                        { personaUrl = personaUrl
                        , needUrl = activeUrl
                        }
                    )

                Nothing ->
                    ( model, Cmd.none )


view : ActiveUrl -> State -> Model -> Element Msg
view activeUrl state model =
    let
        dbg =
            Debug.log "viewstate" state
    in
    if hasPersona state activeUrl then
        button
            { label = "Remove Persona"
            , onPress = Just RemovePersona
            }

    else
        dropdown
            { direction = DropUp
            , label = "Add Persona"
            , menu =
                if model.dropdownOpen then
                    Just <|
                        Element.column [ Element.width fill ]
                            (getOwnedPersonas state
                                |> Dict.map
                                    (\url persona ->
                                        Input.button [ Element.width fill ]
                                            { onPress = Just (SelectPersona url)
                                            , label =
                                                Element.row [ Element.spacing 3 ]
                                                    [ identicon 3 url
                                                    , Element.text <| NonEmpty.get persona.name
                                                    ]
                                            }
                                    )
                                |> Dict.values
                            )

                else
                    Nothing
            , onToggle =
                if Dict.isEmpty (getOwnedPersonas state) then
                    Nothing

                else
                    Just ToggleDropdown
            }


attributeParser : JD.Decoder ActiveUrl
attributeParser =
    Application.urlDecoder


main =
    Application.element
        { init = init
        , update = update
        , view = view
        , subscriptions = \_ _ _ -> Sub.none
        , attributeParser = attributeParser
        }
