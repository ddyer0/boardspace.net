#
#
require 5.001;
#use Win32::Process; 
#use Win32;
use strict;
require "config.pl";
require "../$lib'lib/common.pl";

$'extraargs = "";  # /P to get a message box
$'native_perl = 1; # if 1, use native stuff,otherwise use win32 process
#
# the native=1 version has no known problems, and is simpler.
# the native=0 version has problems passing & to graphic services.
# ... but the native=0 version was originally developed to avoid bugs
# ... in the native=0 version, (we hope) have since been fixed.
#
#
$'graphic_services = "\\apache\\graphic-services\\graphic services.exe";
$'plugins_dir = "\\apache\\graphic-services\\plug-ins\\";

use vars qw($in);
use CGI  qw(:cgi);
#my $in = new CGI;


#
# create a hidden process.  This avoids a problem with per2exe where
# command windows pop up every time perls native `cms` is used
#
sub backquote()
{   my ($cmd,$resultfile) = @_;

	{	if($resultfile)
		{ my $val = `$cmd > $resultfile`;
			return($val,$?);
		}
		else
		{
		my $val = `$cmd`;
		return($val,$?);
		}
	}
}


#
# this is the module that actually invokes the graphic services
# program.  The principle entry point is "service_runner" which
# should be called with in,out,filter,preset,scale,new,log
#
# new forces the UI to be popped, even though in server mode
# log forces appending to a permanant log file, or STDOUT to return the value as a string
#
# $rot is the rotation angle (degrees CCW), and $crop is the crop amount (pixels: l,t,r,b)
#
sub service_runner()
{  my ($in,$out,$filter,$preset,$scale,$new,$log,$xarg,$yarg,$mask,$rot,$crop) = @_;
   my $oo = $out;
   #
   # translate pathname components to windoze
   #
   $in =~ y/\//\\/;
   $out =~ y/\//\\/;
   $filter =~ y/\//\\/;
   $preset =~ y/\//\\/;
	$in = "\"$in\"";
	$out = "\"$out\"";
   # run the filter
   #
   my $com = "\"$'graphic_services\" /c /i$in /o$out /l$log $'extraargs";

	 my $ee = "";
   {my $index = index($preset,'!');
   if($index>0)
	 { my $index2 = index($preset,'.',$index);
	   my $id = substr($preset,$index+1,$index2-$index-1);
	   $ee = " /E$id ";
	 }}
   {my $index = index($filter,'!');
   if($index>0)
	 { my $index2 = index($filter,'.',$index);
	   my $id = substr($filter,$index+1,$index2-$index-1);
	   $ee = " /E$id ";
	   $filter = substr($filter,0,$index) . substr($filter,$index2);
	 }}
	 $com .= $ee;
   if($filter ne "") { $com .="/f$filter"; }
   if($preset ne "") { $com .= "/p$preset"; }
   if($scale ne "") { $com .= "/s$scale"; }
   if($new ne "") { $com .= "/U"; }
   if($xarg ne "") { $com .= "/X".$xarg; }
   if($yarg ne "") { $com .= "/Y".$yarg; }
   if($out eq "") { $com .= "/Q"; }
   if($mask ne "") { $com .= "/M$mask"}
   if($rot ne "") { $com .= "/R$rot"}
   if($crop ne "") { $com .= "/D$crop"}

   my ($val,$result_code) = &backquote($com);
		if($result_code==0)
		{ $val = "ok<br>".$val;
		}
	  else 
	   { if($oo) { unlink($oo);  }
	   	 $val = "error<br>\ncom=$com<br>\nval=$val"; 
	   }
	   return($val);
}

