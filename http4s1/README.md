# Zio Http4s Examples

The purpose of this project is to provide a well-documented collection of examples of the zio library used in conjunction with http4s.

There are several overlapping examples (i.e. only mild attempts will be made to factor out common code). This is to allow the individual examples to be largely stand-alone for ease of comprehension.

Also, comments (maybe obvious, but hopefully all correct) will be liberally sprinkled through the text and types may be given even when the scala compiler is perfectly capable of inferring them. This is to facilitate reading (you may be less quick than the scala compiler to carry out the inferences in your head)

I'm not an expert in Zio or Http4s, so please, if anyone sees a better way of doing stuff raise and issue and I'll update it (or do it with a PR yourself)

Inevitably, with libraries moving so quickly, this code will get out of date with "best practice" and fail to track the latest release. Please raise an issue in this case and I (Tim) or someone else, will hopefully update things. Better yet, do it yourself and raise a PR

There are 4 examples:
1. A simple service with GET
2. An implementation with Authentication
3. An implementation with XML decoder/encoder
4. Authentication + XML

Test code is provide as examples of this.

More examples will be added if I have time.

NB thanks to Mikl@maplambda https://gitlab.com/maplambda - the first example borrowed from his zio-http4s example

Other projects with examples (including the tapir library) can be found by folling the zio documentation.


