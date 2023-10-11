# Dynamic Links

You can replace hyperlinks in the template file with dynamic links by using the `replaceLink` function after a placeholder link in the document: 

<code>
{<i>%=replaceLink(url)%</i>}
</code>

The value of `url` is not validated, it is converted to string if needed. 

The expression replaces the link URL in the hyperlink preceding this expression, therefore, it should be placed immediately after the link, we want to modify.
