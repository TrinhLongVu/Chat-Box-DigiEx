# Chat Box DigiEx

This chat box aim to practice building Distributed System in Java. Barely, all tech we used was built 
by ourselves. Chat-box-DigiEx is built by a small Backend intern group from DigiEx Group, and running in local only.
- Beside, knowing that putting this in the Internet sound interested to us but this project is using pure Socket of Java and not aim to run in cloud.
- We will keep in mind about the idea migrate to cloud
## Tech input and framework

- Spring/Springboot framework
- Apply LoadBalancer, Message Broker (built in java)

## Getting start
### Start with Command line

- Cloning this repo, or click here to do that.
```bash
git clone https://github.com/TrinhLongVu/Chat-Box-DigiEx.git
```

- Build 1 running Server (port 1234 as default), 1 Loadbalancer, 1 Message Broker and 1 our own library (Support).
- Also, Support will be installed in your local .m2 folder so make sure to have 1 in your local machine
- This will take you a while so please, stay calm.
```bash
./run-cli.sh
```

- Use this to start your Chat application GUI. (As default, you can only start 2 Clients as same time).
```bash
./run-client.sh
```

### Start with Docker
- Build 2 running Server (port 1234 and 1235 as default), 1 Loadbalancer, 1 Message Broker and 1 our own library (Support).
- Also, Support will be installed in your local .m2 folder so make sure to have 1 in your local machine
```bash
./run.sh
```

- Use this to start your Chat application GUI. (As default, you can only start 2 Clients as same time).
```bash
./run-client.sh
```

