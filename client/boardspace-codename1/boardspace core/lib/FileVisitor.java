package lib;

import bridge.File;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;


public interface FileVisitor {
	public void visit(File u);
	public void visitZip(File zipFile,ZipEntry e,ZipInputStream s);
	public boolean filter(String name,boolean isDirectory,File parent);
}