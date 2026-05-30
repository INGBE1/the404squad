package com.the404squad.model;

import java.time.LocalDate;

/**
 * Une operation sur le compte. Le montant est toujours positif ;
 * le signe (entree/sortie) est deduit du Kind de la categorie.
 */
public record Transaction(int id, LocalDate date, String label, double amount, Category category) {

    /** Montant signe : positif pour un revenu, negatif pour tout le reste (depense / epargne / invest / credit). */
    public double signedAmount() {
        return category.kind == Category.Kind.REVENU ? amount : -amount;
    }
}
