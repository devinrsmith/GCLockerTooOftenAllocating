# GCLockerTooOftenAllocating

This is meant to be a small reproduction of the error "Retried waiting for GCLocker too often allocating".
In combination with `java.lang.ref.SoftReference`, this seems to break its JavaDoc contract

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
[1.821s][warning][gc,alloc] main: Retried waiting for GCLocker too often allocating 524290 words
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
        at java.base/java.nio.HeapByteBuffer.<init>(HeapByteBuffer.java:64)
        at java.base/java.nio.ByteBuffer.allocate(ByteBuffer.java:363)
        at org.example.App.main(App.java:37)
```

## References

* https://shipilev.net/jvm/anatomy-quarks/9-jni-critical-gclocker/
* https://tech.clevertap.com/demystifying-g1-gc-gclocker-jni-critical-and-fake-oom-exceptions/