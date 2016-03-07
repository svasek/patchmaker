package net.svasek.java.patchmaker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens jars to lock in order to lock it on windows.
 * This is just an workaround.
 * <p/>
 * First argument is installation location
 * Second and next arguments are names of jars to be locked (relative to installation directory)
 */
public class FileLocker {
    public static void main(String[] args) {
        String workingDir = args[0] + File.separator + "working";
        String stopFile = args[0] + File.separator + "stop.txt";

        List<String> fileList = getFileList(args[1]);
        FileInputStream fis[] = new FileInputStream[fileList.size()];
        File file = new File(workingDir);
        File file2 = new File(stopFile);

        try {
            for (int i = 0; i < fileList.size(); i++) {
                fis[i] = new FileInputStream(args[0] + File.separator + fileList.get(i));
            }
            while (file.exists() && !file2.exists()) {
                Thread.sleep(5000);
            }

        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                for (FileInputStream fi : fis) {
                    fi.close();
                }
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }
    }

    private static List<String> getFileList(String fileList) {
        List<String> contents = new ArrayList<String>();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(fileList));
            String question;
            while ((question = br.readLine()) != null)
                contents.add(question);
            br.close();
        } catch (FileNotFoundException e) {
            System.err.println(e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }
}
