## Convert csv to Outline XML
This is a port of a tool for converting csv Outline files to a tbrc.org Outline XML doc 
### Building
mvn clean package

### Running
For help:
```
java -jar target/convert-2-outline-xml-1.6.0.jar -help
```
Simple run with a single input file:
```
java -jar target/convert-2-outline-xml-1.6.0.jar -doc /path/Outline-W8LS32723.csv -outdir /outPath
```
The output file will be:
```
/outPath/O8LS32723.xml
```
Note the csv uses the vertical bar, ```'|'``` as a column separator and no string quotes, ```'"'```, for cell contents.
The sample file, ```Outline-W8LS32723.csv```, is located in the `resources` directory.
