================
CSV Data Sources
================

A comma-separated values (CSV) file is a delimited text file that uses commas to separate the values.
While this definition looks straightforward, the main problem is that in practice, the actual format can vary wildly.
As a result, the CSV module supports many configuration parameters to accomodate for every possible format variant.

- ``charset``: The charset of the file
- ``newLine``: The new line character(s) of the file
- ``header``: Whether the file contains header data / column names. Can be either ``included`` or ``omitted``
- ``delimiter``: The delimiter character. (Default is ``,``)
- ``quote``: The quote character. (Default is ``"``)
