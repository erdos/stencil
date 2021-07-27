# Dynamic Images

You can replace an image in the template file with dynamic content by writing the following expression after the image in the document:

<code>
{<i>%=replaceImage(data)%</i>}
</code>

The value of `data` should be Base64-encoded [data URI](https://en.wikipedia.org/wiki/Data_URI_scheme) string.

The expression replaces the content of image preceding this expression, therefore, it should be placed immediately after the image we want to modify.

## Error messages

When the image data is not valid value, the following error is printed:

```
Image data should be in valid base64-encoded data uri format!
```

The following error tells you that the marker expression is placed in a wrong position (perhaps before instead of after the image tag):

```
Did not find image to replace. The location of target image must precede the replaceImage() function call location.
```
