package com.uade.tpo.demo.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Interés de PLAN ("Vinos", "Senderismo"…). Lo administra el admin desde el panel.
 * Une a las personas con las experiencias: el usuario elige sus intereses y el anfitrión etiqueta su experiencia
 * con los mismos, así el back puede recomendar ("Para vos") y contar cuántos planes hay de cada uno.
 * Pertenece a una categoría, que es el grupo con el que se muestra al elegirlos.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interests")
public class Interest {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Include
    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExperienceCategory category;

    // Lado inverso de Experience.tags y User.interests (la relación la guardan ellos)
    @ManyToMany(mappedBy = "tags")
    private Set<Experience> experiences;

    @ManyToMany(mappedBy = "interests")
    private Set<User> users;
}
