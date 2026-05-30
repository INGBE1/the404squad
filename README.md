# the404squad — Gestionnaire de compte 💸

Application web de **contrôle des dépenses** pour un·e étudiant·e de 20 ans à Bruxelles.
Suivi des dépenses par catégorie, budgets avec alertes de dépassement, et projection
d'épargne sur 1 à 10 ans.

> Projet de hackathon — **Java pur (JDK), zéro dépendance** côté backend, Chart.js côté frontend.

---

## ✨ Fonctionnalités

- **Catégories de dépenses** (loyer, alimentation, transport, abonnements, loisirs, santé)
  + épargne, investissement et crédit.
- **Données mock réalistes** : ~17 mois d'historique (jan 2025 → mai 2026) pour le profil
  étudiant, générées de façon déterministe.
- **Graphiques** :
  - camembert de la répartition des dépenses ;
  - barres revenus vs dépenses sur 12 mois ;
  - **projection du patrimoine** sur 1 / 10 ans, 2 scénarios (livret ~1 %/an vs
    investissement ~5 %/an).
- **Budgets & alertes** : budget mensuel par catégorie, **modifiable en direct** (sliders),
  barres de progression et alertes automatiques en cas de dépassement.
- **Export SQL** de la base au démarrage (`database/bank.sql`).

---

## 🧰 Prérequis

- **JDK 17 ou supérieur** (utilise les *records* et le *pattern matching* de switch).

Vérifie ton installation :

```bash
java -version
javac -version
```

> Aucun Maven / Gradle / npm requis. Tout se compile avec `javac`.

---

## 🚀 Lancer le projet

### Windows

```bat
run.cmd
```

### macOS / Linux

```bash
chmod +x run.sh      # la première fois seulement
./run.sh
```

Les deux scripts **compilent** les sources dans `out/` puis **démarrent le serveur**.

Tu verras dans la console :

```
============================================================
  the404squad - Gestionnaire de compte
  Titulaire : Lina Moreau (20 ans, Bruxelles ...)
  >> Ouvre ton navigateur sur : http://localhost:8080
============================================================
```

👉 **Ouvre ensuite [http://localhost:8080](http://localhost:8080) dans ton navigateur.**

Pour arrêter le serveur : `Ctrl + C` dans le terminal.

### Lancement manuel (sans script)

```bash
# Compilation
javac -d out $(find src/main/java -name "*.java")        # macOS/Linux
# (Windows PowerShell)
javac -d out (Get-ChildItem -Recurse -Filter *.java src\main\java).FullName

# Démarrage
java -cp out com.the404squad.App
```

---

## 🌐 API

Le serveur expose une petite API JSON consommée par le dashboard :

| Endpoint | Description |
|---|---|
| `GET /api/overview?month=YYYY-MM&year=YYYY` | Profil + soldes (mois et année) |
| `GET /api/by-category?month=YYYY-MM` | Répartition des dépenses (camembert) |
| `GET /api/monthly?year=YYYY` | Série revenus/dépenses sur 12 mois |
| `GET /api/transactions?month=YYYY-MM` | Liste des transactions du mois |
| `GET /api/budgets?month=YYYY-MM&LOISIRS=80&...` | Budgets vs dépensé + statut d'alerte |
| `GET /api/projection?years=10` | Projection patrimoine (2 scénarios) |
| `GET /api/months` | Mois disponibles (sélecteur) |

Exemple :

```bash
curl "http://localhost:8080/api/projection?years=10"
```

---

## 📁 Structure

```
the404squad/
├── run.cmd / run.sh                 # compile + lance
├── src/main/java/com/the404squad/
│   ├── App.java                     # serveur HTTP + routage API
│   ├── model/                       # Account, Category, Transaction
│   ├── data/BankDatabase.java       # données mock + export SQL
│   ├── service/StatsService.java    # calculs & sérialisation JSON
│   └── util/Json.java               # sérialisation JSON minimale
├── web/                             # index.html, app.js, style.css (dashboard)
├── database/bank.sql                # export SQL généré au démarrage
└── out/                             # classes compilées (généré)
```

---

## 🛠️ Dépannage

- **`'javac' is not recognized` / commande introuvable** → installe un JDK (≥ 17) et
  assure-toi qu'il est dans le `PATH`.
- **Port 8080 déjà utilisé** → modifie la constante `PORT` dans
  `src/main/java/com/the404squad/App.java`, puis relance.
- **`Unable to establish loopback connection` au démarrage** → certains environnements
  restreints (sandbox/CI) bloquent les sockets loopback dont le serveur a besoin ;
  lance le projet sur une machine locale classique.

---

_Données fictives générées pour la démo · **the404squad**_
