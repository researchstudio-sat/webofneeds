const path = require('path');
const webpack = require('webpack');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const sassImporter = require('node-sass-import-once');
const UglifyJsPlugin = require("uglifyjs-webpack-plugin");
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const glob = require('glob');
const SpriteLoaderPlugin = require('svg-sprite-loader/plugin');

module.exports = function(env, argv) {
    const mode = argv.mode || (argv.watch ? 'development': 'production');

    const extractSass = new MiniCssExtractPlugin({
        filename: "won.min.css"
    });

    return {
        entry: {
            main: ['./style/won.scss', 'babel-polyfill', './app/app_jspm.js'].concat(glob.sync('./images/**/*.svg'))
        },
        mode: mode,
        optimization: {
            minimizer: [
                new UglifyJsPlugin({
                    parallel: true
                  }),
                new OptimizeCSSAssetsPlugin({})
            ]
        },
        output: {
            path: path.resolve(__dirname, 'generated'),
            filename: 'app_jspm.bundle.js'
        },
        resolve: {
            alias: {
                'fetch': 'whatwg-fetch',
                'rdfstore-js$': path.resolve(__dirname, 'scripts/rdfstore-js/rdf_store.js'),
                'angular-ui-router-shim$': require.resolve('angular-ui-router/release/stateEvents.js')
            }
        },
        module: {
            rules: [
                {
                    test: require.resolve('./scripts/rdfstore-js/rdf_store.js'),
                    use: 'exports-loader?rdfstore'
                },
                {
                    test: /\.js$/,
                    exclude: [/node_modules/, /rdf_store\.js/],
                    use: [{
                        loader: 'babel-loader',
                        options: {
                            presets: [
                                ['env', {
                                    targets: {
                                        browsers: ['last 2 versions']
                                    }
                                }]
                            ],
                            plugins: [
                                'transform-object-rest-spread',
                                'transform-remove-strict-mode'
                            ]
                        }
                    }]
                },
                {
                    test: /\.scss$/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        {
                        loader: "css-loader",
                        options: {
                            sourceMap: mode == 'development',
                            minimize: mode == 'production',
                            import: false,
                            url: false
                        }
                    }, {
                        loader: "sass-loader",
                        options: {
                            sourceMap: mode == 'development',
                            importer: sassImporter
                        }
                    }]
                },
                {
                    test: /\.svg$/,
                    loader: 'svg-sprite-loader',
                    options: {
                        extract: true,
                        spriteFilename: 'icon-sprite.svg'
                    }
                }
            ]
        },
        plugins: [
            extractSass,
            new SpriteLoaderPlugin({ plainSprite: true })
        ],
        devtool: mode == 'development' ? 'eval-source-map' : false
    };
};