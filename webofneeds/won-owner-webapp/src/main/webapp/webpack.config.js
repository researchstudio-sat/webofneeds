const path = require('path');
const webpack = require('webpack');

module.exports = function(env, argv) {
    const mode = argv.mode || (argv.watch ? 'development': 'production');
    return {
        mode: mode,
        entry: ['babel-polyfill', './app/app_jspm.js'],
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
                    use: {
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
                                'transform-object-rest-spread'
                            ]
                        }
                    }
                }
            ]
        },
        devtool: mode == 'development' ? 'eval-source-map' : false
    };
};