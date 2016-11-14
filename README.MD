Run admin play frontend
 
 ```sbt```
 ```admin/run```

Create package
```sbt```
```admin\run```
```Ctrl-d (get play console)```
```dist```
New zip file will be in admin/project/target/universal. To run application extract zip file and call:
```./bin/admin -Dplay.crypto.secret=abcdefghijk -Dhttp.port=9001```
