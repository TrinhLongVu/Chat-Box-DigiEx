SOURCE_DIR="src"
OUTPUT_DIR="target/classes"
JAR_FILE="target/support.jar"
MANIFEST_FILE="META-INF/MANIFEST.MF"

LB_LIB_DIR="../LoadBalance/lib"
CLIENT_LIB_DIR="../Client/lib"
SERVER_LIB_DIR="../Server/lib"

find $SOURCE_DIR -name "*.java" -print | xargs javac -d $OUTPUT_DIR

jar cvfm $JAR_FILE $MANIFEST_FILE -C $OUTPUT_DIR .

mkdir -p $LB_LIB_DIR
cp $JAR_FILE $LB_LIB_DIR

mkdir -p $CLIENT_LIB_DIR
cp $JAR_FILE $CLIENT_LIB_DIR

mkdir -p $SERVER_LIB_DIR
cp $JAR_FILE $SERVER_LIB_DIR