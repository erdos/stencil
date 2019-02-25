# Standalone Mode

Runnig Stencil as a standalone application make it possible to process templates from batch scripts.
It may also be easier if your application's architecture is not written in java.

## Building

Build the project with the `lein uberjar` command to get a standalone application. The built output will be found in the `target` directory.

Run the file with the `java -jar *-standalone.jar` command.

## Running

The syntax for runnig the application is the following.

`java -jar *-standalone.jar [args and flags] [--] [filenames ...]`

The return status code is `0` iff no error happened when rendering the files.

The arguments and flags are the following:

- `-o DIR` or `--output=DIR` is the output directory of the rendered documents. It defaults to the current working directory. This directory must exist when running Stencil.
- `-O` or `--overwrite` flag states that the rendered documents should be overwritten when the target file already exists instead of exiting with and error. Defaults to false.
-

The `filenames` part is a repeating sequence of a template file and data files. For example: `template1.docx t1data1.json t1data2.json template2.json t2data2.json ...` Each template
file will be rendered with the data files after it. So the output will be three rendered documents with the names `template1-t1data1.docx`, `template1-t1data2.docx` and `template2-t2data2.json`.



### Example calls

The following renders template `template1.docx` with three data files. It will put the resulting documents to `/data/rendered` and it will overwrite existing files if any:

```
java -jar stencil-*-standalone.jar -o /data/rendered --overwrite -- template1.docx data1.docx data2.docx data3.docx
```

