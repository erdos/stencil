# Functions

You can call functions from within the template files and embed the call result easily by writing
<code>{<i>%=functionName(arg1, arg2, arg3, ...)%</i>}</code> expression in the document template.

This is a short description of the functions implemented in Stencil.

## Basic Functions

### Coalesce

Accepts any number of arguments, returns the first not-empty value.

**Exampe:**

- to insert the first filled name value: <code>{<i>%=coalesce(partnerFullName, partnerShortName, partnerName)%</i>}</code>
- to insert the price of an item or default to zero: <code>{<i>%=coalesce(x.price, x.premium, 0)%</i>}</code>

### Empty

Decides if a parameter is empty or missing. Useful in conditional statements.

**Example:**

<code>{<i>%if empty(userName) %</i>}Unknown User{<i>%else%</i>}{<i>%=userName%</i>}{<i>%end%</i>}</code>

If the value of `userName` is missing then `Unknown User` will be inserted, otherwise the value is used.

The `empty()` function is useful when we want to either enumerate the contents
of an array or hide the whole paragraph when the array is empty.

<img src="screenshot-function-empty-before.png"/>

## String functions

These functions deal with textual data.

## Format

Calls [String.format](https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html) function.

**Example:**

This example formats the value of `price` as a price string:
<code>{<i>%=format("$ %(,.2f"", price) %</i>}</code>. It may output `$ (6,217.58)`.


## Date

Formats a date value according to a given [format string](https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html).

Arguments:

1. First argument is a format string.
2. Second argument is a string containing a date value.

**Example:**

This example formats the value of `partner.birthDate` as a date string: <code>{<i>%=date("yyyy-MM-dd", partner.birthDate) %</i>}</code>

Also, try these formats strings:

- `"yyyy-MM-dd HH:mm:ss"` for example: `2018-02-28 13:01:31`
- `"EEE, dd MMM yyyy HH:mm:ss zzz"` (also known as RFC1123)
- `"EEEE, dd-MMM-yy HH:mm:ss zzz"` (a.k.a. RFC1036)
- `"EEE MMM d HH:mm:ss yyyy"` (ASCTIME)
- `"yyyy-MM-dd'T'HH:mm:ss.SSSXXX"` (ISO8601)
