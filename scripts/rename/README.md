# Renaming Scripts
The scripts in this folder are used to rename some concepcts used throughout the codebase.

The main script is `rename.sh`. It can be executed in any directory and changes files recursively, starting with that directory.

The main config files are 
* `oldforms.txt` - containing all the terms to be replaced
* `newforms.txt` - containing the replacement terms. 
The lines of the two files are aligned - each line in `oldforms.txt` has a corresponding line in `newforms.txt`
* rename-filename-filter.txt - contains a regular expression matching files or folders to be excluded from processing.


# Usage
## Try a dry run

```
cd ${webofneeds-project-dir}
./scripts/rename/rename.sh
```
## Check files by endings

See endings of all files that will be affected:
```
./scripts/rename/list-file-extensions.sh
```

See the files for a given extension (here, all '.md' files)
```
./scripts/rename/list-files-by-extension.sh md -p
```

## Actually perform the renaming (DANGER ZONE!)

### Rename
```
./scripts/rename/rename.sh (using the FORCE parameter)
```

### Reformat java and javascript files
```

cd webofneeds
mvn formatter:format
cd won-owner-webapp/src/main/webapp
shopt -s globstar
node_modules/prettier/bin-prettier.js --write app/**/*.js config/**/*.js *.js 
cd ../../../../

```

### Set up the DB migration
```
./scripts/rename/generate-db-migration-files.sh
cp ./scripts/rename/generated/rename-migration-node.sql webofneeds/won-node/src/main/resources/db/migration/V #[complete with approproate file name]
cp ./scripts/rename/generated/rename-migration-owner.sql webofneeds/won-owner/src/main/resources/db/migration/V #[complete with approproate file name]
```

### Build and test
First, `fix the won-utils-conversation` unit test that has a `taxi-no-show.trig` file containing base64-encoded xml. 
1. use e.g. Notepad++ to convert the xml content in `petrinet-taxi.xml` to base64
2. replace the content of the `https://w3id.org/won/workflow#hasInlinePetriNetDefinition` property with the base64 string you created.

### Build the whole project
```
cd ${webofneeeds-project-dir}
cd webofneeds
mvn install
```
