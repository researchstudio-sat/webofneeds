module Elements exposing
    ( card
    , mainButton
    , outlinedButton
    , svgIcon
    )

import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Html exposing (node)
import Html.Attributes as HA
import Skin exposing (Skin)


type alias Card msg =
    { skin : Skin
    , header : Element msg
    , sections : List (Element msg)
    }


card : List (Attribute msg) -> Card msg -> Element msg
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


type alias SvgIcon =
    { color : Color
    , name : String
    }


svgIcon : List (Attribute msg) -> SvgIcon -> Element msg
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


type alias Button msg =
    { disabled : Bool
    , text : String
    , onClick : msg
    }


mainButton : Button msg -> Element msg
mainButton { disabled, text, onClick } =
    el [ Events.onClick onClick ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--filled", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]


outlinedButton : Button msg -> Element msg
outlinedButton { disabled, text, onClick } =
    el [ Events.onClick onClick ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--outlined", True )
                    , ( "thin", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]
