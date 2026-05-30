// ====================================================================
//  ING — Mes comptes (the404squad) · app mobile
//  Consomme l'API JSON Java existante et pilote 3 écrans :
//  accueil → comptes → détail catégorie.  i18n FR/EN.
// ====================================================================

// ---------- Catalogue de catégories de base ----------
const BASE_CATALOG = {
    LOYER:          { fr: "Loyer",          en: "Rent",          api: "Loyer",          color: "#6366f1", icon: "🏠" },
    ALIMENTATION:   { fr: "Alimentation",   en: "Groceries",     api: "Alimentation",   color: "#f59e0b", icon: "🛒" },
    TRANSPORT:      { fr: "Transport",      en: "Transport",     api: "Transport",      color: "#06b6d4", icon: "🚌" },
    ABONNEMENTS:    { fr: "Abonnements",    en: "Subscriptions", api: "Abonnements",    color: "#8b5cf6", icon: "📺" },
    LOISIRS:        { fr: "Loisirs",        en: "Leisure",       api: "Loisirs",        color: "#ec4899", icon: "🎮" },
    SANTE:          { fr: "Santé",          en: "Health",        api: "Sante",          color: "#ef4444", icon: "➕" },
    EPARGNE:        { fr: "Épargne",        en: "Savings",       api: "Epargne",        color: "#22c55e", icon: "🐷" },
    INVESTISSEMENT: { fr: "Investissement", en: "Investment",    api: "Investissement", color: "#14b8a6", icon: "📈" },
};

// ---------- Traductions ----------
const I18N = {
    fr: {
        "home.badge": "Banque mobile",
        "home.title": "Gardez le contrôle|de votre argent.",
        "home.cta": "Découvrir mes comptes",
        "home.legal": "Données de démonstration · the404squad",
        "acc.current": "Compte courant",
        "acc.income": "Revenus",
        "acc.expenses": "Dépenses",
        "acc.send": "Envoyer vers une catégorie",
        "acc.where": "Où part votre argent",
        "acc.totalSpent": "Total dépensé",
        "acc.myCats": "Mes catégories",
        "acc.tapHint": "Touchez pour les détails",
        "acc.addCat": "Ajouter une catégorie",
        "acc.footer": "Vos données restent locales · the404squad pour ING",
        "acc.tabAccounts": "Comptes",
        "acc.tabHome": "Accueil",
        "acc.tabProfile": "Profil",
        "det.spentThisMonth": "Dépensé ce mois",
        "det.trend6": "Tendance sur 6 mois",
        "det.transactions": "Opérations",
        "det.empty": "Aucune opération ce mois-ci.",
        "det.ops": "op.",
        "det.alloc": "épargne / placement",
        "sheet.sendTitle": "Envoyer de l'argent",
        "sheet.to": "Vers quelle catégorie",
        "sheet.amount": "Montant",
        "sheet.available": "Solde disponible",
        "sheet.sendCta": "Envoyer",
        "sheet.errAmount": "Saisissez un montant valide.",
        "sheet.errFunds": "Solde insuffisant.",
        "sheet.errCat": "Choisissez une catégorie.",
        "sheet.sent": "Envoyé vers",
        "sheet.newCatTitle": "Nouvelle catégorie",
        "sheet.name": "Nom",
        "sheet.namePh": "Ex : Voyages",
        "sheet.icon": "Icône",
        "sheet.color": "Couleur",
        "sheet.budget": "Budget mensuel (optionnel)",
        "sheet.createCta": "Créer la catégorie",
        "sheet.errName": "Donnez un nom à la catégorie.",
        "sheet.created": "Catégorie créée",
        "menu.title": "Mon profil",
        "menu.settings": "Paramètres",
        "menu.language": "Langue",
        "menu.help": "Aide & support",
        "menu.security": "Sécurité",
        "menu.logout": "Déconnexion",
        "transfer.label": "Virement vers",
    },
    en: {
        "home.badge": "Mobile banking",
        "home.title": "Stay in control|of your money.",
        "home.cta": "Explore my accounts",
        "home.legal": "Demo data · the404squad",
        "acc.current": "Current account",
        "acc.income": "Income",
        "acc.expenses": "Expenses",
        "acc.send": "Send to a category",
        "acc.where": "Where your money goes",
        "acc.totalSpent": "Total spent",
        "acc.myCats": "My categories",
        "acc.tapHint": "Tap for details",
        "acc.addCat": "Add a category",
        "acc.footer": "Your data stays local · the404squad for ING",
        "acc.tabAccounts": "Accounts",
        "acc.tabHome": "Home",
        "acc.tabProfile": "Profile",
        "det.spentThisMonth": "Spent this month",
        "det.trend6": "6-month trend",
        "det.transactions": "Transactions",
        "det.empty": "No transactions this month.",
        "det.ops": "ops",
        "det.alloc": "savings / investment",
        "sheet.sendTitle": "Send money",
        "sheet.to": "To which category",
        "sheet.amount": "Amount",
        "sheet.available": "Available balance",
        "sheet.sendCta": "Send",
        "sheet.errAmount": "Enter a valid amount.",
        "sheet.errFunds": "Insufficient balance.",
        "sheet.errCat": "Pick a category.",
        "sheet.sent": "Sent to",
        "sheet.newCatTitle": "New category",
        "sheet.name": "Name",
        "sheet.namePh": "e.g. Travel",
        "sheet.icon": "Icon",
        "sheet.color": "Color",
        "sheet.budget": "Monthly budget (optional)",
        "sheet.createCta": "Create category",
        "sheet.errName": "Give the category a name.",
        "sheet.created": "Category created",
        "menu.title": "My profile",
        "menu.settings": "Settings",
        "menu.language": "Language",
        "menu.help": "Help & support",
        "menu.security": "Security",
        "menu.logout": "Log out",
        "transfer.label": "Transfer to",
    },
};

