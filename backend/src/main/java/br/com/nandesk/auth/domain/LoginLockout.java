package br.com.nandesk.auth.domain;

/** Espera progressiva após falhas de login (12.1): depois de 5 falhas, 1, 2, 4… minutos, até 60. */
public final class LoginLockout {

    private static final int MAX_MINUTES = 60;

    private LoginLockout() {}

    public static long lockMinutes(int failures, int threshold) {
        if (failures < threshold) {
            return 0;
        }
        int exponent = Math.min(failures - threshold, 6);
        return Math.min(1L << exponent, MAX_MINUTES);
    }
}
