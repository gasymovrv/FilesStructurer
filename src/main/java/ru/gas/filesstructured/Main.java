package ru.gas.filesstructured;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println(
                "Программа структурирования файлов.\n" +
                "По умолчанию - переименовывает по шаблону [год]_[№ месяца]_[№ файла]\n" +
                "С ключом -r - рекурсивное извлечение из всех подпапок и переименование (несовместим с [-f])\n" +
                "С ключом -f - распределяет файлы по подпапкам с именем [год]_[№ месяца]\n" +
                "Год и номер месяца - берутся из даты съемки если это фото, иначе из даты изменения файла.\n" +
                "Путь к корневой директории указать в аргументах программы.\n");
        if(args.length == 0) {
            info();
        } else {
            MovingOrRenamingFiles movingOrRenamingFiles;
            boolean createFolders = false;
            boolean recursiveRename = false;
            String argsString = Arrays.toString(args);
            if(argsString.contains("-f")){
                createFolders = true;
            }
            if(argsString.contains("-r")){
                recursiveRename = true;
            }
            if(createFolders && recursiveRename){
                info();
                return;
            }
            movingOrRenamingFiles = new MovingOrRenamingFiles(args[0], createFolders, recursiveRename);
            movingOrRenamingFiles.run();
        }
    }
    private static void info(){
        System.out.println("1й обязательный аргумент: адрес корневой директории");
        System.out.println("2й необязательный аргумент: [-f] - создать подпапки");
        System.out.println("3й необязательный аргумент: [-r] - рекурсивное извлечение и переименование (несовместим с [-f])\n");
    }
}
