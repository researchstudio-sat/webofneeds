# Generating WoN Ontology Documentation

We use [Widoco](https://github.com/dgarijo/Widoco) for generating the ontology documentation. 

This is a 2-step process:
1. Download the latest Widoco release jar from https://github.com/dgarijo/Widoco/releases
2. Run our script `widoco.sh`:

Assuming that ${widoco_jar} points to the widoco release jar, run
```
./widoco.sh ${widoco_jar}
```

This will generate the documentation for all ontologies. 

## Adding ontologies

When adding new ontologies to the won-vocab project, the `widoco.sh` script needs to be updated:
* When adding a main ontology, i.e. one in `src/main/resources/ontology`, add its name to the `onts` array.
* When adding an ontology extension, i.e. one in `src/main/resources/ontology/ext`, add its name to the `ext_onts`array.

## Building Docs just for one ontology

Sometimes it is handy not to build the whole set but just the ontology under development. To do so, run `widoco.sh` with the ontology's name (=key in the respective array in the script) as the second parameter. 

For example, the following only builds the `won-message` ontology docs:
```
./widoco.sh ${widoco_jar} message
```
