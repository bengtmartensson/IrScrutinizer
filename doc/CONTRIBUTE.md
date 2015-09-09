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

The code uses "normal Java formatting". Indentation 4 spaces, no
tabs. Braces in _simple_ if-statements (etc) discouraged. Observe the
"principle of locality": Distance between declaration and usage
should be minimal; C style declaration of local variables in the beginning
of a block discouraged.

## Text file format

The format of text files follows the requirement of portability:

* Text files that are only of interest for Windows (e.g.
<code>IrScrutinizer_inno.iss</code>) should use CRLF,
* Text files that a Windows user may open with Textedit
(e.g. <code>IrpProtocols.ini</code>)  should use CRLF, unless this creates other problems,
* Otherwise LF.

## GUI files
Most Java classes with GUI content are maintained with
[Netbeans](http://www.netbeans.org). This is indicated by a <code>.form</code> file
in parallel to the <code>.java</code> file. Contributions that breaks Netbeans
cannot be accepted -- as a rule of thumb, the parts of the code that
belongs to Netbeans are clearly marked as such, and may not be hand
edited. (Some GUI files has been initially created by Netbeans, but
then "de-Netbeanized", identified by the lack of a .form file. The
paragraph does not apply to these, of course.)

## Git branches

(This section stolen from LIRC, with minor editing.)  We basically use the branching
scheme described
[here](http://nvie.com/posts/a-successful-git-branching-model). However,
what is called 'devel' in that document we call 'master'. Likewise,
what is called 'master' there we call 'release'. In short:

* master is the current development, from time to time unstable.
* release contains the last stable version, and also all tagged releases.
* When a release is upcoming, we fork a release branch from master. This
is kept stable, only bug fixes are allowed. Eventually it is merged into
release and tagged.
* Other branches are feature branches for test and review. They can not
be trusted, and are often rewritten.
