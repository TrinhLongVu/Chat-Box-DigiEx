current_dir=$(pwd)

# Return to the original directory
cd "$current_dir"

find src -name "*.java" -print | xargs javac -cp ../support/target/support.jar -cp ../server/target/server.jar -d target/classes

mkdir -p target/lib
cp ../support/target/support.jar target/lib/
cp ../server/target/server.jar target/lib/

jar cvfm target/load.jar META-INF/MANIFEST.MF -C target/classes . -C target lib

java -jar target/load.jar