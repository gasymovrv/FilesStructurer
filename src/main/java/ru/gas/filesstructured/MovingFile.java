package ru.gas.filesstructured;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Iterator;

public class MovingFile {
    private File directory;

    public MovingFile(String directory) {
        File f = new File(directory);
        if (f.isDirectory())
            this.directory = f;
        else
            throw new IllegalArgumentException("Необходимо указать директорию");
    }

    public void renameFolders() {
        File[] innerDirectories = directory.listFiles();
        if (innerDirectories != null) {
            for (File innerDirectory : innerDirectories) {
                String oldFullName = innerDirectory.getAbsolutePath();
                String oldName = innerDirectory.getName();
                String mp4FIleName = getMP4FileName(innerDirectory);
                if (mp4FIleName == null) {
                    System.out.printf("\"%s\" - в данной папке нет MP4 файлов, начинающихся с цифр и тире%n", oldName);
                } else {
                    String newName =
                            oldFullName.substring(
                                    0, oldFullName.lastIndexOf(oldName)
                            ) + mp4FIleName;
                    //выполняем переименование и проверяем успешно оно или нет
                    if (innerDirectory.renameTo(new File(newName)))
                        System.out.printf("\"%s\" - успешно переименовано в \"%s\"%n", oldName, mp4FIleName);
                    else
                        System.out.printf("\"%s\" - ошибка переименования в \"%s\"%n", oldName, mp4FIleName);

                }
            }
        } else
            throw new IllegalArgumentException(directory.getAbsolutePath() + " - укзанная директория пуста");
    }

    private String getMP4FileName(File innerDirectory) {
        Iterator<File> iter = FileUtils.iterateFiles(innerDirectory, new String[]{"mp4"}, false);
        File f;
        if (iter.hasNext()) {
            f = iter.next();
            if (f.getName().matches("^\\d+-.*"))//если файл начинается с цифры и тире
                return f.getName().replace(".mp4", "");
        }
        return null;
    }
}
