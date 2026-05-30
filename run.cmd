@echo off
REM ============================================================
REM  the404squad - Gestionnaire de compte
REM  Compile et lance l'application (Java pur, aucune dependance)
REM ============================================================
setlocal

echo [1/2] Compilation...
if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -d out @sources.txt
if errorlevel 1 (
    echo.
    echo ERREUR de compilation.
    del sources.txt
    exit /b 1
)
del sources.txt

echo [2/2] Demarrage du serveur...
echo.
java -cp out com.the404squad.App

endlocal
