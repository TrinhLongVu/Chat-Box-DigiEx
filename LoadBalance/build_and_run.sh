find src -name "*.java" -print | xargs javac -cp lib/support.jar -cp lib/server.jar -d target/classes

mkdir -p target/lib
cp lib/support.jar target/lib/
cp lib/server.jar target/lib/

jar cvfm target/app.jar META-INF/MANIFEST.MF -C target/classes . -C target lib

java -jar target/app.jar

