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
    private File root;
    private Options option;

    public MovingOrRenamingFiles(String root, Options option) {
        this.option = option == null ? Options.EMPTY : option;
        File f = new File(root);
        if (f.isDirectory())
            this.root = f;
        else
            throw new IllegalArgumentException("Необходимо указать директорию (абсолютный путь)");
    }

    public void run() {
        Iterator<File> iter = FileUtils.iterateFiles(root, null, option==Options.RM || option==Options.RMR);
        List<String> errors = new ArrayList<>();
        if (iter.hasNext()) {
            File f;
            int commonIndex = 0;
            int fileNameIndex = 0;
            int month = -1;
            int year = -1;
            while (iter.hasNext()) {
                commonIndex++;
                f = iter.next();
                StringBuilder dateLog = new StringBuilder();
                LocalDateTime date;
                switch (option){
                    case F:
                        date = getLastModifiedDateTime(f, dateLog);
                        createAndMoveToNewFolders(f, date, errors, dateLog, commonIndex);
                        break;
                    case RM:
                        moveToRoot(f, errors, commonIndex);
                        break;
                    case RMR: case EMPTY:
                        date = getLastModifiedDateTime(f, dateLog);
                        if(month != date.getMonthValue() || year != date.getYear()){
                            fileNameIndex = 0;
                            month = date.getMonthValue();
                            year = date.getYear();
                        }
                        int countFiles = moveToRootAndRename(f, date, errors, dateLog, fileNameIndex, commonIndex);
                        fileNameIndex += countFiles;
                        break;
                }
            }
        } else {
            String errText = root.getAbsolutePath() + " - this directory is empty";
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
                logger.append(String.format("It has normal metadata (%s): %s_%s_%s_%sh", f.getName(), date.getYear(), addZeros(date.getMonthValue()), addZeros(date.getDayOfMonth()), addZeros(date.getHour())));
            }
        } catch (Exception ignored) {}
        LocalDateTime lastModifiedLocalDate = convertMillisToLocalDate(f.lastModified());
        if (date == null || date.isAfter(LocalDateTime.now()) || date.isAfter(lastModifiedLocalDate)) {
            date = lastModifiedLocalDate;
            logger.setLength(0);
            logger.append(String.format("It doesn't have metadata or wrong metadata (%s), got from lastModified: %s_%s_%s_%sh", f.getName(), date.getYear(), addZeros(date.getMonthValue()), addZeros(date.getDayOfMonth()), addZeros(date.getHour())));
        }
        return date;
    }

    private void createAndMoveToNewFolders(File f, LocalDateTime date, List<String> errors, StringBuilder dateLog, int i){
        String dirName = null;
        try {
            dirName = String.format("%s\\%s_%s", root, date.getYear(), addZeros(date.getMonthValue()));
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
                System.out.println(String.format("\t%d Success moved - from <%s> to <%s>", i, f.getName(), dir.getName()));
            } else {
                throw new RuntimeException("Failed to create directory!");
            }
        } catch (Exception e) {
            String errText = String.format("\tError by moving <%s> to: <%s> (%s)", f.getAbsolutePath(), dirName, e.getMessage());
            System.out.println(errText);
            errors.add(errText);
        }
    }

    private void moveToRoot(File f, List<String> errors, int commonIndex){
        String movedName = null;
        try {
            movedName = String.format("%s\\%s", root, f.getName());
            if (f.getAbsolutePath().equals(movedName)) {
                return;
            }
            File movedFile = new File(movedName);
            int addIndex = 1;
            String infoIfChangedName = "";
            while(movedFile.exists()){
                String oldMovedName = movedName;
                movedName = String.format("%s\\%s_%s.%s", root, f.getName().substring(0, f.getName().lastIndexOf(".")), addIndex++, getExtension(f));
                movedFile = new File(movedName);
                if(!movedFile.exists()){
                    infoIfChangedName="File with name <"+oldMovedName+"> already exists, name of moving file was changed";
                }
            }
            FileUtils.moveFile(f, movedFile);
            System.out.println(String.format("\t%d Success moved to root - <%s>; %s", commonIndex, movedFile.getName(), infoIfChangedName));
        } catch (Exception e) {
            String errText = String.format("\tError by moving <%s> to: <%s> (%s)", f.getAbsolutePath(), movedName, e.getMessage());
            System.out.println(errText);
            errors.add(errText);
        }
    }

    private int moveToRootAndRename(File f, LocalDateTime date, List<String> errors, StringBuilder dateLog, int i, int commonIndex){
        String newName = null;
        try {
            newName = String.format("%s\\%s_%s_%s.%s", root, date.getYear(), addZeros(date.getMonthValue()), i, getExtension(f));
            if (f.getAbsolutePath().equals(newName)) {
                return 1;
            }
            File renamedFile = new File(newName);
            int countFiles = 1;
            while(renamedFile.exists()){
                int newIndex = Integer.parseInt(renamedFile.getName().substring(renamedFile.getName().lastIndexOf("_")+1, renamedFile.getName().lastIndexOf("."))) + 1;
                newName = String.format("%s\\%s_%s_%s.%s", root, date.getYear(), addZeros(date.getMonthValue()), newIndex, getExtension(f));
                renamedFile = new File(newName);
                countFiles++;
            }
            FileUtils.moveFile(f, renamedFile);
            System.out.println(String.format("\t%d Success renamed and moved to root - from <%s> to <%s>", commonIndex, f.getName(), renamedFile.getName()));
            return countFiles;
        } catch (Exception e) {
            String errText = String.format("\tError by renaming or moving <%s> to: <%s> (%s)", f.getAbsolutePath(), newName, e.getMessage());
            System.out.println(errText);
            errors.add(errText);
            return 0;
        }
    }

    private String getExtension(File f){
        return f.getName().substring(f.getName().lastIndexOf(".")+1);
    }

}
