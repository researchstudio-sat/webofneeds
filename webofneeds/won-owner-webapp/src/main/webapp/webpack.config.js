const path = require('path');
const webpack = require('webpack');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const sassImporter = require('node-sass-import-once');
const UglifyJsPlugin = require("uglifyjs-webpack-plugin");
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const glob = require('glob');
const SpriteLoaderPlugin = require('svg-sprite-loader/plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const LiveReloadPlugin = require('webpack-livereload-plugin');

module.exports = function(env, argv) {
    const mode = argv.mode || (argv.watch ? 'development': 'production');

    const isLive = env && env.WON_DEPLOY_NODE_ENV == 'live'

    const extractSass = new MiniCssExtractPlugin({
        filename: "won.min.css"
    });

    return {
        entry: './app/app_webpack.js',
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
                    use: [{
                        loader: 'svg-sprite-loader',
                        options: {
                            extract: true,
                            spriteFilename: 'symbol/svg/sprite.symbol.svg'
                        }
                    },{
                        loader: 'svgo-loader',
                        options: {
                        }
                    }]
                }
            ]
        },
        plugins: [
            extractSass,
            new SpriteLoaderPlugin({ plainSprite: true }),
            new CopyWebpackPlugin([
                {
                    context: path.resolve(path.dirname(require.resolve('leaflet/package.json')), 'dist', 'images'),
                    from: '**/*',
                    to: './images/'
                }
            ], {}),
            new webpack.NormalModuleReplacementPlugin(
                /config\.js$/,
                function(resource) {
                    if(path.resolve(resource.context, resource.request) == path.resolve(__dirname, 'app', 'config.js')) {
                        resource.request = path.resolve(__dirname, 'config', `${isLive ? 'live' : 'default'}.js`);
                    }
                }
            ),
            new webpack.NormalModuleReplacementPlugin(
                /utils\.js$/,
                function(resource) {
                    if(resource.context == path.resolve(__dirname, 'config')) {
                       resource.request = path.resolve(__dirname, 'app', 'utils.js');
                    }
                }
            ),
            new LiveReloadPlugin()
        ],
        devtool: mode == 'development' ? 'eval-source-map' : false
    };
};