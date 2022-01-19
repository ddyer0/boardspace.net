# Tea.pm 
#########################################################################
#        This Perl module is Copyright (c) 2000, Peter J Billam         #
#               c/o P J B Computing, www.pjb.com.au                     #
#                                                                       #
#     This module is free software; you can redistribute it and/or      #
#            modify it under the same terms as Perl itself.             #
#########################################################################
#
# implements TEA, the Tiny Encryption Algorithm, in Perl and Javascript.
# http://www.cl.cam.ac.uk/ftp/papers/djw-rmn/djw-rmn-tea.html
#
# Usage:
#    use Tea;
#    $key = 'PUFgob$*LKDF D)(F IDD&P?/';
#    $ascii_ciphertext = &encrypt ($plaintext, $key);
#    ...
#    $plaintext_again = &decrypt ($ascii_ciphertext, $key);
#    ...
#    $signature = &asciidigest ($text);
#
# The $key is a sufficiently longish string; at least 17 random 8-bit bytes
#
# Written by Peter J Billam, http://www.pjb.com.au

package Crypt::Tea;
$VERSION = '1.44';

# Don't like depending on externals; this is strong encrytion ... but ...
use Exporter; @ISA = qw(Exporter);
@EXPORT=qw(asciidigest encrypt decrypt
 tea_in_javascript str2ascii ascii2str encrypt_and_write);

# begin config
my %a2b = (
	A=>000, B=>001, C=>002, D=>003, E=>004, F=>005, G=>006, H=>007,
	I=>010, J=>011, K=>012, L=>013, M=>014, N=>015, O=>016, P=>017,
	Q=>020, R=>021, S=>022, T=>023, U=>024, V=>025, W=>026, X=>027,
	Y=>030, Z=>031, a=>032, b=>033, c=>034, d=>035, e=>036, f=>037,
	g=>040, h=>041, i=>042, j=>043, k=>044, l=>045, m=>046, n=>047,
	o=>050, p=>051, q=>052, r=>053, s=>054, t=>055, u=>056, v=>057,
	w=>060, x=>061, y=>062, z=>063, '0'=>064,  '1'=>065, '2'=>066, '3'=>067,
	'4'=>070,'5'=>071,'6'=>072,'7'=>073,'8'=>074,'9'=>075,'+'=>076,'_'=>077,
);
my %b2a = reverse %a2b;
# end config

# ------------------ infrastructure ...

