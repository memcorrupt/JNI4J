# Java4J

Use [JNI](https://docs.oracle.com/en/java/javase/19/docs/specs/jni/functions.html#findclass "Java Native Interface") and [JVMTI](https://docs.oracle.com/en/java/javase/19/docs/specs/jvmti.html "JVM Tool Inteface") features within the comfort of the Java Virtual Machine, without any native libraries required.

This library uses the recently unveiled [Foreign Function & Memory API](https://openjdk.org/jeps/454), officially released as a stable feature in Java 22.

---

The rest of this documentation may use the following abbreviations for the sake of brevity:
- FFM API: Foreign Function & Memory API
- JDK: Java Development Kit
- JVM: Java Virtual Machine

## Why? What's the point? ([skip this and go to the actual documentation...](#documentation))

This project was originally created in late 2023 as the Java 21 release was introduced as the next LTS release, superseding Java 17. As part of this release, [`Thread#stop`](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/lang/Thread.html#stop()) was replaced with a method stub that always threw UnsupportedOperationException. Since some of my projects take advantage of Thread.stop, despite its safety concerns, I decided to research ways to bring the behavior back for future versions.

After some brief research, I realized it was not possible to accomplish this using the APIs offered for normal usage, and that solving this would require a deeper dive. The most viable long term solution was to utilize the JNI and JVMTI APIs to reimplement the primitives I needed; however, this would require the distribution of an additional native module (.dll/.dylib/.so) alongside my software, which would completely remove the portability benefits offered by Java's JAR format.

Soon, I discovered the Foreign Function & Memory API ("FFM API") that was offered as a preview, and nearing standardized release, and realized it would be a perfect solution for my needs. Well..., that's *if* I could get it working.

## It isn't as easy as I thought...

Development of this project took the equivalent of multiple full-time workweeks due to various major obscure difficulties that aimed to prevent my successful completion of this library.

For example, an issue that originally caused annoyance was the 1-based numbering of the functions on the JVMTI documentation, despite the function table actually being 0-based C arrays. On the other hand, the JNI documentation contains the correct 0-based indexes, requiring me to **instead** refer to the source code for proper implementation details.

As I continued development, another issue that plagued me for many hours was the fact that local JNI handles do not play nice with usage of the FFM API since handles are scheduled for release once execution is transferred back to the JVM. After countless hours of digging extensively through the OpenJDK source code and building a debug version of the JVM to make sense of the obscure crash dumps, I was able to figure out a workaround for the seemingly unresolvable crashes I was experiencing.

It seems that when a FFM call to a the native JNI functions returned, the references were getting immediately transferred to a free handles list, and subsequently invalidated as the garbage collector continued to run. The best workaround I was able to implement is to create global references for every local reference *immediately* once a native call finished, so that I could retain the object without the garbage collector trying to invalidate it.

During the extensive resarch required to workaround the local handles issue, I was able to come up with many other possible solutions, such as constructing my own references by copying the [Oop](https://wiki.openjdk.org/display/HotSpot/CompressedOops) data located inside each reference; however, that may not work across all VM implementations available for Java. Additionally, I had to aim to have the least amount of operations between the margin where I obtained a local reference and then successfully converted it to a safe global reference, since my code is essentially racing the garbage collector to obtain a safe reference before the garbage collector completely invalidates it.

To make matters worse, this problem was completely exacerbated when using JVMTI functions that return multiple references at once. Since the frame containing all the local handles from my last native call would be sent to be reused on future calls, the method I would call to convert the unsafe local references to safe, long-term global references would overwrite my handles! This caused a massive issue during my initial implementations of this software, as I needed to iterate through all the program's current threads within the JNI/JVMTI context, but would receive an invalid reference for the first value of the Thread list since it was being overwritten by future calls!

As an initial solution for this issue, I decided to try the idea of manually crafting my own references based on the internal data from the originals, which allowed me to successfully complete my implementation of `Thread.stop`. After this accomplishment, I decided to take a hiatus from the project to allow my mind to rest after being deeply engaged for multiple days in pursuit of making this project functional and suitable for production.

## A few *months* later...

Soon, I decided to finally pick up the project again, armed with new ideas to simplify the inner workings of the techniques involved in this project. With a fresh mind, my new strategy was to implement native functions in an attempt to bypass the local references invalidation issue.

A couple hours later, I finally had native calls functional, with the handler being an [upcalled Java method](https://docs.oracle.com/en/java/javase/19/core/foreign-function-and-memory-api.html#GUID-908061BA-DC97-4524-A390-8FCEF7C5978F). Unfortunately, this did **not** resolve the ongoing issue I had with local references, but on a brighter note, it paved the way for a new idea I had to simplify the functionality of this library.

I decided to use my new functionality allowing native calls to implement a handler that would take an Object as an argument from the Java side, and use the safe local reference that would be passed to my native to create a global reference, which would be returned back to the caller and wrapped into a safe object for usage with other JNI/JVMTI functions. This allowed me to get rid of most of the extra code required to construct Objects on the JNI side before I could do anything useful, such as the code required to fetch class objects, method IDs, create instances, and mutate fields.

This change essentially eliminated a **majority** of the work required to properly interface with the JNI/JVMTI API, as I could do most of the work within Java's normal primitives, and then generate a JNI handle based on the final objects that would need to be passed to the functions I wanted to utilize, *essentially making all my research and hard work finally complete.*

### With that, **JNI4J** was born!

## Documentation

**Coming soon.**

## Contributing

This project currently supports a subset of the JNI/JVMTI API, prioritizing the features that were important to me. If you want to see new methods included, please open an issue or contribute the additions via a pull request.

Extensions to the functionality of this project that **are not already possible to replicate** using normal Java APIs will be prioritized.

## Support and Warranty

This project is provided free of charge with no warranty, and will remain actively maintained while actively used in my products. Once I am no longer actively using this software, updates will happen on a best-effort basis when issues are reported; however, pull requests are always encouraged and will continue to be considered.

You can contact me via email at [mem@mem.rip](mailto:mem@mem.rip).