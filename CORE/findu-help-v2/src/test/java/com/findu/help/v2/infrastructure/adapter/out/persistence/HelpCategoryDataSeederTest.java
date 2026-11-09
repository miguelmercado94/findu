package com.findu.help.v2.infrastructure.adapter.out.persistence;

import com.findu.help.v2.application.port.out.persistence.HelpCategoryRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.domain.model.category.HelpSubcategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpCategoryDataSeederTest {

    @Mock
    private HelpCategoryRepository repository;

    private HelpCategory perfil(List<HelpSubcategory> subs) {
        return HelpCategory.builder()
                .id("cat-perfil-documentos").name("Perfil y Documentos").code("PERFIL")
                .description("d").icon("i").order(3).isActive(true)
                .subcategories(subs).build();
    }

    private List<HelpSubcategory> oldSubs() {
        return List.of(
                new HelpSubcategory("a", "A", "x", false, PriorityLevel.MEDIA, 40, true),
                new HelpSubcategory("b", "B", "x", false, PriorityLevel.MEDIA, 40, true),
                new HelpSubcategory("c", "C", "x", false, PriorityLevel.MEDIA, 40, true),
                new HelpSubcategory("sub-otros-perfil", "Otros", "x", false, PriorityLevel.BAJA, 30, true));
    }

    @Test
    void emptyCollectionSeedsFourCategoriesIncludingCorrection() {
        when(repository.findAllActive()).thenReturn(List.of());
        new HelpCategoryDataSeeder(repository).run();

        ArgumentCaptor<HelpCategory> captor = ArgumentCaptor.forClass(HelpCategory.class);
        verify(repository, times(4)).save(captor.capture());
        HelpCategory p = captor.getAllValues().stream()
                .filter(c -> "cat-perfil-documentos".equals(c.getId())).findFirst().orElseThrow();
        assertThat(p.getSubcategories()).extracting(HelpSubcategory::getSubcategoryId)
                .contains("sub-correccion-datos-verificados");
    }

    @Test
    void seededPerfilWithoutCorrectionGetsItOnce() {
        HelpCategory existing = perfil(oldSubs());
        when(repository.findAllActive()).thenReturn(List.of(existing));
        when(repository.findById("cat-perfil-documentos")).thenReturn(Optional.of(existing));
        new HelpCategoryDataSeeder(repository).run();

        ArgumentCaptor<HelpCategory> captor = ArgumentCaptor.forClass(HelpCategory.class);
        verify(repository, times(1)).save(captor.capture());
        HelpCategory saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("cat-perfil-documentos");
        assertThat(saved.getSubcategories()).hasSize(5);
        assertThat(saved.getSubcategories().subList(0, 4)).isEqualTo(oldSubs());
        HelpSubcategory added = saved.getSubcategories().get(4);
        assertThat(added.getSubcategoryId()).isEqualTo("sub-correccion-datos-verificados");
        assertThat(added.getBasePriority()).isEqualTo(PriorityLevel.ALTA);
    }

    @Test
    void perfilAlreadyHavingCorrectionIsNotSaved() {
        List<HelpSubcategory> subs = new java.util.ArrayList<>(oldSubs());
        subs.add(HelpCategoryDataSeeder.correctionSubcategory());
        HelpCategory existing = perfil(subs);
        when(repository.findAllActive()).thenReturn(List.of(existing));
        when(repository.findById("cat-perfil-documentos")).thenReturn(Optional.of(existing));
        new HelpCategoryDataSeeder(repository).run();

        verify(repository, never()).save(any());
    }

    @Test
    void nonEmptyWithoutPerfilCategoryDoesNothing() {
        when(repository.findAllActive()).thenReturn(List.of(perfil(oldSubs())));
        when(repository.findById("cat-perfil-documentos")).thenReturn(Optional.empty());
        new HelpCategoryDataSeeder(repository).run();

        verify(repository, never()).save(any());
    }
}
