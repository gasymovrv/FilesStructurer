package ru.gas.filesstructured;

public enum Options {
    RM("-rm Рекурсивное перемещение в корневую директорию"),
    RMR("-rmr Рекурсивное перемещение в корневую директорию и переименование"),
    RMF("-rmf Рекурсивное перемещение в корневую директорию и заполнение даты съемки"),
    F("-f Создание новых директорий и перемещение в них"),
    EMPTY("Доп. опции отсутствуют - переименование в корневой директории");

    private final String info;

    Options(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }
}
