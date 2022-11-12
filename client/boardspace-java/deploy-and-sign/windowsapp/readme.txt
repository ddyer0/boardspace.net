
Making an appx suitable for microsoft


create microsoft partner account https://partner.microsoft.com/ 

enable windows "container" feature
acquire a base image (3.5gb!)  https://aka.ms/converter 
  -> C:\Users\Ddyer\Downloads\Windows_BaseImage_DAC_17134.wim

once, DesktopAppConverter -Setup

create msi of the file package using visual studio

each time: 
	turn off windows defender real time protection before running this
	createappx.bat:

	DesktopAppConverter.exe -Installer G:\share\projects\boardspace-java\deploy-and-sign\java-embedded\java-embedded\java-embedded\Express\CD_ROM\DiskImages\DISK1\boardspace.net.msi -Destination g:\share\projects\boardspace-java\deploy-and-sign\windowsapp\ -PackageName "49101Boardspace.net.boardspace.net" -appid "Boardspace.net" -Publisher "CN=BFC04812-51E0-4D8C-8E82-7904031AEDFA" -AppDisplayName "Boardspace.net" -AppDescription "Client program for Boardspace.net" -PackageDisplayName "Boardspace.net" -PackagePublisherDisplayName "Boardspace.net" -Version 2.71.0.0 -MakeAppx

unpack the appx
"c:\Program Files (x86)\Windows Kits\10\App Certification Kit"\makeappx unpack /
d x /p 49101Boardspace.net.boardspace.net.appx

edit the appx manifest to remove "completetrust" and change the entrypoint to Windows:WinMain

repack the appx:
G:\share\projects\boardspace-java\deploy-and-sign\windowsapp\49101Boardspace.net.boardspace.net>"c:\Program Files (x86)\Windows Kits\10\App Certification Kit"\makeappx pack /
d x /p 49101Boardspace.net.boardspace.net.appx

complaints about non-compliant apis, removed WindowsAccessBridge-32.dll and jaas_nt.dll from the java package, 
which it seems (hopefully) are not required.

getting the exes produced to comply with windows requirements
https://stackoverflow.com/questions/12909139/can-windows-store-applications-be-built-with-mingw

// check on windows compliance
"c:\Program Files\Microsoft BinScope 2014\Binscope.exe" jwrap.exe  /checks AppContainerCheck /checks NXCheck /checks DBCheck 
"c:\Program Files\Microsoft BinScope 2014\Binscope.exe" jwrap.exe  /listchecks

https://github.com/status-im/mingw-windows10-uwp/blob/master/MinGWDLL/appcontainer.pl
use appcontainer.pl to set the appcontainer bit for the exe, and all dlls

Final word on Java portable, it still uses a lot of "forbidden" apis.
