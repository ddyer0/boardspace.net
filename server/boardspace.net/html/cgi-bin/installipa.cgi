#!/usr/bin/perl
use strict;
use warnings;
use CGI qw(:all);
#use CGI::Carp qw(fatalsToBrowser);
use File::Basename;
use URI::Escape;

# this will cause a graceless error if the upload is too large
$CGI::POST_MAX = 100 * 1_048_576;  # 100 MB

my $cgi       = CGI->new;


my $uploadDir = '/home/boardspa/boardspace.net/html/ipa';
my $baseUrl   = 'https://boardspace.net/ipa';


my $ua    = $ENV{HTTP_USER_AGENT} // '';
my $isIos = &param('install') || (($ua =~ /Safari/i) ? 1 : 0);

# --- Download mode ---
if ($cgi->param('download')) {
    my $file = basename($cgi->param('download'));
    $file =~ s/[^A-Za-z0-9._-]/_/g;
    my $path     = "$uploadDir/$file";
    my $savename = $file =~ s/\.ipa$/.zip/r;

    if (-f $path && $file =~ /\.ipa$/i) {
        my $size = -s $path;
        print $cgi->header(
            -type                 => 'application/zip',
            -attachment           => $savename,
            'Content-Length'      => $size,
            'Content-Disposition' => "attachment; filename=\"$savename\"",
        );
        open(my $fh, '<', $path) or die "Cannot open $path: $!";
        binmode $fh;
        binmode STDOUT;
        local $SIG{PIPE} = 'IGNORE';
        my $buf;
        while (my $bytes = read($fh, $buf, 65536)) {
            last unless $bytes;
            print $buf or last;
        }
        close $fh;
        exit;
    } else {
        print $cgi->header('text/plain');
        print "File not found.\n";
        exit;
    }
}

# --- Scan uploads dir for existing ipas ---
my @ipas;
if (opendir(my $dh, $uploadDir)) {
    @ipas = sort grep { /\.ipa$/i } readdir($dh);
    closedir $dh;
}

# --- Build file list HTML ---
my $fileListHtml = '';
if (@ipas) {
    $fileListHtml .= "<h2>Available Builds</h2><table border='0' cellpadding='6'>";
    for my $ipa (@ipas) {
        my $stem       = $ipa =~ s/\.ipa$//r;
        my $plistPath  = "$uploadDir/$stem.plist";
        my $escapedIpa = escapeHTML($ipa);
        my $size       = -s "$uploadDir/$ipa";
        my $sizeMb     = sprintf("%.1f MB", $size / 1_048_576);

        my $installBtn = '';
        if ($isIos && -f $plistPath) {
            my $plistUrl = "$baseUrl/" . uri_escape($stem) . ".plist";
            my $itmsUrl  = escapeHTML('itms-services://?action=download-manifest&url='
                           . uri_escape($plistUrl));
            $installBtn = "<a class='button green' href='$itmsUrl'>Install</a>";
        }

        my $downloadUrl = escapeHTML($ENV{'SCRIPT_NAME'} . '?download=' . uri_escape($ipa));
        my $savename    = escapeHTML($ipa =~ s/\.ipa$/.zip/r);

        $fileListHtml .= <<ROW;
<tr>
    <td><b>$escapedIpa</b><br/><small>$sizeMb</small></td>
    <td>$installBtn</td>
    <td><a class='button' href='$downloadUrl'>Download as $savename</a></td>
</tr>
ROW
    }
    $fileListHtml .= "</table><hr/>";
} else {
    $fileListHtml = "<p><i>No builds uploaded yet.</i></p><hr/>";
}

# --- Upload handling ---
my ($message, $installLink) = ('', '');

