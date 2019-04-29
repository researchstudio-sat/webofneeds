import * as path from "path";
import { Configuration } from "webpack";
import * as MiniCssExtractPlugin from "mini-css-extract-plugin";
import * as UglifyJsPlugin from "uglifyjs-webpack-plugin";
import * as OptimizeCSSAssetsPlugin from "optimize-css-assets-webpack-plugin";
import * as SpriteLoaderPlugin from "svg-sprite-loader/plugin";
import * as CopyWebpackPlugin from "copy-webpack-plugin";
import * as WatchTimePlugin from "webpack-watch-time-plugin";
import * as UnusedWebpackPlugin from "unused-webpack-plugin";
import * as DartSass from "dart-sass";
import * as HtmlWebpackPlugin from "html-webpack-plugin";

export default config;

function config(env, argv): Configuration {
  const mode: "development" | "production" =
    argv.mode || (argv.watch ? "development" : "production");

  const nodeEnv = process.env.WON_DEPLOY_NODE_ENV || "default";

  return {
    entry: "./app/app.js",
    mode: mode,
    optimization: {
      minimizer: [
        new UglifyJsPlugin({
          parallel: true,
          sourceMap: true,
        }),
        new OptimizeCSSAssetsPlugin(),
      ],
    },
    output: {
      path: path.resolve(__dirname, "generated"),
      filename: "[name].[contenthash].js",
    },
    resolve: {
      modules: [__dirname, "node_modules"],
      alias: {
        fetch: "whatwg-fetch",
        "rdfstore-js$": path.resolve(
          __dirname,
          "scripts/rdfstore-js/rdf_store.js"
        ),
        "angular-ui-router-shim$": require.resolve(
          "angular-ui-router/release/stateEvents.js"
        ),
        detailDefinitions$: path.resolve(
          __dirname,
          "config",
          `detail-definitions.js`
        ),
        useCaseDefinitions$: path.resolve(
          __dirname,
          "config",
          `usecase-definitions.js`
        ),
        config$: path.resolve(__dirname, "config", `${nodeEnv}.js`),
        jsonld$: require.resolve("jsonld/dist/jsonld.js"), // This is needed because `jsonld`s entrypoint is not compiled to compatible js (uses spread operators). With this resolve hook we instead use the compiled version.
      },
    },
    module: {
      rules: [
        {
          test: require.resolve("./scripts/rdfstore-js/rdf_store.js"),
          use: "exports-loader?rdfstore",
        },
        {
          test: /\.jsx?$/,
          exclude: [/node_modules/, /rdf_store\.js/],
          use: [
            {
              loader: "babel-loader",
            },
            {
              loader: "eslint-loader",
            },
          ],
        },
        {
          test: /\.s?css$/,
          use: [
            MiniCssExtractPlugin.loader,
            {
              loader: "css-loader",
              options: {
                sourceMap: true,
                minimize: true,
                import: false,
              },
            },
            {
              loader: "postcss-loader",
              options: {
                sourceMap: true,
              },
            },
            {
              loader: "sass-loader",
              options: {
                sourceMap: true,
                implementation: DartSass,
              },
            },
          ],
        },
        {
          test: /\.svg$/,
          include: [path.resolve(__dirname, "images", "won-icons")],
          use: [
            {
              loader: "svg-sprite-loader",
              options: {
                extract: true,
                spriteFilename: "spritesheet.[hash].svg",
              },
            },
            {
              loader: "svgo-loader",
              options: {},
            },
          ],
        },
        {
          test: /\.elm$/,
          exclude: [/elm-stuff/, /node_modules/],
          use: {
            loader: "elm-webpack-loader",
            options: {
              optimize: mode == "production",
            },
          },
        },
        {
          test: /\.(woff2?|ttf)$/,
          exclude: [path.resolve(__dirname, "images", "won-icons")],
          use: {
            loader: "file-loader",
            options: {
              outputPath: "fonts",
            },
          },
        },
        {
          test: /\.(png|svg)$/,
          exclude: [path.resolve(__dirname, "images", "won-icons")],
          use: {
            loader: "file-loader",
            options: {
              outputPath: "images",
            },
          },
        },
      ],
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: path.resolve(__dirname, "template.ejs"),
      }),
      new MiniCssExtractPlugin({
        filename: "[name].[contenthash].css",
      }),
      new SpriteLoaderPlugin({ plainSprite: true }),
      new CopyWebpackPlugin(
        [
          {
            from: "WEB-INF",
            to: "WEB-INF",
          },
          {
            from: "skin",
            to: "skin",
          },
        ],
        {}
      ),
      new WatchTimePlugin({
        noChanges: {
          detect: true,
          report: true,
        },
      }),
      new UnusedWebpackPlugin({
        // Source directories
        directories: [
          path.join(__dirname, "app"),
          path.join(__dirname, "style"),
        ],
        // Root directory (optional)
        root: __dirname,
      }),
    ],
    devtool: "source-map",
    stats: {
      children: false,
    },
  };
}
