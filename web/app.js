// ====================================================================
//  the404squad — Gestionnaire de compte (frontend)
//  Recupere les stats depuis l'API Java et dessine le dashboard.
// ====================================================================

const state = {
    month: null,    // "YYYY-MM"
    view: "month",  // "month" | "year"
    budgets: {},    // overrides utilisateur { CATEGORIE: montant }
    projYears: 10,  // horizon de projection (1 | 10)
};

let pieChart = null;
let barChart = null;
let projChart = null;

const euro = (n) =>
    new Intl.NumberFormat("fr-BE", { style: "currency", currency: "EUR" }).format(n);

async function api(path) {
    const res = await fetch(path);
    if (!res.ok) throw new Error("API " + path + " -> " + res.status);
    return res.json();
}

// --------------------------------------------------------------------
//  Initialisation
// --------------------------------------------------------------------
async function init() {
    const months = await api("/api/months");
    const sel = document.getElementById("monthSelect");
    sel.innerHTML = months
        .map((m) => `<option value="${m}">${formatMonthLabel(m)}</option>`)
        .join("");
    state.month = months[0];
    sel.value = state.month;

    sel.addEventListener("change", () => {
        state.month = sel.value;
        render();
    });

    document.querySelectorAll(".controls .view-toggle button").forEach((btn) => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".controls .view-toggle button").forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            state.view = btn.dataset.view;
            render();
        });
    });

    // Toggle horizon de projection (1 an / 10 ans).
    document.querySelectorAll("#projToggle button").forEach((btn) => {
        btn.addEventListener("click", () => {
            document.querySelectorAll("#projToggle button").forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            state.projYears = parseInt(btn.dataset.years);
            renderProjection();
        });
    });

    render();
}

function formatMonthLabel(m) {
    const [y, mo] = m.split("-");
    const noms = ["janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"];
    return `${noms[parseInt(mo) - 1]} ${y}`;
}

// --------------------------------------------------------------------
//  Rendu
// --------------------------------------------------------------------
async function render() {
    const [y, mo] = state.month.split("-");
    const year = parseInt(y);

    const overview = await api(`/api/overview?month=${state.month}&year=${year}`);
    renderProfile(overview);
    renderCards(overview);

    const scope = state.view === "month" ? formatMonthLabel(state.month) : `année ${year}`;
    document.getElementById("pieScope").textContent = `· ${scope}`;
    document.getElementById("barScope").textContent = `· ${year}`;
    document.getElementById("budgetScope").textContent = `· ${formatMonthLabel(state.month)}`;
    document.getElementById("tableScope").textContent = `· ${formatMonthLabel(state.month)}`;

    await renderBudgets();
    await renderPie(year, mo);
    await renderBar(year);
    await renderProjection();
    await renderTable();
}

function renderProfile(o) {
    document.getElementById("profileName").textContent = o.holder;
    document.getElementById("profileSub").textContent =
        `${o.age} ans · ${o.city} · ${o.iban}`;
}

function renderCards(o) {
    const d = state.view === "month" ? o.month : o.year;
    const periodLabel = state.view === "month" ? "ce mois" : "cette année";

    const cards = [
        { label: "Solde du compte courant", value: o.soldeCourant, color: "#3b82f6",
          sub: "aujourd'hui", signed: true },
        { label: `Revenus (${periodLabel})`, value: d.revenus, color: "#22c55e",
          sub: "entrées d'argent", signed: false, pos: true },
        { label: `Dépenses (${periodLabel})`, value: d.depenses, color: "#ef4444",
          sub: "consommation", signed: false, neg: true },
        { label: `Reste à vivre (${periodLabel})`, value: d.resteAVivre, color: "#6366f1",
          sub: "après épargne & crédits", signed: true },
        { label: `Épargne (${periodLabel})`, value: d.epargne, color: "#22c55e",
          sub: `cumul : ${euro(o.epargneCumulee)}`, signed: false },
        { label: `Investissements (${periodLabel})`, value: d.investissement, color: "#14b8a6",
          sub: `cumul : ${euro(o.investCumule)}`, signed: false },
        { label: `Crédits remboursés (${periodLabel})`, value: d.credit, color: "#f97316",
          sub: "PC portable", signed: false },
    ];

    document.getElementById("cards").innerHTML = cards.map((c) => {
        let cls = "value";
        if (c.signed) cls += c.value >= 0 ? " pos" : " neg";
        else if (c.pos) cls += " pos";
        else if (c.neg) cls += " neg";
        return `
        <div class="kpi" style="--kpi-color:${c.color}">
            <div class="label">${c.label}</div>
            <div class="${cls}">${euro(c.value)}</div>
            <div class="sub">${c.sub}</div>
        </div>`;
    }).join("");
}

