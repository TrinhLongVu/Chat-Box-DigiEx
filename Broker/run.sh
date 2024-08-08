find src -name "*.java" -print | xargs javac -cp lib/support.jar -d target/classes

mkdir -p target/lib
cp lib/support.jar target/lib/

jar cvfm target/broker.jar META-INF/MANIFEST.MF -C target/classes . -C target lib

java -jar target/broker.jar

