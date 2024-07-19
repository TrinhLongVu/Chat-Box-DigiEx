# Chat-Box-DigiEx
# Hướng dẫn sử dụng
## clone git
```bash
git clone https://github.com/TrinhLongVu/Chat-Box-DigiEx.git
```
## nhảy vào thư mục client (tạo file jar cho client)
```bash
find src -name "*.java" -print | xargs javac -d target/classes
jar cvfm target/app.jar META-INF/MANIFEST.MF -C target/classes .
```
## nhảy vào thư mục server (tạo file jar cho server)
```bash
find src -name "*.java" -print | xargs javac -d target/classes
jar cvfm target/app.jar META-INF/MANIFEST.MF -C target/classes .
```
## run server (từ thư mục server)
```bash
java -jar target/app.jar
```
## run client (từ thư mục client)
```bash
java -jar target/app.jar
```
