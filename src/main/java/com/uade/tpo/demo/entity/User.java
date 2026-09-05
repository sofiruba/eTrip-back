package com.uade.tpo.demo.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
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
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User implements UserDetails {
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

    @OneToMany(mappedBy = "user")
    private List<Order> orders;

    @OneToMany(mappedBy = "publisher")
    private List<Experience> experiences;

    @OneToOne(mappedBy = "user")
    private Cart cart;

    @Enumerated(EnumType.STRING)
    private Role role;

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
        return true;
    }
}
