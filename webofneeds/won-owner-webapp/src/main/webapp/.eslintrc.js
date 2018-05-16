module.exports = {
  root: true,
  extends: ["plugin:prettier/recommended", "eslint:recommended"],
  parser: "babel-eslint",
  env: {
    browser: true,
    es6: true,
  },
  parserOptions: {
    sourceType: "module",
    ecmaVersion: 2017,
  },
  rules: {
    "no-console": "warn",
    "no-var": "error",
  },
  globals: {
    Exception: true,
  },
};
