# Używamy obrazu Javy 23
FROM eclipse-temurin:23-jdk-alpine

# Katalog roboczy w kontenerze
WORKDIR /app

# Kopiujemy zbudowany plik .jar do kontenera
# Gwiazdka *.jar sprawi, że nie musisz wpisywać konkretnej wersji
COPY target/*.jar app.jar

# Komenda startowa
ENTRYPOINT ["java", "-jar", "app.jar"]