if (($ENV{REQUEST_METHOD} // '') eq 'POST') {
    my $fh      = $cgi->upload('ipa');
    my $rawName = $cgi->param('ipa');

#    $message .= "DEBUG rawName=" . ($rawName//'UNDEF') . "<br/>";
#    $message .= "DEBUG fh=" . ($fh//'UNDEF') . "<br/>";
#    $message .= "DEBUG upload_error=" . ($cgi->cgi_error//'none') . "<br/>";
#    $message .= "DEBUG uploadDir=$uploadDir exists=" . (-d $uploadDir ? 'yes' : 'no') . "<br/>";
#    $message .= "DEBUG uploadDir writable=" . (-w $uploadDir ? 'yes' : 'no') . "<br/>";

    if (!$fh || $rawName !~ /\.ipa$/i) {
        $message .= 'Error: must be a .ipa file';
    } else {
        my $name = basename($rawName);
        $name =~ s/[^A-Za-z0-9._-]/_/g;
        my $dest = "$uploadDir/$name";
        my $stem = $name =~ s/\.ipa$//r;

#        $message .= "DEBUG dest=$dest<br/>";

        open(my $out, '>', $dest) or do {
            $message .= "Error: Cannot write $dest: $!";
            goto DONE;
        };
        binmode $out;
        binmode $fh;
        my $written = 0;
        while (read($fh, my $buf, 65536)) {
            print $out $buf;
            $written += length($buf);
        }
        close $out;
#        $message .= "DEBUG wrote $written bytes<br/>";

        my $bundleId = 'who.cares';
 
        {
            my $ipaUrl   = "$baseUrl/" . uri_escape($name);
            my $plistUrl = "$baseUrl/" . uri_escape($stem) . ".plist";

            open(my $pl, '>', "$uploadDir/$stem.plist") or do {
                $message .= "Error: Cannot write plist: $!";
                goto DONE;
            };
            print $pl "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            print $pl "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\"\n";
            print $pl "  \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n";
            print $pl "<plist version=\"1.0\">\n";
            print $pl "<dict>\n";
            print $pl "    <key>items</key>\n";
            print $pl "    <array>\n";
            print $pl "        <dict>\n";
            print $pl "            <key>assets</key>\n";
            print $pl "            <array>\n";
            print $pl "                <dict>\n";
            print $pl "                    <key>kind</key>\n";
            print $pl "                    <string>software-package</string>\n";
            print $pl "                    <key>url</key>\n";
            print $pl "                    <string>$ipaUrl</string>\n";
            print $pl "                </dict>\n";
            print $pl "            </array>\n";
            print $pl "            <key>metadata</key>\n";
            print $pl "            <dict>\n";
            print $pl "                <key>bundle-identifier</key>\n";
            print $pl "                <string>$bundleId</string>\n";
            print $pl "                <key>bundle-version</key>\n";
            print $pl "                <string>1.0</string>\n";
            print $pl "                <key>kind</key>\n";
            print $pl "                <string>software</string>\n";
            print $pl "                <key>title</key>\n";
            print $pl "                <string>$stem</string>\n";
            print $pl "            </dict>\n";
            print $pl "        </dict>\n";
            print $pl "    </array>\n";
            print $pl "</dict>\n";
            print $pl "</plist>\n";
            close $pl;

            $installLink = 'itms-services://?action=download-manifest&url=' . uri_escape($plistUrl);
            $message = "Upload successful.`";
        }
    }
}

DONE:
my $msgColor = ($message =~ /Upload successful/) ? 'green' : 'red';
my $msgHtml  = $message
    ? "<p style='color:$msgColor;font-weight:bold;font-size:14px'>$message</p>"
    : '';

my $installHtml = '';
if ($installLink && $isIos) {
    my $safeLink = escapeHTML($installLink);
    $installHtml = "<a class='button green' href='$safeLink'>Install on iPad</a>"
                 . "<p><small>Open this page on the iPad to tap Install.</small></p>";
}

my $uploadHtml = $isIos ? '' : <<FORM;
<h2>Upload New Build</h2>
<form method="post" enctype="multipart/form-data">
    <label>IPA File</label><br/>
    <input type="file" name="ipa" accept=".ipa" required/><br/><br/>
    <input type="submit" class="button green" value="Upload"/>
</form>
FORM

print $cgi->header('text/html');
print <<HTML;
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta name="viewport" content="initial-scale=1.0, user-scalable=no, width=320"/>
    <title>IPA Manager</title>
    <style>
        body { font-family: Helvetica, Arial, sans-serif; padding: 20px; }
        .button {
            display: inline-block; margin: 4px; padding: 8px 16px;
            font: bold 16px/20px Helvetica, Arial, sans-serif;
            border-radius: 20px; text-decoration: none; color: #3e5706;
            box-shadow: 1px 1px 1px rgba(0,0,0,.29);
            background: #ddd;
        }
        .green {
            background: linear-gradient(to bottom, #a5cd4e 0%, #6b8f1a 100%);
        }
        .button:active { box-shadow: inset 0 0 3px rgba(0,0,0,.8); }
        input[type=file]   { font-size:16px; margin-bottom:10px; }
        input[type=submit] { cursor:pointer; }
        label { font-weight:bold; font-size:14px; }
        table { width:100%; }
        td { vertical-align:middle; }
        hr { margin: 20px 0; }
    </style>
</head>
<body>
    $fileListHtml
    $uploadHtml
    $msgHtml
    $installHtml

    <p><b>If your build doesn't install see
        <a href="https://www.codenameone.com/blog/my-ios-build-wont-install.html">this article</a>
    </b></p>
</body>
</html>
HTML