sub service_runner_x()
{  my ($in,$out,$filter,$preset,$args) = @_;
	 my $tmp = &addroot("temp/info$$");
   my $oo = $out;
   my $val="";
   #
   # translate pathname components to windoze
   #
   $in =~ y/\//\\/;
   $out =~ y/\//\\/;
   $filter =~ y/\//\\/;
   $preset =~ y/\//\\/;
   #
   # run the filter
   #
    my $com = "\"$'graphic_services\" /c /i$in /o$out $'extraargs";
	  my $ee = "";
   {my $index = index($preset,'!');
   if($index>0)
	 { my $index2 = index($preset,'.',$index);
	   my $id = substr($preset,$index+1,$index2-$index-1);
	   $ee = " /E$id ";
	 }}
   {my $index = index($filter,'!');
   if($index>0)
	 { my $index2 = index($filter,'.',$index);
	   my $id = substr($filter,$index+1,$index2-$index-1);
	   $ee = " /E$id ";
	   $filter = substr($filter,0,$index) . substr($filter,$index2);
	 }}
	 $com .= $ee;
	 

   if($filter ne "") { $com .="/f$filter"; }
   if($preset ne "") { $com .= "/p$preset"; }
   if($out eq "") { $com .= "/Q"; }
   if($args ne "") { $com .= $args; }

   my ($val,$result_code) = &backquote($com);
	 if($result_code==0) 
	 	{ $val = "ok<br>".$val; }
	   else  
	   	{
			&log_event(&config('logfile'),"service_runner_x","error $result_code running $com");
			if(-f $oo) { unlink($oo);  }
			$val = "error<br>\ncom=$com<br>\nval=$val"; 
			}
   return($val);
}

sub getImageInfo()
{	my ($name) = @_;
	my $val = &service_runner_x($name,"","",""," /l$STDOUT/C");
	#print "info $name is: $val\n";
  return($val);
}

sub getImageInfoOk()
{ my ($name) = @_;
	my ($val) = &getImageInfo($name);
	if(!(substr($val,0,6) eq "ok<br>")) 
		{ $val=""; 
		}
	return($val);
}
sub getImageSize()
{	my ($path,$info) = @_;
	if(!$info) { $info = &getImageInfoOk($path); }
	if($info ne "")
		{
		 my ($format, $bytes, $w, $h, $sel, $type) = split(/ /,substr($info,6));
		 return($w,$h,$info);
		}
}
sub getImageHasSelection()
{	my ($path,$info) = @_;
	if(!$info) { $info = &getImageInfoOk($path); }
	if($info ne "")
		{
		 my ($format, $bytes, $w, $h, $sel, $type) = split(/ /,substr($info,6));
		 return($sel,$info);
		}
}

#
# create the subscructure for a customer account
#
sub create_customer()
{	my ($uid) = @_;
	my ($cname) = &dircat(&getroot(),"customers/$uid");
	mkdir($cname,0);
	mkdir(&dircat($cname,"images"),0);
	mkdir(&dircat($cname,"thumbnails"),0);
	mkdir(&dircat($cname,"temp"),0);
}
#
# given a filter name and a parameter file name, look for the reference image
# and create it if it is not there.  If the sample image name is omitted, sample.jpg
# is used.  The sample should be the actual size the thumbnail samples should be.
# INPUT: filter name, effect name, sample image name relative to the web server
# OUTPUT: the reference image file name, relative to the web server
# SIDE Effect: Creates the image if not already present, by running the web service.
# 
sub get_reference_image()
{   my ($filter,$effect,$srcimage) =@_;
    my $root = &getroot();
		my ($name,$path,$type) = &filename_split($effect);
    if($srcimage eq "") { $srcimage = $path."sample.jpg"; }
    my ($thumbname) = "$effect.jpg";
    my ($fullthumb) = &dircat($root,$thumbname);
    if(!(-f $fullthumb))
    { #file doesn't exist, we need to create it
		&service_runner(&dircat($root,$srcimage),		#$in,
										$fullthumb,									#out
										$filter,   	            		#filter
										&dircat($root,$effect),			#preset
										""    ,""   ,""  ,					#scale,new,log
										""  ,"",    "1");						#xarg,yarg,mask
    }
    return($thumbname);
}