// --------------------------------------------------------------------
//  Budgets par catégorie + alertes de dépassement
// --------------------------------------------------------------------
async function renderBudgets() {
    // Construit la query avec les budgets surchargés par l'utilisateur.
    const params = new URLSearchParams({ month: state.month });
    Object.entries(state.budgets).forEach(([k, v]) => params.set(k, v));
    const data = await api(`/api/budgets?${params.toString()}`);

    // Bannière d'alertes (catégories en dépassement ou proches du budget).
    const alerts = data.filter((b) => b.status !== "ok");
    const alertsEl = document.getElementById("alerts");
    if (alerts.length === 0) {
        alertsEl.innerHTML = `<div class="alert-banner ok">✅ Tout est sous contrôle ce mois-ci.</div>`;
    } else {
        alertsEl.innerHTML = alerts.map((b) => {
            const over = b.spent - b.budget;
            const msg = b.status === "over"
                ? `dépassement de ${euro(over)}`
                : `bientôt au max (${Math.round(b.pct)} %)`;
            return `<div class="alert-banner ${b.status}">${b.status === "over" ? "⛔" : "⚠️"}
                <strong>${b.category}</strong> : ${euro(b.spent)} / ${euro(b.budget)} — ${msg}</div>`;
        }).join("");
    }

    // Lignes de budget avec slider + saisie + barre de progression.
    const budgetsEl = document.getElementById("budgets");
    budgetsEl.innerHTML = data.map((b) => {
        const width = Math.min(100, b.pct);
        return `
        <div class="budget-row">
            <div class="budget-info">
                <span class="budget-name"><span class="dot" style="background:${b.color}"></span>${b.category}</span>
                <span class="budget-figures ${b.status}">${euro(b.spent)} / ${euro(b.budget)}</span>
            </div>
            <div class="budget-bar"><div class="budget-fill ${b.status}" style="width:${width}%"></div></div>
            <div class="budget-controls">
                <input type="range" min="0" max="800" step="10" value="${b.budget}" data-cat="${b.key}" class="budget-range">
                <input type="number" min="0" step="10" value="${b.budget}" data-cat="${b.key}" class="budget-number">
                <span class="unit">€</span>
            </div>
        </div>`;
    }).join("");

    // Slider : maj en direct (debounce léger pour ne pas spammer l'API pendant le drag).
    budgetsEl.querySelectorAll(".budget-range").forEach((input) => {
        input.addEventListener("input", () => {
            const val = parseFloat(input.value);
            if (!isNaN(val)) state.budgets[input.dataset.cat] = val;
            scheduleBudgetRefresh();
        });
    });
    // Champ numérique : on valide à la fin de saisie (évite de perdre le focus en plein typing).
    budgetsEl.querySelectorAll(".budget-number").forEach((input) => {
        input.addEventListener("change", () => {
            const val = parseFloat(input.value);
            if (!isNaN(val)) state.budgets[input.dataset.cat] = val;
            renderBudgets();
        });
    });
}

let budgetTimer = null;
function scheduleBudgetRefresh() {
    clearTimeout(budgetTimer);
    budgetTimer = setTimeout(renderBudgets, 180);
}

// --------------------------------------------------------------------
//  Projection du patrimoine (2 scénarios)
// --------------------------------------------------------------------
async function renderProjection() {
    const data = await api(`/api/projection?years=${state.projYears}`);
    const ctx = document.getElementById("projChart");
    if (projChart) projChart.destroy();
    projChart = new Chart(ctx, {
        type: "line",
        data: {
            labels: data.map((d) => `+${d.annee} an${d.annee > 1 ? "s" : ""}`),
            datasets: [
                {
                    label: "Avec investissement (~5 %/an)", data: data.map((d) => d.invest),
                    borderColor: "#14b8a6", backgroundColor: "rgba(20,184,166,.15)",
                    fill: true, tension: 0.25, pointRadius: 3,
                },
                {
                    label: "Épargne livret (~1 %/an)", data: data.map((d) => d.livret),
                    borderColor: "#22c55e", backgroundColor: "rgba(34,197,94,.10)",
                    fill: true, tension: 0.25, pointRadius: 3,
                },
            ],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: "#94a3b8" }, grid: { color: "#1e293b" } },
                y: { ticks: { color: "#94a3b8", callback: (v) => Math.round(v) + " €" }, grid: { color: "#1e293b" } },
            },
            plugins: {
                legend: { labels: { color: "#e2e8f0", font: { size: 12 } } },
                tooltip: { callbacks: { label: (c) => `${c.dataset.label} : ${euro(c.raw)}` } },
            },
        },
    });
}

