SOURCE_DIR="src"
OUTPUT_DIR="target/classes"
JAR_FILE="target/Support-1.0.jar"
MANIFEST_FILE="META-INF/MANIFEST.MF"

LB_LIB_DIR="../LoadBalance/lib"
CLIENT_LIB_DIR="../Client/lib"
SERVER_LIB_DIR="../Server/lib"
BROKER_LIB_DIR="../Broker/lib"

mkdir -p $LB_LIB_DIR
cp $JAR_FILE $LB_LIB_DIR

mkdir -p $CLIENT_LIB_DIR
cp $JAR_FILE $CLIENT_LIB_DIR

mkdir -p $SERVER_LIB_DIR
cp $JAR_FILE $SERVER_LIB_DIR

mkdir -p $BROKER_LIB_DIR
cp $JAR_FILE $BROKER_LIB_DIR