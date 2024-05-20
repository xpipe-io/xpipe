An h1 header
============

Paragraphs are separated by a blank line.

2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists
look like:

* this one
* that one
* the other one

> Block quotes are
> written like so.
>
> They can span multiple paragraphs,
> if you like.

Unicode is supported. ☺



An h2 header
------------

Here's a numbered list:

1. first item
2. second item
3. third item

Note again how the actual text starts at 4 columns in (4 characters
from the left side). Here's a code sample:

    # Let me re-iterate ...
    for i in 1 .. 10 { do-something(i) }

As you probably guessed, indented 4 spaces. By the way, instead of
indenting the block, you can use delimited blocks, if you like:

~~~
define foobar() {
    print "Welcome to flavor country!";
}
~~~



### An h3 header ###

Now a nested list:

1. First, get these ingredients:

    * carrots
    * celery
    * lentils

2. Boil some water.

3. Dump everything in the pot and follow
   this algorithm:

       find wooden spoon
       uncover pot
       stir
       cover pot
       balance wooden spoon precariously on pot handle
       wait 10 minutes
       goto first step (or shut off burner when done)

   Do not bump wooden spoon or it will fall.

Notice again how text always lines up on 4-space indents (including
that last line which continues item 3 above).

Here's a link to [a website](http://foo.bar) and to a [section heading in the current
doc](#an-h2-header). Here's a footnote [^1].

[^1]: Footnote text goes here.

Tables can look like this:

| size | material    | color       |
|------|-------------|-------------|
| 9    | leather     | brown       |
| 10   | hemp canvas | natural     |
| 11   | glass       | transparent |

Table: Shoes, their sizes, and what they're made of

A horizontal rule follows.

***

Here's a definition list:

apples
: Good for making applesauce.
oranges
: Citrus!
tomatoes
: There's no "e" in tomatoe.

Again, text is indented 4 spaces. (Put a blank line between each
term/definition pair to spread things out more.)

And note that you can backslash-escape any punctuation characters
which you wish to be displayed literally, ex.: \`foo\`, \*bar\*, etc.