#
# make a thumbnail image of a certain size, unless it already exists
# the arguments are web-relative pathnames
#
sub make_image_thumbnail()
{	my ($image_name,$thumb_name,$maxx) = @_;
  my $root = &getroot();
  my ($fullthumb) = &dircat($root,$thumb_name);
  my ($fullsrc) = &dircat($root,$image_name);
  return(&make_root_image_thumbnail($fullsrc,$fullthumb,$maxx));
}
#
# make a thumbnail image of a certain size, unless it already exists
# the arguments are absolute pathnames
#
sub make_root_image_thumbnail()
{	my ($fullsrc,$fullthumb,$maxx,$auxargs) = @_;
	#&xprint("thumb from $fullsrc to $fullthumb<p>");
  if(!(-f $fullthumb))
    { #file doesn't exist, we need to create it
	  if($maxx eq "") { $maxx = 200; }
	  #&xprint("running<p>");
		return(&service_runner_x($fullsrc,$fullthumb,"","","/s$maxx,$maxx$auxargs"));
   }
   else
	{	return("ok<br>");
	}
  #&xprint("done<p>");
}
#
# make a thumbnail image of a certain dots per inch, unless it already exists
# the arguments are absolute pathnames.  According to Donn, the algorithm
# for guessing the DPI of the source is as follows:
#
#	If the input resolution is undetermined (ie JPEG files), it is assumed
#	to be 300 dpi for the purposes of dpi scaling. For all other purposes it
#	is assumed to be 72 dpi.
#	If the output file is TIFF or PSD, the reso in the file header is reset
#	to the new value.
#
# return "ok<br>" if ok
sub make_root_image_dpi_thumbnail()
{	my ($fullsrc,$fullthumb,$maxx) = @_;
	#&xprint("thumb from $fullsrc to $fullthumb<p>");
  if(!(-f $fullthumb))
    { #file doesn't exist, we need to create it
	  if($maxx eq "") { $maxx = 100; }
		return(&service_runner($fullsrc,$fullthumb,"","","$maxx,0",
										  "","","","","1"));
    }
    else
	{	return("ok<br>");
	}
}

#
# given an image name, look for a corresponding thumbnail image, and create
# it if it doesn't exist.  The thumbnail image will always be a jpeg whose.
# if the max size is not specified, 200 is used.
# INPUT: image name relative to the web server, max dimension
# OUTPUT: the thumbnail file name, relative to the web server
# SIDE Effect: Creates the image if not already present, by running the web service.
#
sub get_thumbnail_image()
{   my ($image,$maxx) =@_;
    my ($name,$path,$type) = &filename_split($image);
    my $root = &getroot();
    my ($thumbname) = &dircat("/temp",$name)."-thumb.jpg";
    &make_image_thumbnail($image,$thumbname,$maxx);
    return($thumbname);
}

#
# make sure there's a thumbnail image
#
sub make_effect_thumbnail()
{	my ($effect_name) = @_;
  my ($filter_name,$effect_path,$effect_id) = &filename_split($effect_name);
  my ($refimage) = &get_reference_image(&dircat("plug-ins/",$filter_name).".8bf",$effect_name);
	return($refimage);
}

#
# print a table cell intended as the radio selection of the effect.
# INPUT: given the effect parameter file, path relative to the web server
# OUTPUT: some text
#
sub print_table_entry()
{   my ($effect_name) = @_;
		my $refimage = &make_effect_thumbnail($effect_name);
		my ($name,$dir,$type) = &filename_split($effect_name);
	  $refimage =~ s/\ /%20/g;

    print "<td width=200 ><img src=\"$refimage\" alt='$effect_name'>\n";
    print "<input type=radio name=effect value=\"$effect_name\">\n";
    print "$name$type";
    print "</td>\n";
}

