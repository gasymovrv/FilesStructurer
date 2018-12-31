:: файл обязательно должен быть в кодировке "Cyrillic (Windows 1251)"
:: иначе параметры передаваемые программе будут нечитаемы 
:: В консоле тип шрифта должен быть "Lucida Console"
chcp 1251

java -jar target/FilesStructured-1.0-SNAPSHOT-jar-with-dependencies.jar "C:\Users\Dma\Desktop\2017"

pause