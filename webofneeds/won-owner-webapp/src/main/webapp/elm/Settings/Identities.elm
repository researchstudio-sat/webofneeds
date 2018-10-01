module Settings.Identities exposing
    ( Identity
    , Model
    , Msg
    , init
    , update
    , view
    )

import Browser
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Element.Font as Font
import Element.Input as Input
import Elements
import Html exposing (Html, node)
import Html.Attributes as HA
import Skin exposing (Skin)
import String.Extra as String
import Validate exposing (Valid, Validator)


main =
    Browser.element
        { init = \() -> ( init, Cmd.none )
        , update = update
        , view = view
        , subscriptions = always Sub.none
        }



---- IDENTIY ----


type alias IdentityForm =
    { description : String
    , displayName : String
    , image : String
    , website : String
    , aboutMe : String
    }


type alias Identity =
    { description : Maybe String
    , displayName : String
    , image : Maybe String
    , website : Maybe String
    , aboutMe : Maybe String
    }


identityValidator : Validator String IdentityForm
identityValidator =
    Validate.all
        [ Validate.ifBlank .displayName "Please enter a display name."
        ]


toForm : Identity -> IdentityForm
toForm identity =
    { description = Maybe.withDefault "" identity.description
    , displayName = identity.displayName
    , image = Maybe.withDefault "" identity.image
    , website = Maybe.withDefault "" identity.website
    , aboutMe = Maybe.withDefault "" identity.aboutMe
    }


fromForm : Valid IdentityForm -> Identity
fromForm valid =
    let
        form =
            Validate.fromValid valid
    in
    { description = String.nonEmpty form.description
    , displayName = form.displayName
    , image = String.nonEmpty form.image
    , website = String.nonEmpty form.website
    , aboutMe = String.nonEmpty form.aboutMe
    }



---- MODEL ----


type alias Model =
    { identities : Dict Url Identity
    , editing : EditingModel
    , skin : Skin
    }


type alias EditingInfo =
    { url : Url
    , original : Identity
    , form : IdentityForm
    }


type EditingModel
    = NotEditing
    | Editing EditingInfo


type alias Url =
    String


type Msg
    = SelectIdentity Url
    | SaveIdentity
    | CancelEditing
    | EditIdentity IdentityForm


modified : EditingInfo -> Bool
modified { form, original } =
    toForm original /= form


mockIdentities : Dict String Identity
mockIdentities =
    Dict.empty
        |> Dict.insert "https://node.matchat.org/won/resource/need/kc9lvmo7sz0p"
            { description = Nothing
            , displayName = "Test"
            , image = Nothing
            , aboutMe = Nothing
            , website = Nothing
            }
        |> Dict.insert "https://node.matchat.org/won/resource/need/5kmlvmo7sz9x"
            { description = Just "my id"
            , displayName = "John"
            , image = Just "https://foxrudor.de/"
            , aboutMe = Nothing
            , website = Nothing
            }


init : Model
init =
    { identities = mockIdentities
    , editing = NotEditing
    , skin =
        { primaryColor = rgb255 240 70 70
        , lightGray = rgb255 240 242 244
        , lineGray = rgb255 203 210 209
        , subtitleGray = rgb255 128 128 128
        , black = rgb255 0 0 0
        }
    }



---- VIEW ----


view : Model -> Html Msg
view model =
    layout [] <|
        column
            [ centerX
            , spacing 10
            , width
                (fill
                    |> maximum 600
                )
            , padding 10
            ]
        <|
            [ viewIdentities model ]


viewIdentities : Model -> Element Msg
viewIdentities model =
    column
        [ width fill
        , spacing 10
        ]
    <|
        case model.editing of
            NotEditing ->
                Dict.map (identityCard model.skin) model.identities
                    |> Dict.values

            Editing info ->
                Dict.map
                    (\url identity ->
                        if url == info.url then
                            identityEditor model.skin info

                        else
                            identityCard model.skin url identity
                    )
                    model.identities
                    |> Dict.values


identityCard : Skin -> Url -> Identity -> Element Msg
identityCard skin url identity =
    card
        [ width fill
        , Events.onClick (SelectIdentity url)
        ]
        { skin = skin
        , header =
            row
                [ spacing 10
                , width fill
                ]
                [ identityImage skin url identity
                , column
                    [ height fill
                    ]
                    [ el [ Font.size 18 ] <|
                        case identity.description of
                            Just description ->
                                text description

                            Nothing ->
                                el [ Font.italic ] <| text "Unnamed Identity"
                    , el [ height fill ] none
                    , el
                        [ Font.color skin.subtitleGray
                        ]
                      <|
                        text ("Name: " ++ identity.displayName)
                    ]
                ]
        , sections = []
        }


