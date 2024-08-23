# Navigate to Support directory and run commands
cd Support
mvn clean package
mvn install

# Navigate to Broker directory and run commands
cd ../Broker
mvn clean package
mvn spring-boot:run &

# Navigate to LoadBalance directory and run commands
cd ../LoadBalance
mvn clean package
mvn spring-boot:run &

# Navigate to Server directory and run commands
cd ../Server
mvn clean package
mvn spring-boot:run &