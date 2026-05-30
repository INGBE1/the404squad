package com.the404squad.model;

/** Profil du titulaire du compte (utilise pour l'en-tete du dashboard). */
public record Account(String holder, int age, String city, String iban, double startingBalance) {}
