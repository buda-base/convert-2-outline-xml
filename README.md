## Convert csv to Outline XML
This is a port of a tool for converting csv Outline files to a tbrc.org Outline XML doc 
### Building
mvn clean package

### Running
For help:
```
java -jar target/convert-2-outline-xml-1.7.1.jar -help
```
Simple run with a single input file:
```
java -jar target/convert-2-outline-xml-1.7.1.jar \
     -doc /path/Outline-W8LS32723.csv -outdir /outPath \
     -who CodeFerret
```
The output file will be:
```
/outPath/O8LS32723.xml
```
Note the csv uses the vertical bar, ```'|'``` as a column separator and no string quotes, ```'"'```, for cell contents.
The sample file, ```Outline-W8LS32723.csv```, is located in the `resources` directory.

Since version 1.7.0 there is now support for extended csv files that include optional comma separated lists of 
authors and subjects and a note and finally on the first row after headers the Work title in the final column:
```
java -jar target/convert-2-outline-xml-1.7.1.jar \
     -doc /path/Outline-W8LS32723.csv -outdir /outPath \
     -who CodeFerret -extended
```
See the example csv and output in the ```resources``` folder.
### Notes
**The csv file should only have the Work rid on the first row (after heading) and a volume number only on the
first row of the volume.**

The Instructions source document is located in folder BUDA/OutlineConversion.