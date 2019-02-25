module EditNeed exposing (main)

import Actions
import Application
    exposing
        ( Need(..)
        , NeedData
        , NeedStorage(..)
        , Persona
        , State
        , ownedNeeds
        )
import AssocList as Dict exposing (Dict)
import AssocList.Extra as Dict
import Browser
import Element.Input.Styled as Input
import Element.Styled as Element exposing (Element, fill, none)
import Json.Decode as JD exposing (Value)
import NonEmpty
import Palette exposing (..)
import Url exposing (Url)


type alias ActiveUrl =
    Url


type alias Model =
    { dropdownOpen : Bool
    }


type Msg
    = ToggleDropdown
    | SelectPersona Url
    | RemovePersona


getNeed : State -> Url -> Maybe Need
getNeed state url =
    case Dict.get url state.needs of
        Just (Loaded needData) ->
            Just needData.need

        _ ->
            Nothing


hasPersona : State -> ActiveUrl -> Bool
hasPersona state activeNeed =
    getNeed state activeNeed
        |> Maybe.map
            (\need ->
                case Debug.log "need" need of
                    Other { holder } ->
                        case holder of
                            Just _ ->
                                True

                            Nothing ->
                                False

                    _ ->
                        False
            )
        |> Maybe.withDefault False


getOwnedPersonas : State -> Dict Url Persona
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
                                            , label = Element.text <| NonEmpty.get persona.name
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
