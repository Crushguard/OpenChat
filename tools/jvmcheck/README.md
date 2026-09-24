# jvmcheck

A tiny JVM-only Gradle project that compiles `app/src/main/java/com/piptechnologies/openchat/core/**`
and runs the matching tests under `app/src/test/java/.../core/**` without an Android SDK.

    cd tools/jvmcheck && gradle test

Everything under `core` must stay free of Android imports so this keeps working.
