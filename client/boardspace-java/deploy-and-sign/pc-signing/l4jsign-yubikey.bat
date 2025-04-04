rem #
rem # signs the boardspace.exe created by launch4j using certificate on yubikey
rem # see https://support.yubico.com/hc/en-us/articles/360016614840-Code-Signing-with-the-YubiKey-on-Windows
rem # the third arg is the fingerprint of the key, presumably will change when a new key is issued
rem #
echo on
.\sign-yubikey.bat "unsigned\Boardspace.exe" "signed\boardspace.exe" 970b8ca191f7fc94e387f93d2e51b6a2f0fb4335
copy signed\boardspace.exe g:\share\projects\boardspace-html\htdocs\java\launcher\boardspace.exe
