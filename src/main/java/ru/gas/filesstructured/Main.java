package ru.gas.filesstructured;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        System.out.println("Программа структурирования файлов\n");
        System.out.println(getArgsInfo());
        Options option = Options.EMPTY;
        Set<String> exclusions = Set.of();
        System.out.println("----------------------------------------------------");

        if (args.length == 0) {
            printErrorInfo();
            return;
        } else if (args.length > 1) {
            Iterator<String> argsWithoutDirectory = Arrays.stream(Arrays.copyOfRange(args, 1, args.length)).iterator();
            int count = 0;

            while (argsWithoutDirectory.hasNext()) {
                final var s = argsWithoutDirectory.next();

                if (s.equals("-e") && argsWithoutDirectory.hasNext()) {
                    exclusions = Arrays.stream(argsWithoutDirectory.next().split(","))
                            .collect(Collectors.toSet());
                    continue;
                }
                if (s.equals("-f")) {
                    option = Options.F;
                    count++;
                }
                if (s.equals("-rm")) {
                    option = Options.RM;
                    count++;
                }
                if (s.equals("-rmr")) {
                    option = Options.RMR;
                    count++;
                }
                if (s.equals("-rmf")) {
                    option = Options.RMF;
                    count++;
                }
            }
            if (count > 1) {
                printErrorInfo();
                return;
            }
        }
        MovingOrRenamingFiles movingOrRenamingFiles = new MovingOrRenamingFiles(args[0], option, exclusions);
        System.out.println(option.getInfo());
        System.out.println("----------------------run---------------------------");
        movingOrRenamingFiles.run();
    }

    private static void printErrorInfo() {
        System.out.println("Неверные параметры запуска.");
        System.out.println(getArgsInfo());
    }

    private static String getArgsInfo() {
        return """
                Обязательные аргументы:
                    1.[путь_к_корневой_директории]

                Необязательные аргументы
                                
                    Тип запуска (может быть выбран только один):
                        1.[-rm] - рекурсивное извлечение файлов из всех подпапок
                        2.[-rmr] - рекурсивное извлечение файлов из всех подпапок и переименование по шаблону: [год]_[№ месяца]_[№ файла]
                        2.[-rmf] - рекурсивное извлечение файлов из всех подпапок и заполнение даты съемки (если пуста) из даты изменения для JPEG-файлов
                        3.[-f] - распределяет файлы по подпапкам с именем [год]_[№ месяца]
                        4. Без необязательных аргументов - переименовывает файлы в указанной директории по шаблону: [год]_[№ месяца]_[№ файла]

                    Исключения - необязательный аргумент, но если указан, то должен идти последним:
                        5. [-e <список имен файлов через запятую для исключения работы с ними>]
                               
                Примечания:
                Год, номер месяца и день - берутся из даты съемки если это фото, иначе из даты изменения файла.

                Пример запуска:
                java -jar FilesStructured.jar "C:\\Users\\Dma\\Desktop\\2017" -f
                """;
    }

}
