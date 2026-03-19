rm ../v102/*.jar
rm ../jws/*.jar
cp *.jar ../v102

rem
rem delete old certificate
rem keytool -storepass BSkeystore -keystore g:/share/usr/ddyer/crypto/Boardspace.keystore  -delete
rem
rem make new certificate
rem keytool -storepass BSkeystore -keystore g:/share/usr/ddyer/crypto/Boardspace.keystore  -genkey -validity 1000 -alias Boardspace
rem
rem check certificate details
rem keytool -printcert -keystore "Boardspace.p12" -storepass "davescommodocert" -list -v
rem sign the copies, not the originals
for %%f in (../v102/*.jar) do F:\java\jdk-15.0.1\bin\jarsigner.exe -tsa http://timestamp.digicert.com -storepass BSkeystore -keystore g:/share/usr/ddyer/crypto/Boardspace-2023.keystore ../v102/%%f "mykey"
rem
cp ../v102/*.jar ../jws
cp ../v102/*.jar ../../cheerpj/
