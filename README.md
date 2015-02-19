# java-piped-streams-test

This repository is used to demonstrate a (supposed) bug in `java.io.PipedInputStream` (the Oracle Java 1.7.0_67 implementation). It is a minimal working example to deminstrate that the `PipedInputStream` contains a bug (?) which causes the corresponding `PipedOutputStream` to wait up to one second for free space to write to, when actually the space is free sooner. One second might be rather long for some applications.

## Problem Description

The basic problem boils down to the following code:

    private static void threadA() throws IOException, InterruptedException {
      logA("Filling pipe...");
      pos.write(new byte[5]);
      logA("Pipe full. Writing one more byte...");
      pos.write(0);
      logA("Done.");
    }
    
    private static void threadB() throws IOException, InterruptedException {
      logB("Sleeping a bit...");
      Thread.sleep(100);
      logB("Making space in pipe...");
      pis.read();
      logB("Done.");
    }

`pis` and `pos` are connected `PipedInputStream` and `PipedOutputStream` instances, respectively. `logA` and `logB` are helper functions which output the thread name (A or B), a timestamp in milliseconds and the message. The output is as follows:

         0 A: Filling pipe...
         6 B: Sleeping a bit...
         7 A: Pipe full. Writing one more byte...
       108 B: Making space in pipe...
       109 B: Done.
      1009 A: Done.

As one can see, there is one second (1000 ms) between `B: Done` and `A: Done`. This is caused by the implementation of `PipedInputStream` in the Oracle Java 1.7.0_67, which is as follows:

    private void awaitSpace() throws IOException {
        while (in == out) {
            checkStateForReceive();

            /* full: kick any waiting readers */
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
    }

The `wait(1000)` is only interrupted by either hitting the timeout (1000 ms, as seen above), or a call to `notifyAll()`, which only happens in the following cases:

* In `awaitSpace()`, before `wait(1000)`, as we can seein the snippet above
* In `receivedLast()`, which is called when the stream is closed (not applicable here)
* In `read()`, but **only if `read()` is waiting for an empty buffer to fill up** -- also not applicable here

Oracle's implementation of the `read()` method is quite long, I won't include it here, but it can be found on the web (for example [here](http://www.docjar.com/html/api/java/io/PipedInputStream.java.html) at lines 304 to 342).

## Suggested Fix

A suggested patch would be to add a call to `notifyAll()` in `read()` immediately before the `return` statement. I'm not sure how that would affect overall performance, but it fixes the demonstrated issue (`awaitSpace()` exits immediately instead of waiting up to 1000 ms).

## Affected Java Versions

The following versions have been verified as affected.

    $ java -version
    java version "1.7.0_67"
    Java(TM) SE Runtime Environment (build 1.7.0_67-b01)
    Java HotSpot(TM) 64-Bit Server VM (build 24.65-b04, mixed mode)

    $ java -version
    java version "1.8.0_31"
    Java(TM) SE Runtime Environment (build 1.8.0_31-b13)
    Java HotSpot(TM) 64-Bit Server VM (build 25.31-b07, mixed mode)

## StackOverflow Question

http://stackoverflow.com/q/28617175/4585628
