//module.exports = {
//    entry: [
//        'whatwg-fetch',
//        './app/app_jspm.js',
//    ],
//    output: {
//        filename: 'generated/webpack.bundle.js'
//    }
//}


const path = require('path');
const ClosureCompilerPlugin = require('webpack-closure-compiler');
const UglifyJSPlugin = require('uglifyjs-webpack-plugin');

module.exports = {
    entry: [
        'whatwg-fetch',
        path.join(__dirname, 'app/app_jspm.js')
    ],
    output: {
        path: path.join(__dirname, '/generated/'),
        //filename: 'webpack.bundle.js'
        filename: 'generated/wp.bundle.min.js',
    },
    plugins: [
        new ClosureCompilerPlugin({
            compiler: {
                //jar: 'path/to/your/custom/compiler.jar' //optional
                language_in: 'ECMASCRIPT6',
                language_out: 'ECMASCRIPT5',
                compilation_level: 'ADVANCED'
            },
            //jsCompiler: true,
            concurrency: 3,
        }),
        new UglifyJSPlugin(),
    ]
};