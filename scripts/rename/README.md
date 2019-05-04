# Renaming Scripts
The scripts in this folder are used to rename some concepcts used throughout the codebase.

The main script is `rename.sh`. It can be executed in any directory and changes files recursively, starting with that directory.

The script requires a parameter, the name of the 'configuration directory'

## Contents of the configuration directory:
* `oldforms.txt` - containing all the terms to be replaced
	- lines starting with '#' are ignored
	- lines starting with '!' are not expanded
	- all other lines are expanded. This means that the line is used, and in addition to that, it is converted into several forms (uppercase, lowercase, first letter uppercase, camel case converted to '_' separated, and more). See `expand.pl` for details.
* `newforms.txt` - containing the replacement terms. 
The lines of the two files are aligned - each line in `oldforms.txt` has a corresponding line in `newforms.txt`
* `renameselect` - contains regular expressions (for grep -e) matching files or folders to be included in processing. Will be created if not provided, content defaults to '*'.
* `renameignore` - contains regular expressions (for grep -e) matching files or folders to be excluded from processing. Will be created empty if not provided.


# Usage

## Fast track: apply a rename to a conf directory

To apply rename conf 001 to your local config directory, assumed in `{won-dir}/webofneeds/conf.local`
```
cd webofneeds/webofneeds/conf.local 
../../scripts/rename/rename.sh ../../scripts/rename/conf.rename-001-main-concepts/ FORCE
```
Make sure to apply all rename jobs you've missed.

## Try a dry run

```
cd ${webofneeds-project-dir}
./scripts/rename/rename.sh <config-dir>
```
## Check files by endings

See endings of all files that will be affected:
```
./scripts/rename/list-file-extensions.sh <config-dir>
```

See the files for a given extension (optionally, print affected lines: -p option):
```
./scripts/rename/list-files-by-extension.sh <config-dir> <file-extension> -p
```

## Actually perform the renaming (DANGER ZONE!)

### Rename

In a shell, move to the top directory you want to rename, then type 

```
./scripts/rename/rename.sh <config-dir> FORCE
```

### Required manual post-processing

#### Fix broken test

First, `fix the won-utils-conversation` unit test that has a `taxi-no-show.trig` file containing base64-encoded xml. 
1. use e.g. Notepad++ to convert the xml content in `petrinet-taxi.xml` to base64
2. replace the content of the `https://w3id.org/won/workflow#hasInlinePetriNetDefinition` property with the base64 string you created.

#### Reformat java and javascript files

```

cd webofneeds
mvn formatter:format
# Note: if you haven't built the project at all, you have to 
# do `mvn install` to download nodejs now or the next step will fail
pushd won-owner-webapp/src/main/webapp
shopt -s globstar
node_modules/prettier/bin-prettier.js --write app/**/*.js config/**/*.js *.js 
popd

```

#### Set up the DB migration

**Note:** if the generated sql file is empty, you either did not manage to connect to the db or there is nothing to migrate. Check the script's output for `psql:FATAL` messages and decide.

```
./scripts/rename/generate-db-migration-files.sh
cp ./scripts/rename/conf.rename-001-main-concepts/generated/rename-migration-node.sql webofneeds/won-node/src/main/resources/db/migration/V #[complete with approproate file name]
cp ./scripts/rename/conf.rename-001-main-concepts/generated/rename-migration-owner.sql webofneeds/won-owner/src/main/resources/db/migration/V1 #[complete with approproate file name]
```


#### Build the whole project

```
cd ${webofneeeds-project-dir}
cd webofneeds
mvn install
```
