package ru.gas.filesstructured;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MovingFile {
    private File directory;

    public MovingFile(String directory) {
        File f = new File(directory);
        if (f.isDirectory())
            this.directory = f;
        else
            throw new IllegalArgumentException("Необходимо указать директорию");
    }

    public void run() {
        Iterator<File> iter = FileUtils.iterateFiles(directory, null, false);
        List<String> errors = new ArrayList<>();
        if (iter.hasNext()) {
            File f;
            long i = 0;
            while (iter.hasNext()) {
                i++;
                f = iter.next();

                LocalDateTime date = null;
                String log = null;
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(f);
                    ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    if (exif != null) {
                        date = convertMillisToLocalDateUTC(exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).getTime());
                        log = String.format("%s: %s_%s_%s_%sh", f.getName(), date.getYear(), date.getMonthValue(), date.getDayOfMonth(), date.getHour());
                    }
                } catch (Exception ignored) {}
                if (date == null) {
                    date = convertMillisToLocalDate(f.lastModified());
                    log = String.format("It doesn't have metadata (%s): %s_%s_%s_%sh", f.getName(), date.getYear(), date.getMonthValue(), date.getDayOfMonth(), date.getHour());
                }

                String dirName = "NOT LOAD NAME";
                try {
                    dirName = String.format("%s\\%s_%s", directory, date.getYear(), date.getMonthValue());
                    File dir = new File(dirName);
                    if (!dir.exists()) {
                        if (dir.mkdir()) {
                            System.out.println(String.format("Created directory: %s", dirName));
                        } else {
                            throw new RuntimeException("Failed to create directory!");
                        }
                    }
                    FileUtils.moveFileToDirectory(f, dir, false);
                    System.out.println(String.format("\t%d Success moved - %s", i, log));
                } catch (Exception e) {
                    String errText = String.format("\tError by moving %s to: %s (%s)", f.getName(), dirName, e.getMessage());
                    System.out.println(errText);
                    errors.add(errText);
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

}