#
# print a table of all the effects available in the directory
#
sub print_effects_table()
{ my ($dir,$columns) = @_;
  my $root = &getroot();
  my $gstring = &dircat($root,$dir);
  my @files = &list_dir($gstring);
  my $filename;
  my $index=0;
  if($columns==0) { $columns=2; } 
  print "<table nrows=3>";
  print "<tr>\n";
  foreach $filename(@files)
  { if(($filename ne ".") && ($filename ne ".."))
    {my $file = &dircat($gstring,$filename);
     my ($name,$path,$type) = &filename_split($file);
     if( $type =~ /\.[0-9]+/ )
        { #reassemble a server-relative file name instead of a root-relative file name
	  if($index==0) { print "<tr>"; }
          &print_table_entry(&dircat($dir,$name).$type);
	  $index++;
	  if($index==$columns)
	     { print "</tr>\n";
	       $index=0; 
	     }
          }
    }
  }
  if($index!=0) { print "</tr>\n"; }
  print "</table>\n";
}

#
# print a table of all the effects available in the directory
#
sub print_effects_in_category()
{ my ($dir,$andinfo,$size) = @_;
  my $root = &getroot();
  my $gstring = &dircat($root,$dir);
  my @files = &list_dir($gstring);
  my $filename;
  my $index=0;
  foreach $filename(@files)
  {
    if(($filename ne ".") && ($filename ne ".."))
    {my $file = &dircat($gstring,$filename);
     my ($name,$path,$type) = &filename_split($file);
    if( $type =~ /\.[0-9]+/ )
                { #reassemble a server-relative file name instead of a root-relative file name
 							 	  my $fullname = &dircat($dir,$name.$type);
 									my $refimage = &make_effect_thumbnail($fullname,$size);
                  print "\"$filename\" ";
						      if($andinfo)
						    	{
						  	 		my ($info)=&getImageInfoOk(&dircat($root,$refimage));
							  		print " $info\n";
						    	}
               }
	  }
	 #terminate with eol.  Netscape really cares!
	 print "\n";
}}

#
# print the list of images belonging to a customer
#
sub print_images_in_category()
{ my ($dir,$andinfo,$size) = @_;
  my $root = &getroot();
  my $gstring = &dircat(&dircat($root,$dir),"images");
  my @files = &list_dir($gstring);
  my $filename;
  my $index=0;
  foreach $filename(@files)
	  { if(($filename ne ".") && ($filename ne ".."))
	    { my $file = &dircat($gstring,$filename);
	      my ($name,$path,$type) = &filename_split($file);
	      #reassemble a server-relative file name instead of a root-relative file name
	      my $fullname = &dircat($dir,"images/").$name.$type;
	      my $thumbdir = &dircat($dir,"thumbnails/").$name.$type;
	      my $isjpeg = ($type eq ".jpg");
	      &make_image_thumbnail($fullname,$thumbdir,$size);
	      if(!$isjpeg)
	    		{#also create a jpeg for viewing 
	    	 	 my $jpegdir = &dircat($dir,"thumbnails/").$name.".jpg";
	    		 &make_image_thumbnail($thumbdir,$jpegdir,$size);	
	    		}
	      print "\"$name$type\" ";
	      if($andinfo)
	    	{
	    		my ($info)=&getImageInfoOk(&dircat($root,$fullname));
	    		if(!$info) { $info="error<br>"; }
	    		print " $info\n";
	    	}
	    }
	  }
	 #terminate with eol.  Netscape really cares!
	 print "\n";
}