identityImage : Skin -> Url -> Identity -> Element msg
identityImage skin url identity =
    el
        [ width (px 50)
        , height (px 50)
        , Background.color skin.lineGray
        ]
    <|
        case identity.image of
            Just img ->
                el
                    [ Background.uncropped img
                    , width fill
                    , height fill
                    ]
                    none

            Nothing ->
                identicon [] url


identicon : List (Attribute msg) -> String -> Element msg
identicon attributes string =
    el
        attributes
    <|
        html <|
            node "won-identicon"
                [ HA.attribute "data" string
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []


identityEditor : Skin -> EditingInfo -> Element Msg
identityEditor skin info =
    let
        form =
            info.form

        validated =
            Validate.validate identityValidator form

        ( isValid, errors ) =
            case validated of
                Ok _ ->
                    ( True, [] )

                Err err ->
                    ( False, err )
    in
    card
        [ width fill ]
        { skin = skin
        , header =
            row
                [ width fill
                , spacing 10
                ]
                [ identityImage skin info.url info.original
                , Input.text [ alignTop ]
                    { onChange = \str -> EditIdentity { form | description = str }
                    , text = form.description
                    , placeholder = Just (Input.placeholder [] <| text "Unnamed Identity")
                    , label =
                        Input.labelAbove
                            [ width (px 0)
                            , height (px 0)
                            , htmlAttribute (HA.style "display" "none")
                            ]
                            (text "Display Name")
                    }
                ]
        , sections =
            [ identityForm form
            , column
                [ width fill
                , spacing 10
                ]
                [ if isValid then
                    none

                  else
                    column
                        [ Font.color skin.primaryColor
                        ]
                        (List.map
                            text
                            errors
                        )
                , row
                    [ spacing 10
                    , width fill
                    ]
                    [ Elements.mainButton
                        { disabled = not isValid || not (modified info)
                        , onClick = SaveIdentity
                        , text = "Save"
                        }
                    , Elements.outlinedButton
                        { disabled = False
                        , onClick = CancelEditing
                        , text = "Cancel"
                        }
                    ]
                ]
            ]
        }


identityForm : IdentityForm -> Element Msg
identityForm form =
    column
        [ spacing 10
        , width fill
        ]
        [ Input.text []
            { onChange = \str -> EditIdentity { form | displayName = str }
            , text = form.displayName
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "Display Name")
            }
        , Input.text []
            { onChange = \str -> EditIdentity { form | website = str }
            , text = form.website
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "Website")
            }
        , Input.multiline []
            { onChange = \str -> EditIdentity { form | aboutMe = str }
            , text = form.aboutMe
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "About Me")
            , spellcheck = True
            }
        ]



---- UPDATE ----


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    ( case msg of
        SelectIdentity url ->
            { model | editing = selectIdentity url model }

        CancelEditing ->
            { model | editing = NotEditing }

        EditIdentity newForm ->
            editIdentity newForm model

        SaveIdentity ->
            case model.editing of
                Editing info ->
                    saveIdentity info model

                NotEditing ->
                    model
    , Cmd.none
    )


selectIdentity : Url -> Model -> EditingModel
selectIdentity url model =
    let
        edited =
            case model.editing of
                Editing info ->
                    modified info

                NotEditing ->
                    False
    in
    if not edited then
        case Dict.get url model.identities of
            Just identity ->
                Editing
                    { url = url
                    , original = identity
                    , form = toForm identity
                    }

            Nothing ->
                model.editing

    else
        model.editing


editIdentity : IdentityForm -> Model -> Model
editIdentity form model =
    case model.editing of
        Editing info ->
            { model
                | editing =
                    Editing { info | form = form }
            }

        NotEditing ->
            model


saveIdentity : EditingInfo -> Model -> Model
saveIdentity info model =
    case Validate.validate identityValidator info.form of
        Ok valid ->
            { model
                | identities = Dict.insert info.url (fromForm valid) model.identities
                , editing = NotEditing
            }

        Err _ ->
            model



---- ELEMENTS ----


card :
    List (Attribute msg)
    ->
        { skin : Skin
        , header : Element msg
        , sections : List (Element msg)
        }
    -> Element msg
card attributes { skin, header, sections } =
    let
        baseStyle =
            [ Border.width 1
            , Border.color skin.lineGray
            , padding 10
            , width fill
            ]
    in
    column
        (attributes
            ++ [ spacing -1 ]
        )
        ([ el
            (baseStyle
                ++ [ Background.color skin.lightGray ]
            )
            header
         ]
            ++ List.map
                (\section ->
                    el baseStyle section
                )
                sections
        )


svgIcon :
    List (Attribute msg)
    ->
        { color : Color
        , name : String
        }
    -> Element msg
svgIcon attributes { color, name } =
    el attributes <|
        html <|
            node "svg-icon"
                [ HA.attribute "icon" name
                , HA.attribute "color" (Skin.cssColor color)
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []
