package ru.gas.filesstructured;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println("Программа структурирования файлов.");
        System.out.println(getArgsInfo());
        Options option = Options.EMPTY;
        System.out.println("----------------------------------------------------");

        if(args.length == 0) {
            printErrorInfo();
            return;
        } else if(args.length > 1) {
            String[] argsWithoutDirectory = Arrays.copyOfRange(args, 1, args.length);
            int count = 0;
            for (String s : argsWithoutDirectory) {
                if(s.equals("-f")){
                    option = Options.F;
                    count++;
                }
                if(s.equals("-rm")){
                    option = Options.RM;
                    count++;
                }
                if(s.equals("-rmr")){
                    option = Options.RMR;
                    count++;
                }
            }
            if(count>1){
                printErrorInfo();
                return;
            }
        }
        MovingOrRenamingFiles movingOrRenamingFiles = new MovingOrRenamingFiles(args[0], option);
        System.out.println(option.getInfo());
        System.out.println("----------------------run---------------------------");
        movingOrRenamingFiles.run();
    }

    private static void printErrorInfo(){
        System.out.println("Неверные параметры запуска.");
        System.out.println(getArgsInfo());
    }

    private static String getArgsInfo() {
        return  "Обязательные аргументы:\n" +
                "\t1.[путь_к_корневой_директории]\n\n" +
                "Необязательные аргументы (может быть выбран только один):\n" +
                "\t1.[-rm] - рекурсивное извлечение файлов из всех подпапок\n" +
                "\t2.[-rmr] - рекурсивное извлечение файлов из всех подпапок и переименование по шаблону: [год]_[№ месяца]_[№ файла]\n" +
                "\t3.[-f] - распределяет файлы по подпапкам с именем [год]_[№ месяца]\n" +
                "\t4. Без необязательных аргументов - переименовывает файлы в указанной директории по шаблону: [год]_[№ месяца]_[№ файла]\n\n"+
                "Примечания:\n" +
                "Год и номер месяца - берутся из даты съемки если это фото, иначе из даты изменения файла.\n\n" +
                "Пример запуска:\n" +
                "java -jar FilesStructured.jar \"C:\\Users\\Dma\\Desktop\\2017\" -f\n";
    }

}
