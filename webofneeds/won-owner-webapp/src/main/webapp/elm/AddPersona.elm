module AddPersona exposing (main)

import Application exposing (Style)
import Html exposing (..)
import Html.Attributes as Attributes
import Html.Events as Events
import Json.Decode as Decode exposing (Decoder)
import Json.Decode.Extra as Decode
import Maybe.Extra as Maybe
import Palette
import Persona exposing (Persona)
import Time



---- MODEL ----


type alias Id =
    String


type Model
    = Selecting (Maybe Id)
    | AddingPersona Id



---- PROPS ----


type alias Props =
    { postUri : Id
    , personas : List Persona
    }


propDecoder : Decoder Props
propDecoder =
    Decode.map2 Props
        (Decode.at [ "post", "uri" ] Decode.string)
        (Decode.field "personas" <| Decode.list Persona.decoder)



---- INIT ----


init : Props -> ( Model, Cmd Msg )
init _ =
    ( Selecting Nothing
    , Cmd.none
    )



---- UPDATE ----


type Msg
    = SelectPersona Id
    | AddPersona


selectedPersona : List Persona -> Model -> Maybe Persona
selectedPersona personas model =
    let
        getPersona id =
            personas
                |> List.filter (\persona -> persona.uri == id)
                |> List.head
    in
    case model of
        Selecting id ->
            id |> Maybe.andThen getPersona

        AddingPersona id ->
            getPersona id


update :
    Msg
    ->
        { model : Model
        , props : Props
        }
    -> ( Model, Cmd Msg )
update msg { model, props } =
    case msg of
        AddPersona ->
            case model of
                AddingPersona _ ->
                    ( model, Cmd.none )

                Selecting Nothing ->
                    ( model, Cmd.none )

                Selecting (Just id) ->
                    case selectedPersona props.personas model of
                        Just persona ->
                            ( AddingPersona id
                            , Persona.connect
                                { persona = persona
                                , needUrl = props.postUri
                                }
                            )

                        Nothing ->
                            ( model, Cmd.none )

        SelectPersona id ->
            case model of
                AddingPersona _ ->
                    ( model, Cmd.none )

                Selecting _ ->
                    ( Selecting (Just id), Cmd.none )



---- VIEW ----


view :
    { style : Style
    , model : Model
    , props : Props
    }
    -> Html Msg
view { model, props } =
    let
        saving =
            case model of
                AddingPersona _ ->
                    True

                _ ->
                    False

        selected =
            selectedPersona props.personas model
                |> Maybe.isJust
    in
    div
        [ Attributes.classList
            [ ( "won-add-persona", True )
            , ( "saving", saving )
            ]
        ]
        ([ h1 [] [ text "Add a persona to your post" ]
         ]
            ++ (if List.isEmpty props.personas then
                    [ div [] [ text "You have no personas yet" ]
                    , a
                        [ Attributes.href "#!/settings"
                        , Attributes.class "won-button--filled"
                        , Attributes.class "red"
                        ]
                        [ text "Create a Persona" ]
                    ]

                else
                    [ personaList props.personas model
                    , Palette.wonButton
                        [ Attributes.disabled (not selected)
                        , Events.onClick AddPersona
                        , Attributes.class "add-persona-button"
                        ]
                        [ if saving then
                            text "Saving..."

                          else
                            text "Add"
                        ]
                    ]
               )
        )


personaSelected : Model -> Persona -> Bool
personaSelected model persona =
    case model of
        Selecting (Just id) ->
            id == persona.uri

        AddingPersona id ->
            id == persona.uri

        Selecting Nothing ->
            False


personaList : List Persona -> Model -> Html Msg
personaList personas model =
    ul [ Attributes.class "won-persona-list" ]
        (personas
            |> List.sortBy (.name >> String.toLower)
            |> List.reverse
            |> List.map (personaView model)
        )


personaView : Model -> Persona -> Html Msg
personaView model persona =
    let
        isSelected =
            personaSelected model persona
    in
    Persona.inlineView
        [ Events.onClick (SelectPersona persona.uri)
        , Attributes.classList
            [ ( "selected", isSelected )
            ]
        ]
        persona



---- MAIN ----


main =
    Application.element
        { init = init
        , update = update
        , view = view
        , subscriptions = \_ -> Sub.none
        , propDecoder = propDecoder
        }
