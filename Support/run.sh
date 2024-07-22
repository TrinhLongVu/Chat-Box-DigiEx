find src -name "*.java" -print | xargs javac -d target/classes

jar cvfm target/support.jar META-INF/MANIFEST.MF -C target/classes .
