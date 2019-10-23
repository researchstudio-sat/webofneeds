module Settings exposing (main)

import Browser.Events exposing (onResize)
import Element exposing (..)
import Element.Background as Background
import Element.Font as Font
import Element.Input as Input
import Elements
import Html exposing (Html)
import Old.Skin as Skin exposing (Skin)
import Settings.Account as Account
import Settings.Export as Export


main =
    Skin.skinnedElement
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }



---- MODEL ----


type DeviceClass
    = Mobile
    | Desktop


classifyDevice : { window | width : Int, height : Int } -> DeviceClass
classifyDevice { width } =
    if width > 600 then
        Desktop

    else
        Mobile


type alias Size =
    { width : Int
    , height : Int
    }


type alias Model =
    { page : Page
    , menuOpen : Bool
    , size : Size
    }


type Page
    = Export Export.Model
    | Account Account.Model


type Route
    = ExportR
    | AccountR


toRoute : Page -> Route
toRoute page =
    case page of
        Export _ ->
            ExportR

        Account _ ->
            AccountR


init : { width : Int, height : Int } -> ( Model, Cmd Msg )
init size =
    let
        ( model, cmd ) =
            Account.init ()
    in
    ( { size = size
      , page = Account model
      , menuOpen = True
      }
    , Cmd.map AccountMsg cmd
    )


subInit : (model -> Page) -> (msg -> Msg) -> ( model, Cmd msg ) -> ( Page, Cmd Msg )
subInit toPage toMsg ( subModel, cmd ) =
    ( toPage subModel
    , Cmd.map toMsg cmd
    )



---- UPDATE ----


type Msg
    = ExportMsg Export.Msg
    | AccountMsg Account.Msg
    | ChangeRoute Route
    | Resized Size
    | Back


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model.page ) of
        ( ExportMsg subMsg, Export subModel ) ->
            Export.update subMsg subModel
                |> updateWith Export ExportMsg model

        ( AccountMsg subMsg, Account subModel ) ->
            Account.update subMsg subModel
                |> updateWith Account AccountMsg model

        ( ChangeRoute newRoute, _ ) ->
            if toRoute model.page == newRoute then
                ( { model
                    | menuOpen = False
                  }
                , Cmd.none
                )

            else
                changeRoute newRoute model

        ( Resized size, _ ) ->
            ( { model
                | size = size
              }
            , Cmd.none
            )

        ( Back, _ ) ->
            ( { model
                | menuOpen = True
              }
            , Cmd.none
            )

        ( _, _ ) ->
            ( model, Cmd.none )


changeRoute : Route -> Model -> ( Model, Cmd Msg )
changeRoute route model =
    let
        ( newPage, cmd ) =
            case route of
                ExportR ->
                    subInit Export ExportMsg (Export.init ())

                AccountR ->
                    subInit Account AccountMsg (Account.init ())
    in
    ( { model
        | page = newPage
        , menuOpen = False
      }
    , cmd
    )


updateWith : (subModel -> Page) -> (subMsg -> Msg) -> Model -> ( subModel, Cmd subMsg ) -> ( Model, Cmd Msg )
updateWith toModel toMsg model ( subModel, subCmd ) =
    ( { model
        | page = toModel subModel
      }
    , Cmd.map toMsg subCmd
    )



---- VIEW ----


view : Skin -> Model -> Html Msg
view skin model =
    layout [ width (minimum 0 shrink) ] <|
        let
            deviceClass =
                classifyDevice model.size

            viewPage toMsg viewFunc viewModel =
                case deviceClass of
                    Desktop ->
                        row
                            [ width fill
                            , spacing 20
                            , padding 10
                            ]
                            [ navigation deviceClass skin (toRoute model.page)
                            , Element.map toMsg (viewFunc skin viewModel)
                            ]

                    Mobile ->
                        if model.menuOpen then
                            navigation deviceClass skin (toRoute model.page)

                        else
                            column
                                [ width fill
                                , spacing 10
                                ]
                                [ row
                                    [ width fill
                                    , height (px 50)
                                    , Background.color Skin.white
                                    ]
                                    [ Input.button
                                        [ padding 5
                                        , width (px 50)
                                        , height (px 50)
                                        ]
                                        { onPress = Just Back
                                        , label =
                                            Elements.svgIcon
                                                []
                                                { color = skin.primaryColor
                                                , name = "ico36_backarrow"
                                                }
                                        }
                                    , text (routeLabel <| toRoute model.page)
                                    ]
                                , el [ padding 10 ] <|
                                    Element.map toMsg (viewFunc skin viewModel)
                                ]
        in
        case model.page of
            Export subModel ->
                viewPage ExportMsg Export.view subModel

            Account subModel ->
                viewPage AccountMsg Account.view subModel


navigation : DeviceClass -> Skin -> Route -> Element Msg
navigation deviceClass skin route =
    let
        attrs =
            case deviceClass of
                Desktop ->
                    [ alignTop ]

                Mobile ->
                    [ width fill ]

        navItem targetRoute =
            let
                ( bgColor, textColor ) =
                    if
                        targetRoute
                            == route
                            && deviceClass
                            == Desktop
                    then
                        ( skin.primaryColor, Skin.white )

                    else
                        ( Skin.setAlpha 0 Skin.white, Skin.black )
            in
            Input.button
                [ Background.color bgColor
                , Font.color textColor
                , width fill
                , padding 10
                ]
                { onPress = Just (ChangeRoute targetRoute)
                , label = text <| routeLabel targetRoute
                }
    in
    column
        attrs
        [ navItem ExportR
        , navItem AccountR
        ]


routeLabel : Route -> String
routeLabel route =
    case route of
        ExportR ->
            "Export"

        AccountR ->
            "Account"



---- SUBSCRIPTIONS ----


subSubscriptions : Page -> Sub Msg
subSubscriptions page =
    case page of
        Export _ ->
            Export.subscriptions
                |> Sub.map ExportMsg

        Account _ ->
            Account.subscriptions
                |> Sub.map AccountMsg


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ subSubscriptions model.page
        , onResize
            (\width height ->
                Resized
                    { width = width
                    , height = height
                    }
            )
        ]
