find src -name "*.java" -print | xargs javac -cp lib/support.jar -cp ../Server/target/server.jar -d target/classes

mkdir -p target/lib
cp lib/support.jar target/lib/
cp ../Server/target/server.jar target/lib/

jar cvfm target/load.jar META-INF/MANIFEST.MF -C target/classes . -C target lib

java -jar target/load.jar