const SLOGANS = {
    fr: ["Chaque euro compte.", "Vos finances, enfin lisibles.", "Dépensez malin, épargnez serein.", "Votre avenir se construit aujourd'hui."],
    en: ["Every euro counts.", "Your finances, finally clear.", "Spend smart, save easy.", "Your future starts today."],
};

const FEATURES = {
    fr: [
        { ic: "📊", t: "Vos dépenses, en clair", d: "Réparties par catégorie, en temps réel." },
        { ic: "🎯", t: "Des budgets qui vous parlent", d: "Alertes dès que ça dérape." },
        { ic: "🚀", t: "Faites grandir votre épargne", d: "Projetez votre patrimoine sur 10 ans." },
    ],
    en: [
        { ic: "📊", t: "Your spending, made clear", d: "Split by category, in real time." },
        { ic: "🎯", t: "Budgets that talk to you", d: "Alerts the moment you overspend." },
        { ic: "🚀", t: "Grow your savings", d: "Project your wealth over 10 years." },
    ],
};

const ICON_CHOICES = ["🎒", "✈️", "🍔", "☕", "🎁", "💊", "👕", "🐶", "💡", "📚", "🏋️", "🎵", "🚗", "🌱", "💻", "🎨"];
const COLOR_CHOICES = ["#FF6200", "#6366f1", "#ec4899", "#06b6d4", "#22c55e", "#f59e0b", "#8b5cf6", "#ef4444", "#14b8a6", "#0ea5e9"];

// ---------- État ----------
const state = {
    lang: "fr",
    months: [],
    month: null,
    overview: null,
    budgets: [],        // catégories de dépense (API /budgets)
    txs: [],            // transactions du mois
    catTotals: {},      // key -> dépensé réel (API) ce mois
    catalog: structuredClone(BASE_CATALOG),
    order: Object.keys(BASE_CATALOG),
    balance: 3000,      // solde du compte courant (client)
    transfers: [],      // { key, amount, date } virements émis
};

let pieChart = null;
let sloganTimer = null;

const t = (k) => (I18N[state.lang][k] ?? k);
const catName = (key) => { const c = state.catalog[key]; return c ? (c[state.lang] || c.fr || c.label) : key; };

const euro = (n) => new Intl.NumberFormat(state.lang === "fr" ? "fr-BE" : "en-IE", { style: "currency", currency: "EUR", maximumFractionDigits: 0 }).format(n);
const euro2 = (n) => new Intl.NumberFormat(state.lang === "fr" ? "fr-BE" : "en-IE", { style: "currency", currency: "EUR" }).format(n);

