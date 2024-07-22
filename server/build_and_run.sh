find src -name "*.java" -print | xargs javac -d target/classes

jar cvfm target/app.jar META-INF/MANIFEST.MF -C target/classes .

java -jar target/app.jar