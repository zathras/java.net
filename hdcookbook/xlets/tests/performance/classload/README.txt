
This test was designed to measure two things:

  *  Get an estimate of classload time for xlets on various players
     in general

  *  Specifically measure the effect of combining multiple GRIN commands
     into one class by using switch(), as opposed to having seperate classes
     for each.

A real xlet will use obfuscated class names, so the classes being measured
all have very terse names.

The test runs nine different configurations of the same xlet, best visualized
as a 3x3 matrix.  It does this twice - once with normal GC in effect, and then
once with a call to System.gc() before it starts timing each run.  This was
done to reveal any surprises in the results, such as the possibility of a
a large amount of jitter due to when GCs happen.

To understand how to use this, check out the results spreadsheet, in
docs/cl_ps3_1.jpg.  This is the observed result of running this test
on a PS/3.  Note that the PS/3 is probably the least interesting player
to run this one, because we already know the answer ("classloading is fast
on a PS/3").  This example is just included for illustration of the
technique - and, besides, it was run on a consumer PS/3, so we didn't
sign any NDA to acquire the player :-)

The test xlet is, in each case, a simulation of a trivial xlet that
contains classes implementing 10, 20 or 100 "commands".  It's talking
about the GoF command pattern, which when implemented in the most
straightforward way leads to one class per command, where each class
defines one essential method (like execute()).  An alternate, "less OO"
implementation strategy is to combine multiple commands into one class
definition by using a switch statement.  We simulate this by generating
fewer classes, but with more methods each in proportion to what would
be required.  Thus, for each number of "commands," we run the test three
times:  One with one command per class, once with two commands per class,
and once with ten commands per class.

For each run, we collect the mean execution time (t-bar), the median
(t-squiggle), and the standard deviation.  The median is probably the
most useful measure, but collecting the mean and standard deviation is
useful to detect anomalies and "random" factors that cause inconsistent run
times.  In the dataset illustrated here, the mean and median are close and
the standard deviation is nominal, so no anomalies were detected.

Using the ten commands case as a control, you can calculate the overhead
of adding a class.  For example, looking at row 1, the median execution time
goes from 73ms to 87ms when ten classes are added, so the time to load one
class is about 1.4ms.  From 20 to 100 it goes from 87 to 184, resulting
in (184-87)/80 or 1.2 ms/class.  Having one control and two samples like
this demonstrates that the results scale and are consistent.

A more interesting comparison is to look at row 1 column 3 vs. row 3
column 3.  Both are testing 100 "commands", with one using 100 classes,
and the other using 10 larger classes.  See command_1_sample.txt and
command_10_sample.txt in docs to see what the generated classes look like.

Comparing these, we see that the median startup time falls from 184ms
to 91ms, for a simulated xlet that does the same thing.  The second
xlet has 90 fewer classes, so the extra classload overhead of dividing
the same number of methods over a greater number of classes is
(184-91)/90, or 1.0ms in this test run.

Thus, we derive two interesting, high-level data points from these
test runs:

    A trivial class takes about 1.2 ms to load on a PS/3

    Combining two small classes into one bigger class can be
    expected to save about 1 ms for the PS/3

As noted above, these actual results aren't very interesting, because they
mostly tell us that a PS/3 is fast.  It's much more interesting to run this
test on slower players -- generally, you'll want to concentrate on the
slowest players you plan to target when evaluating startup time.

There's one other interesting  result lurking in this data.  Look at the
time for "Normal GC" vs. "GC suppressed".  If System.gc() were implemented as
a synchronous GC of the heap, we'd expect the "GC suppressed" numbers to be
faster, since not as much GC would be required.  As you can see, the opposite
is true.  From this, it would appear that System.gc() is /not/ implemented
synchronously in the PS/3; rather, it might just hint to a concurrent GC task
that it should attempt to collect garbage more aggressively.  This kind of
behavior can be counter-intuitive, but it's pretty normal for a moderately
sophisticated generational GC implementation, which is the kind of GC you'd
usually want for media applications.  So, kudos to the PS/3 implementation team!


