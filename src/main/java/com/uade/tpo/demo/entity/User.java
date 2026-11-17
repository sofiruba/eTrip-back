package com.uade.tpo.demo.entity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    // Nombre de usuario público. El campo no se llama "username" porque UserDetails.getUsername() tiene que
    // devolver el email (con eso se inicia sesión y se arma el JWT): por eso se mapea a la columna "username".
    @Column(name = "username", nullable = false, unique = true, length = 50)
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

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ¿Confirmó que el email es suyo (con el código que le mandamos al registrarse)? Sin eso no puede iniciar sesión.
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

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
