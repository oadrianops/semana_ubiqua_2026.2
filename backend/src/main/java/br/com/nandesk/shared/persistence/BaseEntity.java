package br.com.nandesk.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import org.hibernate.proxy.HibernateProxy;

/**
 * Base das entidades: id interno {@code BIGINT AUTO_INCREMENT} (nunca exposto — 10.1) e equals/hashCode pelo
 * id, conforme a recomendação do Hibernate para entidades com id gerado.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        Class<?> thisClass = effectiveClass(this);
        Class<?> otherClass = effectiveClass(other);
        if (thisClass != otherClass) {
            return false;
        }
        Long otherId = ((BaseEntity) other).getId();
        return id != null && Objects.equals(id, otherId);
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    private static Class<?> effectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
    }
}
