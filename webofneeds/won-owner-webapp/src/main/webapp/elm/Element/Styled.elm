module Element.Styled exposing
    ( Attr
    , Attribute
    , Color
    , Element
    , above
    , below
    , column
    , el
    , element
    , fill
    , getAttrList
    , getElement
    , height
    , layout
    , modular
    , none
    , padding
    , pureAttr
    , px
    , row
    , spacing
    , text
    , width
    , withSkin
    )

import Element
import Html exposing (Html)
import Skin exposing (Skin)


type Element msg
    = Element (Skin -> Element.Element msg)


type alias Attribute msg =
    Attr () msg


type Attr decorative msg
    = Attr (Skin -> Element.Attr decorative msg)


type alias Color =
    Element.Color


type alias Length =
    Element.Length


fill : Length
fill =
    Element.fill


px : Int -> Length
px len =
    Element.px len


width : Length -> Attribute msg
width len =
    pureAttr <| Element.width len


height : Length -> Attribute msg
height len =
    pureAttr <| Element.height len


padding : Int -> Attribute msg
padding len =
    pureAttr <| Element.padding len


spacing : Int -> Attribute msg
spacing len =
    pureAttr <| Element.spacing len


text : String -> Element msg
text str =
    Element <|
        \_ -> Element.text str


applyAttr skin (Attr fn) =
    fn skin


applyEl skin (Element fn) =
    fn skin


modular : Float -> Float -> Int -> Float
modular =
    Element.modular


layout : Skin -> List (Attribute msg) -> Element msg -> Html msg
layout skin attrs elem =
    Element.layout
        (List.map
            (applyAttr skin)
            attrs
        )
        (applyEl skin elem)


withSkin : (Skin -> Element msg) -> Element msg
withSkin elemFn =
    Element <|
        \skin ->
            let
                (Element fn) =
                    elemFn skin
            in
            fn skin


row : List (Attribute msg) -> List (Element msg) -> Element msg
row attrs elements =
    Element <|
        \skin ->
            Element.row
                (List.map (applyAttr skin) attrs)
                (List.map (applyEl skin) elements)


column : List (Attribute msg) -> List (Element msg) -> Element msg
column attrs elements =
    Element <|
        \skin ->
            Element.column
                (List.map (applyAttr skin) attrs)
                (List.map (applyEl skin) elements)


above : Element msg -> Attribute msg
above elem =
    Attr
        (\skin ->
            Element.above <|
                applyEl skin elem
        )


below : Element msg -> Attribute msg
below elem =
    Attr
        (\skin ->
            Element.below <|
                applyEl skin elem
        )


el : List (Attribute msg) -> Element msg -> Element msg
el attrs elem =
    Element <|
        \skin ->
            Element.el
                (List.map (applyAttr skin) attrs)
                (applyEl skin elem)


pureAttr : Element.Attr decorative msg -> Attr decorative msg
pureAttr attr =
    Attr (\_ -> attr)


element : (Skin -> Element.Element msg) -> Element msg
element fn =
    Element fn


getElement :
    Element msg
    -> (Skin -> Element.Element msg -> b)
    -> Skin
    -> b
getElement elem fn skin =
    fn skin (applyEl skin elem)


getAttrList :
    List (Attr decorative msg)
    -> (Skin -> List (Element.Attr decorative msg) -> b)
    -> Skin
    -> b
getAttrList attrs fn skin =
    fn skin (List.map (applyAttr skin) attrs)


none : Element msg
none =
    Element (\_ -> Element.none)