#
# print a selection table for all images in a category
#
sub print_images_selector()
{ my ($dir) = @_;
  my $root = &getroot();
  my $gstring = &dircat(&dircat($root,$dir),"/images");
  my @files = &list_dir($gstring);
  my $filename;
  my $index=0;
  #print "$gstring\n @files\n";
  print "<table><tr>\n";
  foreach $filename(@files)
	  { if(($filename ne ".") && ($filename ne ".."))
	    { my $file = &dircat($gstring,$filename);
	      my ($name,$path,$type) = &filename_split($file);
				#reassemble a server-relative file name instead of a root-relative file name
 				my $fullname = &dircat($dir,"images/").$name.$type;
 				my $thumbdir = &dircat($dir,"thumbnails/").$name.".jpg";
				&make_image_thumbnail($fullname,$thumbdir,"");
	      if($index==0) { print "<tr>"; }
     	  $thumbdir =~ s/\ /%20/g;
	      print "<td><img src=\"$thumbdir\"><br>\n";
	      my $finalname = "$name$type";
	      print "<input type=radio value=\"$finalname\" name=selection><br>\n";
	      print "</td>";
	      $index++;
	      if($index==4) { $index=0; print "</tr>\n"; }
	  }}
  if($index!=0) { print "</tr>\n"; }
  print "</table>\n";
}

