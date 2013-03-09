package info.bouzuya;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

public class ExportBatch {

	public static void main(String[] args) throws SVNException,
			URISyntaxException, IOException {
		ExportBatch batch = new ExportBatch();
		batch.run();
	}

	public void run() throws SVNException, URISyntaxException, IOException {
		URI repoUrl = new URI("file:///home/user/svn/repo/trunk");
		File exportDir = new File("/home/user/svn/exp");
		File srcDir = new File(exportDir, "src");
		File binDir = new File("/home/user/svn/bin");
		File dstDir = new File("/home/user/svn/dst");

		FileUtils.deleteDirectory(exportDir);
		export(repoUrl, exportDir);
		Collection<File> javaFiles = listJavaFiles(exportDir);
		Collection<File> classFiles = listClassFiles(javaFiles, srcDir, binDir);
		copyFiles(classFiles, binDir, dstDir);
	}

	private void export(URI url, File dstPath) throws SVNException {
		SVNClientManager manager = SVNClientManager.newInstance();
		SVNUpdateClient client = manager.getUpdateClient();
		SVNURL svnUrl = SVNURL.parseURIEncoded(url.toString());
		SVNRevision pegRevision = SVNRevision.HEAD;
		SVNRevision revision = SVNRevision.HEAD;
		String eolStyle = "";
		boolean overwrite = false;
		SVNDepth depth = SVNDepth.INFINITY;
		client.doExport(svnUrl, dstPath, pegRevision, revision, eolStyle,
				overwrite, depth);
	}

	private Collection<File> listJavaFiles(File exportDir) {
		return FileUtils.listFiles(exportDir, new String[] { "java" }, true);
	}

	private Collection<File> listClassFiles(Collection<File> javaFiles,
			File srcDir, File binDir) {
		List<File> classFiles = new ArrayList<File>();
		for (File f : javaFiles) {
			classFiles.addAll(listClassFiles(f, srcDir, binDir));
		}
		return classFiles;
	}

	private Collection<File> listClassFiles(File javaFile, File srcDir,
			File dstDir) {
		File classFile = resolve(javaFile, srcDir, dstDir);
		File classDir = classFile.getParentFile();

		final String classFileNamePattern = getClassFileNamePattern(javaFile);

		File[] matchedClassFiles = classDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File classFile) {
				String classFileName = classFile.getName();
				return classFileName.matches(classFileNamePattern);
			}
		});

		List<File> classFiles = new ArrayList<File>();
		for (File f : matchedClassFiles) {
			classFiles.add(f);
		}
		return classFiles;
	}

	private String getClassFileNamePattern(File javaFile) {
		String javaFilePath = javaFile.getAbsolutePath();
		String javaFileBaseName = FilenameUtils.getBaseName(javaFilePath);
		String quotedJavaFileBaseName = Pattern.quote(javaFileBaseName);
		final String classFileNamePattern = String.format(
				"^%s(\\$.*)?\\.class", quotedJavaFileBaseName);
		return classFileNamePattern;
	}

	private void copyFiles(Collection<File> classFiles, File classRoot,
			File dstRoot) throws IOException {
		dstRoot.mkdirs();
		for (File classFile : classFiles) {
			File dstFile = resolve(classFile, classRoot, dstRoot);
			FileUtils.copyFile(classFile, dstFile);
		}
	}

	private File resolve(File file, File srcDir, File dstDir) {
		URI srcFileUri = file.toURI();
		URI srcDirUri = srcDir.toURI();
		URI dstDirUri = dstDir.toURI();
		URI relativeUri = srcDirUri.relativize(srcFileUri);
		URI dstUri = dstDirUri.resolve(relativeUri);
		return new File(dstUri);
	}

}
