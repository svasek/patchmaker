package net.svasek.java.patchmaker.core;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 23.11.2009
 */
public class PatchCreator {
    private final DiffResult diffResults;
    private final String distEarPath;
    private final String updateName;
    private final File tmpDir;
    private String productFamily;
    private List<String> filesToBeLocked;

    public PatchCreator(DiffResult diffResults, String distEarPath, String updateName, File tmpDir, String productFamily, List<String> filesToBeLocked) {
        this.diffResults = diffResults;
        this.distEarPath = distEarPath;
        this.updateName = updateName;
        this.tmpDir = tmpDir;
        this.productFamily = productFamily;
        this.filesToBeLocked = filesToBeLocked;
    }

    public void makeJarChecksums(File dstJar) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            StreamingMultiDigester.compute(dstJar, md5, sha1);
            final byte[] md5sum = md5.digest();
            final byte[] sha1sum = sha1.digest();
            // Make MD5 checksum file
            PrintWriter md5file = new PrintWriter(new FileWriter(dstJar.getAbsoluteFile() + ".md5"));
            md5file.println(StreamingMultiDigester.getHex(md5sum) + "  " + dstJar.getName());
            md5file.close();
            // Make SHA1 checksum file
            PrintWriter sha1file = new PrintWriter(new FileWriter(dstJar.getAbsoluteFile() + ".sha1"));
            sha1file.println(StreamingMultiDigester.getHex(sha1sum) + "  " + dstJar.getName());
            sha1file.close();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void createBuildFile(String productMask) {
        final File buildFile = new File(tmpDir + File.separator + "build.xml");

        if (productFamily == null || productFamily.length() < 1) {
            productFamily = "XYZ";
        }

        ClassLoader cl = this.getClass().getClassLoader();
        java.io.InputStream in = cl.getResourceAsStream("net/svasek/java/patchmaker/core/template-" + productFamily + "-build.xml");

        try {
            //////////////////////////
            //Creating an XML Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.parse(in);

            //Element element = (Element) firstNode;
            Element project = doc.getDocumentElement();
            project.setAttribute("name", updateName);

            //TODO: Change to better search of target
            NodeList list = doc.getElementsByTagName("target");
            Node generatedTarget = list.item(0);
            generatedTarget.normalize();

            Element buildtimeElement = doc.createElement("property");
            buildtimeElement.setAttribute("name", "buildtime.pattern");
            buildtimeElement.setAttribute("value", productMask);
            project.insertBefore(buildtimeElement, generatedTarget);

            Element updateNameElement = doc.createElement("property");
            updateNameElement.setAttribute("name", "update.name");
            updateNameElement.setAttribute("value", updateName);
            project.insertBefore(updateNameElement, generatedTarget);

            //Generate lines with macro calls
            addMacroLinesToBuildXml(doc, generatedTarget);

            //Workaround for defect #63478: add file locking on windows.
            addFileLockerTarget(cl);

            /////////////////
            //Output the XML
            //set up a transformer
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();
            aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // This doesn't work with JDK 1.5 JAXP
            aTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            //write into the file
            Source src = new DOMSource(doc);
            Result dest = new StreamResult(buildFile);
            aTransformer.transform(src, dest);
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    //Workaround for defect #63478
    private void addFileLockerTarget(ClassLoader cl) {
        File fileLockerLocationDir = new File(tmpDir + File.separator + "META-INF" + File.separator + "lib");
        if (filesToBeLocked != null && !filesToBeLocked.isEmpty()) {
            //Copy file-locker.jar to the update archive (META-INF/lib)
            MyUtils.createDir(fileLockerLocationDir);
            java.io.InputStream in = cl.getResourceAsStream("META-INF/lib/file-locker.jar");
            File to = new File(fileLockerLocationDir, "file-locker.jar");
            OutputStream out;
            try {
                out = new FileOutputStream(to);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Create locked files list (file)
            File lockfilelist = new File(fileLockerLocationDir, "file-locker.lst");
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new FileWriter(lockfilelist));
                for (String fileToLock : filesToBeLocked) {
                    pw.println(fileToLock);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (pw != null) {
                    pw.close(); // Without this, the output file may be empty
                }
            }
        }

    }

    private void addMacroLinesToBuildXml(Document doc, Node generatedTarget) {
        final String distEarName = MyUtils.parseEarPath(distEarPath)[2];
        final List<String> removedEntries = new ArrayList<String>();

        for (ArchPath r : diffResults.getRemovedEntries()) {
            final String myEntryPath = MyUtils.fixEarPathInUpdate(r.toString(), distEarPath);
            if (MyUtils.isArchive(r.toString())) {
                addNewMacroLineToBuildXml("remove", myEntryPath, "", doc, generatedTarget, distEarPath);
            } else {
                //Add it into list of removed files (not archive)
                removedEntries.add(myEntryPath);
            }
        }

        for (ArchPath a : diffResults.getAddedEntries()) {
            final String myEntryPath = MyUtils.fixEarPathInUpdate(a.toString(), distEarPath);
            if (MyUtils.isArchive(a.toString())) {
                addNewMacroLineToBuildXml("insert", myEntryPath, "", doc, generatedTarget, distEarPath);
            }
        }

        for (ArchPath c : diffResults.getChangedEntries()) {
            final String myEntryPath = MyUtils.fixEarPathInUpdate(c.toString(), distEarPath);
            if (!myEntryPath.equals(distEarName)) {
                if (MyUtils.isArchive(myEntryPath)) {
                    //Add content of archive that should be deleted
                    String toDelete = listToDelete(myEntryPath, removedEntries);
                    //Add archive
                    addNewMacroLineToBuildXml("update", myEntryPath, toDelete, doc, generatedTarget, distEarPath);
                }
            }
        }
    }

    private String listToDelete(String find, List<String> all) {
        List<String> foundEntries = new ArrayList<String>();
        for (String entry : all) {
            if (entry.contains(find)) {
                String toDel = entry;
                toDel = toDel.replaceFirst("^" + find + "/", "");
                foundEntries.add(toDel);
            }
        }
        return MyUtils.concatenate(foundEntries, ", ");
    }

    private void addNewMacroLineToBuildXml(String action, String archEntry, String toDelete, Document doc, Node generatedTarget, String distEarPath) {
        final String distributionDir = MyUtils.parseEarPath(distEarPath)[0];
        final String distEarName = MyUtils.parseEarPath(distEarPath)[2];
        boolean update = false;
        boolean skipIt = false;
        String dir = "";
        String ext = "";
        String type = "";
        String file = "";

        if (archEntry.startsWith(distributionDir)) {
            dir = "${install.location}";
            type = "distribution";
            file = archEntry.replaceFirst("^" + distributionDir + "/", "");
            if (file.startsWith("lib")) update = true;
        } else if (archEntry.startsWith(distEarName)) {
            dir = "${ear.dir}";
            type = distEarName;
            file = archEntry.replaceFirst("^" + distEarName + "/", "");
        }

        if (archEntry.endsWith(".jar") || archEntry.endsWith(".zip")) ext = "jar";
        //WARs outside the EAR should be handled same as JARs
        if (archEntry.endsWith(".war") || archEntry.endsWith(".ear")) {
            if (type.equals("distribution")) {
                ext = "jar";
            } else {
                ext = "war";
            }
        }

        //Skip included archives in distribution (not in ear)
        if (type.equals("distribution") && (archEntry.matches(".*\\.([jw]ar|zip)/.*\\.([jw]ar|zip)"))) skipIt = true;

        if (!skipIt) {
            Element insertjarElement = doc.createElement(action + "-" + ext);
            insertjarElement.setAttribute("dir", dir);
            insertjarElement.setAttribute("type", type);
            insertjarElement.setAttribute("file", file);
            if ((update) && (action.equals("update"))) insertjarElement.setAttribute("update", ".update");
            if (toDelete.length() > 0) insertjarElement.setAttribute("remove", toDelete);
            generatedTarget.appendChild(insertjarElement);
        }
    }


    public void createFilesList() {
        final File filelist = new File(tmpDir + File.separator + "META-INF" + File.separator + "files.txt");

        if (!diffResults.getAddedEntries().isEmpty() || !diffResults.getChangedEntries().isEmpty() || !diffResults.getRemovedEntries().isEmpty()) {
            MyUtils.createDir(new File(tmpDir + File.separator + "META-INF"));
        }

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(filelist));

            for (ArchPath a : diffResults.getAddedEntries()) {
                pw.println("+ " + MyUtils.fixEarPathInUpdate(a.toString(), distEarPath));
            }
            for (ArchPath c : diffResults.getChangedEntries()) {
                if (!MyUtils.isArchive(c.toString())) {
                    pw.println("* " + MyUtils.fixEarPathInUpdate(c.toString(), distEarPath));
                }
            }
            for (ArchPath r : diffResults.getRemovedEntries()) {
                pw.println("- " + MyUtils.fixEarPathInUpdate(r.toString(), distEarPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close(); // Without this, the output file may be empty
            }
        }
    }

    String getAfectedFiles() {
        List<String> affectedFiles = new ArrayList<String>();
        for (ArchPath a : diffResults.getAddedEntries()) {
            affectedFiles.add(MyUtils.fixEarPathInUpdate(a.toString(), distEarPath));
        }
        for (ArchPath c : diffResults.getChangedEntries()) {
            if (!MyUtils.isArchive(c.toString())) {
                affectedFiles.add(MyUtils.fixEarPathInUpdate(c.toString(), distEarPath));
            }
        }
        for (ArchPath r : diffResults.getRemovedEntries()) {
            affectedFiles.add(MyUtils.fixEarPathInUpdate(r.toString(), distEarPath));
        }
        Collections.sort(affectedFiles);
        return MyUtils.concatenate(affectedFiles, "\n");
    }

    public void createInfoFile(String productRegexp, String updateDependsOn, String descriptionTag) {
        final File infoFile = new File(tmpDir + File.separator + "META-INF" + File.separator + "info.xml");
        // Create string used in tag <description> in file info.xml

        if (!diffResults.getAddedEntries().isEmpty() || !diffResults.getChangedEntries().isEmpty() || !diffResults.getRemovedEntries().isEmpty()) {
            MyUtils.createDir(new File(tmpDir + File.separator + "META-INF"));
        }

        try {
            ////////////////////////////////
            //Creating an empty XML Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.newDocument();

            ////////////////////////
            //Creating the XML tree
            //create the root element and add it to the document
            Element root = doc.createElement("update");
            root.setAttribute("name", updateName);
            doc.appendChild(root);

            //create child elements
            Element descriptionElement = doc.createElement("description");
            descriptionElement.setTextContent(descriptionTag);
            root.appendChild(descriptionElement);

            Element productElement = doc.createElement("product");
            productElement.setTextContent(productRegexp);
            root.appendChild(productElement);

            if ((updateDependsOn != null) && (updateDependsOn.length() < 0)) {
                Element dependsElement = doc.createElement("depends");
                dependsElement.setTextContent(updateDependsOn);
                root.appendChild(productElement);
            }

            Element affectedFilesElement = doc.createElement("affectedFiles");
            //Add affected files (one on each line)
            affectedFilesElement.setTextContent("\n" + getAfectedFiles() + "\n");
            root.appendChild(affectedFilesElement);

            /////////////////
            //Output the XML
            //set up a transformer
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();
            aTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // This doesn't work with JDK 1.5 JAXP
            aTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            //write into the file
            Source src = new DOMSource(doc);
            Result dest = new StreamResult(infoFile);
            aTransformer.transform(src, dest);
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
}