#
# print a selector pulldown of all the parent directories including the selected directory
#
sub print_effects_selector()
{  my ($selected_dir) = @_;
   my ($name,$path,$type) = &filename_split($selected_dir);
   my $root = &getroot();
   my $rdir = &dircat($root,$path);
   my @files = &list_dir($rdir);
   #print "$root -> $rdir\n";
   #print "@files\n";
   print "<b>Effect Group: </b>\n";
   print "<select name=filter_group>\n";

   my $filename;
   foreach $filename(@files)
   { my $file = &dircat($rdir,$filename);
     if(-d $file && ($filename ne ".") && ($filename ne ".."))
     {	 my $file = &dircat($rdir,$filename);
	 my ($xname, $xpath, $xtype) = &filename_split($file);
	 my $newpath = &dircat($path,$xname).$xtype;
	 my $sel = (($newpath eq $selected_dir) ? "selected" : "");
         print "<option value=\"$newpath\" $sel>$xname.$xtype</option>\n";
     }
   }
   print "</select>\n";
   print "<input type=submit name=newgroup value=Change></input>\n";
   print "<p>\n";
}
#
# print the list of subcategories of graphic services
#
sub print_effects_categories()
{  my ($selected_dir) = @_;
   my ($name,$path,$type) = &filename_split($selected_dir);
   my $root = &getroot();
   my $rdir = &dircat($root,$path);
   my @files = &list_dir($rdir);
   #print "$root -> $rdir\n";
   #print "@files\n";
   my $file;
   foreach $file(@files)
   { my $fullfile = &dircat($rdir,$file);
     if(-d $fullfile && ($file ne ".") && ($file ne ".."))
     { print "\"$file\" ";
     }
   }
  #terminate with eol.  Netscape really cares!
  print "\n";
}
sub print_filter_selector()
{	 	my ($default) = @_;
		my @plugs = &list_dir($'plugins_dir);
		my $filename;
		print "<select name=filtername>\n";
  	foreach $filename(@plugs)
		{ my ($name,$dir,$type) = &filename_split($filename);
			if(lc($type) eq ".8bf")
			{ my $sel = (lc($name) eq lc($default)) ? "selected" : "";
			  print "<option value='$name' $sel>$name</option>\n";
			}
		}
		print "</select>\n";
}
sub print_effects_frame()
{ 
	 my($input_file_name,$output_file_name,$effects_dir,$makenew,$filtername,$entrypoint) = @_;
   my $thumbnail_name;

   if($makenew) 
      { $thumbnail_name = &dircat($effects_dir,"sample.jpg");
      }else
      { $thumbnail_name = &get_thumbnail_image($input_file_name,300);
      }
	 my ($ptype) = &program_type();

   print "<form action=/cgi-bin/gs/gsdemo-present$ptype>\n";

   print "<input type=hidden value=\"$thumbnail_name\" name=thumbnail_file_name>\n";
   print "<input type=hidden value=\"$input_file_name\" name=input_file_name>\n";
   print "<input type=hidden value=\"$output_file_name\" name=output_file_name>\n";
   print "<input type=hidden value=\"$effects_dir\" name=effects_dir>\n";
   print "<input type=hidden value=\"$makenew\" name=makenew>\n";
   print "<table>\n";
   print "<tr><td><table>\n";

   my $sample_name = &dircat($effects_dir,"sample.jpg");
   $sample_name =~ s/\ /%20/g;
   print "<tr><td><img src=\"$sample_name\" alt='sample.jpg'></td></tr>\n";
   if($makenew eq "")
   {
   $thumbnail_name =~ s/\ /%20/g;
   my ($tn,$tp,$tt) = &filename_split($thumbnail_name);
	 print "<tr><td><img src=\"$thumbnail_name\" alt='$tn.$tt'>\n";
   print "</td></tr>";
   }
   print "</table><td>\n";
   print "<h2>Select an Effect to apply</h2>\n";
   &print_effects_selector($effects_dir);
   &print_effects_table($effects_dir);
   print "</td></tr></table>\n";
   if($makenew ne "")
   { print "<b>filter:</b> ";
   	 &print_filter_selector($filtername);
   	 if($entrypoint eq "") { $entrypoint = "0"; }
   	 print "subfilter: <input type=text name=entrypoint value=\"$entrypoint\" size=2>\n";
   	 print "number: <input type=text name=isnew value =\"\" size=4>\n";
   }
   print "<input type=submit name=thumbnail value=\"Do It\">\n";
   print "</form>";
}

#
# print the frame containing the list of images and the option to upload
#
sub print_index_frame()
{   my ($dir) = @_;
		my ($ptype) = &program_type();
    print "<form method='POST' enctype='multipart/form-data' action='/cgi-bin/gs/gsdemo$ptype'>\n";
    &print_images_selector($dir);
    $dir =~ s/\ /%20/g;
    print "<input type=hidden value=\"$dir\" name=source_area>\n";
    print "<input type=submit value=\"Edit This\" name=useimage>\n";
    print "<br>\n";
    print "Or Upload your own jpeg image to manipulate<br>\n";
    print "<input name=upfile type=file SIZE=30>\n";
    print "<input type=submit value=\"Upload Image\" name=upload>\n";
    print "</form>\n";
}
#
# print an upload form
#
sub print_upload_form()
{		my ($first,$cust) = @_;
	  if($first)
		{ print "<script language=javascript>  opener.pushuploadbutton(this); </script>\n";
		}
		my ($ptype) = &program_type();

    print "<form method='POST' enctype='multipart/form-data' action='/cgi-bin/gs/gsupload$ptype'>\n";
    print "<input type=hidden name=\"customername\" value=\"$cust\">\n";
    print "<input name=upfile type=file SIZE=50>\n";
    print "<input type=submit value=\"Upload Image\" name=upload>\n";
    print "</form>\n";
    print "Image formats we support: include JPEG, GIF, TIFF and PSD.<br>";
		print "<b>psd</b> format images with saved selections are supported<p>";
		print "Also, be patient after hitting the <i>upload</i> button;  The data is";
		print "transferred first, before there is any obvious response.";
}

#
# this list should agree with graphic services.exe
# until we implement a better mechanism
#
my @cross_display_filters =
	(
			"cmulti", "designs", "diffract" , "halo",
				"prism","rainbow", "reflect", "smulti",
				"star","velocity","velociraptor",
				"3-d","screens","techture","shadow-install","varifocus",
				"redeye"
					);

sub is_cross_display_effect()
{	my ($effectspec) = @_;
	my $ef;
	my ($effect,$path,$type) = &filename_split($effectspec);
	$effect = lc($effect);
	foreach $ef(@cross_display_filters)
	{ if ($effect eq $ef) { return(1); }
	}
	return(0);  
}