async function api(path) {
    const res = await fetch(path);
    if (!res.ok) throw new Error("API " + path + " → " + res.status);
    return res.json();
}

// --------------------------------------------------------------------
//  Navigation
// --------------------------------------------------------------------
function show(id) {
    document.querySelectorAll(".screen").forEach((s) => s.classList.remove("is-active", "is-left"));
    const el = document.getElementById(id);
    el.classList.add("is-active");
    const sc = el.querySelector(".accounts-scroll, .detail-scroll");
    if (sc) sc.scrollTop = 0;
}

// --------------------------------------------------------------------
//  i18n
// --------------------------------------------------------------------
function applyLang() {
    document.documentElement.lang = state.lang;
    document.querySelectorAll("[data-i18n]").forEach((el) => { el.textContent = t(el.dataset.i18n); });
    document.querySelectorAll("#langToggle button").forEach((b) => b.classList.toggle("is-active", b.dataset.lang === state.lang));

    // Accueil dynamique
    renderHomeTitle();
    renderFeatures();
    restartSlogans();

    // Si l'écran comptes est chargé, on rafraîchit le contenu dynamique.
    if (state.overview) { renderMainSlot(); renderDonut(); renderCatSlots(); }
}

function setLang(lang) {
    if (lang === state.lang) return;
    state.lang = lang;
    applyLang();
}

// --------------------------------------------------------------------
//  Accueil : logo animé + titre/description dynamiques
// --------------------------------------------------------------------
function renderHomeTitle() {
    const el = document.getElementById("heroTitle");
    const lines = t("home.title").split("|");
    el.innerHTML = lines.map((line, li) =>
        line.split(" ").map((w, wi) =>
            `<span class="word" style="animation-delay:${(li * 4 + wi) * 0.08 + 0.1}s">${w}</span>`
        ).join(" ") + (li < lines.length - 1 ? "<br>" : "")
    ).join("");
}

function renderFeatures() {
    const ul = document.getElementById("homeFeatures");
    ul.innerHTML = FEATURES[state.lang].map((f, i) => `
        <li class="feat" style="--d:${0.5 + i * 0.12}s">
            <span class="feat-ic">${f.ic}</span>
            <div><strong>${f.t}</strong><span>${f.d}</span></div>
        </li>`).join("");
}

// Effet machine à écrire sur le slogan + rotation.
let typeTimer = null;
function restartSlogans() {
    clearInterval(sloganTimer);
    clearTimeout(typeTimer);
    const el = document.getElementById("heroSlogan");
    const list = SLOGANS[state.lang];
    let idx = 0;

    function type(text, done) {
        let i = 0;
        el.innerHTML = `<span class="typed"></span><span class="cursor"></span>`;
        const span = el.querySelector(".typed");
        (function step() {
            span.textContent = text.slice(0, i);
            if (i++ <= text.length) typeTimer = setTimeout(step, 38);
            else if (done) done();
        })();
    }
    type(list[idx]);
    sloganTimer = setInterval(() => {
        idx = (idx + 1) % list.length;
        type(list[idx]);
    }, 3600);
}

// --------------------------------------------------------------------
//  Initialisation
// --------------------------------------------------------------------
async function init() {
    applyLang();

    document.getElementById("langToggle").addEventListener("click", (e) => {
        const b = e.target.closest("button"); if (b) setLang(b.dataset.lang);
    });
    document.getElementById("btnStart").addEventListener("click", () => { show("screen-accounts"); loadAccounts(); });
    document.getElementById("tabBackHome").addEventListener("click", () => show("screen-home"));
    document.getElementById("btnBack").addEventListener("click", () => show("screen-accounts"));
    document.getElementById("btnSend").addEventListener("click", openSendSheet);
    document.getElementById("btnAddCat").addEventListener("click", openAddCatSheet);
    document.getElementById("avatar").addEventListener("click", openProfileSheet);
    document.getElementById("tabProfile").addEventListener("click", openProfileSheet);
    document.getElementById("sheetClose").addEventListener("click", closeSheet);
    document.getElementById("overlay").addEventListener("click", (e) => { if (e.target.id === "overlay") closeSheet(); });

    state.months = await api("/api/months");
    const sel = document.getElementById("monthSelect");
    sel.innerHTML = state.months.map((m) => `<option value="${m}">${monthLabel(m)}</option>`).join("");
    state.month = state.months[0];
    sel.value = state.month;
    sel.addEventListener("change", () => { state.month = sel.value; loadAccounts(); });
}

