package ru.gas.filesstructured;

public class Main {
    public static void main(String[] args) {
        System.out.println(
                "Программа структурирования файлов по подпапкам с именем [год]_[номер месяца]\n" +
                "Год и номер месяца - берутся из даты съемки если это фото, иначе из даты изменения файла.\n" +
                "Путь к корневой директории указать в аргументах программы.\n");
        if(args.length == 0)
            System.out.println("Введите в аргументах адрес корневой директории\n");
        else {
            MovingFile movingFile = new MovingFile(args[0]);
            movingFile.run();
        }
    }
}
