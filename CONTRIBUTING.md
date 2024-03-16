# Coding style etc.

Contributions of all sorts are welcome, but to be useful has to follow
certain conventions. Some of those ( :-) ) are described here.

## General

Most importantly, this project is intended to be *portable*, meaning
to run equally good on all supported platforms (to the extent
possible). Therefore, features running only on one particular platform
are not a priority -- which does not exclude the best possible
integration in a particular platform. But they are not prohibited either.

## Code layout

The code uses "normal Java formatting": Indentation 4 spaces, no
tabs. Braces in _simple_ if-statements (etc) discouraged. No linefeed before opening brace. The
"principle of locality" is to be observed: Distance between declaration and usage
should be minimal; C style declaration of local variables in the beginning
of a block discouraged.

## Text file format

The format of text files follows the requirement of portability:

* Text files that are only of interest for Windows (e.g.
<code>IrScrutinizer_inno.iss</code>) should use CRLF,
* Text files that a Windows user may open with Textedit
should use CRLF, unless this creates other problems,
* Otherwise LF.

## Build process

[Apache Maven](https://maven.apache.org) is used for builds.
Normal builds must not require Internet access.
All parameters (URLs, version numbers etc.) should be contained therein within /project/properties.
(Other programs can then extract that information, see `tools/get-jdk-tar.sh` for an example.)

## GUI files
Most (but not all) Java classes with GUI content are maintained with
[Netbeans](http://www.netbeans.org). This is indicated by a <code>.form</code> file
in parallel to the <code>.java</code> file. Contributions that breaks Netbeans
cannot be accepted -- as a rule of thumb, the parts of the code that
belongs to Netbeans are clearly marked as such, and may not be hand
edited. (Some GUI files has been initially created by Netbeans, but
then "de-Netbeanized", identified by the lack of a .form file. The
paragraph does not apply to these, of course.)
