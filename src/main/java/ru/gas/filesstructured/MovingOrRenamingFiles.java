package ru.gas.filesstructured;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.FileUtils;

public class MovingOrRenamingFiles {
    private final File root;
    private final Options option;
    private Set<String> exclusions = Set.of();

    public MovingOrRenamingFiles(String root, Options option, Set<String> exclusions) {
        this.option = option == null ? Options.EMPTY : option;
        File f = new File(root);
        if (f.isDirectory())
            this.root = f;
        else
            throw new IllegalArgumentException("Необходимо указать директорию (абсолютный путь)");
        if (exclusions != null && !exclusions.isEmpty()) {
            this.exclusions = exclusions;
        }
    }

    public void run() {
        Iterator<File> iter = FileUtils.iterateFiles(root, null, option == Options.RM || option == Options.RMR);
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
                if (exclusions.contains(f.getName())) {
                    System.out.printf("File %s matches with exclusions, skipped.\n", f.getName());
                    continue;
                }

                StringBuilder dateLog = new StringBuilder();
                LocalDateTime date;

                switch (option) {
                    case F:
                        date = getAndFillDateTaken(f, dateLog);
                        createAndMoveToNewFolders(f, date, errors, dateLog, commonIndex);
                        break;
                    case RM:
                        moveToRoot(f, errors, commonIndex);
                        break;
                    case RMR:
                    case EMPTY:
                        date = getAndFillDateTaken(f, dateLog);
                        if (month != date.getMonthValue() || year != date.getYear()) {
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
            System.out.printf("Error %d : %s\n", i + 1, errors.get(i));
        }
    }

    private LocalDateTime convertMillisToLocalDate(Long l) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
    }

    private LocalDateTime convertMillisToLocalDateUTC(Long l) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC);
    }

    private String addZeros(int m) {
        if (m >= 10) {
            return String.valueOf(m);
        } else {
            return String.format("0%d", m);
        }
    }

    private LocalDateTime getAndFillDateTaken(File f, StringBuilder logger) {
        // Get the original date taken from metadata if available
        LocalDateTime dateTaken = null;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exif != null) {
                dateTaken = convertMillisToLocalDateUTC(exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).getTime());
                logger.setLength(0);
                logger.append(String.format("It has normal metadata (%s): %s_%s_%s_%sh", f.getName(), dateTaken.getYear(), addZeros(dateTaken.getMonthValue()), addZeros(dateTaken.getDayOfMonth()), addZeros(dateTaken.getHour())));
            }
        } catch (Exception ignored) {
            // If metadata reading fails, we'll use the last modified date
        }

        // If dateTaken is null or invalid, use the modified date and update the metadata
        if (dateTaken == null || dateTaken.isAfter(LocalDateTime.now())) {
            dateTaken = convertMillisToLocalDate(f.lastModified());
            try {
                // Only try to update metadata for JPEG files
                if (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg")) {
                    var outputSet = new TiffOutputSet();

                    // Set the DateTimeOriginal tag
                    outputSet.getOrCreateExifDirectory().add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                            dateTaken.format(java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")));

                    // Write the metadata back to the file
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    new ExifRewriter().updateExifMetadataLossless(f, outputStream, outputSet);

                    // Write the modified file back
                    try (OutputStream os = new FileOutputStream(f)) {
                        outputStream.writeTo(os);
                    }
                }
            } catch (ImageReadException | ImageWriteException | IOException e) {
                // Log error but continue with the file operation
                System.out.println("Warning: Could not update EXIF metadata for " + f.getName());
            }
        }
        return dateTaken;
    }

    private void createAndMoveToNewFolders(File f, LocalDateTime date, List<String> errors, StringBuilder dateLog, int i) {
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
            if (dir != null) {
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

    private void moveToRoot(File f, List<String> errors, int commonIndex) {
        String movedName = null;
        try {
            movedName = String.format("%s\\%s", root, f.getName());
            if (f.getAbsolutePath().equals(movedName)) {
                return;
            }
            File movedFile = new File(movedName);
            int addIndex = 1;
            String infoIfChangedName = "";
            while (movedFile.exists()) {
                String oldMovedName = movedName;
                movedName = String.format("%s\\%s_%s.%s", root, f.getName().substring(0, f.getName().lastIndexOf(".")), addIndex++, getExtension(f));
                movedFile = new File(movedName);
                if (!movedFile.exists()) {
                    infoIfChangedName = "File with name <" + oldMovedName + "> already exists, name of moving file was changed";
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

    private int moveToRootAndRename(File f, LocalDateTime date, List<String> errors, StringBuilder dateLog, int i, int commonIndex) {
        String newName = null;
        try {
            newName = String.format("%s\\%s_%s_%s_%s.%s", root, date.getYear(), addZeros(date.getMonthValue()), addZeros(date.getDayOfMonth()), i, getExtension(f));
            if (f.getAbsolutePath().equals(newName)) {
                return 1;
            }
            File renamedFile = new File(newName);
            int countFiles = 1;
            while (renamedFile.exists()) {
                int newIndex = Integer.parseInt(renamedFile.getName().substring(renamedFile.getName().lastIndexOf("_") + 1, renamedFile.getName().lastIndexOf("."))) + 1;
                newName = String.format("%s\\%s_%s_%s_%s.%s", root, date.getYear(), addZeros(date.getMonthValue()), addZeros(date.getDayOfMonth()), newIndex, getExtension(f));
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

    private String getExtension(File f) {
        return f.getName().substring(f.getName().lastIndexOf(".") + 1);
    }

}
