
for %%f in (../v102/*.jar) do F:\java\jdk-15.0.1\bin\jarsigner.exe -tsa http://timestamp.digicert.com -keystore NONE -storetype PKCS11 -storepass 599100 -addProvider SunPKCS11 -providerArg -providerArg g:/share/usr/ddyer/crypto/yubikey-pkcs11-java.cfg   ../v102/%%f "mykey"


jarsigner -tsa http://ts.ssl.com -keystore NONE -storetype PKCS11  -addProvider SunPKCS11 -providerArg g:/share/usr/ddyer/crypto/yubikey-pkcs11-java.cfg -storepass 599100 -signedjar ../v102/Yspahan.jar Yspahan.jar "Certificate for PIV Authentication"



keytool  -keystore NONE -storetype PKCS11  -addProvider SunPKCS11 -providerArg yubikey-pkcs11-java.cfg -storepass 599100 -list

// using web site certificates
cd "C:\Program Files\Yubico\YubiKey Manager"
.\ykman piv certificates import 82 g:\share\usr\ddyer\crypto\SSLcom-CodeSigning-Root-2022-ECC.pem 
.\ykman piv certificates import 83 g:\share\usr\ddyer\crypto\SSLcom-Intermediate-codeSigning-RSA-4096-R1.pem

// using attachment certificatges
.\ykman piv certificates import 83 g:\share\usr\ddyer\crypto\SSL_COM_CODE_SIGNING_INTERMEDIATE_CA_ECC_R2.crt
.\ykman piv certificates import 82 g:\share\usr\ddyer\crypto\SSL_COM_ROOT_CERTIFICATION_AUTHORITY_ECC.crt

// sign one
f:\java\jdk-18\bin\jarsigner -tsa http://ts.ssl.com -keystore NONE -storetype PKCS11  -addProvider SunPKCS11 -providerArg g:/share/usr/ddyer/crypto/yubikey-pkcs11-java.cfg -storepass 599100 -signedjar ../v102/Yspahan.jar Yspahan.jar "Certificate for PIV Authentication"

// yubikey info
ykman.exe piv info 


