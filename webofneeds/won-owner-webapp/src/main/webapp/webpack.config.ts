import * as path from "path";
import { Configuration } from "webpack";
import * as MiniCssExtractPlugin from "mini-css-extract-plugin";
import * as SassImporter from "node-sass-import-once";
import * as UglifyJsPlugin from "uglifyjs-webpack-plugin";
import * as OptimizeCSSAssetsPlugin from "optimize-css-assets-webpack-plugin";
import * as SpriteLoaderPlugin from "svg-sprite-loader/plugin";
import * as CopyWebpackPlugin from "copy-webpack-plugin";
import * as LiveReloadPlugin from "webpack-livereload-plugin";
import * as UnusedWebpackPlugin from "unused-webpack-plugin";

export default config;

function config(env, argv): Configuration {
  const mode: "development" | "production" =
    argv.mode || (argv.watch ? "development" : "production");

  const isLive: boolean = env && env.WON_DEPLOY_NODE_ENV == "live";

  //TODO: When `webpack-watch-time-plugin` is updated for newer versions of webpack switch to that.
  const WatchTimePlugin = {
    apply: compiler => {
      const RED = "\x1B[0;31m";
      const GREEN = "\x1B[0;32m";
      const NC = "\x1B[0m";

      compiler.hooks.watchRun.tap("TimePrinter", () => {
        const time = new Date();
        console.log(
          `_____________\n`,
          `${GREEN}${time.getHours()}:${RED}${("0" + time.getMinutes()).slice(
            -2
          )}:${("0" + time.getSeconds()).slice(-2)} ${GREEN}⇩${NC}`
        );
      });
    },
  };

  return {
    entry: "./app/app_webpack.js",
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
      filename: "app_jspm.bundle.js",
    },
    resolve: {
      alias: {
        fetch: "whatwg-fetch",
        "rdfstore-js$": path.resolve(
          __dirname,
          "scripts/rdfstore-js/rdf_store.js"
        ),
        "angular-ui-router-shim$": require.resolve(
          "angular-ui-router/release/stateEvents.js"
        ),
        config$: path.resolve(
          __dirname,
          "config",
          `${isLive ? "live" : "default"}.js`
        ),
      },
    },
    module: {
      rules: [
        {
          test: require.resolve("./scripts/rdfstore-js/rdf_store.js"),
          use: "exports-loader?rdfstore",
        },
        {
          test: /\.js$/,
          exclude: [/node_modules/, /rdf_store\.js/],
          use: [
            {
              loader: "babel-loader",
            },
            {
              loader: "eslint-loader",
            }
          ],
        },
        {
          test: /\.scss$/,
          use: [
            MiniCssExtractPlugin.loader,
            {
              loader: "css-loader",
              options: {
                sourceMap: mode == "development",
                minimize: mode == "production",
                import: false,
                url: false,
              },
            },
            {
              loader: "sass-loader",
              options: {
                sourceMap: mode == "development",
                importer: SassImporter,
              },
            },
          ],
        },
        {
          test: /\.svg$/,
          use: [
            {
              loader: "svg-sprite-loader",
              options: {
                extract: true,
                spriteFilename: "symbol/svg/sprite.symbol.svg",
              },
            },
            {
              loader: "svgo-loader",
              options: {},
            },
          ],
        },
      ],
    },
    plugins: [
      new MiniCssExtractPlugin({
        filename: "won.min.css",
      }),
      new SpriteLoaderPlugin({ plainSprite: true }),
      new CopyWebpackPlugin(
        [
          {
            context: path.resolve(
              path.dirname(require.resolve("leaflet/package.json")),
              "dist",
              "images"
            ),
            from: "**/*",
            to: "./images/",
          },
        ],
        {}
      ),
      new LiveReloadPlugin(),
      WatchTimePlugin,
      new UnusedWebpackPlugin({
        // Source directories
        directories: [
          path.join(__dirname, "app"),
          path.join(__dirname, "style"),
        ],
        // Exclude patterns
        //exclude: ["*.test.js"],
        // Root directory (optional)
        root: __dirname,
      }),
    ],
    devtool: "source-map",
  };
}
