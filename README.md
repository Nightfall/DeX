Nameless Data Exchange Format Version 1
---------------------------------------

We support tables, primitive datatypes (number, boolean), strings and tagged tables as extension to normal tables.

#### Numbers
Integers are allowed in decimal, hexadecimal, binary and octal representation.
```
255, -255   # Decimal
0xFF        # Hexadecimal
0o377       # Octal
0b11111111  # Binary
```
Floating ponts, exponents, as well as NaN and Infinity are also supported.
```
1.255       # Floating point
1e10, 1E10  # Exponent
Infinity, -Infinity, NaN
```

#### Booleans
Booleans of yes/no, true/false, on/off are supported.

Numbers and booleans can be treated like strings by the API, unless specifically excluded.

#### Flags
Flags are boolean properties with a shorthand syntax.

```
+flag
#Is equivalent to
"flag" = true

-flag
#Is equivalent to
"flag" = false
```

#### Strings
Strings are encapsulated in quotation marks. Quotation marks, colons and curly brackets have to be escaped with backslash (As well as backslash).
You can also write strings without quotation marks if they don't contain curly brackets or other (unescaped) illegal characters, keep numbers and booleans in mind to avoid ambiguity. Strings that aren't encapsulated will ignore any preceding and trailing whitespaces.

Strings can span over multiple lines (if encapsulated), any preceding whitespace and line breaks will be included, so keep that in mind.

Another way is to continue the string on the next line with `\`, this will not count as linebreak. Any preceeding whitespace will be included, but the API allows you to specify a line seperator char like `|`.

```
name: John
name: "John Smith"
name: John Smith #Also allowed
first name: John #Even this
```

#### Tables
A table can contain any of the above datatypes.
A table is created with curly brackets and ended with curly brackets.
A table can be preceeded by a string in order to associate a tag type, the API allow to filter elements by tag type and custom serialization / deserialization based on tag type.
The key MUST be followed by the start of a value on the same line. If you want to continue the line and ignore the line break
you can use `\`

A table contains key value pairs, separated by commas. Keys can be of any type, as well as the values.
All table keys have to be unique.
A line break is equivalent to a comma, empty lines get ignored.
If no key is provided, the index will be treated as numeric key (starting from 0).
Tagged tables will have their tag type treated as key, *but only if not in array context!*

```
tag { ... }
#Is equivalent to
"tag": "tag" { ... }

Foo
#Is equivalent to
0: "Foo"
```


Tables can be treated like arrays or like objects.

```
MAYU : vocaloid {
  taglist { yandere, lolita, gothic, small }
  
  gender   : female
  age      : 15
  company  : EXIT TUNES
  language : Japanese
  code     : QWCE-00264
  
  description: "
    MAYU's design is based on gothic lolita fashion. 
    Her hair itself fades from a light blonde to rainbow, 
    and she is depicted with a hat that has a speaker attached.
    Her earrings appear to be styled like in-ear headphones that hook over the ear.
  "
}
```

#### Comments
Comments start with `#` and the parser will ignore the rest of the line.
Multiline comments start with `#{` and end with `}#`.

A note about "comments with meaning": I don't really like them but can we please all agree to start them with an `@`? Thanks.

There is no nil or null type. If you want something to be not specified, just leave it out.
The entire file will be treated like a table, you DON'T encapsulate everything with a giant {}.