// --------------------------------------------------------------------
//  Camembert : répartition des dépenses
// --------------------------------------------------------------------
async function renderPie(year, mo) {
    // En vue annuelle on agrège les 12 mois ; sinon le mois choisi.
    let data;
    if (state.view === "year") {
        const agg = {};
        const colors = {};
        const series = await api(`/api/monthly?year=${year}`); // pour borner les mois
        for (let m = 1; m <= 12; m++) {
            const key = `${year}-${String(m).padStart(2, "0")}`;
            const cats = await api(`/api/by-category?month=${key}`);
            cats.forEach((c) => {
                agg[c.category] = (agg[c.category] || 0) + c.total;
                colors[c.category] = c.color;
            });
        }
        data = Object.keys(agg).map((k) => ({ category: k, total: agg[k], color: colors[k] }));
    } else {
        data = await api(`/api/by-category?month=${year}-${mo}`);
    }

    const ctx = document.getElementById("pieChart");
    if (pieChart) pieChart.destroy();
    pieChart = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: data.map((d) => d.category),
            datasets: [{
                data: data.map((d) => d.total),
                backgroundColor: data.map((d) => d.color),
                borderColor: "#0f172a",
                borderWidth: 2,
            }],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: "right", labels: { color: "#e2e8f0", padding: 14, font: { size: 12 } } },
                tooltip: {
                    callbacks: {
                        label: (c) => `${c.label} : ${euro(c.raw)}`,
                    },
                },
            },
        },
    });
}

// --------------------------------------------------------------------
//  Barres : revenus vs dépenses sur 12 mois
// --------------------------------------------------------------------
async function renderBar(year) {
    const series = await api(`/api/monthly?year=${year}`);
    const ctx = document.getElementById("barChart");
    if (barChart) barChart.destroy();
    barChart = new Chart(ctx, {
        type: "bar",
        data: {
            labels: series.map((s) => s.label),
            datasets: [
                { label: "Revenus", data: series.map((s) => s.revenus), backgroundColor: "#22c55e", borderRadius: 5 },
                { label: "Dépenses", data: series.map((s) => s.depenses), backgroundColor: "#ef4444", borderRadius: 5 },
                { label: "Épargne + invest + crédit", data: series.map((s) => s.effort), backgroundColor: "#6366f1", borderRadius: 5 },
            ],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: "#94a3b8" }, grid: { color: "#1e293b" } },
                y: { ticks: { color: "#94a3b8", callback: (v) => v + " €" }, grid: { color: "#1e293b" } },
            },
            plugins: {
                legend: { labels: { color: "#e2e8f0", font: { size: 12 } } },
                tooltip: { callbacks: { label: (c) => `${c.dataset.label} : ${euro(c.raw)}` } },
            },
        },
    });
}

// --------------------------------------------------------------------
//  Tableau des transactions
// --------------------------------------------------------------------
async function renderTable() {
    const txs = await api(`/api/transactions?month=${state.month}`);
    const tbody = document.querySelector("#txTable tbody");
    tbody.innerHTML = txs.map((t) => {
        const cls = t.amount >= 0 ? "amount-pos" : "amount-neg";
        const sign = t.amount >= 0 ? "+" : "";
        return `
        <tr>
            <td>${formatDate(t.date)}</td>
            <td>${escapeHtml(t.label)}</td>
            <td><span class="pill" style="background:${t.color}">${t.category}</span></td>
            <td class="right ${cls}">${sign}${euro(t.amount)}</td>
        </tr>`;
    }).join("");
}

function formatDate(iso) {
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
}

function escapeHtml(s) {
    return s.replace(/[&<>"]/g, (c) =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}

init().catch((e) => {
    document.body.insertAdjacentHTML("afterbegin",
        `<p style="color:#fca5a5;padding:20px">Erreur de chargement : ${e.message}</p>`);
});