function monthLabel(m) {
    const [y, mo] = m.split("-");
    const noms = state.lang === "fr"
        ? ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"]
        : ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    return `${noms[parseInt(mo) - 1]} ${y}`;
}

// --------------------------------------------------------------------
//  Helpers données
// --------------------------------------------------------------------
function keyFromApiLabel(apiLabel) {
    return Object.keys(state.catalog).find((k) => state.catalog[k].api === apiLabel) || null;
}
function transferTotal(key) {
    return state.transfers.filter((x) => x.key === key).reduce((s, x) => s + x.amount, 0);
}
function spentOf(key) {
    return (state.catTotals[key] || 0) + transferTotal(key);
}
function budgetOf(key) {
    const b = state.budgets.find((x) => x.key === key);
    if (b) return b.budget;
    const c = state.catalog[key];
    return c && c.budget ? c.budget : 0;
}
function statusOf(key) {
    const budget = budgetOf(key);
    if (budget <= 0) return "ok";
    const pct = (spentOf(key) / budget) * 100;
    return pct >= 100 ? "over" : pct >= 80 ? "warn" : "ok";
}

// --------------------------------------------------------------------
//  Écran COMPTES
// --------------------------------------------------------------------
async function loadAccounts() {
    const year = parseInt(state.month.split("-")[0]);
    const [overview, budgets, txs] = await Promise.all([
        api(`/api/overview?month=${state.month}&year=${year}`),
        api(`/api/budgets?month=${state.month}`),
        api(`/api/transactions?month=${state.month}`),
    ]);
    state.overview = overview;
    state.budgets = budgets;
    state.txs = txs;

    const totals = {};
    txs.forEach((tx) => {
        const key = keyFromApiLabel(tx.category);
        if (key) totals[key] = (totals[key] || 0) + Math.abs(tx.amount);
    });
    state.catTotals = totals;

    renderMainSlot();
    renderDonut();
    renderCatSlots();
}

function renderMainSlot() {
    const o = state.overview;
    document.getElementById("avatar").textContent = initials(o.holder);
    document.getElementById("msHolder").textContent = o.holder;
    document.getElementById("msIban").textContent = maskIban(o.iban);
    animateValue(document.getElementById("msBalance"), state.balance, euro2, 1000);
    document.getElementById("msIn").textContent = euro(o.month.revenus);
    const totalTransfers = state.transfers.reduce((s, x) => s + x.amount, 0);
    document.getElementById("msOut").textContent = euro(o.month.depenses + totalTransfers);
}

function initials(name) { return name.split(/\s+/).map((w) => w[0]).slice(0, 2).join("").toUpperCase(); }
function maskIban(iban) { return iban ? "•••• •••• •••• " + iban.replace(/\s+/g, "").slice(-4) : ""; }

function renderDonut() {
    const data = state.order
        .map((key) => ({ key, spent: spentOf(key) }))
        .filter((d) => d.spent > 0);
    const total = data.reduce((s, d) => s + d.spent, 0);

    document.getElementById("pieScope").textContent = monthLabel(state.month);
    animateValue(document.getElementById("donutTotal"), total, euro, 900);

    const ctx = document.getElementById("pieChart");
    if (pieChart) pieChart.destroy();
    pieChart = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: data.map((d) => catName(d.key)),
            datasets: [{
                data: data.map((d) => d.spent),
                backgroundColor: data.map((d) => state.catalog[d.key].color),
                borderColor: "#ffffff", borderWidth: 3, hoverOffset: 6,
            }],
        },
        options: {
            cutout: "70%", responsive: true, maintainAspectRatio: false,
            animation: { animateRotate: true, duration: 900 },
            plugins: {
                legend: { display: false },
                tooltip: { callbacks: { label: (c) => `${c.label} : ${euro2(c.raw)}` } },
            },
        },
    });
}

