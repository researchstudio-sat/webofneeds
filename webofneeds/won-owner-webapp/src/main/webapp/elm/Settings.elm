module Settings exposing (main)

import Browser
import Browser.Dom
import Browser.Events
import Element exposing (..)
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


type DisplayType
    = Mobile
    | Desktop


type Navigation
    = Identities Identities.Model


type Model
    = Wide
        { skin : Skin
        , navigation : Navigation
        }
    | Narrow
        { skin : Skin
        , navigation : Maybe Navigation
        }


init : () -> ( Model, Cmd Msg )
init () =
    ( Narrow
        { skin =
            { primaryColor = rgb255 240 70 70
            , lightGray = rgb255 240 242 244
            , lineGray = rgb255 203 210 209
            , subtitleGray = rgb255 128 128 128
            , black = rgb255 0 0 0
            }
        , navigation = Nothing
        }
    , Task.perform
        (\{ viewport } ->
            classifyDevice
                (round viewport.width)
                (round viewport.height)
                |> ScreenResized
        )
        Browser.Dom.getViewport
    )



--
-- View
--


classifyDevice : Int -> Int -> DisplayType
classifyDevice w h =
    if w > 560 then
        Desktop

    else
        Mobile


type alias Category =
    { icon : String
    , skin : Skin
    , name : String
    , active : Bool
    }


category : Category -> Element msg
category { icon, skin, name, active } =
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
        ]
        [ Elements.svgIcon
            [ height (px 18)
            , width (px 18)
            ]
            { name = icon
            , color = color
            }
        , text name
        ]


sidebar : Skin -> Element msg
sidebar skin =
    column
        [ width shrink
        , alignTop
        ]
        [ category { active = True, icon = "ico36_person", name = "Identities", skin = skin } ]


view : Model -> Html Msg
view model =
    case model of
        Wide wideModel ->
            layout [] <|
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
                    [ sidebar wideModel.skin
                    , case wideModel.navigation of
                        Identities identitiesModel ->
                            Identities.view wideModel.skin identitiesModel
                                |> map (SubMessage << MessageFromIdentities)
                    ]

        Narrow narrowModel ->
            layout [] <|
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
                    [ case narrowModel.navigation of
                        Just navigation ->
                            case navigation of
                                Identities identitiesModel ->
                                    Identities.view narrowModel.skin identitiesModel
                                        |> map (SubMessage << MessageFromIdentities)

                        Nothing ->
                            sidebar narrowModel.skin
                    ]



--
-- Update
--


type SubMessage
    = MessageFromIdentities Identities.Msg


type Msg
    = SubMessage SubMessage
    | ScreenResized DisplayType
    | SkinChanged Skin


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model ) of
        ( SubMessage subMsg, Wide wideModel ) ->
            ( Wide
                { wideModel
                    | navigation =
                        updateNavigation subMsg wideModel.navigation
                }
            , Cmd.none
            )

        ( SubMessage subMsg, Narrow narrowModel ) ->
            ( Narrow
                { narrowModel
                    | navigation = Maybe.map (updateNavigation subMsg) narrowModel.navigation
                }
            , Cmd.none
            )

        ( ScreenResized Desktop, Narrow narrowModel ) ->
            ( Wide
                { skin = narrowModel.skin
                , navigation = Maybe.withDefault (Identities Identities.init) narrowModel.navigation
                }
            , Cmd.none
            )

        ( ScreenResized Mobile, Wide wideModel ) ->
            ( Narrow
                { skin = wideModel.skin
                , navigation = Just wideModel.navigation
                }
            , Cmd.none
            )

        ( SkinChanged skin, Wide wideModel ) ->
            ( Wide
                { wideModel
                    | skin = skin
                }
            , Cmd.none
            )

        ( SkinChanged skin, Narrow narrowModel ) ->
            ( Narrow
                { narrowModel
                    | skin = skin
                }
            , Cmd.none
            )

        _ ->
            ( model, Cmd.none )


updateNavigation : SubMessage -> Navigation -> Navigation
updateNavigation msg navigation =
    case ( msg, navigation ) of
        ( MessageFromIdentities identitiesMsg, Identities model ) ->
            Identities (Identities.update identitiesMsg model)


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Browser.Events.onResize
            (\w h ->
                ScreenResized <| classifyDevice w h
            )
        ]
