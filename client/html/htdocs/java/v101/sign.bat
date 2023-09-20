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

rm ..\v102\*.jar
rm ..\jws\*.jar
rm ..\..\cheerpj\*.jar

for %%f in (*.jar) do jarsigner -tsa http://ts.ssl.com -keystore NONE -storetype PKCS11  -providerClass sun.security.pkcs11.SunPKCS11 -certchain "g:/share/usr/ddyer/crypto/david dyer.p7b" -addProvider SunPKCS11 -providerArg g:/share/usr/ddyer/crypto/yubikey-pkcs11-java.cfg -storepass 599100 -signedjar ../v102/%%f %%f "Certificate for PIV Authentication"
rem
cp ../v102/*.jar ../jws
cp ../v102/*.jar ../../cheerpj/