function renderCatSlots() {
    const grid = document.getElementById("catsGrid");
    grid.innerHTML = state.order.map((key, i) => {
        const c = state.catalog[key];
        const spent = spentOf(key);
        const budget = budgetOf(key);
        const hasBudget = budget > 0;
        const status = statusOf(key);
        const fillColor = status === "over" ? "#e23b3b" : status === "warn" ? "#e89313" : c.color;

        let sub, subCls = "";
        if (hasBudget) {
            sub = `<span>${euro(spent)} / ${euro(budget)}</span><span>${Math.round((spent / budget) * 100)}%</span>`;
            subCls = status === "over" ? "is-over" : status === "warn" ? "is-warn" : "";
        } else if (key === "EPARGNE" || key === "INVESTISSEMENT") {
            sub = `<span>${t("det.alloc")}</span>`;
        } else {
            sub = `<span>&nbsp;</span>`;
        }

        return `
        <button class="cat-slot" data-key="${key}" style="--d:${i * 0.04}s">
            <span class="cat-slot__go">›</span>
            <span class="cat-slot__ic" style="background:${hexA(c.color, .14)}">${c.icon}</span>
            <div class="cat-slot__name">${catName(key)}</div>
            <div class="cat-slot__amount">${euro(spent)}</div>
            ${hasBudget ? `<div class="cat-slot__bar"><div class="cat-slot__fill" data-pct="${Math.min(100, (spent / budget) * 100)}" style="background:${fillColor}"></div></div>` : ""}
            <div class="cat-slot__sub ${subCls}">${sub}</div>
        </button>`;
    }).join("");

    grid.querySelectorAll(".cat-slot").forEach((slot) => {
        const fill = slot.querySelector(".cat-slot__fill");
        if (fill) requestAnimationFrame(() => { fill.style.width = fill.dataset.pct + "%"; });
        slot.addEventListener("click", () => openCategory(slot.dataset.key));
    });
}

