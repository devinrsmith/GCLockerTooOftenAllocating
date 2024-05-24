# GCLockerTooOftenAllocating

This is meant to be a small reproduction of the error "Retried waiting for GCLocker too often allocating".
In combination with `java.lang.ref.SoftReference`, we are able to get an {@link OutOfMemoryError} error, which seems to
break its JavaDoc contract:

> All soft references to softly-reachable objects are guaranteed to have been cleared before the virtual machine throws an OutOfMemoryError

## Build

```shell
./gradlew installDist
```

## Run

```shell
JAVA_HOME=... APP_OPTS="-Xmx4g" ./app/build/install/app/bin/app
```

## Error

Various combinations of JVMs and GC collectors will produce (or seem to be immune to) the error 

```
[1.242s][warning][gc,alloc] main: Retried waiting for GCLocker too often allocating 524290 words
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
        at java.base/java.nio.HeapByteBuffer.<init>(HeapByteBuffer.java:71)
        at java.base/java.nio.ByteBuffer.allocate(ByteBuffer.java:391)
        at io.deephaven.example.GCLockerTooOftenAllocating.main(GCLockerTooOftenAllocating.java:45)
```

## Notes

It's useful to set `-XX:+HeapDumpOnOutOfMemoryError`; upon examination, the dump will show that the majority of the heap
is taken up by reclaimable `byte[]` objects (via `Reference[]` -> `SoftReference` -> `HeapByteBuffer` -> `byte[]`).

The `-XX:GCLockerRetryAllocationCount` _may_ be set to a high number to workaround this issue, although that it isn't a
very satisfying solution.

It appears that the ZGC collector is not prone to this error condition.
Probably because it had already been noticed and fixed in [JDK-8289838 ZGC: OOM before clearing all SoftReferences](https://bugs.openjdk.org/browse/JDK-8289838)?

## References

* https://shipilev.net/jvm/anatomy-quarks/9-jni-critical-gclocker/
* https://tech.clevertap.com/demystifying-g1-gc-gclocker-jni-critical-and-fake-oom-exceptions/
* https://github.com/adoptium/adoptium-support/issues/1096
* https://mail.openjdk.org/pipermail/hotspot-gc-use/2024-May/002938.html
