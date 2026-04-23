package br.leetjourney.encurtador.util;


public class Base62Util {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = ALPHABET.length();


    public static String enconde(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder hash = new StringBuilder();
        long numeroSendoConvertido = id;


        while (numeroSendoConvertido > 0) {
            int resto = (int) (numeroSendoConvertido % BASE);
            hash.append(ALPHABET.charAt(resto));
            numeroSendoConvertido /= BASE;
        }

        return hash.reverse().toString();
    }
}