function hexA(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`;
}

// --------------------------------------------------------------------
//  Écran DÉTAIL catégorie
// --------------------------------------------------------------------
async function openCategory(key) {
    const c = state.catalog[key];
    const head = document.getElementById("detailHead");
    head.style.background = `linear-gradient(140deg, ${c.color}, ${shade(c.color, -0.28)})`;
    document.getElementById("dIcon").textContent = c.icon;
    document.getElementById("dTitle").textContent = catName(key);

    // Transactions réelles + virements de la catégorie.
    const real = state.txs.filter((tx) => tx.category === c.api)
        .map((tx) => ({ date: tx.date, label: tx.label, amount: tx.amount }));
    const virt = state.transfers.filter((x) => x.key === key)
        .map((x) => ({ date: x.date, label: `${t("transfer.label")} ${catName(key)}`, amount: -x.amount, isTransfer: true }));
    const txs = [...real, ...virt].sort((a, b) => b.date.localeCompare(a.date));

    const total = spentOf(key);
    document.getElementById("dCount").textContent = `${txs.length} ${t("det.ops")}`;
    animateValue(document.getElementById("dTotal"), total, euro2, 800);

    const budget = budgetOf(key);
    const budgetEl = document.getElementById("dBudget");
    if (budget > 0) {
        budgetEl.classList.remove("is-hidden");
        const status = statusOf(key);
        const fill = document.getElementById("dBudgetFill");
        fill.className = "budget__fill" + (status === "over" ? " is-over" : status === "warn" ? " is-warn" : "");
        fill.style.width = "0%";
        requestAnimationFrame(() => { fill.style.width = Math.min(100, (total / budget) * 100) + "%"; });
        const left = budget - total;
        document.getElementById("dBudgetMeta").textContent = state.lang === "fr"
            ? (left >= 0 ? `${euro(left)} restants sur un budget de ${euro(budget)}` : `Dépassé de ${euro(-left)} (budget ${euro(budget)})`)
            : (left >= 0 ? `${euro(left)} left of a ${euro(budget)} budget` : `Over by ${euro(-left)} (budget ${euro(budget)})`);
    } else {
        budgetEl.classList.add("is-hidden");
    }

    const list = document.getElementById("dTxList");
    if (txs.length === 0) {
        list.innerHTML = `<li class="tx-empty">${t("det.empty")}</li>`;
    } else {
        list.innerHTML = txs.map((tx, i) => {
            const neg = tx.amount < 0;
            return `
            <li class="tx" style="--d:${Math.min(i, 12) * 0.03}s">
                <span class="tx__ic" style="background:${hexA(c.color, .14)}">${tx.isTransfer ? "⇄" : c.icon}</span>
                <div class="tx__body">
                    <div class="tx__label">${escapeHtml(tx.label)}</div>
                    <div class="tx__date">${formatDate(tx.date)}</div>
                </div>
                <div class="tx__amount ${neg ? "neg" : "pos"}">${neg ? "−" : "+"}${euro2(Math.abs(tx.amount))}</div>
            </li>`;
        }).join("");
    }

    show("screen-detail");
    loadTrend(key, c);
}

async function loadTrend(key, c) {
    const wrap = document.getElementById("dTrendBars");
    wrap.innerHTML = "";
    const months = lastMonths(state.month, 6);
    const results = await Promise.all(months.map((m) => api(`/api/transactions?month=${m}`).catch(() => [])));
    const series = results.map((txs, i) => {
        let total = txs.filter((tx) => tx.category === c.api).reduce((s, tx) => s + Math.abs(tx.amount), 0);
        if (months[i] === state.month) total += transferTotal(key);
        return { month: months[i], total };
    });
    const max = Math.max(1, ...series.map((s) => s.total));
    wrap.innerHTML = series.map((s) => {
        const isCur = s.month === state.month;
        const h = Math.max(4, (s.total / max) * 100);
        return `
        <div class="tbar">
            <span class="tbar__val">${s.total > 0 ? euro(s.total) : ""}</span>
            <div class="tbar__col ${isCur ? "is-current" : ""}" data-h="${h}" style="height:0;${isCur ? "" : `background:${hexA(c.color, .25)}`}"></div>
            <span class="tbar__lab">${shortMonth(s.month)}</span>
        </div>`;
    }).join("");
    requestAnimationFrame(() => wrap.querySelectorAll(".tbar__col").forEach((col) => { col.style.height = col.dataset.h + "%"; }));
}

// --------------------------------------------------------------------
//  Feuilles modales (sheets)
// --------------------------------------------------------------------
function openSheet(title, bodyHTML) {
    document.getElementById("sheetTitle").textContent = title;
    document.getElementById("sheetBody").innerHTML = bodyHTML;
    const ov = document.getElementById("overlay");
    ov.hidden = false;
    ov.classList.remove("is-closing");
    return document.getElementById("sheetBody");
}
function closeSheet() {
    const ov = document.getElementById("overlay");
    if (ov.hidden) return;
    ov.classList.add("is-closing");
    setTimeout(() => { ov.hidden = true; ov.classList.remove("is-closing"); }, 200);
}

// ---- Envoyer de l'argent ----
function openSendSheet() {
    const opts = state.order.map((key) => {
        const c = state.catalog[key];
        return `
        <button class="cat-opt" data-key="${key}">
            <span class="cat-opt__ic" style="background:${hexA(c.color, .15)}">${c.icon}</span>
            <span class="cat-opt__name">${catName(key)}</span>
            <span class="cat-opt__spent">${euro(spentOf(key))}</span>
        </button>`;
    }).join("");

    const body = openSheet(t("sheet.sendTitle"), `
        <div class="sheet-balance">${t("sheet.available")} : <strong id="sendBal">${euro2(state.balance)}</strong></div>
        <div class="field">
            <label>${t("sheet.amount")}</label>
            <div class="amount-field">
                <input type="number" id="sendAmount" inputmode="decimal" min="0" step="10" placeholder="0">
                <span class="cur">€</span>
            </div>
        </div>
        <div class="field">
            <label>${t("sheet.to")}</label>
            <div class="cat-picker" id="sendPicker">${opts}</div>
        </div>
        <div class="sheet-err" id="sendErr"></div>
        <button class="btn--cta" id="sendConfirm">${t("sheet.sendCta")}</button>
    `);

    let selKey = null;
    body.querySelectorAll(".cat-opt").forEach((el) => el.addEventListener("click", () => {
        body.querySelectorAll(".cat-opt").forEach((x) => x.classList.remove("is-sel"));
        el.classList.add("is-sel");
        selKey = el.dataset.key;
    }));

    body.querySelector("#sendConfirm").addEventListener("click", () => {
        const err = body.querySelector("#sendErr");
        const amount = Math.round(parseFloat(body.querySelector("#sendAmount").value) * 100) / 100;
        if (!selKey) { err.textContent = t("sheet.errCat"); return; }
        if (isNaN(amount) || amount <= 0) { err.textContent = t("sheet.errAmount"); return; }
        if (amount > state.balance) { err.textContent = t("sheet.errFunds"); return; }

        state.balance -= amount;
        state.transfers.push({ key: selKey, amount, date: state.month + "-" + String(new Date().getDate()).padStart(2, "0") });
        closeSheet();
        renderMainSlot(); renderDonut(); renderCatSlots();
        toast(`${t("sheet.sent")} ${catName(selKey)} · ${euro2(amount)}`, true);
    });
}

// ---- Ajouter une catégorie ----
function openAddCatSheet() {
    const icons = ICON_CHOICES.map((ic, i) => `<button class="pick ${i === 0 ? "is-sel" : ""}" data-icon="${ic}">${ic}</button>`).join("");
    const colors = COLOR_CHOICES.map((col, i) => `<button class="pick swatch ${i === 0 ? "is-sel" : ""}" data-color="${col}" style="background:${col}"></button>`).join("");

    const body = openSheet(t("sheet.newCatTitle"), `
        <div class="field">
            <label>${t("sheet.name")}</label>
            <input type="text" id="catName" maxlength="22" placeholder="${t("sheet.namePh")}">
        </div>
        <div class="field">
            <label>${t("sheet.icon")}</label>
            <div class="picker-row" id="iconRow">${icons}</div>
        </div>
        <div class="field">
            <label>${t("sheet.color")}</label>
            <div class="picker-row" id="colorRow">${colors}</div>
        </div>
        <div class="field">
            <label>${t("sheet.budget")}</label>
            <div class="amount-field">
                <input type="number" id="catBudget" inputmode="decimal" min="0" step="10" placeholder="0">
                <span class="cur">€</span>
            </div>
        </div>
        <div class="sheet-err" id="catErr"></div>
        <button class="btn--cta" id="catConfirm">${t("sheet.createCta")}</button>
    `);

    let icon = ICON_CHOICES[0], color = COLOR_CHOICES[0];
    body.querySelector("#iconRow").addEventListener("click", (e) => {
        const b = e.target.closest(".pick"); if (!b) return;
        body.querySelectorAll("#iconRow .pick").forEach((x) => x.classList.remove("is-sel"));
        b.classList.add("is-sel"); icon = b.dataset.icon;
    });
    body.querySelector("#colorRow").addEventListener("click", (e) => {
        const b = e.target.closest(".pick"); if (!b) return;
        body.querySelectorAll("#colorRow .pick").forEach((x) => x.classList.remove("is-sel"));
        b.classList.add("is-sel"); color = b.dataset.color;
    });

    body.querySelector("#catConfirm").addEventListener("click", () => {
        const err = body.querySelector("#catErr");
        const name = body.querySelector("#catName").value.trim();
        if (!name) { err.textContent = t("sheet.errName"); return; }
        const budget = parseFloat(body.querySelector("#catBudget").value) || 0;
        const key = "CUSTOM_" + Date.now();
        state.catalog[key] = { fr: name, en: name, api: key, color, icon, budget };
        state.order.push(key);
        closeSheet();
        renderDonut(); renderCatSlots();
        toast(`${t("sheet.created")} · ${name}`, true);
        // Petit scroll vers la nouvelle catégorie.
        document.querySelector(".accounts-scroll").scrollTo({ top: document.querySelector("#catsGrid").offsetTop, behavior: "smooth" });
    });
}

// ---- Menu profil ----
function openProfileSheet() {
    const o = state.overview;
    const name = o ? o.holder : "—";
    const sub = o ? `${o.age} ${state.lang === "fr" ? "ans" : "y.o."} · ${o.city}` : "";
    const body = openSheet(t("menu.title"), `
        <div class="menu-user">
            <div class="avatar">${o ? initials(name) : "··"}</div>
            <div><div class="menu-user__name">${name}</div><div class="menu-user__sub">${sub}</div></div>
        </div>
        <ul class="menu-list">
            <li><button class="menu-item"><span class="menu-item__ic">⚙️</span>${t("menu.settings")}<span class="menu-item__chev">›</span></button></li>
            <li><button class="menu-item"><span class="menu-item__ic">🔒</span>${t("menu.security")}<span class="menu-item__chev">›</span></button></li>
            <li><div class="menu-item"><span class="menu-item__ic">🌐</span>${t("menu.language")}
                <span class="menu-lang" id="menuLang">
                    <button data-lang="fr" class="${state.lang === "fr" ? "is-active" : ""}">FR</button>
                    <button data-lang="en" class="${state.lang === "en" ? "is-active" : ""}">EN</button>
                </span></div></li>
            <li><button class="menu-item"><span class="menu-item__ic">💬</span>${t("menu.help")}<span class="menu-item__chev">›</span></button></li>
            <li><button class="menu-item menu-item--danger" id="menuLogout"><span class="menu-item__ic">⏻</span>${t("menu.logout")}</button></li>
        </ul>
    `);

    body.querySelector("#menuLang").addEventListener("click", (e) => {
        const b = e.target.closest("button"); if (!b) return;
        setLang(b.dataset.lang);
        body.querySelectorAll("#menuLang button").forEach((x) => x.classList.toggle("is-active", x.dataset.lang === state.lang));
        document.getElementById("sheetTitle").textContent = t("menu.title");
    });
    body.querySelector("#menuLogout").addEventListener("click", () => { closeSheet(); show("screen-home"); });
}

// --------------------------------------------------------------------
//  Utilitaires
// --------------------------------------------------------------------
function animateValue(el, to, fmt = euro, dur = 900) {
    const start = performance.now();
    (function tick(now) {
        const p = Math.min(1, (now - start) / dur);
        const eased = 1 - Math.pow(1 - p, 3);
        el.textContent = fmt(to * eased);
        if (p < 1) requestAnimationFrame(tick);
    })(start);
}

let toastTimer = null;
function toast(msg, ok = false) {
    const el = document.getElementById("toast");
    el.textContent = msg;
    el.className = "toast" + (ok ? " toast--ok" : "");
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 2600);
}

function lastMonths(month, n) {
    const [y, m] = month.split("-").map(Number);
    const out = []; let yy = y, mm = m;
    for (let i = 0; i < n; i++) { out.unshift(`${yy}-${String(mm).padStart(2, "0")}`); mm--; if (mm === 0) { mm = 12; yy--; } }
    return out;
}
function shortMonth(m) {
    const fr = ["jan", "fév", "mar", "avr", "mai", "juin", "juil", "aoû", "sep", "oct", "nov", "déc"];
    const en = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"];
    return (state.lang === "fr" ? fr : en)[parseInt(m.split("-")[1]) - 1];
}
function shade(hex, amt) {
    const n = parseInt(hex.slice(1), 16);
    let r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
    r = Math.round(Math.min(255, Math.max(0, r + r * amt)));
    g = Math.round(Math.min(255, Math.max(0, g + g * amt)));
    b = Math.round(Math.min(255, Math.max(0, b + b * amt)));
    return `rgb(${r},${g},${b})`;
}
function formatDate(iso) { const [y, m, d] = iso.split("-"); return `${d}/${m}/${y}`; }
function escapeHtml(s) { return s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c])); }

// --------------------------------------------------------------------
init().catch((e) => {
    document.querySelector(".phone").insertAdjacentHTML("beforeend",
        `<div class="toast">Erreur de chargement : ${e.message}</div>`);
});
