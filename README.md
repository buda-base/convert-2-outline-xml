## Convert csv to Outline XML

This is a port of a tool for converting csv Outline files to a tbrc.org Outline XML doc 

### Download

[convert-2-outline-xml-1.8.3](https://github.com/buda-base/convert-2-outline-xml/releases/download/v1.8.3/convert-2-outline-xml-1.8.3.jar)

### Building
mvn clean package

### Running
For help:

```sh
java -jar target/convert-2-outline-xml-1.8.3.jar -help
```

Simple run with a single input file:

```sh
java -jar target/convert-2-outline-xml-1.8.3.jar \
     -doc /path/Outline-W8LS32723.csv -outdir /outPath \
     -who CodeFerret
```

The output file will be `/outPath/O8LS32723.xml`.

Or you can run it on a folder with:

```sh
java -jar target/convert-2-outline-xml-1.8.3.jar \
     -docdir /inpath -outdir /outPath \
     -who CodeFerret -authorshipnote "provided by XYZ"
```

### Notes

**The csv file should only have the Work rid on the first row (after heading).**

The Instructions source document is located in Google Drive folder `BUDA/OutlineConversion`.
