import * as path from "path";
import { Configuration, EnvironmentPlugin } from "webpack";
import * as MiniCssExtractPlugin from "mini-css-extract-plugin";
import * as UglifyJsPlugin from "uglifyjs-webpack-plugin";
import * as OptimizeCSSAssetsPlugin from "optimize-css-assets-webpack-plugin";
import * as SpriteLoaderPlugin from "svg-sprite-loader/plugin";
import * as CopyWebpackPlugin from "copy-webpack-plugin";
// import { BundleAnalyzerPlugin } from "webpack-bundle-analyzer";
import * as WatchTimePlugin from "webpack-watch-time-plugin";
import * as UnusedWebpackPlugin from "unused-webpack-plugin";
import * as DartSass from "dart-sass";
import * as HtmlWebpackPlugin from "html-webpack-plugin";
import * as ServiceWorkerWebpackPlugin from "serviceworker-webpack-plugin";

export default config;

function config(env, argv): Configuration {
  const mode: "production" | "development" =
    process.env.NODE_ENV == "production" ? "production" : "development";

  return {
    entry: "./app/app.js",
    mode: mode,
    optimization: {
      moduleIds: "hashed",
      splitChunks: {
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: "vendors",
            chunks: "all",
          },
        },
      },
      minimizer: [
        new UglifyJsPlugin({
          parallel: true,
          sourceMap: true,
        }),
        new OptimizeCSSAssetsPlugin(),
      ],
    },
    output: {
      path: path.resolve(__dirname, "dist"),
      filename: "[name].[contenthash].js",
    },
    resolve: {
      alias: {
        "~": path.resolve(__dirname),
      },
      extensions: [".jsx", ".js"],
    },
    module: {
      rules: [
        {
          test: /\.jsx?$/,
          exclude: [/node_modules/],
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
            () => {
              if (mode == "development") {
                return {
                  loader: "style-loader",
                };
              } else {
                return MiniCssExtractPlugin.loader;
              }
            },
            {
              loader: "css-loader",
              options: {
                sourceMap: true,
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
                spriteFilename: "spritesheet.[contenthash].svg",
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
      // new BundleAnalyzerPlugin(),
      new HtmlWebpackPlugin({
        template: path.resolve(__dirname, "template.ejs"),
      }),
      new MiniCssExtractPlugin({
        filename: "[name].[contenthash].css",
      }),
      new ServiceWorkerWebpackPlugin({
        entry: path.join(__dirname, "sw.js"),
        publicPath: "/",
      }),
      new SpriteLoaderPlugin({ plainSprite: true }),
      new CopyWebpackPlugin({
        patterns: [
          {
            from: "skin",
            to: "skin",
          },
          {
            from: "assets",
            to: "assets",
          },
          {
            from: "WEB-INF",
            to: "WEB-INF",
          },
        ],
      }),
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
          path.join(__dirname, "images"),
          path.join(__dirname, "fonts"),
        ],
        // Root directory (optional)
        root: __dirname,
      }),
      new EnvironmentPlugin({
        NODE_ENV: "development",
        WON_DEPLOY_NODE_ENV: "default",
        WON_OWNER_BASE_URL: "/owner",
      }),
    ],
    devtool: "source-map",
    stats: {
      children: false,
    },
  };
}
