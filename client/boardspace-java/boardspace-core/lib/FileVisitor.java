package lib;

import java.io.File;
import jzlib.ZipEntry;
import jzlib.ZipInputStream;

public interface FileVisitor {
	public void visit(File u);
	public void visitZip(File zipFile,ZipEntry e,ZipInputStream s);
	public boolean filter(String name,boolean isDirectory,File parent);
}
