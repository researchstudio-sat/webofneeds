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
    = Wide
    | Narrow


classifyViewMode : Int -> ViewMode
classifyViewMode width =
    if width > 560 then
        Wide

    else
        Narrow


type View
    = Page ViewMode Page
    | Overview


type Page
    = IdentitiesPage Identities.Model


type alias Model =
    { skin : Skin
    , view : View
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
      , view = Overview
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
        case model.view of
            Page Wide page ->
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

            Page Narrow page ->
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

            Overview ->
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
                | view = mapView (updatePage pMsg) model.view
              }
            , Cmd.none
            )

        ScreenResized { width } ->
            ( { model
                | view =
                    case ( classifyViewMode width, model.view ) of
                        ( Wide, Overview ) ->
                            Page Wide <| changeRoute Identities

                        ( Wide, Page Narrow page ) ->
                            Page Wide page

                        ( Wide, Page Wide _ ) ->
                            model.view

                        ( Narrow, Overview ) ->
                            Overview

                        ( Narrow, Page Wide page ) ->
                            Page Narrow page

                        ( Narrow, Page Narrow _ ) ->
                            model.view
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
                | view =
                    case model.view of
                        Page width page ->
                            if toRoute page /= newRoute then
                                Page width (changeRoute newRoute)

                            else
                                model.view

                        Overview ->
                            Page Narrow (changeRoute newRoute)
              }
            , Cmd.none
            )

        GoBack ->
            ( { model
                | view =
                    case model.view of
                        Page Narrow _ ->
                            Overview

                        Overview ->
                            Overview

                        Page Wide _ ->
                            model.view
              }
            , Cmd.none
            )


mapView : (Page -> Page) -> View -> View
mapView f oldView =
    case oldView of
        Overview ->
            Overview

        Page width page ->
            Page width (f page)


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
