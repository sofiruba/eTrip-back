package com.uade.tpo.demo.entity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Lob;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User implements UserDetails {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Ojo con el nombre del campo: "username" chocaria con UserDetails.getUsername()
    // (mas abajo, que debe devolver el email para que el login/JWT sigan andando).
    // Lombok no genera el getter/setter de un campo si ya existe un metodo con ese
    // nombre en la clase, asi que un campo llamado "username" quedaria SIN getter
    // real (siempre te devolveria el email por la otra funcion). Por eso el campo
    // java se llama displayUsername pero mapea a la misma columna "username"
    // (unique = true; no nullable=false porque ya habia usuarios de prueba sin
    // username antes de este cambio -- que sea obligatorio se valida en
    // AuthenticationService.register()).
    @Column(name = "username", unique = true)
    private String displayUsername;

    private String password;

    private String firstName;

    private String lastName;

    /** Perfil público (lo ven los demás en /anfitriones/{id}): presentación, ciudad, foto e intereses. */
    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String city;

    /** Foto de perfil (JPEG chico, el front la achica antes de mandarla). */
    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] avatar;

    /** Intereses elegidos (hasta 20). Las experiencias se etiquetan con los mismos: así el back recomienda ("Para vos"). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_interest",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    @OrderBy("name ASC")
    private Set<Interest> interests;

    @OneToMany(mappedBy = "user")
    private List<Order> orders;

    @OneToMany(mappedBy = "publisher")
    private List<Experience> experiences;

    @OneToOne(mappedBy = "user")
    private Cart cart;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ¿Confirmó que el email es suyo (con el código que le mandamos al registrarse)? Sin eso no puede iniciar sesión.
    // En Java arranca en false (cuenta nueva). El "default true" de la base es solo para las cuentas que ya existían
    // cuando se agregó la columna: esas quedan verificadas y pueden seguir entrando.
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean emailVerified = false;

    // Los códigos de verificación y de "Olvidé mi contraseña" viven en su propia tabla: ver VerificationCode.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
