# this script uploaded a .ipa file to the apple store from a pc
# so you don't really need a real mac and "transporter" after all.
# download the .ipa and the AppStoreInfo.plist files from the build server,
# then run this script.
echo "uploading %1"
"C:\Program Files (x86)\itms\iTMSTransporter.cmd" -m upload -o g:\temp\log\mylogFile.log -u "ddyer-apple@real-me.net" -p vofm-iyor-uhmx-pksm -v eXtreme -assetFile "g:\share\projects\boardspace-codename1\releases\%1" -assetDescription g:\share\projects\boardspace-codename1\releases\v

