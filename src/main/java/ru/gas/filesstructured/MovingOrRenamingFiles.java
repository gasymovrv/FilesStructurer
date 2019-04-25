package ru.gas.filesstructured;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MovingOrRenamingFiles {
    private File directory;
    private boolean createFolders;
    private boolean recursiveRename;

    public MovingOrRenamingFiles(String directory, boolean createFolders, boolean recursiveRename) {
        this.createFolders = createFolders;
        this.recursiveRename = recursiveRename;
        File f = new File(directory);
        if (f.isDirectory())
            this.directory = f;
        else
            throw new IllegalArgumentException("Необходимо указать директорию (абсолютный путь)");
    }

    public void run() {
        Iterator<File> iter = FileUtils.iterateFiles(directory, null, recursiveRename);
        List<String> errors = new ArrayList<>();
        if (iter.hasNext()) {
            File f;
            int commonIndex = 0;
            int i = 0;
            int month = -1;
            while (iter.hasNext()) {
                commonIndex++;
                f = iter.next();
                StringBuilder dateLog = new StringBuilder();
                LocalDateTime date = getLastModifiedDateTime(f, dateLog);
                if(createFolders){
                    moveToDirectories(f, date, errors, dateLog, commonIndex);
                } else {
                    if(month != date.getMonthValue()){
                        i = 0;
                        month = date.getMonthValue();
                    } else {
                        i++;
                    }
                    renameFiles(f, date, errors, dateLog, i, commonIndex);
                }

            }
        } else {
            String errText = directory.getAbsolutePath() + " - this directory is empty";
            errors.add(errText);
        }
        System.out.printf("Process completed with %d errors.\n", errors.size());
        for (int i = 0; i < errors.size(); i++) {
            System.out.printf("Error %d : %s\n", i+1, errors.get(i));
        }
    }

    private LocalDateTime convertMillisToLocalDate(Long l) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
    }

    private LocalDateTime convertMillisToLocalDateUTC(Long l) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC);
    }

    private String addZeros(int m) {
        if(m>=10){
            return String.valueOf(m);
        } else {
            return String.format("0%d", m);
        }
    }

    private LocalDateTime getLastModifiedDateTime(File f, StringBuilder logger){
        LocalDateTime date = null;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exif != null) {
                date = convertMillisToLocalDateUTC(exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).getTime());
                logger.setLength(0);
                logger.append(String.format("%s: %s_%s_%s_%sh", f.getName(), date.getYear(), addZeros(date.getMonthValue()), addZeros(date.getDayOfMonth()), addZeros(date.getHour())));
            }
        } catch (Exception ignored) {}
        if (date == null) {
            date = convertMillisToLocalDate(f.lastModified());
            logger.setLength(0);
            logger.append(String.format("It doesn't have metadata (%s): %s_%s_%s_%sh", f.getName(), date.getYear(), addZeros(date.getMonthValue()), addZeros(date.getDayOfMonth()), addZeros(date.getHour())));
        }
        return date;
    }

    private void moveToDirectories(File f, LocalDateTime date, List<String> errors, StringBuilder dateLog, int i){
        String dirName = null;
        try {
            dirName = String.format("%s\\%s_%s", directory, date.getYear(), addZeros(date.getMonthValue()));
            File dir = null;
            if (dirName != null) {
                dir = new File(dirName);
            }
            if (dir != null && !dir.exists()) {
                if (dir.mkdir()) {
                    System.out.println(String.format("Created directory: %s", dirName));
                } else {
                    throw new RuntimeException("Failed to create directory!");
                }
            }
            if(dir != null) {
                FileUtils.moveFileToDirectory(f, dir, false);
                System.out.println(String.format("\t%d Success moved - %s", i, dateLog.toString()));
            } else {
                throw new RuntimeException("Failed to create directory!");
            }
        } catch (Exception e) {
            String errText = String.format("\tError by moving %s to: %s (%s)", f.getName(), dirName, e.getMessage());
            System.out.println(errText);
            errors.add(errText);
        }
    }

    private void renameFiles(File f, LocalDateTime date, List<String> errors, StringBuilder dateLog, int i, int commonIndex){
        String newName = null;
        try {
            newName = String.format("%s\\%s_%s_%s.%s", directory, date.getYear(), addZeros(date.getMonthValue()), i, f.getName().substring(f.getName().lastIndexOf(".")+1));
            File renamedFile = new File(newName);
            while(renamedFile.exists()){
                int newIndex = Integer.parseInt(renamedFile.getName().substring(renamedFile.getName().lastIndexOf("_")+1, renamedFile.getName().lastIndexOf("."))) + 1;
                newName = String.format("%s\\%s_%s_%s.%s", directory, date.getYear(), addZeros(date.getMonthValue()), newIndex, f.getName().substring(f.getName().lastIndexOf(".")+1));
                renamedFile = new File(newName);
            }
            FileUtils.moveFile(f, renamedFile);
            System.out.println(String.format("\t%d Success renamed and moved - %s", commonIndex, dateLog.toString()));
        } catch (Exception e) {
            String errText = String.format("\tError by renaming %s to: %s (%s)", f.getName(), newName, e.getMessage());
            System.out.println(errText);
            errors.add(errText);
        }
    }

}
