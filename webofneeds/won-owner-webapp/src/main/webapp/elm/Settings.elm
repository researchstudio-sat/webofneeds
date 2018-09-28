module Settings exposing (main)

import Browser
import Browser.Dom
import Browser.Events
import Element exposing (..)
import Element.Events as Events
import Element.Font as Font
import Elements
import Html exposing (Html, node)
import Settings.Identities as Identities
import Skin exposing (Skin)
import Task


main =
    Browser.element
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }



--
-- Model
--


type ViewMode
    = Narrow (Maybe Page)
    | Wide Page


type Page
    = IdentitiesPage Identities.Model


type alias Model =
    { skin : Skin
    , viewMode : ViewMode
    }


init : () -> ( Model, Cmd Msg )
init () =
    ( { skin =
            { primaryColor = rgb255 240 70 70
            , lightGray = rgb255 240 242 244
            , lineGray = rgb255 203 210 209
            , subtitleGray = rgb255 128 128 128
            , black = rgb255 0 0 0
            }
      , viewMode = Narrow Nothing
      }
    , Task.perform
        (\{ viewport } ->
            ScreenResized
                { width = round viewport.width
                , height = round viewport.height
                }
        )
        Browser.Dom.getViewport
    )



--
-- View
--


view : Model -> Html Msg
view model =
    layout [] <|
        case model.viewMode of
            Wide page ->
                row
                    [ centerX
                    , width
                        (fill
                            |> maximum 1000
                        )
                    , padding 20
                    , spacing 20
                    , Font.size 12
                    ]
                    [ sidebar
                        { route = Just <| toRoute page
                        , skin = model.skin
                        }
                    , case page of
                        IdentitiesPage identititesModel ->
                            Identities.view model.skin identititesModel
                                |> map (PageMessage << IdentitiesMsg)
                    ]

            Narrow (Just page) ->
                column
                    [ width fill
                    , padding 20
                    , spacing 20
                    , Font.size 12
                    ]
                    [ backButton model.skin
                    , case page of
                        IdentitiesPage identititesModel ->
                            Identities.view model.skin identititesModel
                                |> map (PageMessage << IdentitiesMsg)
                    ]

            Narrow Nothing ->
                column
                    [ width fill
                    , padding 20
                    , spacing 20
                    , Font.size 12
                    ]
                    [ sidebar
                        { route = Nothing
                        , skin = model.skin
                        }
                    ]


backButton : Skin -> Element Msg
backButton skin =
    row
        [ width fill
        , spacing 10
        , Font.size 20
        , Font.color skin.primaryColor
        , Events.onClick GoBack
        ]
        [ Elements.svgIcon
            [ height (px 20)
            , width (px 20)
            ]
            { name = "ico36_backarrow"
            , color = skin.primaryColor
            }
        , text "Back"
        ]


type alias Category =
    { icon : String
    , skin : Skin
    , active : Bool
    , route : Route
    }


category : Category -> Element Msg
category { icon, skin, active, route } =
    let
        color =
            if active then
                skin.primaryColor

            else
                skin.black
    in
    row
        [ spacing 10
        , Font.color color
        , Font.size 18
        , Events.onClick (ChangeRoute route)
        ]
        [ Elements.svgIcon
            [ height (px 18)
            , width (px 18)
            ]
            { name = icon
            , color = color
            }
        , text (routeName route)
        ]


sidebar :
    { route : Maybe Route
    , skin : Skin
    }
    -> Element Msg
sidebar { route, skin } =
    column
        [ width shrink
        , alignTop
        ]
        [ category
            { active = route == Just Identities
            , icon = "ico36_person"
            , route = Identities
            , skin = skin
            }
        ]



--
-- Update
--


type Route
    = Identities


toRoute : Page -> Route
toRoute page =
    case page of
        IdentitiesPage _ ->
            Identities


routeName : Route -> String
routeName route =
    case route of
        Identities ->
            "Identities"


type PageMessage
    = IdentitiesMsg Identities.Msg


type Msg
    = PageMessage PageMessage
    | ScreenResized DisplaySize
    | SkinChanged Skin
    | ChangeRoute Route
    | GoBack


type alias DisplaySize =
    { width : Int
    , height : Int
    }


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        PageMessage pMsg ->
            ( { model
                | viewMode = mapViewMode (updatePage pMsg) model.viewMode
              }
            , Cmd.none
            )

        ScreenResized { width } ->
            ( { model
                | viewMode =
                    case ( width > 560, model.viewMode ) of
                        ( True, Narrow Nothing ) ->
                            Wide <| changeRoute Identities

                        ( True, Narrow (Just page) ) ->
                            Wide page

                        ( True, Wide _ ) ->
                            model.viewMode

                        ( False, Wide page ) ->
                            Narrow (Just page)

                        ( False, Narrow _ ) ->
                            model.viewMode
              }
            , Cmd.none
            )

        SkinChanged skin ->
            ( { model
                | skin = skin
              }
            , Cmd.none
            )

        ChangeRoute newRoute ->
            ( { model
                | viewMode =
                    case model.viewMode of
                        Narrow (Just page) ->
                            if toRoute page /= newRoute then
                                Narrow <| Just (changeRoute newRoute)

                            else
                                model.viewMode

                        Narrow Nothing ->
                            Narrow <| Just (changeRoute newRoute)

                        Wide page ->
                            if toRoute page /= newRoute then
                                Wide <| changeRoute newRoute

                            else
                                model.viewMode
              }
            , Cmd.none
            )

        GoBack ->
            ( { model
                | viewMode =
                    case model.viewMode of
                        Narrow _ ->
                            Narrow Nothing

                        Wide _ ->
                            model.viewMode
              }
            , Cmd.none
            )


mapViewMode : (Page -> Page) -> ViewMode -> ViewMode
mapViewMode f viewMode =
    case viewMode of
        Wide page ->
            Wide (f page)

        Narrow maybePage ->
            Narrow <| Maybe.map f maybePage


changeRoute : Route -> Page
changeRoute route =
    case route of
        Identities ->
            IdentitiesPage Identities.init


updatePage : PageMessage -> Page -> Page
updatePage pageMsg page =
    case ( pageMsg, page ) of
        ( IdentitiesMsg msg, IdentitiesPage model ) ->
            IdentitiesPage <| Identities.update msg model


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Browser.Events.onResize
            (\width height ->
                ScreenResized
                    { width = width
                    , height = height
                    }
            )
        ]
