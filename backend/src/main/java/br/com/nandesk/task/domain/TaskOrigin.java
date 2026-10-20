package br.com.nandesk.task.domain;

/** Quem originou a tarefa: moderador, job de frescor (RN-12) ou denúncias (RN-16). */
public enum TaskOrigin {
    MANUAL,
    STALE_DATA,
    REPORTS
}