sub tea_in_javascript {
	my @js; while (<DATA>) { last if /^EOT$/; push @js, $_; } join '', @js;
}
sub encrypt_and_write { my ($str, $key) = @_;
	return unless $str; return unless $key;
	print
	"<SCRIPT LANGUAGE=\"JavaScript\">\n<!--\nparent.decrypt_and_write('";
	print &encrypt($str,$key);
	print "');\n// -->\n</SCRIPT>\n";
}
sub binary2ascii {
	return &str2ascii(&binary2str(@_));
}
sub ascii2binary {
	return &str2binary(&ascii2str($_[$[]));
}
sub str2binary {   my @str = split //, $_[$[];
	my @intarray = (); my $ii = $[;
	while (1) {
		last unless @str; $intarray[$ii]  = (0xFF & ord shift @str)<<24;
		last unless @str; $intarray[$ii] |= (0xFF & ord shift @str)<<16;
		last unless @str; $intarray[$ii] |= (0xFF & ord shift @str)<<8;
		last unless @str; $intarray[$ii] |=  0xFF & ord shift @str;
		$ii++;
	}
	return @intarray;
}
sub binary2str {
	my @str = ();
	foreach $i (@_) {
		push @str, chr (0xFF & ($i>>24)), chr (0xFF & ($i>>16)),
		 chr (0xFF & ($i>>8)), chr (0xFF & $i);
	}
	return join '', @str;
}
sub ascii2str {   my $a = $_[$[]; # converts pseudo-base64 to string of bytes
	$a =~ tr#A-Za-z0-9+_##cd;
	my $ia = $[-1;  my $la = length $a;   # BUG not length, final!
	my $ib = $[;  my @b = ();
	my $carry;
	while (1) {   # reads 4 ascii chars and produces 3 bytes
		$ia++; last if ($ia>=$la);
		$b[$ib]  = $a2b{substr $a, $ia+$[, 1}<<2;
		$ia++; last if ($ia>=$la);
		$carry=$a2b{substr $a, $ia+$[, 1};  $b[$ib] |= ($carry>>4); $ib++;
		# if low 4 bits of $carry are 0 and its the last char, then break
		$carry = 0xF & $carry; last if ($carry == 0 && $ia == ($la-1));
		$b[$ib]  = $carry<<4;
		$ia++; last if ($ia>=$la);
		$carry=$a2b{substr $a, $ia+$[, 1};  $b[$ib] |= ($carry>>2); $ib++;
		# if low 2 bits of $carry are 0 and its the last char, then break
		$carry = 03 & $carry; last if ($carry == 0 && $ia == ($la-1));
		$b[$ib]  = $carry<<6;
		$ia++; last if ($ia>=$la);
		$b[$ib] |= $a2b{substr $a, $ia+$[, 1}; $ib++;
	}
	return pack 'c*', @b;
}
sub str2ascii {   my $b = $_[$[]; # converts string of bytes to pseudo-base64
	my $ib = $[;  my $lb = length $b;  my @s = ();
	my $b1; my $b2; my $b3;
	my $carry;
	while (1) {   # reads 3 bytes and produces 4 ascii chars
		if ($ib >= $lb) { last; };
		$b1 = ord substr $b, $ib+$[, 1;  $ib++;
		push @s, $b2a{$b1>>2}; $carry = 03 & $b1;
		if ($ib >= $lb) { push @s, $b2a{$carry<<4}; last; }
		$b2 = ord substr $b, $ib+$[, 1;  $ib++;
		push @s, $b2a{($b2>>4) | ($carry<<4)}; $carry = 0xF & $b2;
		if ($ib >= $lb) { push @s, $b2a{$carry<<2}; last; }
		$b3 = ord substr $b, $ib+$[, 1;  $ib++;
		push @s, $b2a{($b3>>6) | ($carry<<2)}, $b2a{077 & $b3};
		if (!$ENV{REMOTE_ADDR} && (($ib % 36) == 0)) { push @s, "\n"; }
	}
	return join ('', @s);
}
sub asciidigest {   # returns 22-char ascii signature
	return &binary2ascii(&binarydigest($_[$[]));
}
sub binarydigest { my $str = $_[$[];  # returns 4 32-bit-int binary signature
	# warning: mode of use invented by Peter Billam 1998, needs checking !
	return '' unless $str;
	# add 1 char ('0'..'15') at front to specify no of pad chars at end ...
	my $npads = 15 - ((length $str) % 16);
	$str  = chr($npads) . $str;
	if ($npads) { $str .= "\0" x $npads; }
	my @str = &str2binary($str);
	my @key = (0x61626364, 0x62636465, 0x63646566, 0x64656667);

	my ($cswap, $v0, $v1, $v2, $v3);
	my $c0 = 0x61626364; my $c1 = 0x62636465; # CBC Initial Value. Retain !
	my $c2 = 0x61626364; my $c3 = 0x62636465; # likewise (abcdbcde).
	while (@str) {
		# shift 2 blocks off front of str ...
		$v0 = shift @str; $v1 = shift @str; $v2 = shift @str; $v3 = shift @str;
		# cipher them XOR'd with previous stage ...
		($c0,$c1) = &tea_code ($v0^$c0, $v1^$c1, @key);
		($c2,$c3) = &tea_code ($v2^$c2, $v3^$c3, @key);
		# mix up the two cipher blocks with a 4-byte left rotation ...
		$cswap  = $c0; $c0=$c1; $c1=$c2; $c2=$c3; $c3=$cswap;
	}
	return ($c0,$c1,$c2,$c3);
}
sub encrypt { my ($str,$key)=@_; # encodes with CBC (Cipher Block Chaining)
	use integer;
	return '' unless $str; return '' unless $key;
	@key = &binarydigest($key);

	# add 1 char ('0'..'7') at front to specify no of pad chars at end ...
	my $npads = 7 - ((length $str) % 8);
	$str  = chr($npads|(0xF8 & &rand_byte)) . $str;
	if ($npads) {
		my $padding = pack 'CCCCCCC', &rand_byte, &rand_byte,
		 &rand_byte, &rand_byte, &rand_byte, &rand_byte, &rand_byte; 
		$str  = $str . substr($padding,$[,$npads);
	}
	my @pblocks = &str2binary($str);
	my $v0; my $v1;
	my $c0 = 0x61626364; my $c1 = 0x62636465; # CBC Initial Value. Retain !
	my @cblocks;
	while (1) {
		last unless @pblocks; $v0 = shift @pblocks; $v1 = shift @pblocks;
		($c0,$c1) = &tea_code ($v0^$c0, $v1^$c1, @key);
		push @cblocks, $c0, $c1;
	}
	my $btmp = &binary2str(@cblocks);
	return &str2ascii( &binary2str(@cblocks) );
}
sub decrypt { my ($acstr, $key) = @_;   # decodes with CBC
	use integer;
	return '' unless $acstr; return '' unless $key;
	@key = &binarydigest($key);
	my $v0; my $v1; my $c0; my $c1; my @pblocks = (); my $de0; my $de1;
	my $lastc0 = 0x61626364; my $lastc1 = 0x62636465; # CBC Init Val. Retain!
	my @cblocks = &str2binary( &ascii2str($acstr) );
	while (1) {
		last unless @cblocks; $c0 = shift @cblocks; $c1 = shift @cblocks;
		($de0, $de1) = &tea_decode ($c0,$c1, @key);
		$v0 = $lastc0 ^ $de0;   $v1 = $lastc1 ^ $de1;
		push @pblocks, $v0, $v1;
		$lastc0 = $c0;   $lastc1 = $c1;
	}
	my $str = &binary2str( @pblocks );
	# remove no of pad chars at end specified by 1 char ('0'..'7') at front
	my $npads = 0x7 & ord $str; substr ($str, $[, 1) = '';
	if ($npads) { substr ($str, 0 - $npads) = ''; }
	return $str;
}
sub triple_encrypt { my ($plaintext,  $long_key) = @_;  # not yet ...
}
sub triple_decrypt { my ($cyphertext, $long_key) = @_;  # not yet ...
}
sub tea_code  { my ($v0,$v1, $k0,$k1,$k2,$k3) = @_;
	# TEA. 64-bit cleartext block in $v0,$v1. 128-bit key in $k0..$k3.
	# &prn("tea_code: v0=$v0 v1=$v1");
	use integer;
	my $sum = 0; my $n = 32;
	while ($n-- > 0) {
		$sum += 0x9e3779b9;   # TEA magic number delta
		$v0 += (($v1<<4)+$k0) ^ ($v1+$sum) ^ ((0x07FFFFFF & ($v1>>5))+$k1) ;
		$v1 += (($v0<<4)+$k2) ^ ($v0+$sum) ^ ((0x07FFFFFF & ($v0>>5))+$k3) ;
	}
	return ($v0, $v1);
}
sub tea_decode  { my ($v0,$v1, $k0,$k1,$k2,$k3) = @_;
	# TEA. 64-bit cyphertext block in $v0,$v1. 128-bit key in $k0..$k3.
	use integer;
	my $sum = 0; my $n = 32;
	$sum = 0x9e3779b9 << 5 ;   # TEA magic number delta
	while ($n-- > 0) {
		$v1 -= (($v0<<4)+$k2) ^ ($v0+$sum) ^ ((0x07FFFFFF & ($v0>>5))+$k3) ;
		$v0 -= (($v1<<4)+$k0) ^ ($v1+$sum) ^ ((0x07FFFFFF & ($v1>>5))+$k1) ;
		$sum -= 0x9e3779b9 ;
	}
	return ($v0, $v1);
}
sub rand_byte {
	if (! $rand_byte_already_called) {
		srand(time() ^ ($$+($$<<15))); # could do better, but its only padding
		$rand_byte_already_called = 1;
	}
	int(rand 256);
}
1;

__DATA__

<SCRIPT LANGUAGE="JavaScript">
<!--
//        This JavaScript is Copyright (c) 2000, Peter J Billam
//              c/o P J B Computing, www.pjb.com.au
// It was generated by the Crypt::Tea.pm Perl module and is free software;
// you can redistribute and modify it under the same terms as Perl itself.

// -- conversion routines between string, bytes, ascii encoding, & blocks --
function binary2ascii (s) {
 return bytes2ascii( blocks2bytes(s) );
}
function binary2str (s) {
 return bytes2str( blocks2bytes(s) );
}
function ascii2binary (s) {
 return bytes2blocks( ascii2bytes(s) );
}
function str2binary (s) {
 return bytes2blocks( str2bytes(s) );
}
function str2bytes(s) {   // converts string to array of bytes
 var is = 0;  var ls = s.length;  var b = new Array();
 while (1) {
  if (is >= ls) break;
  if (c2b[s.charAt(is)] == null) { b[is] = 0xF7;
   alert ('is = '+is + '\nchar = '+s.charAt(is) + '\nls = '+ls);
  } else { b[is] = c2b[s.charAt(is)];
  }
  is++;
 }
 return b;
}
function bytes2str(b) {   // converts array of bytes to string
 var ib = 0;  var lb = b.length;  var s = '';
 while (1) {
  if (ib >= lb) break;
  s += b2c[0xFF&b[ib]];   // if its like perl, could be faster with join
  ib++;
 }
 return s;
}
function ascii2bytes(a) { // converts pseudo-base64 to array of bytes
 var ia = -1;  var la = a.length;
 var ib = 0;  var b = new Array();
 var carry;
 while (1) {   // reads 4 chars and produces 3 bytes
  while (1) { ia++; if (ia>=la) return b; if (a2b[a.charAt(ia)]!=null) break; }
  b[ib]  = a2b[a.charAt(ia)]<<2;
  while (1) { ia++; if (ia>=la) return b; if (a2b[a.charAt(ia)]!=null) break; }
  carry=a2b[a.charAt(ia)];  b[ib] |= carry>>>4; ib++;
  // if low 4 bits of carry are 0 and its the last char, then break
  carry = 0xF & carry;
  if (carry == 0 && ia == (la-1)) return b;
  b[ib]  = carry<<4;
  while (1) { ia++; if (ia>=la) return b; if (a2b[a.charAt(ia)]!=null) break; }
  carry=a2b[a.charAt(ia)];  b[ib] |= carry>>>2; ib++;
  // if low 2 bits of carry are 0 and its the last char, then break
  carry = 3 & carry;
  if (carry == 0 && ia == (la-1)) return b;
  b[ib]  = carry<<6;
  while (1) { ia++; if (ia>=la) return b; if (a2b[a.charAt(ia)]!=null) break; }
  b[ib] |= a2b[a.charAt(ia)];   ib++;
 }
 return b;
}
function bytes2ascii(b) { // converts array of bytes to pseudo-base64 ascii
 var ib = 0;   var lb = b.length;  var s = '';
 var b1; var b2; var b3;
 var carry;
 while (1) {   // reads 3 bytes and produces 4 chars
  if (ib >= lb) break;   b1 = 0xFF & b[ib];
  s += b2a[63 & (b1>>>2)];
  carry = 3 & b1;
  ib++;  if (ib >= lb) { s += b2a[carry<<4]; break; }  b2 = 0xFF & b[ib];
  s += b2a[(0xF0 & (carry<<4)) | (b2>>>4)];
  carry = 0xF & b2;
  ib++;  if (ib >= lb) { s += b2a[carry<<2]; break; }  b3 = 0xFF & b[ib];
  s += b2a[(60 & (carry<<2)) | (b3>>>6)] + b2a[63 & b3];
  ib++;
  if (ib % 36 == 0) s += "\n";
 }
 return s;
}
function bytes2blocks(bytes) {
 var blocks = new Array(); var ibl = 0;
 var iby = 0; var nby = bytes.length;
 while (1) {
  blocks[ibl]  = (0xFF & bytes[iby])<<24; iby++; if (iby >= nby) break;
  blocks[ibl] |= (0xFF & bytes[iby])<<16; iby++; if (iby >= nby) break;
  blocks[ibl] |= (0xFF & bytes[iby])<<8;  iby++; if (iby >= nby) break;
  blocks[ibl] |=  0xFF & bytes[iby];      iby++; if (iby >= nby) break;
  ibl++;
 }
 return blocks;
}
function blocks2bytes(blocks) {
 var bytes = new Array(); var iby = 0;
 var ibl = 0; var nbl = blocks.length;
 while (1) {
  if (ibl >= nbl) break;
  bytes[iby] = 0xFF & (blocks[ibl] >>> 24); iby++;
  bytes[iby] = 0xFF & (blocks[ibl] >>> 16); iby++;
  bytes[iby] = 0xFF & (blocks[ibl] >>> 8);  iby++;
  bytes[iby] = 0xFF & blocks[ibl]; iby++;
  ibl++;
 }
 return bytes;
}
function digest_pad (bytearray) {
 // add 1 char ('0'..'15') at front to specify no of \0 pad chars at end
 var newarray = new Array();  var ina = 0;
 var iba = 0; var nba = bytearray.length;
 var npads = 15 - (nba % 16); newarray[ina] = npads; ina++;
 while (iba < nba) { newarray[ina] = bytearray[iba]; ina++; iba++; }
 var ip = npads; while (ip>0) { newarray[ina] = 0; ina++; ip--; }
 return newarray;
}
function pad (bytearray) {
 // add 1 char ('0'..'7') at front to specify no of rand pad chars at end
 // unshift and push fail on Netscape 4.7 :-(
 var newarray = new Array();  var ina = 0;
 var iba = 0; var nba = bytearray.length;
 var npads = 7 - (nba % 8);
 newarray[ina] = (0xF8 & rand_byte()) | (7 & npads); ina++;
 while (iba < nba) { newarray[ina] = bytearray[iba]; ina++; iba++; }
 var ip = npads; while (ip>0) { newarray[ina] = rand_byte(); ina++; ip--; }
 return newarray;
}
function rand_byte() {   // used by pad
 return Math.floor( 256*Math.random() );  // Random needs js1.1 . Seed ?
 // for js1.0 compatibility, could try following ...
 if (! rand_byte_already_called) {
  var now = new Date();  seed = now.milliseconds;
  rand_byte_already_called = true;
 }
 seed = (1029*seed + 221591) % 1048576;  // see Fortran77, Wagener, p177
 return Math.floor(seed / 4096);
}
function unpad (bytearray) {
 // remove no of pad chars at end specified by 1 char ('0'..'7') at front
 // unshift and push fail on Netscape 4.7 :-(
 var iba = 0;
 var newarray = new Array();  var ina = 0;
 var npads = 0x7 & bytearray[iba]; iba++; var nba = bytearray.length - npads;
 while (iba < nba) { newarray[ina] = bytearray[iba]; ina++; iba++; }
 return newarray;
}

// --- TEA stuff, translated from the Perl Tea.pm see www.pjb.com.au/comp ---

// In JavaScript we express an 8-byte block as an array of 2 32-bit ints
function asciidigest (str) {
 return binary2ascii( binarydigest(str) );
}
function binarydigest (str, keystr) {  // returns 22-char ascii signature
 var key = new Array(); // key = binarydigest(keystr);
 key[0]=0x61626364; key[1]=0x62636465; key[2]=0x63646566; key[3]=0x64656667;

 // Initial Value for CBC mode = "abcdbcde". Retain for interoperability.
 var c0 = new Array(); c0[0] = 0x61626364; c0[1] = 0x62636465;
 var c1 = new Array(); c1 = c0;

 var v0 = new Array(); var v1 = new Array(); var swap;
 var blocks = new Array(); blocks = bytes2blocks(digest_pad(str2bytes(str))); 
 var ibl = 0;   var nbl = blocks.length;
 while (1) {
  if (ibl >= nbl) break;
  v0[0] = blocks[ibl]; ibl++; v0[1] = blocks[ibl]; ibl++;
  v1[0] = blocks[ibl]; ibl++; v1[1] = blocks[ibl]; ibl++;
  // cipher them XOR'd with previous stage ...
  c0 = tea_code( xor_blocks(v0,c0), key );
  c1 = tea_code( xor_blocks(v1,c1), key );
  // mix up the two cipher blocks with a 32-bit left rotation ...
  swap=c0[0]; c0[0]=c0[1]; c0[1]=c1[0]; c1[0]=c1[1]; c1[1]=swap;
 }
 var concat = new Array();
 concat[0]=c0[0]; concat[1]=c0[1]; concat[2]=c1[0]; concat[3]=c1[1];
 return concat;
}
function encrypt (str,keystr) {  // encodes with CBC (Cipher Block Chaining)
 if (! keystr) { alert("encrypt: no key"); return false; }
 var key = new Array();  key = binarydigest(keystr);
 if (! str) return "";
 var blocks = new Array(); blocks = bytes2blocks(pad(str2bytes(str)));
 var ibl = 0;  var nbl = blocks.length;
 // Initial Value for CBC mode = "abcdbcde". Retain for interoperability.
 var c = new Array(); c[0] = 0x61626364; c[1] = 0x62636465;
 var v = new Array(); var cblocks = new Array();  var icb = 0;
 while (1) {
  if (ibl >= nbl) break;
  v[0] = blocks[ibl];  ibl++; v[1] = blocks[ibl];  ibl++;
  c = tea_code( xor_blocks(v,c), key );
  cblocks[icb] = c[0]; icb++; cblocks[icb] = c[1]; icb++;
 }
 return binary2ascii(cblocks);
}
function decrypt (ascii, keystr) {   // decodes with CBC
 if (! keystr) { alert("decrypt: no key"); return false; }
 var key = new Array();  key = binarydigest(keystr);
 if (! ascii) return "";
 var cblocks = new Array(); cblocks = ascii2binary(ascii);
 var icbl = 0;  var ncbl = cblocks.length;
 // Initial Value for CBC mode = "abcdbcde". Retain for interoperability.
 var lastc = new Array(); lastc[0] = 0x61626364; lastc[1] = 0x62636465;
 var v = new Array(); var c = new Array();
 var blocks = new Array(); var ibl = 0;
 while (1) {
  if (icbl >= ncbl) break;
  c[0] = cblocks[icbl];  icbl++;  c[1] = cblocks[icbl];  icbl++;
  v = xor_blocks( lastc, tea_decode(c,key) );
  blocks[ibl] = v[0];  ibl++;  blocks[ibl] = v[1];  ibl++;
  lastc[0] = c[0]; lastc[1] = c[1];
 }
 return bytes2str(unpad(blocks2bytes(blocks)));
}
function xor_blocks(blk1, blk2) { // xor of two 8-byte blocks
 var blk = new Array();
 blk[0] = blk1[0]^blk2[0]; blk[1] = blk1[1]^blk2[1];
 return blk;
}
function tea_code (v, k) {
 // TEA. 2-int (64-bit) cyphertext block in v. 4-int (128-bit) key in k.
 var v0  = v[0]; var v1 = v[1];
 var k0  = k[0]; var k1 = k[1]; var k2 = k[2]; var k3 = k[3];
 var sum = 0; var n = 32;
 while (n-- > 0) {
  sum -= 1640531527; // TEA magic number 0x9e3779b9 
  sum = sum|0;  // force it back to 32-bit int
  v0 += ((v1<<4)+k0) ^ (v1+sum) ^ ((v1>>>5)+k1) ;
  v1 += ((v0<<4)+k2) ^ (v0+sum) ^ ((v0>>>5)+k3) ;
 }
 var w = new Array(); w[0] = v0|0; w[1] = v1|0; return w;
}
function tea_decode (v, k) {
 // TEA. 2-int (64-bit) cyphertext block in v. 4-int (128-bit) key in k.
 var v0 = v[0]; var v1 = v[1];
 var k0 = k[0]; var k1 = k[1]; var k2 = k[2]; var k3 = k[3];
 var sum = 0; var n = 32;
 sum = -957401312 ; // TEA magic number 0x9e3779b9<<5 
 while (n-- > 0) {
  v1 -= ((v0<<4)+k2) ^ (v0+sum) ^ ((v0>>>5)+k3) ;
  v0 -= ((v1<<4)+k0) ^ (v1+sum) ^ ((v1>>>5)+k1) ;
  sum += 1640531527; // TEA magic number 0x9e3779b9 ;
  sum = sum|0; // force it back to 32-bit int
 }
 var w = new Array(); w[0] = v0|0; w[1] = v1|0; return w;
}

// ------------- assocarys used by the conversion routines -----------
c2b = new Object();
c2b["\000"]=0;  c2b["\001"]=1;  c2b["\002"]=2;  c2b["\003"]=3;
c2b["\004"]=4;  c2b["\005"]=5;  c2b["\006"]=6;  c2b["\007"]=7;
c2b["\010"]=8;  c2b["\011"]=9;  c2b["\012"]=10; c2b["\013"]=11;
c2b["\014"]=12; c2b["\015"]=13; c2b["\016"]=14; c2b["\017"]=15;
c2b["\020"]=16; c2b["\021"]=17; c2b["\022"]=18; c2b["\023"]=19;
c2b["\024"]=20; c2b["\025"]=21; c2b["\026"]=22; c2b["\027"]=23;
c2b["\030"]=24; c2b["\031"]=25; c2b["\032"]=26; c2b["\033"]=27;
c2b["\034"]=28; c2b["\035"]=29; c2b["\036"]=30; c2b["\037"]=31;
c2b["\040"]=32; c2b["\041"]=33; c2b["\042"]=34; c2b["\043"]=35;
c2b["\044"]=36; c2b["\045"]=37; c2b["\046"]=38; c2b["\047"]=39;
c2b["\050"]=40; c2b["\051"]=41; c2b["\052"]=42; c2b["\053"]=43;
c2b["\054"]=44; c2b["\055"]=45; c2b["\056"]=46; c2b["\057"]=47;
c2b["\060"]=48; c2b["\061"]=49; c2b["\062"]=50; c2b["\063"]=51;
c2b["\064"]=52; c2b["\065"]=53; c2b["\066"]=54; c2b["\067"]=55;
c2b["\070"]=56; c2b["\071"]=57; c2b["\072"]=58; c2b["\073"]=59;
c2b["\074"]=60; c2b["\075"]=61; c2b["\076"]=62; c2b["\077"]=63;
c2b["\100"]=64; c2b["\101"]=65; c2b["\102"]=66; c2b["\103"]=67;
c2b["\104"]=68; c2b["\105"]=69; c2b["\106"]=70; c2b["\107"]=71;
c2b["\110"]=72; c2b["\111"]=73; c2b["\112"]=74; c2b["\113"]=75;
c2b["\114"]=76; c2b["\115"]=77; c2b["\116"]=78; c2b["\117"]=79;
c2b["\120"]=80; c2b["\121"]=81; c2b["\122"]=82; c2b["\123"]=83;
c2b["\124"]=84; c2b["\125"]=85; c2b["\126"]=86; c2b["\127"]=87;
c2b["\130"]=88; c2b["\131"]=89; c2b["\132"]=90; c2b["\133"]=91;
c2b["\134"]=92; c2b["\135"]=93; c2b["\136"]=94; c2b["\137"]=95;
c2b["\140"]=96; c2b["\141"]=97; c2b["\142"]=98; c2b["\143"]=99;
c2b["\144"]=100; c2b["\145"]=101; c2b["\146"]=102; c2b["\147"]=103;
c2b["\150"]=104; c2b["\151"]=105; c2b["\152"]=106; c2b["\153"]=107;
c2b["\154"]=108; c2b["\155"]=109; c2b["\156"]=110; c2b["\157"]=111;
c2b["\160"]=112; c2b["\161"]=113; c2b["\162"]=114; c2b["\163"]=115;
c2b["\164"]=116; c2b["\165"]=117; c2b["\166"]=118; c2b["\167"]=119;
c2b["\170"]=120; c2b["\171"]=121; c2b["\172"]=122; c2b["\173"]=123;
c2b["\174"]=124; c2b["\175"]=125; c2b["\176"]=126; c2b["\177"]=127;
c2b["\200"]=128; c2b["\201"]=129; c2b["\202"]=130; c2b["\203"]=131;
c2b["\204"]=132; c2b["\205"]=133; c2b["\206"]=134; c2b["\207"]=135;
c2b["\210"]=136; c2b["\211"]=137; c2b["\212"]=138; c2b["\213"]=139;
c2b["\214"]=140; c2b["\215"]=141; c2b["\216"]=142; c2b["\217"]=143;
c2b["\220"]=144; c2b["\221"]=145; c2b["\222"]=146; c2b["\223"]=147;
c2b["\224"]=148; c2b["\225"]=149; c2b["\226"]=150; c2b["\227"]=151;
c2b["\230"]=152; c2b["\231"]=153; c2b["\232"]=154; c2b["\233"]=155;
c2b["\234"]=156; c2b["\235"]=157; c2b["\236"]=158; c2b["\237"]=159;
c2b["\240"]=160; c2b["\241"]=161; c2b["\242"]=162; c2b["\243"]=163;
c2b["\244"]=164; c2b["\245"]=165; c2b["\246"]=166; c2b["\247"]=167;
c2b["\250"]=168; c2b["\251"]=169; c2b["\252"]=170; c2b["\253"]=171;
c2b["\254"]=172; c2b["\255"]=173; c2b["\256"]=174; c2b["\257"]=175;
c2b["\260"]=176; c2b["\261"]=177; c2b["\262"]=178; c2b["\263"]=179;
c2b["\264"]=180; c2b["\265"]=181; c2b["\266"]=182; c2b["\267"]=183;
c2b["\270"]=184; c2b["\271"]=185; c2b["\272"]=186; c2b["\273"]=187;
c2b["\274"]=188; c2b["\275"]=189; c2b["\276"]=190; c2b["\277"]=191;
c2b["\300"]=192; c2b["\301"]=193; c2b["\302"]=194; c2b["\303"]=195;
c2b["\304"]=196; c2b["\305"]=197; c2b["\306"]=198; c2b["\307"]=199;
c2b["\310"]=200; c2b["\311"]=201; c2b["\312"]=202; c2b["\313"]=203;
c2b["\314"]=204; c2b["\315"]=205; c2b["\316"]=206; c2b["\317"]=207;
c2b["\320"]=208; c2b["\321"]=209; c2b["\322"]=210; c2b["\323"]=211;
c2b["\324"]=212; c2b["\325"]=213; c2b["\326"]=214; c2b["\327"]=215;
c2b["\330"]=216; c2b["\331"]=217; c2b["\332"]=218; c2b["\333"]=219;
c2b["\334"]=220; c2b["\335"]=221; c2b["\336"]=222; c2b["\337"]=223;
c2b["\340"]=224; c2b["\341"]=225; c2b["\342"]=226; c2b["\343"]=227;
c2b["\344"]=228; c2b["\345"]=229; c2b["\346"]=230; c2b["\347"]=231;
c2b["\350"]=232; c2b["\351"]=233; c2b["\352"]=234; c2b["\353"]=235;
c2b["\354"]=236; c2b["\355"]=237; c2b["\356"]=238; c2b["\357"]=239;
c2b["\360"]=240; c2b["\361"]=241; c2b["\362"]=242; c2b["\363"]=243;
c2b["\364"]=244; c2b["\365"]=245; c2b["\366"]=246; c2b["\367"]=247;
c2b["\370"]=248; c2b["\371"]=249; c2b["\372"]=250; c2b["\373"]=251;
c2b["\374"]=252; c2b["\375"]=253; c2b["\376"]=254; c2b["\377"]=255;
b2c = new Object();
for (b in c2b) { b2c[c2b[b]] = b; }

// ascii to 6-bit bin to ascii
a2b = new Object();
a2b["A"]=0;  a2b["B"]=1;  a2b["C"]=2;  a2b["D"]=3;
a2b["E"]=4;  a2b["F"]=5;  a2b["G"]=6;  a2b["H"]=7;
a2b["I"]=8;  a2b["J"]=9;  a2b["K"]=10; a2b["L"]=11;
a2b["M"]=12; a2b["N"]=13; a2b["O"]=14; a2b["P"]=15;
a2b["Q"]=16; a2b["R"]=17; a2b["S"]=18; a2b["T"]=19;
a2b["U"]=20; a2b["V"]=21; a2b["W"]=22; a2b["X"]=23;
a2b["Y"]=24; a2b["Z"]=25; a2b["a"]=26; a2b["b"]=27;
a2b["c"]=28; a2b["d"]=29; a2b["e"]=30; a2b["f"]=31;
a2b["g"]=32; a2b["h"]=33; a2b["i"]=34; a2b["j"]=35;
a2b["k"]=36; a2b["l"]=37; a2b["m"]=38; a2b["n"]=39;
a2b["o"]=40; a2b["p"]=41; a2b["q"]=42; a2b["r"]=43;
a2b["s"]=44; a2b["t"]=45; a2b["u"]=46; a2b["v"]=47;
a2b["w"]=48; a2b["x"]=49; a2b["y"]=50; a2b["z"]=51;
a2b["0"]=52; a2b["1"]=53; a2b["2"]=54; a2b["3"]=55;
a2b["4"]=56; a2b["5"]=57; a2b["6"]=58; a2b["7"]=59;
a2b["8"]=60; a2b["9"]=61; a2b["+"]=62; a2b["_"]=63;

b2a = new Object();
for (b in a2b) { b2a[a2b[b]] = ''+b; }
// -->
</SCRIPT>
EOT


=head1 NAME

Tea.pm - The Tiny Encryption Algorithm in Perl and JavaScript

=head1 SYNOPSIS

Usage:

	use Crypt::Tea;
	$key = 'PUFgob$*LKDF D)(F IDD&P?/';
	$ascii_ciphertext = &encrypt ($plaintext, $key);
	...
	$plaintext_again = &decrypt ($ascii_ciphertext, $key);
	...
	$signature = &asciidigest ($text);

In CGI scripts:

	use Crypt::Tea;
	print &tea_in_javascript;  # now the browser can encrypt
	# and decrypt ! see CGI::Htauth.pm for examples ...

=head1 DESCRIPTION

This module implements TEA, the Tiny Encryption Algorithm,
and some Modes of Use, in Perl and JavaScript.

The $key is a sufficiently longish string; at least 17 random 8-bit
bytes for single encryption.

As of version 1.34, various Htauth-specific hook routines
have now been moved out into the I<CGI::Htauth.pm> module.

Version 1.44,
#COMMENT#

(c) Peter J Billam 1998

=head1 SUBROUTINES

=over 3

=item I<encrypt>( $plaintext, $key );

Encrypts with CBC (Cipher Block Chaining)

=item I<decrypt>( $ciphertext, $key );

Decrypts with CBC (Cipher Block Chaining)

=item I<binary2ascii>( $a_binary_string );

Provide an ascii text encoding of the binary argument.
If Tea.pm is not being invoked from a GCI script,
the ascii is split into lines of 72 characters.

=item I<ascii2binary>( $an_ascii_string );

=item I<asciidigest>( $a_string );

Returns an asciified binary signature of the argument.

=item I<tea_in_javascript>();

Returns a compatible implementation of TEA in JavaScript,
for use in CGI scripts to communicate with browsers.

=back

=head1 AUTHOR

Peter J Billam <peter@pjb.com.au>,
with thanks also to Neil Watkiss for MakeMaker packaging.

=head1 CREDITS

Based on TEA, as described in
http://www.cl.cam.ac.uk/ftp/papers/djw-rmn/djw-rmn-tea.html ,
and on some help from I<Applied Cryptography> by Bruce Schneier
as regards the modes of use.

=head1 SEE ALSO

http://www.pjb.com.au/, CGI::Htauth.pm, perl(1).

=cut

