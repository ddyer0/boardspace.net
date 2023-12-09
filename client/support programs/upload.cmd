rem this script uploaded a .ipa file to the apple store from a pc
rem so you don't really need a real mac and "transporter" after all.
rem download the .ipa and the AppStoreInfo.plist files from the build server,
rem then run this script.
set dir=%__CD__%
echo "uploading "%dir%%1"
c:
cd "\Program Files (x86)\itms\"
set ITMSTRANSPORTER_FORCE_ITMS_PACKAGE_UPLOAD=true
iTMSTransporter.cmd  -asc_provider "N9ZJLP5456" -m upload -o %dir%upload.log -u "ddyer-apple@real-me.net" -p vofm-iyor-uhmx-pksm  -assetFile "%dir%%1" -assetDescription "%dir%AppStoreInfo.plist" 
g:
