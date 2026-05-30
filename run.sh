#!/usr/bin/env bash
# ============================================================
#  the404squad - Gestionnaire de compte
#  Compile et lance l'application (Java pur, aucune dependance)
# ============================================================
set -e

echo "[1/2] Compilation..."
mkdir -p out
find src/main/java -name "*.java" > sources.txt
javac -d out @sources.txt
rm -f sources.txt

echo "[2/2] Demarrage du serveur..."
echo
java -cp out com.the404squad.App
